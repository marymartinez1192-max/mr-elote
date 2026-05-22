(async () => {
  const user = await Auth.bootstrap();
  if (user) {
    window.location.href = user.rol === 'ADMIN' ? 'admin.html' : 'catalog.html';
  }
})();

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
    const usuario = await API.login({
      correo:   document.getElementById('login-correo').value,
      password: document.getElementById('login-password').value,
    });
    Auth.setUser(usuario);
    window.location.href = usuario.rol === 'ADMIN' ? 'admin.html' : 'catalog.html';
  } catch (err) {
    errEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
    btn.disabled = false;
    btn.textContent = 'Iniciar Sesión';
  }
}

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
const PASSWORD_HINT  = 'La contraseña debe incluir mínimo 8 caracteres, una mayúscula, una minúscula, un dígito y un carácter especial.';

async function doRegister(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-register');
  const errEl = document.getElementById('register-error');
  errEl.innerHTML = '';

  const password = document.getElementById('reg-password').value;
  if (!PASSWORD_REGEX.test(password)) {
    errEl.innerHTML = `<div class="alert alert-danger">${PASSWORD_HINT}</div>`;
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Creando cuenta...';

  try {
    await API.register({
      nombre:    document.getElementById('reg-nombre').value,
      telefono:  document.getElementById('reg-telefono').value,
      correo:    document.getElementById('reg-correo').value,
      direccion: document.getElementById('reg-direccion').value,
      password,
      redirectTo: `${window.location.origin}/confirm.html`,
    });
    document.getElementById('form-register').reset();
    errEl.innerHTML = `<div class="alert alert-success">
      Cuenta creada. Te enviamos un correo de confirmación —
      haz click en el link para activarla. Luego podrás iniciar sesión.
    </div>`;
  } catch (err) {
    errEl.innerHTML = `<div class="alert alert-danger">${err.message}</div>`;
  } finally {
    btn.disabled = false;
    btn.textContent = 'Crear Cuenta';
  }
}
