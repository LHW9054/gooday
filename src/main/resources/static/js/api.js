// 공통 fetch 래퍼: JSON 송수신 + 서버 에러 메시지({"error": ...}) 표시
const api = {
  async request(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      const msg = (data && data.error) || `요청 실패 (${res.status})`;
      toast(msg);
      throw new Error(msg);
    }
    return data;
  },
  get: (url) => api.request('GET', url),
  post: (url, body) => api.request('POST', url, body),
  put: (url, body) => api.request('PUT', url, body),
  del: (url) => api.request('DELETE', url),
};

function toast(msg) {
  const el = document.getElementById('toast');
  if (!el) return alert(msg);
  el.textContent = msg;
  el.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => (el.hidden = true), 2500);
}

// 사용자 입력을 innerHTML 에 넣을 때 반드시 사용
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}
