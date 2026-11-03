// Utilidades compartidas de GERONEX: comunicación con la API y manejo de la sesión.
const API = 'http://localhost:8080/api'; // En producción se cambia por la URL real (con HTTPS).

// El token vive en sessionStorage: se borra al cerrar la pestaña.
const Sesion = {
  guardar(token, usuario) {
    sessionStorage.setItem('gx_token', token);
    sessionStorage.setItem('gx_usuario', JSON.stringify(usuario));
  },
  token() { return sessionStorage.getItem('gx_token'); },
  usuario() {
    try { return JSON.parse(sessionStorage.getItem('gx_usuario')); } catch { return null; }
  },
  cerrar() {
    sessionStorage.removeItem('gx_token');
    sessionStorage.removeItem('gx_usuario');
  }
};

// Llama a la API. Si falla, lanza { estado, mensaje, errores } listo para mostrarse.
async function api(ruta, opciones = {}) {
  const cabeceras = { 'Content-Type': 'application/json' };
  const token = Sesion.token();
  if (token) cabeceras.Authorization = 'Bearer ' + token;
  let r;
  try {
    r = await fetch(API + ruta, { ...opciones, headers: cabeceras });
  } catch {
    throw { estado: 0, mensaje: 'No se pudo conectar con el servidor. Revisa que el backend esté encendido.', errores: {} };
  }
  const datos = r.status === 204 ? null : await r.json().catch(() => null);
  if (!r.ok) {
    if (r.status === 401 && token) Sesion.cerrar(); // token vencido o cuenta inactiva
    throw { estado: r.status, mensaje: (datos && datos.mensaje) || 'Ocurrió un error inesperado.', errores: (datos && datos.errores) || {} };
  }
  return datos;
}

// Texto de error para mostrar: el mensaje del backend más el detalle por campo, si lo hay.
function textoError(e) {
  const detalle = Object.values(e.errores || {});
  return detalle.length ? detalle.join(' ') : e.mensaje;
}

function mostrarMensaje(id, texto, tipo) {
  const el = document.getElementById(id);
  el.textContent = texto;
  el.className = 'msg ' + (tipo === 'ok' ? 'msg-ok' : 'msg-error');
}

// Barra de navegación: con sesión muestra nombre, "Mi cuenta" y "Cerrar sesión".
function pintarNav() {
  const li = document.getElementById('nav-sesion');
  const u = Sesion.usuario();
  if (!li || !u || !Sesion.token()) return;
  const hola = document.createElement('span');
  hola.className = 'nav-usuario';
  hola.textContent = 'Hola, ' + (u.nombre || u.nombreUsuario);
  const cuenta = document.createElement('a');
  cuenta.href = 'cuenta.html';
  cuenta.textContent = 'Mi cuenta';
  const salir = document.createElement('a');
  salir.href = '#';
  salir.className = 'btn-login-nav';
  salir.textContent = 'Cerrar sesión';
  salir.onclick = (ev) => { ev.preventDefault(); Sesion.cerrar(); location.href = 'index.html'; };
  li.replaceChildren(hola, cuenta, salir);
}
