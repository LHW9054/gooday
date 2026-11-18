// 자산(돈 계획표) 화면
const state = { tab: 'month', ym: null, year: null };
const fmt = (n, cur = 'JPY') => (cur === 'KRW' ? '₩' : '¥') + Number(n).toLocaleString('ko-KR');
const pctTxt = (p) => (p === null || p === undefined ? '-' : `${p > 0 ? '+' : ''}${p}%`);
const pctCls = (p) => (p === null || p === undefined ? 'muted' : p > 0 ? 'up' : p < 0 ? 'down' : 'muted');
const shift = (ym, d) => { const [y, m] = ym.split('-').map(Number); const t = new Date(y, m - 1 + d, 1);
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}`; };
const save = () => { try { localStorage.setItem('gooday.money', JSON.stringify(state)); } catch (_) {} };

// ───── 월별 ─────
async function loadMonth() {
  const focusId = document.activeElement?.dataset?.item;   // 저장 후 다시 그려도 커서 유지
  const m = await api.get(`/api/finance/month?ym=${state.ym}`);
  const [y, mo] = m.ym.split('-');
  document.getElementById('period-label').textContent = `${y}년 ${Number(mo)}월`;
  document.getElementById('month-empty').hidden = !m.empty;
  document.getElementById('warnings').innerHTML = m.warnings.map((w) => `<div class="warn">⚠️ ${esc(w)}</div>`).join('');

  const leftover = m.expense.flatMap((g) => g.lines).find((l) => l.auto && l.itemId);
  document.getElementById('summary').innerHTML = [
    ['수입', fmt(m.incomeTotal)],
    ['지출(저축 포함)', fmt(m.expenseTotal)],
    ['이달 엔화저축', fmt(m.savingJpyMonth)],
    ['용돈', leftover ? fmt(leftover.amount) : '-'],
    ['원화저축 총액', fmt(m.savingKrwTotal, 'KRW')],
  ].map(([k, v]) => `<div class="stat"><span>${k}</span><b>${v}</b></div>`).join('');

  document.getElementById('income').innerHTML = table(['구분', '항목', '금액', '비율'], m.income, (g, l) =>
    [esc(l.name), money(l, g.currency), l.percent === null ? '-' : `${l.percent}%`]);
  document.getElementById('expense').innerHTML = table(['구분', '항목', '금액', '전달대비'], m.expense, (g, l, i) =>
    [esc(l.name), money(l, g.currency), i === 0 ? `<span class="${pctCls(g.changePct)}">${pctTxt(g.changePct)}</span>` : ''], true);
  const sav = (gs) => table(['구분', '항목', '이번달', '누적', '성장률'], gs, (g, l, i) =>
    [esc(l.name), money(l, g.currency), fmt(l.cumulative, g.currency),
     i === 0 ? `<span class="${pctCls(g.changePct)}">${pctTxt(g.changePct)}</span>` : '']);
  document.getElementById('saving-jpy').innerHTML = sav(m.savingJpy);
  document.getElementById('saving-krw').innerHTML = sav(m.savingKrw);
  document.getElementById('fx').value = m.krwPerJpy ?? '';

  if (focusId) document.querySelector(`input[data-item="${focusId}"]`)?.focus();
}

// 그룹별 표. 그룹 첫 줄에만 그룹명, 그룹이 2줄 이상이면 합계 줄 추가
function table(heads, groups, cells, showGroupTotal) {
  if (!groups.length) return '<p class="empty">항목이 없어요.</p>';
  let rows = '';
  for (const g of groups) {
    g.lines.forEach((l, i) => {
      const del = l.itemId && !l.auto ? `<button class="x" data-del="${l.itemId}" title="항목 숨기기">✕</button>` : '';
      rows += `<tr class="${i === 0 ? 'g-first' : ''}"><td class="g">${i === 0 ? esc(g.name) : ''}</td>` +
        cells(g, l, i).map((c, k) => `<td class="${k ? 'num' : ''}">${c}${k === 0 ? del : ''}</td>`).join('') + '</tr>';
    });
    if (showGroupTotal && g.lines.length > 1)
      rows += `<tr class="sub"><td></td><td>소계</td><td class="num">${fmt(g.total, g.currency)}</td><td></td></tr>`;
  }
  return `<table class="ledger"><thead><tr>${heads.map((h, i) => `<th class="${i > 1 ? 'num' : ''}">${h}</th>`).join('')}</tr></thead><tbody>${rows}</tbody></table>`;
}

function money(l, cur) {
  if (l.auto || !l.itemId) return `<span class="auto" title="자동 계산">${fmt(l.amount, cur)}</span>`;
  return `<input class="amt" data-item="${l.itemId}" inputmode="numeric" value="${Number(l.amount).toLocaleString('ko-KR')}">`;
}

// ───── 종합표 ─────
async function loadYear() {
  const y = await api.get(`/api/finance/year?year=${state.year}`);
  document.getElementById('period-label').textContent = `${y.year}년`;
  const bar = (p) => `<div class="bar ${p >= 100 ? 'full' : ''}"><i style="width:${Math.min(p, 100)}%"></i></div>`;

  document.getElementById('assets').innerHTML = y.assets.length ? y.assets.map((a) => `
    <div class="rate"><div class="top"><span>${esc(a.name)}</span><b>${a.percent}%</b></div>${bar(a.percent)}
    <div class="muted small">${fmt(a.current, a.currency)} / ${fmt(a.target, a.currency)} · 남은 ${fmt(a.remaining, a.currency)}</div></div>`).join('')
    : '<p class="empty">자산 목표가 없어요.</p>';

  document.getElementById('quarters').innerHTML = y.quarters.length ? y.quarters.map((q) => `
    <div class="rate"><div class="top"><span>${q.quarter}분기 (${q.quarter * 3 - 2}~${q.quarter * 3}월)</span><b>${q.percent}%</b></div>${bar(q.percent)}
    <div class="muted small">${fmt(q.saved)} / ${fmt(q.target)} · 전분기 대비 <span class="${pctCls(q.growthPct)}">${pctTxt(q.growthPct)}</span></div></div>`).join('')
    : '<p class="empty">분기 목표가 없어요.</p>';

  const krwSum = y.krwSavings.reduce((s, k) => s + k.cumulative, 0);
  document.getElementById('krw-status').innerHTML = y.krwSavings.map((k) =>
    `<div class="kv"><span>${esc(k.group)} · ${esc(k.name)}</span><b>${fmt(k.cumulative, 'KRW')}</b></div>`).join('') +
    `<div class="kv total"><span>합계 ${y.latestYm ? `(${y.latestYm} 기준)` : ''}</span><b>${fmt(krwSum, 'KRW')}</b></div>`;

  document.getElementById('debts').innerHTML = y.debts.length ? y.debts.map((d) => `
    <div class="rate"><div class="top"><span>${esc(d.name)}</span><b>${d.remaining === 0 ? '완납 🎉' : d.percent + '%'}</b></div>${bar(d.percent)}
    <div class="muted small">${fmt(d.paid, d.currency)} / ${fmt(d.total, d.currency)} · ${d.payments.map((p) => `${p.ym.slice(2)} ${fmt(p.amount, d.currency)}`).join(', ')}</div></div>`).join('')
    : '<p class="empty">빚이 없어요 👍</p>';

  const grid = (rows, totals) => {
    const h = `<tr><th>구분</th>${[...Array(12)].map((_, i) => `<th class="num">${i + 1}월</th>`).join('')}<th class="num">총합</th></tr>`;
    const b = rows.map((r) => `<tr><td class="g">${esc(r.group)}</td>${r.months.map((v) => `<td class="num">${v ? Number(v).toLocaleString('ko-KR') : ''}</td>`).join('')}<td class="num"><b>${Number(r.total).toLocaleString('ko-KR')}</b></td></tr>`).join('');
    const t = `<tr class="sub"><td>합계</td>${totals.map((v) => `<td class="num">${v ? Number(v).toLocaleString('ko-KR') : ''}</td>`).join('')}<td class="num"><b>${totals.reduce((a, c) => a + c, 0).toLocaleString('ko-KR')}</b></td></tr>`;
    return `<table class="ledger wide"><thead>${h}</thead><tbody>${b}${t}</tbody></table>`;
  };
  document.getElementById('year-income').innerHTML = grid(y.income, y.incomeTotals);
  document.getElementById('year-expense').innerHTML = grid(y.expense, y.expenseTotals);
}

function render() {
  document.getElementById('view-month').hidden = state.tab !== 'month';
  document.getElementById('view-year').hidden = state.tab !== 'year';
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === state.tab));
  save();
  return state.tab === 'month' ? loadMonth() : loadYear();
}

// ───── 이벤트 ─────
document.addEventListener('change', async (e) => {
  if (e.target.matches('input.amt')) {
    const raw = e.target.value.replace(/[^\d-]/g, '');
    if (raw === '' || isNaN(Number(raw))) return toast('숫자만 입력해 주세요.');
    await api.put('/api/finance/entries', { itemId: Number(e.target.dataset.item), ym: state.ym, amount: Number(raw) });
    loadMonth();
  } else if (e.target.id === 'fx') {
    const v = Number(e.target.value);
    if (!v) return;
    await api.put('/api/finance/fx', { ym: state.ym, krwPerJpy: v });
    toast('환율 저장');
  }
});
document.addEventListener('keydown', (e) => { if (e.key === 'Enter' && e.target.matches('input.amt')) e.target.blur(); });

document.addEventListener('click', async (e) => {
  const t = e.target.closest('[data-tab],[data-move],[data-add],[data-del],#btn-copy');
  if (!t) return;
  if (t.dataset.tab) { state.tab = t.dataset.tab; state.year = Number(state.ym.slice(0, 4)); return render(); }
  if (t.dataset.move) {
    if (state.tab === 'month') state.ym = shift(state.ym, Number(t.dataset.move));
    else state.year += Number(t.dataset.move);
    return render();
  }
  if (t.id === 'btn-copy') {
    const r = await api.post(`/api/finance/month/copy?ym=${state.ym}`);
    toast(r.copied ? `${r.copied}개 항목을 복사했어요` : '지난달에도 데이터가 없어요');
    return loadMonth();
  }
  if (t.dataset.del) {
    if (!confirm('이 항목을 숨길까요? (지난 기록은 남아요)')) return;
    await api.del(`/api/finance/items/${t.dataset.del}`);
    return loadMonth();
  }
  if (t.dataset.add) {
    const group = prompt('구분 (예: 생활비☀️, 현금💴). 기존 구분과 같게 쓰면 그 안에 들어가요');
    if (!group) return;
    const name = prompt('항목 이름 (예: 통신비)');
    if (!name) return;
    let opening = 0;
    if (t.dataset.add === 'SAVING') opening = Number((prompt('이미 모인 돈 (없으면 0)', '0') || '0').replace(/[^\d]/g, ''));
    await api.post('/api/finance/items', { kind: t.dataset.add, group, name, currency: t.dataset.cur || 'JPY', opening });
    loadMonth();
  }
});

// ───── 시작 ─────
(async () => {
  try { Object.assign(state, JSON.parse(localStorage.getItem('gooday.money') || '{}')); } catch (_) {}
  if (!state.ym) {
    const now = new Date();
    state.ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    // 이번 달이 비어 있고 내년 계획이 있으면 내년 1월부터 보여주기
    const next = await api.get(`/api/finance/year?year=${now.getFullYear() + 1}`);
    const cur = await api.get(`/api/finance/year?year=${now.getFullYear()}`);
    if (!cur.latestYm && next.latestYm) state.ym = `${now.getFullYear() + 1}-01`;
  }
  state.year = state.year || Number(state.ym.slice(0, 4));
  render();
})();
