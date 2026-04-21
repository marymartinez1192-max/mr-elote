if (!requireAuth()) {}

async function loadOrders() {
  renderNav('orders');
  const content = document.getElementById('orders-content');

  try {
    const { content: orders } = await API.getOrders(0, 50);

    if (!orders?.length) {
      content.innerHTML = `
        <div class="empty-state">
          <div class="icon">📦</div>
          <p>Aún no tienes pedidos</p>
          <a href="catalog.html" class="btn btn-primary" style="margin-top:1rem">Hacer mi primer pedido</a>
        </div>`;
      return;
    }

    const rows = orders.map(o => `
      <tr>
        <td><strong>#${o.id}</strong></td>
        <td>${formatDate(o.fechaCreacion)}</td>
        <td>${statusBadge(o.estado)}</td>
        <td>${formatPrice(o.total)}</td>
        <td>
          <div style="display:flex;gap:0.5rem">
            <button class="btn btn-outline btn-sm" onclick="viewOrder(${o.id})">Ver detalle</button>
            ${o.estado === 'PENDIENTE'
              ? `<button class="btn btn-danger btn-sm" onclick="cancelOrder(${o.id})">Cancelar</button>`
              : ''}
          </div>
        </td>
      </tr>
    `).join('');

    content.innerHTML = `
      <div class="card" style="padding:0">
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>#</th><th>Fecha</th><th>Estado</th><th>Total</th><th>Acciones</th></tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </div>`;
  } catch (err) {
    content.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  }
}

async function viewOrder(id) {
  try {
    const o = await API.getOrder(id);
    document.getElementById('modal-title').textContent = `Pedido #${o.id} – ${o.estado}`;

    const itemRows = o.items.map(i => `
      <tr>
        <td>${i.producto.nombre}</td>
        <td>${i.cantidad}</td>
        <td>${formatPrice(i.precioUnitario)}</td>
        <td>${formatPrice(i.subtotal)}</td>
      </tr>
    `).join('');

    document.getElementById('modal-body').innerHTML = `
      <div class="alert alert-info" style="margin-bottom:1rem">
        Estado: ${statusBadge(o.estado)} &nbsp;·&nbsp; Fecha: ${formatDate(o.fechaCreacion)}
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Producto</th><th>Cant.</th><th>P. Unit.</th><th>Subtotal</th></tr></thead>
          <tbody>${itemRows}</tbody>
        </table>
      </div>
      <hr>
      <div class="summary-row"><span>Envío</span><span>${formatPrice(o.tarifaEnvio)}</span></div>
      <div class="summary-row total"><span>Total</span><span>${formatPrice(o.total)}</span></div>
    `;
    document.getElementById('order-modal').classList.add('open');
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function cancelOrder(id) {
  if (!confirm(`¿Cancelar el pedido #${id}?`)) return;
  try {
    await API.cancelOrder(id);
    showToast('Pedido cancelado');
    loadOrders();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function closeModal() {
  document.getElementById('order-modal').classList.remove('open');
}

document.getElementById('order-modal').addEventListener('click', e => {
  if (e.target === e.currentTarget) closeModal();
});

loadOrders();
