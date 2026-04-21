const BASE_URL = 'http://localhost:8080/api/v1';

// ── Auth state ────────────────────────────────────────────────
const Auth = {
  getToken: () => localStorage.getItem('token'),
  getUser:  () => JSON.parse(localStorage.getItem('user') || 'null'),
  setSession(token, user) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
  isLoggedIn: () => !!localStorage.getItem('token'),
  isAdmin:    () => JSON.parse(localStorage.getItem('user') || 'null')?.rol === 'ADMIN',
};

// ── Base request ──────────────────────────────────────────────
async function request(method, path, body = null) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const options = { method, headers };
  if (body !== null) options.body = JSON.stringify(body);

  const res = await fetch(`${BASE_URL}${path}`, options);

  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw { status: res.status, message: data.message || 'Error al procesar la solicitud' };
  return data;
}

// ── API calls ─────────────────────────────────────────────────
const API = {
  // Auth
  register: d => request('POST', '/auth/register', d),
  login:    d => request('POST', '/auth/login', d),

  // Catálogo (público)
  getCategories:    () => request('GET', '/categories'),
  getProducts(params = {}) {
    const q = new URLSearchParams(Object.entries(params).filter(([, v]) => v != null && v !== '')).toString();
    return request('GET', `/products${q ? '?' + q : ''}`);
  },
  getBusinessStatus: () => request('GET', '/business/status'),

  // Carrito
  getCart:          ()           => request('GET',    '/cart'),
  addCartItem:      d            => request('POST',   '/cart/items', d),
  updateCartItem:   (prodId, d)  => request('PUT',    `/cart/items/${prodId}`, d),
  removeCartItem:   itemId       => request('DELETE', `/cart/items/${itemId}`),

  // Pedidos
  confirmOrder: ()         => request('POST',  '/orders'),
  getOrders:    (p=0,s=20) => request('GET',   `/orders?page=${p}&size=${s}`),
  getOrder:     id         => request('GET',   `/orders/${id}`),
  cancelOrder:  id         => request('PATCH', `/orders/${id}/cancel`),

  // Admin – Categorías
  adminCreateCategory: d      => request('POST',   '/admin/categories', d),
  adminUpdateCategory: (id,d) => request('PUT',    `/admin/categories/${id}`, d),
  adminDeleteCategory: id     => request('DELETE', `/admin/categories/${id}`),

  // Admin – Productos
  adminCreateProduct:             d      => request('POST',  '/admin/products', d),
  adminUpdateProduct:             (id,d) => request('PUT',   `/admin/products/${id}`, d),
  adminUpdateProductAvailability: (id,d) => request('PATCH', `/admin/products/${id}/availability`, d),

  // Admin – Pedidos
  adminGetOrders(status, p=0, s=20) {
    const q = status ? `status=${status}&page=${p}&size=${s}` : `page=${p}&size=${s}`;
    return request('GET', `/admin/orders?${q}`);
  },
  adminUpdateOrderStatus: (id,d) => request('PATCH', `/admin/orders/${id}/status`, d),

  // Admin – Negocio
  adminGetBusinessConfig:    ()  => request('GET', '/admin/business-config'),
  adminUpdateBusinessConfig: d   => request('PUT', '/admin/business-config', d),
};

// ── UI helpers ────────────────────────────────────────────────
function showToast(msg, type = 'success') {
  let t = document.getElementById('toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'toast';
    document.body.appendChild(t);
  }
  t.className = `alert alert-${type}`;
  t.textContent = msg;
  t.style.display = 'block';
  clearTimeout(t._timer);
  t._timer = setTimeout(() => { t.style.display = 'none'; }, 3500);
}

function statusBadge(estado) {
  return `<span class="badge badge-${estado.toLowerCase().replace('_','-')}">${estado}</span>`;
}

function formatPrice(p) {
  return '$' + Number(p).toLocaleString('es-CO');
}

function formatDate(d) {
  return new Date(d).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
}

// ── Nav ───────────────────────────────────────────────────────
function renderNav(activePage) {
  const user = Auth.getUser();
  const isAdmin = Auth.isAdmin();

  const clientLinks = isAdmin ? '' : `
    <a href="catalog.html" class="${activePage==='catalog'?'active':''}">Catálogo</a>
    <a href="cart.html"    class="${activePage==='cart'?'active':''}">🛒 Carrito <span class="nav-badge" id="cart-count">0</span></a>
    <a href="orders.html"  class="${activePage==='orders'?'active':''}">Mis Pedidos</a>
  `;
  const adminLink = isAdmin
    ? `<a href="admin.html" class="${activePage==='admin'?'active':''}">⚙ Admin</a>`
    : '';

  document.querySelector('nav').innerHTML = `
    <a class="nav-brand" href="${isAdmin ? 'admin.html' : 'catalog.html'}">🌽 MrElote</a>
    ${clientLinks}
    ${adminLink}
    <span style="color:rgba(255,255,255,0.5);font-size:0.8rem">${user?.nombre || ''}</span>
    <button class="btn-nav-logout" onclick="logout()">Salir</button>
  `;

  if (!isAdmin) updateCartBadge();
}

async function updateCartBadge() {
  if (!Auth.isLoggedIn()) return;
  try {
    const cart = await API.getCart();
    const el = document.getElementById('cart-count');
    if (el) el.textContent = cart.items?.length || 0;
  } catch (_) {}
}

function logout() {
  Auth.clear();
  window.location.href = 'index.html';
}

function requireAuth() {
  if (!Auth.isLoggedIn()) { window.location.href = 'index.html'; return false; }
  return true;
}

function requireAdmin() {
  if (!Auth.isLoggedIn() || !Auth.isAdmin()) { window.location.href = 'index.html'; return false; }
  return true;
}
