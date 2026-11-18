const CATEGORY = { STUDY: '공부', EXERCISE: '운동', FINANCE: '자산', LIFE: '생활' };
const DDAY_ICON = { EXAM: '📝', BJJ: '🥋', PAYDAY: '💰', CARD: '💳', ETC: '📌' };
const PERIOD = { DAILY: '오늘', WEEKLY: '이번 주', MONTHLY: '이번 달', YEARLY: '올해' };
const WEEK = ['일', '월', '화', '수', '목', '금', '토'];
let ddayCache = [];

async function load() {
  const d = await api.get('/api/dashboard');
  renderHeader(d.today);
  renderDdays(d.ddays);
  renderRoutines(d.routines);
  renderRates(d.routineRates);
  renderGoals(d.goals);
}

function renderHeader(today) {
  const t = new Date(today + 'T00:00:00');
  const h = new Date().getHours();
  const hi = h < 6 ? '늦은 밤이에요 🌙' : h < 12 ? '좋은 아침이에요 ☀️' : h < 18 ? '좋은 오후예요 🌤' : '오늘도 수고했어요 🌆';
  document.getElementById('today-title').textContent = hi;
  document.getElementById('today-sub').textContent =
    `${t.getFullYear()}년 ${t.getMonth() + 1}월 ${t.getDate()}일 (${WEEK[t.getDay()]})`;
}

function renderDdays(list) {
  ddayCache = list;
  const el = document.getElementById('ddays');
  if (!list.length) return (el.innerHTML = '<p class="empty">등록된 D-day 가 없어요.</p>');
  el.innerHTML = list.map((x) => `
    <div class="dday clickable ${x.pinned ? 'pinned' : ''} ${x.daysLeft < 0 ? 'past' : ''}" data-edit-dday="${x.id}" title="눌러서 수정">
      <button class="del" data-del-dday="${x.id}" title="삭제">✕</button>
      <div class="label">${esc(x.label)}</div>
      <div class="title">${DDAY_ICON[x.category] || ''} ${esc(x.title)}</div>
      <div class="date">${esc(x.date)}${x.repeatType === 'MONTHLY' ? ' · 매월' : x.repeatType === 'YEARLY' ? ' · 매년' : ''}</div>
    </div>`).join('');
}

function renderRoutines(list) {
  const el = document.getElementById('routines');
  const done = list.filter((r) => r.checked).length;
  document.getElementById('routine-count').textContent = list.length ? `${done}/${list.length}` : '';
  if (!list.length) return (el.innerHTML = '<li class="empty">오늘 할 루틴이 없어요.</li>');
  el.innerHTML = list.map((r) => `
    <li class="${r.checked ? 'done' : ''}" data-routine="${r.id}">
      <span class="box">${r.checked ? '✓' : ''}</span>
      <span class="t">${esc(r.title)}</span>
      <span class="tag">${CATEGORY[r.category] || esc(r.category)}</span>
    </li>`).join('');
}

function renderRates(rates) {
  const rows = [['오늘', rates.daily], ['이번 주', rates.weekly], ['이번 달', rates.monthly], ['올해 누적', rates.yearly]];
  document.getElementById('rates').innerHTML = rows.map(([name, r]) => `
    <div class="rate">
      <div class="top"><span>${name}</span><b>${r.percent}% <span class="muted">(${r.done}/${r.total})</span></b></div>
      <div class="bar ${r.percent >= 100 ? 'full' : ''}"><i style="width:${Math.min(r.percent, 100)}%"></i></div>
    </div>`).join('');
}

function renderGoals(list) {
  const el = document.getElementById('goals');
  if (!list.length) return (el.innerHTML = '<p class="empty">목표를 추가해 보세요.</p>');
  el.innerHTML = list.map((g) => `
    <div class="goal">
      <div class="head"><b>${esc(g.title)}</b><span class="pct">${g.percent}%</span></div>
      <div class="meta">${PERIOD[g.periodType]} · ${g.current} / ${g.target} ${esc(g.unit)}</div>
      <div class="bar ${g.percent >= 100 ? 'full' : ''}"><i style="width:${Math.min(g.percent, 100)}%"></i></div>
      <div class="ops">
        <button class="btn small" data-progress="${g.id}" data-value="1">+1 ${esc(g.unit)}</button>
        <button class="btn small ghost" data-progress="${g.id}" data-ask="1">직접 입력</button>
        <button class="btn small ghost" data-del-goal="${g.id}" style="margin-left:auto">삭제</button>
      </div>
    </div>`).join('');
}

// ── 이벤트 (위임) ──
document.addEventListener('click', async (e) => {
  const t = e.target.closest('[data-routine],[data-del-dday],[data-edit-dday],[data-new-dday],[data-progress],[data-del-goal],[data-open]');
  if (!t) return;

  if (t.dataset.open) return document.getElementById(t.dataset.open).showModal();
  if (t.hasAttribute('data-new-dday')) return openDdayDialog(null);
  if (t.dataset.editDday) return openDdayDialog(ddayCache.find((x) => x.id === Number(t.dataset.editDday)));

  if (t.dataset.routine) {
    await api.post(`/api/routines/${t.dataset.routine}/toggle`);
  } else if (t.dataset.delDday) {
    if (!confirm('이 D-day 를 삭제할까요?')) return;
    await api.del(`/api/ddays/${t.dataset.delDday}`);
  } else if (t.dataset.progress) {
    let value = Number(t.dataset.value);
    if (t.dataset.ask) {
      const v = prompt('얼마나 했나요? (정정은 음수)', '1');
      if (v === null || v.trim() === '' || isNaN(Number(v))) return;
      value = Number(v);
    }
    await api.post(`/api/goals/${t.dataset.progress}/progress`, { value });
  } else if (t.dataset.delGoal) {
    if (!confirm('이 목표를 삭제할까요? (기록은 보관돼요)')) return;
    await api.del(`/api/goals/${t.dataset.delGoal}`);
  }
  load();
});

// ── 추가 폼 ──
document.querySelectorAll('form[data-submit]').forEach((form) => {
  form.addEventListener('submit', async (e) => {
    if (e.submitter && e.submitter.value === 'cancel') return; // 취소는 그냥 닫기
    e.preventDefault();
    const f = new FormData(form);
    const kind = form.dataset.submit;
    try {
      if (kind === 'dday') {
        const body = {
          title: f.get('title'), targetDate: f.get('targetDate'), category: f.get('category'),
          repeatType: f.get('repeatType'), pinned: f.get('pinned') === 'on', memo: f.get('memo') || null,
        };
        const id = f.get('id');
        await (id ? api.put(`/api/ddays/${id}`, body) : api.post('/api/ddays', body));
        toast(id ? '수정했어요' : '추가했어요');
      } else if (kind === 'routine') {
        const mask = f.getAll('day').reduce((a, b) => a + Number(b), 0);
        if (!mask) return toast('요일을 하나 이상 골라 주세요.');
        await api.post('/api/routines', { title: f.get('title'), category: f.get('category'), daysMask: mask });
      } else if (kind === 'goal') {
        await api.post('/api/goals', {
          title: f.get('title'), periodType: f.get('periodType'),
          targetValue: Number(f.get('targetValue')), unit: f.get('unit'),
        });
      }
      form.reset();
      form.closest('dialog').close();
      load();
    } catch (_) { /* toast 로 이미 표시됨 */ }
  });
});

// D-day 추가/수정 공용 다이얼로그 (x 가 있으면 수정 모드)
function openDdayDialog(x) {
  const dlg = document.getElementById('dlg-dday');
  const form = dlg.querySelector('form');
  form.reset();
  document.getElementById('dday-dlg-title').textContent = x ? 'D-day 수정' : 'D-day 추가';
  form.elements.id.value = x ? x.id : '';
  if (x) {
    form.elements.title.value = x.title;
    // 반복 D-day 는 원래 기준일이 아니라 다음 발생일이 오므로 그대로 써도 같은 반복이 됨
    form.elements.targetDate.value = x.date;
    form.elements.category.value = x.category;
    form.elements.repeatType.value = x.repeatType;
    form.elements.memo.value = x.memo || '';
    form.elements.pinned.checked = x.pinned;
  }
  dlg.showModal();
}

load();
