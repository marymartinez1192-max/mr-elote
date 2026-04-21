if (!requireAuth()) {}

async function loadCart() {
  renderNav('cart');
  const content = document.getElementById('cart-content');

  try {
    const cart = await API.getCart();
    renderCart(cart);
  } catch (err) {
    if (err.status === 404) {
      content.innerHTML = `
        <div class="empty-state">
          <div class="icon">🛒</div>
          <p>Tu carrito está vacío</p>
          <a href="catalog.html" class="btn btn-primary" style="margin-top:1rem">Ver catálogo</a>
        </div>`;
    } else {
      content.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    }
  }
}

function renderCart(cart) {
  const content = document.getElementById('cart-content');

  if (!cart.items?.length) {
    content.innerHTML = `
      <div class="empty-state">
        <div class="icon">🛒</div>
        <p>Tu carrito está vacío</p>
        <a href="catalog.html" class="btn btn-primary" style="margin-top:1rem">Ver catálogo</a>
      </div>`;
    return;
  }

  const rows = cart.items.map(item => `
    <tr>
      <td>
        <strong>${item.producto.nombre}</strong>
        <div style="font-size:0.8rem;color:var(--text-light)">${item.producto.descripcion || ''}</div>
      </td>
      <td>${formatPrice(item.precioUnitario)}</td>
      <td>
        <div class="qty-control">
          <button class="qty-btn" onclick="changeQty(${item.producto.id}, ${item.cantidad - 1})">−</button>
          <span class="qty-value">${item.cantidad}</span>
          <button class="qty-btn" onclick="changeQty(${item.producto.id}, ${item.cantidad + 1})">+</button>
        </div>
      </td>
      <td><strong>${formatPrice(item.subtotal)}</strong></td>
      <td>
        <button class="btn btn-danger btn-sm" onclick="removeItem(${item.id})">🗑</button>
      </td>
    </tr>
  `).join('');

  const subtotal = cart.items.reduce((s, i) => s + Number(i.subtotal), 0);

  content.innerHTML = `
    <div class="cart-grid">
      <div class="card" style="padding:0">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Producto</th><th>Precio Unit.</th><th>Cantidad</th><th>Subtotal</th><th></th>
              </tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      </div>
      <div class="cart-summary">
        <h3 style="margin-bottom:1rem">Resumen del pedido</h3>
        <div class="summary-row"><span>Subtotal</span><span>${formatPrice(subtotal)}</span></div>
        <div class="summary-row"><span>Tarifa de envío</span><span>${formatPrice(cart.tarifaEnvio)}</span></div>
        <div class="summary-row total"><span>Total</span><span>${formatPrice(cart.total)}</span></div>
        <button class="btn btn-primary btn-full" style="margin-top:1.25rem" onclick="confirmOrder()">
          Confirmar Pedido
        </button>
        <a href="catalog.html" class="btn btn-outline btn-full" style="margin-top:0.5rem">Seguir comprando</a>
      </div>
    </div>`;
}

async function changeQty(productoId, newQty) {
  if (newQty < 1) return;
  try {
    const cart = await API.updateCartItem(productoId, { cantidad: newQty });
    renderCart(cart);
    updateCartBadge();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function removeItem(itemId) {
  try {
    const cart = await API.removeCartItem(itemId);
    renderCart(cart);
    updateCartBadge();
    showToast('Producto eliminado del carrito');
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function confirmOrder() {
  if (!confirm('¿Confirmar el pedido?')) return;
  try {
    const order = await API.confirmOrder();
    showToast('¡Pedido creado exitosamente!');
    setTimeout(() => window.location.href = 'orders.html', 1200);
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

loadCart();
