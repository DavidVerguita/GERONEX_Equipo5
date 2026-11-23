// Utilidades compartidas de GERONEX: comunicación con la API, sesión, navegación por rol y formato.
// Desde IntelliJ (puerto 63342) la página y la API están en puertos distintos: se usa localhost:8080.
// En cualquier otro caso (servidor_web.mjs en 8090 o nginx en Azure) la API sale del mismo origen: /api.
const API = window.GERONEX_API || (location.port === '63342' || location.protocol === 'file:' ? 'http://localhost:8080/api' : '/api');

// El token vive en sessionStorage: es distinto en cada pestaña y se borra al cerrarla.
// Por eso en una sola computadora se puede tener una pestaña de cliente, otra de agente, etc.
const Sesion = {
  guardar(token, usuario) {
    sessionStorage.setItem('gx_token', token);
    sessionStorage.setItem('gx_usuario', JSON.stringify(usuario));
  },
  token() { return sessionStorage.getItem('gx_token'); },
  usuario() {
    try { return JSON.parse(sessionStorage.getItem('gx_usuario')); } catch { return null; }
  },
  rol() { const u = this.usuario(); return u ? u.rol : null; },
  cerrar() {
    sessionStorage.removeItem('gx_token');
    sessionStorage.removeItem('gx_usuario');
  }
};

// Llama a la API. Si falla, lanza { estado, mensaje, errores } listo para mostrarse.
// opciones.publica = true: no manda el token (catálogo), así un token vencido no estorba.
async function api(ruta, opciones = {}) {
  const { publica, ...fetchOpciones } = opciones;
  const cabeceras = { 'Content-Type': 'application/json' };
  const token = publica ? null : Sesion.token();
  if (token) cabeceras.Authorization = 'Bearer ' + token;
  let r;
  try {
    r = await fetch(API + ruta, { ...fetchOpciones, headers: cabeceras });
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

// Atajo para POST/PUT/PATCH con cuerpo JSON.
function enviar(ruta, metodo, cuerpo) {
  return api(ruta, { method: metodo, body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo) });
}

// Texto de error para mostrar: el detalle por campo si lo hay; si no, el mensaje general.
function textoError(e) {
  const detalle = Object.values(e.errores || {});
  return detalle.length ? detalle.join(' ') : e.mensaje;
}

function mostrarMensaje(id, texto, tipo) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = texto;
  el.className = 'msg ' + (tipo === 'ok' ? 'msg-ok' : 'msg-error');
}

function ocultarMensaje(id) {
  const el = document.getElementById(id);
  if (el) { el.textContent = ''; el.className = 'msg'; }
}

// Si la API dice 401 (sesión vencida), manda al login. Si no, muestra el error en el elemento indicado.
function manejarError(e, idMensaje) {
  if (e.estado === 401) { location.href = 'login.html'; return; }
  mostrarMensaje(idMensaje, textoError(e), 'error');
}

// ---------------------------------------------------------------- roles y navegación

const NOMBRE_ROL = {
  ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', AGENTE: 'Agente',
  RESPONSABLE_RESERVACIONES: 'Resp. de reservaciones', RESPONSABLE_PROVEEDORES: 'Resp. de proveedores',
  AUDITOR: 'Auditor', CLIENTE: 'Cliente'
};

// Páginas de trabajo que ve cada rol en la barra de navegación.
const PAGINAS_POR_ROL = {
  CLIENTE: [['cliente.html', 'Mis viajes']],
  AGENTE: [['agente.html', 'Solicitudes']],
  RESPONSABLE_RESERVACIONES: [['reservaciones.html', 'Reservaciones']],
  SUPERVISOR: [['agente.html', 'Solicitudes'], ['reservaciones.html', 'Reservaciones']],
  ADMINISTRADOR: [['admin.html', 'Administración'], ['reservaciones.html', 'Reservaciones']],
  AUDITOR: [['agente.html', 'Solicitudes'], ['reservaciones.html', 'Reservaciones']]
};

// A dónde va cada rol al iniciar sesión.
function inicioPorRol(rol) {
  const paginas = PAGINAS_POR_ROL[rol];
  return paginas ? paginas[0][0] : 'index.html';
}

// Protege una página: sin sesión manda al login; con un rol no permitido manda a su inicio.
function requerirRol(...roles) {
  if (!Sesion.token() || !Sesion.usuario()) { location.href = 'login.html'; return false; }
  if (roles.length && !roles.includes(Sesion.rol())) { location.href = inicioPorRol(Sesion.rol()); return false; }
  return true;
}

// Barra de navegación: con sesión muestra las páginas del rol, "Mi cuenta" y "Cerrar sesión".
function pintarNav() {
  prepararNav();
  const li = document.getElementById('nav-sesion');
  const u = Sesion.usuario();
  if (!li || !u || !Sesion.token()) return;
  const elementos = [];
  const actual = location.pathname.split('/').pop() || 'index.html';
  for (const [href, texto] of (PAGINAS_POR_ROL[u.rol] || [])) {
    const a = document.createElement('a');
    a.href = href;
    a.textContent = texto;
    if (href === actual) a.className = 'activo';
    elementos.push(a);
  }
  const cuenta = document.createElement('a');
  cuenta.href = 'cuenta.html';
  cuenta.textContent = 'Mi cuenta';
  if (actual === 'cuenta.html') cuenta.className = 'activo';
  const hola = document.createElement('span');
  hola.className = 'nav-usuario';
  hola.textContent = (u.nombre || u.nombreUsuario) + ' · ' + (NOMBRE_ROL[u.rol] || u.rol);
  const salir = document.createElement('a');
  salir.href = '#';
  salir.className = 'btn-login-nav';
  salir.textContent = 'Cerrar sesión';
  salir.onclick = (ev) => { ev.preventDefault(); Sesion.cerrar(); location.href = 'index.html'; };
  li.replaceChildren(...elementos, cuenta, hola, salir);
}

// Logo con ícono y botón de menú para celular (se agrega una sola vez por página).
function prepararNav() {
  const nav = document.querySelector('nav');
  if (!nav || nav.dataset.listo) return;
  nav.dataset.listo = '1';
  const logo = nav.querySelector('.nav-logo');
  if (logo && !logo.querySelector('img')) logo.insertAdjacentHTML('afterbegin', '<img src="img/logo.svg" alt="" width="32" height="32">');
  const links = nav.querySelector('.nav-links');
  if (!links) return;
  const boton = document.createElement('button');
  boton.className = 'nav-menu';
  boton.type = 'button';
  boton.setAttribute('aria-label', 'Abrir menú');
  boton.setAttribute('aria-expanded', 'false');
  boton.innerHTML = '<span></span><span></span><span></span>';
  boton.onclick = () => {
    const abierto = nav.classList.toggle('abierto');
    boton.setAttribute('aria-expanded', String(abierto));
  };
  nav.insertBefore(boton, links);
}

// El botón que envió el formulario (algunos navegadores viejos no tienen ev.submitter).
function botonDe(ev) {
  return ev.submitter || ev.target.querySelector('[type=submit]') || { disabled: false };
}

// ---------------------------------------------------------------- avisos y confirmaciones

// Aviso flotante que desaparece solo. tipo: 'ok' | 'error' | 'info'
function toast(texto, tipo = 'ok') {
  let zona = document.getElementById('toasts');
  if (!zona) {
    zona = document.createElement('div');
    zona.id = 'toasts';
    zona.setAttribute('role', 'status');
    zona.setAttribute('aria-live', 'polite');
    document.body.appendChild(zona);
  }
  const t = document.createElement('div');
  t.className = 'toast toast-' + tipo;
  t.textContent = texto;
  zona.appendChild(t);
  setTimeout(() => { t.classList.add('saliendo'); setTimeout(() => t.remove(), 300); }, 4200);
}

// Ventana de confirmación con el estilo de GERONEX (en lugar de confirm()).
// Devuelve una promesa: true si acepta, false si cancela.
// opciones.campo = 'Etiqueta' agrega una caja de texto y la promesa devuelve su valor (o null si cancela).
function preguntar(titulo, texto, opciones = {}) {
  const { aceptar = 'Aceptar', cancelar = 'Cancelar', peligro = false, campo = null, obligatorio = false } = opciones;
  return new Promise(resolver => {
    const d = document.createElement('dialog');
    d.className = 'dialogo';
    d.innerHTML = `<form method="dialog">
        <h3>${esc(titulo)}</h3>
        <p>${esc(texto)}</p>
        ${campo ? `<div class="form-group"><label for="dlg-campo">${esc(campo)}</label><textarea id="dlg-campo" maxlength="255" ${obligatorio ? 'required' : ''}></textarea></div>` : ''}
        <div class="acciones">
          <button type="button" class="btn btn-claro" value="no">${esc(cancelar)}</button>
          <button type="submit" class="btn ${peligro ? 'btn-rojo' : 'btn-primario'}" value="si">${esc(aceptar)}</button>
        </div></form>`;
    document.body.appendChild(d);
    const cerrar = valor => { d.close(); d.remove(); resolver(valor); };
    d.querySelector('[value=no]').onclick = () => cerrar(campo ? null : false);
    d.addEventListener('cancel', ev => { ev.preventDefault(); cerrar(campo ? null : false); });
    d.querySelector('form').addEventListener('submit', ev => {
      ev.preventDefault();
      if (!campo) return cerrar(true);
      const valor = d.querySelector('#dlg-campo').value.trim();
      if (obligatorio && !valor) { d.querySelector('#dlg-campo').focus(); return; }
      cerrar(valor);
    });
    d.showModal();
    (d.querySelector('#dlg-campo') || d.querySelector('[value=si]')).focus();
  });
}

// ---------------------------------------------------------------- formato

// Escapa texto antes de meterlo en HTML (evita que un dato rompa la página o inyecte código).
function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const FORMATO_DINERO = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
function dinero(n) { return n == null ? '—' : FORMATO_DINERO.format(Number(n)); }

// "2026-12-20" -> "20 dic 2026" (sin desfase de zona horaria)
function fecha(iso) {
  if (!iso) return '—';
  const [a, m, d] = String(iso).slice(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d).toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' });
}

// "2026-11-16T22:19:52" -> "16 nov 2026, 22:19"
function fechaHora(iso) {
  if (!iso) return '—';
  const f = new Date(iso);
  return isNaN(f) ? '—' : f.toLocaleString('es-MX', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// Fecha de hoy + n días en formato AAAA-MM-DD (para los min de los <input type="date">)
function diaISO(n = 0) {
  const f = new Date();
  f.setDate(f.getDate() + n);
  return f.getFullYear() + '-' + String(f.getMonth() + 1).padStart(2, '0') + '-' + String(f.getDate()).padStart(2, '0');
}

const TEXTO_ESTATUS = {
  NUEVA: 'Esperando cotización', COTIZADA: 'Cotización enviada', ACEPTADA: 'Aceptada',
  ENVIADA: 'Por responder', RECHAZADA: 'Rechazada',
  POR_VALIDAR: 'Por validar', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada',
  ACTIVO: 'Activo', INACTIVO: 'Inactivo', BLOQUEADO: 'Bloqueado'
};

// Etiqueta de color para un estatus.
function badge(estatus) {
  return `<span class="badge badge-${esc(estatus)}">${esc(TEXTO_ESTATUS[estatus] || estatus)}</span>`;
}

const ICONO_CATEGORIA = { HOTEL: '🏨', VUELO: '✈️', TRANSPORTE: '🚌', PAQUETE: '🧭' };
const NOMBRE_CATEGORIA = { HOTEL: 'Hoteles', VUELO: 'Vuelos', TRANSPORTE: 'Transporte', PAQUETE: 'Paquetes y tours' };

// "Cancún" -> "cancun": minúsculas y sin acentos (para buscar sin importar cómo se escriba).
function normalizar(texto) {
  return String(texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

// Ilustración local de cada destino (carpeta img/destinos). Si no hay una, se usa la genérica.
const IMAGEN_DESTINO = { 'cancun': 'cancun', 'los cabos': 'los-cabos', 'cdmx': 'cdmx', 'ciudad de mexico': 'cdmx',
  'oaxaca': 'oaxaca', 'chihuahua': 'chihuahua' };
function imagenDestino(destino) {
  const clave = normalizar(destino);
  const encontrada = Object.keys(IMAGEN_DESTINO).find(k => clave === k || clave.includes(k));
  return 'img/destinos/' + (encontrada ? IMAGEN_DESTINO[encontrada] : 'generico') + '.svg';
}

// Noches entre dos fechas AAAA-MM-DD (mínimo 1).
function nochesEntre(inicio, fin) {
  const [a1, m1, d1] = String(inicio).split('-').map(Number);
  const [a2, m2, d2] = String(fin).split('-').map(Number);
  return Math.max(1, Math.round((new Date(a2, m2 - 1, d2) - new Date(a1, m1 - 1, d1)) / 86400000));
}

// Días que faltan para una fecha AAAA-MM-DD (negativo si ya pasó).
function diasPara(iso) {
  const [a, m, d] = String(iso).split('-').map(Number);
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
  return Math.round((new Date(a, m - 1, d) - hoy) / 86400000);
}

// Tarjetas de resumen (número grande + etiqueta). datos: [{ valor, texto, tono }]
function htmlResumen(datos) {
  return `<div class="kpis">${datos.map(d => `<div class="kpi kpi-${d.tono || 'azul'}"><b>${esc(d.valor)}</b><span>${esc(d.texto)}</span></div>`).join('')}</div>`;
}

// Repite una función cada cierto tiempo (consulta la API, no es tiempo real).
// Se pausa si la pestaña está oculta y se actualiza al volver.
function refrescarCada(segundos, fn) {
  setInterval(() => { if (!document.hidden) fn(); }, segundos * 1000);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) fn(); });
}

// Hora de la última actualización, para que se vea que la página consulta sola.
function marcarActualizado(id) {
  const el = document.getElementById(id);
  if (el) el.textContent = 'Actualizado a las ' + new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

// ---------------------------------------------------------------- pie de página

// Cambia el pie sencillo de cada página por uno con enlaces útiles y datos de contacto.
function pintarPie() {
  const pie = document.querySelector('footer');
  if (!pie || pie.dataset.listo) return;
  pie.dataset.listo = '1';
  pie.className = 'pie';
  const rol = Sesion.token() ? Sesion.rol() : null;
  const trabajo = rol ? (PAGINAS_POR_ROL[rol] || []).map(([h, t]) => `<a href="${h}">${esc(t)}</a>`).join('') + '<a href="cuenta.html">Mi cuenta</a>'
    : '<a href="login.html">Iniciar sesión</a><a href="login.html#registro">Crear una cuenta</a>';
  pie.innerHTML = `<div class="pie-grid">
      <div><h4>GERONEX</h4><p>Agencia de viajes asistida: nos cuentas tu viaje y un agente lo arma por ti con hoteles, vuelos, transporte y tours.</p></div>
      <div><h4>Explora</h4><a href="index.html#destinos">Destinos</a><a href="index.html#catalogo">Catálogo de servicios</a><a href="index.html#como-funciona">¿Cómo funciona?</a></div>
      <div><h4>Tu cuenta</h4>${trabajo}</div>
      <div><h4>Políticas</h4><p>Cancelación sin cargo mientras la reservación esté por validar. Ya confirmada, cargo del 5%. Hasta 3 días antes del viaje.</p></div>
    </div>
    <div class="pie-final">&copy; ${new Date().getFullYear()} GERONEX · Proyecto escolar · Todos los derechos reservados.</div>`;
}
pintarPie();
