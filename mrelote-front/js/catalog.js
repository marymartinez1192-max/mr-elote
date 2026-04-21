let allProducts = [];
let activeCategoryId = null;

async function init() {
  renderNav('catalog');

  // Business status banner
  try {
    const { abierto } = await API.getBusinessStatus();
    const banner = document.getElementById('biz-banner');
    banner.className = `biz-banner ${abierto ? 'open' : 'closed'}`;
    banner.textContent = abierto ? '✅ Negocio abierto – ¡Haz tu pedido!' : '🔴 Negocio cerrado – No se aceptan pedidos en este momento';
  } catch (_) {}

  // Categories
  try {
    const cats = await API.getCategories();
    const container = document.getElementById('category-filters');
    container.innerHTML = `<button class="filter-btn active" onclick="selectCategory(null, this)">Todos</button>` +
      cats.map(c => `<button class="filter-btn" onclick="selectCategory(${c.id}, this)">${c.nombre}</button>`).join('');
  } catch (_) {}

  // Products
  try {
    allProducts = await API.getProducts();
    renderProducts(allProducts);
  } catch (_) {
    document.getElementById('products-grid').innerHTML =
      `<div class="empty-state"><div class="icon">⚠️</div>Error al cargar productos</div>`;
  }
}

function selectCategory(id, btn) {
  activeCategoryId = id;
  document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  filterProducts();
}

function filterProducts() {
  const q = document.getElementById('search').value.toLowerCase();
  const filtered = allProducts.filter(p =>
    (!activeCategoryId || p.categoria?.id === activeCategoryId) &&
    (!q || p.nombre.toLowerCase().includes(q) || (p.descripcion || '').toLowerCase().includes(q))
  );
  renderProducts(filtered);
}

function renderProducts(products) {
  const grid = document.getElementById('products-grid');
  if (!products.length) {
    grid.innerHTML = `<div class="empty-state"><div class="icon">🌽</div>No hay productos disponibles</div>`;
    return;
  }
  grid.innerHTML = products.map(p => `
    <div class="product-card">
      <div class="product-img">
        ${p.imagenUrl
          ? `<img src="${p.imagenUrl}" alt="${p.nombre}" onerror="this.parentElement.textContent='🌽'">`
          : '🌽'}
      </div>
      <div class="product-body">
        <div class="product-name">${p.nombre}</div>
        <div class="product-desc">${p.descripcion || ''}</div>
        <div class="product-price">${formatPrice(p.precio)}</div>
        ${p.disponible ? '' : '<span class="badge badge-unavailable">No disponible</span>'}
      </div>
      <div class="product-footer">
        ${p.disponible
          ? Auth.isLoggedIn() && !Auth.isAdmin()
            ? `<button class="btn btn-primary btn-full btn-sm" onclick="addToCart(${p.id}, '${p.nombre}')">+ Agregar al carrito</button>`
            : !Auth.isLoggedIn()
              ? `<button class="btn btn-outline btn-full btn-sm" onclick="window.location.href='index.html'">Inicia sesión para pedir</button>`
              : ''
          : `<button class="btn btn-full btn-sm" disabled>No disponible</button>`
        }
      </div>
    </div>
  `).join('');
}

async function addToCart(productoId, nombre) {
  try {
    await API.addCartItem({ productoId, cantidad: 1 });
    showToast(`"${nombre}" agregado al carrito`);
    updateCartBadge();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

init();
