(() => {
  const msgEl = document.getElementById('confirm-message');
  const btnEl = document.getElementById('back-to-login');

  const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''));
  const query    = new URLSearchParams(window.location.search);

  const error            = fragment.get('error')            || query.get('error');
  const errorDescription = fragment.get('error_description') || query.get('error_description');
  const accessToken      = fragment.get('access_token');
  const code             = query.get('code');
  const type             = fragment.get('type') || query.get('type');

  if (error) {
    msgEl.innerHTML = `
      <div class="alert alert-danger">
        <strong>No se pudo confirmar la cuenta.</strong><br>
        ${decodeURIComponent(errorDescription || error)}
        <br><br>
        Si el link expiró, vuelve a registrarte o pide reenvío del correo.
      </div>`;
  } else if (
    (accessToken && (type === 'signup' || type === 'magiclink' || type === 'recovery')) ||
    code
  ) {
    msgEl.innerHTML = `
      <div class="alert alert-success">
        ¡Cuenta confirmada correctamente! Ya puedes iniciar sesión con tu correo y contraseña.
      </div>`;
  } else {
    msgEl.innerHTML = `
      <div class="alert alert-warning">
        No se detectó información de confirmación en el link.
        Si llegaste aquí por error, ve a iniciar sesión.
      </div>`;
  }

  btnEl.style.display = '';

  if (window.location.hash) {
    history.replaceState(null, '', window.location.pathname);
  }
})();
