if (!requireAdmin()) {}

const TRANSITIONS = {
  PENDIENTE: ['ACEPTADO', 'CANCELADO'],
  ACEPTADO:  ['EN_CAMINO', 'CANCELADO'],
  EN_CAMINO: ['ENTREGADO'],
};

let categoriesCache = [];

// ── Sections ──────────────────────────────────────────────────
function showSection(name) {
  document.querySelectorAll('.admin-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.sidebar-item').forEach(s => s.classList.remove('active'));
  document.getElementById(`section-${name}`).classList.add('active');
  event.currentTarget.classList.add('active');

  if (name === 'orders')     loadAdminOrders();
  if (name === 'products')   loadAdminProducts();
  if (name === 'categories') loadAdminCategories();
  if (name === 'config')     loadConfig();
}

// ── Pedidos ───────────────────────────────────────────────────
async function loadAdminOrders() {
  const status = document.getElementById('order-filter').value;
  const el = document.getElementById('admin-orders');
  el.innerHTML = '<div class="empty-state"><div class="icon">⏳</div>Cargando...</div>';

  try {
    const { content: orders } = await API.adminGetOrders(status || null, 0, 100);

    if (!orders?.length) {
      el.innerHTML = '<div class="empty-state"><div class="icon">📦</div>No hay pedidos</div>';
      return;
    }

    el.innerHTML = `
      <div class="card" style="padding:0">
        <div class="table-wrap">
          <table>
            <thead><tr><th>#</th><th>Fecha</th><th>Estado</th><th>Total</th><th>Ítems</th><th></th></tr></thead>
            <tbody>
              ${orders.map(o => `
                <tr>
                  <td><strong>#${o.id}</strong></td>
                  <td>${formatDate(o.fechaCreacion)}</td>
                  <td>${statusBadge(o.estado)}</td>
                  <td>${formatPrice(o.total)}</td>
                  <td>${o.items?.length || 0} ítem(s)</td>
                  <td>
                    ${TRANSITIONS[o.estado]?.length
                      ? `<button class="btn btn-outline btn-sm" onclick="openStatusModal(${o.id},'${o.estado}')">Cambiar estado</button>`
                      : '–'}
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`;
  } catch (err) {
    el.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

function openStatusModal(orderId, currentStatus) {
  const opts = TRANSITIONS[currentStatus] || [];
  document.getElementById('status-order-id').value = orderId;
  document.getElementById('status-modal-title').textContent = `Pedido #${orderId} – Cambiar estado`;
  document.getElementById('status-select').innerHTML = opts.map(s =>
    `<option value="${s}">${s}</option>`).join('');
  document.getElementById('status-modal').classList.add('open');
}

async function updateOrderStatus() {
  const id     = document.getElementById('status-order-id').value;
  const estado = document.getElementById('status-select').value;
  try {
    await API.adminUpdateOrderStatus(id, { estado });
    showToast(`Pedido #${id} actualizado a ${estado}`);
    closeStatusModal();
    loadAdminOrders();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function closeStatusModal() {
  document.getElementById('status-modal').classList.remove('open');
}

// ── Categorías ────────────────────────────────────────────────
async function loadAdminCategories() {
  const el = document.getElementById('admin-categories');
  try {
    const cats = await API.getCategories();
    categoriesCache = cats;

    if (!cats.length) {
      el.innerHTML = '<div class="empty-state"><div class="icon">🏷</div>No hay categorías</div>';
      return;
    }

    el.innerHTML = `
      <div class="card" style="padding:0">
        <div class="table-wrap">
          <table>
            <thead><tr><th>#</th><th>Nombre</th><th></th></tr></thead>
            <tbody>
              ${cats.map(c => `
                <tr>
                  <td>${c.id}</td>
                  <td>${c.nombre}</td>
                  <td>
                    <div style="display:flex;gap:0.5rem">
                      <button class="btn btn-outline btn-sm" onclick="openCategoryModal(${c.id},'${c.nombre}')">Editar</button>
                      <button class="btn btn-danger btn-sm" onclick="deleteCategory(${c.id})">Eliminar</button>
                    </div>
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`;
  } catch (err) {
    el.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

function openCategoryModal(id = null, nombre = '') {
  document.getElementById('cat-id').value     = id || '';
  document.getElementById('cat-nombre').value = nombre;
  document.getElementById('cat-modal-title').textContent = id ? 'Editar Categoría' : 'Nueva Categoría';
  document.getElementById('cat-modal-alert').innerHTML = '';
  document.getElementById('cat-modal').classList.add('open');
}

async function saveCategory() {
  const id     = document.getElementById('cat-id').value;
  const nombre = document.getElementById('cat-nombre').value.trim();
  const alertEl = document.getElementById('cat-modal-alert');

  if (!nombre) { alertEl.innerHTML = '<div class="alert alert-danger">El nombre es obligatorio</div>'; return; }

  try {
    if (id) { await API.adminUpdateCategory(id, { nombre }); showToast('Categoría actualizada'); }
    else     { await API.adminCreateCategory({ nombre });      showToast('Categoría creada'); }
    closeCatModal();
    loadAdminCategories();
  } catch (err) {
    alertEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

async function deleteCategory(id) {
  if (!confirm('¿Eliminar esta categoría?')) return;
  try {
    await API.adminDeleteCategory(id);
    showToast('Categoría eliminada');
    loadAdminCategories();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function closeCatModal() { document.getElementById('cat-modal').classList.remove('open'); }

// ── Productos ─────────────────────────────────────────────────
async function loadAdminProducts() {
  const el = document.getElementById('admin-products');
  try {
    const products = await API.getProducts();

    if (!products.length) {
      el.innerHTML = '<div class="empty-state"><div class="icon">🌽</div>No hay productos</div>';
      return;
    }

    el.innerHTML = `
      <div class="card" style="padding:0">
        <div class="table-wrap">
          <table>
            <thead><tr><th>#</th><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Disponible</th><th></th></tr></thead>
            <tbody>
              ${products.map(p => `
                <tr>
                  <td>${p.id}</td>
                  <td>${p.nombre}</td>
                  <td>${p.categoria?.nombre || '–'}</td>
                  <td>${formatPrice(p.precio)}</td>
                  <td>
                    <span class="badge badge-${p.disponible ? 'available' : 'unavailable'}">
                      ${p.disponible ? 'Sí' : 'No'}
                    </span>
                  </td>
                  <td>
                    <div style="display:flex;gap:0.5rem">
                      <button class="btn btn-outline btn-sm" onclick="openProductModal(${JSON.stringify(p).replace(/"/g,'&quot;')})">Editar</button>
                      <button class="btn btn-sm ${p.disponible ? 'btn-danger' : 'btn-success'}"
                        onclick="toggleAvailability(${p.id}, ${!p.disponible})">
                        ${p.disponible ? 'Deshabilitar' : 'Habilitar'}
                      </button>
                    </div>
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`;
  } catch (err) {
    el.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

async function openProductModal(product = null) {
  // Load categories for the select
  if (!categoriesCache.length) categoriesCache = await API.getCategories().catch(() => []);
  const catOpts = categoriesCache.map(c =>
    `<option value="${c.id}" ${product && product.categoria?.id === c.id ? 'selected' : ''}>${c.nombre}</option>`
  ).join('');
  document.getElementById('prod-categoria').innerHTML = catOpts;

  document.getElementById('prod-id').value          = product?.id || '';
  document.getElementById('prod-nombre').value       = product?.nombre || '';
  document.getElementById('prod-descripcion').value  = product?.descripcion || '';
  document.getElementById('prod-precio').value       = product?.precio || '';
  document.getElementById('prod-imagen').value       = product?.imagenUrl || '';
  document.getElementById('prod-disponible').checked = product ? product.disponible : true;
  document.getElementById('prod-modal-title').textContent = product ? 'Editar Producto' : 'Nuevo Producto';
  document.getElementById('prod-modal-alert').innerHTML = '';
  document.getElementById('prod-modal').classList.add('open');
}

async function saveProduct() {
  const id = document.getElementById('prod-id').value;
  const alertEl = document.getElementById('prod-modal-alert');

  const data = {
    nombre:      document.getElementById('prod-nombre').value.trim(),
    descripcion: document.getElementById('prod-descripcion').value.trim(),
    precio:      Number(document.getElementById('prod-precio').value),
    imagenUrl:   document.getElementById('prod-imagen').value.trim() || null,
    disponible:  document.getElementById('prod-disponible').checked,
    categoriaId: Number(document.getElementById('prod-categoria').value),
  };

  if (!data.nombre || !data.precio || !data.categoriaId) {
    alertEl.innerHTML = '<div class="alert alert-danger">Completa todos los campos obligatorios</div>';
    return;
  }

  try {
    if (id) { await API.adminUpdateProduct(id, data); showToast('Producto actualizado'); }
    else     { await API.adminCreateProduct(data);     showToast('Producto creado'); }
    closeProdModal();
    loadAdminProducts();
  } catch (err) {
    alertEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

async function toggleAvailability(id, disponible) {
  try {
    await API.adminUpdateProductAvailability(id, { disponible });
    showToast(`Producto ${disponible ? 'habilitado' : 'deshabilitado'}`);
    loadAdminProducts();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function closeProdModal() { document.getElementById('prod-modal').classList.remove('open'); }

// ── Configuración ─────────────────────────────────────────────
async function loadConfig() {
  try {
    const cfg = await API.adminGetBusinessConfig();
    document.getElementById('cfg-apertura').value  = cfg.horarioApertura?.substring(0, 5) || '08:00';
    document.getElementById('cfg-cierre').value    = cfg.horarioCierre?.substring(0, 5)   || '20:00';
    document.getElementById('cfg-cerrado').checked = cfg.cerradoManual;
  } catch (err) {
    document.getElementById('config-alert').innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

async function saveConfig(e) {
  e.preventDefault();
  const alertEl = document.getElementById('config-alert');
  try {
    await API.adminUpdateBusinessConfig({
      horarioApertura: document.getElementById('cfg-apertura').value + ':00',
      horarioCierre:   document.getElementById('cfg-cierre').value   + ':00',
      cerradoManual:   document.getElementById('cfg-cerrado').checked,
    });
    alertEl.innerHTML = '<div class="alert alert-success">Configuración guardada</div>';
    setTimeout(() => alertEl.innerHTML = '', 3000);
  } catch (err) {
    alertEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

// ── Close modals on backdrop click ───────────────────────────
['cat-modal','prod-modal','status-modal'].forEach(id => {
  document.getElementById(id).addEventListener('click', e => {
    if (e.target === e.currentTarget) e.currentTarget.classList.remove('open');
  });
});

// ── Init ──────────────────────────────────────────────────────
renderNav('admin');
loadAdminOrders();
