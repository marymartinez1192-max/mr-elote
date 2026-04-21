if (Auth.isLoggedIn()) {
  window.location.href = Auth.isAdmin() ? 'admin.html' : 'catalog.html';
}

function switchTab(tab) {
  document.querySelectorAll('.auth-tab').forEach((t, i) => {
    t.classList.toggle('active', (i === 0) === (tab === 'login'));
  });
  document.getElementById('form-login').style.display     = tab === 'login'    ? '' : 'none';
  document.getElementById('form-register').style.display  = tab === 'register' ? '' : 'none';
}

async function doLogin(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-login');
  const errEl = document.getElementById('login-error');
  errEl.innerHTML = '';
  btn.disabled = true;
  btn.textContent = 'Ingresando...';

  try {
    const data = await API.login({
      correo:   document.getElementById('login-correo').value,
      password: document.getElementById('login-password').value,
    });
    Auth.setSession(data.token, data.usuario);
    window.location.href = data.usuario.rol === 'ADMIN' ? 'admin.html' : 'catalog.html';
  } catch (err) {
    errEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    btn.disabled = false;
    btn.textContent = 'Iniciar Sesión';
  }
}

async function doRegister(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-register');
  const errEl = document.getElementById('register-error');
  errEl.innerHTML = '';
  btn.disabled = true;
  btn.textContent = 'Creando cuenta...';

  try {
    await API.register({
      nombre:    document.getElementById('reg-nombre').value,
      telefono:  document.getElementById('reg-telefono').value,
      correo:    document.getElementById('reg-correo').value,
      direccion: document.getElementById('reg-direccion').value,
      password:  document.getElementById('reg-password').value,
    });
    errEl.innerHTML = `<div class="alert alert-success">¡Cuenta creada! Ahora inicia sesión.</div>`;
    setTimeout(() => switchTab('login'), 1500);
  } catch (err) {
    errEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  } finally {
    btn.disabled = false;
    btn.textContent = 'Crear Cuenta';
  }
}
