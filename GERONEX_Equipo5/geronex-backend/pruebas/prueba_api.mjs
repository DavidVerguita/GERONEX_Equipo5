// =====================================================================
// GERONEX - Prueba automática de TODA la API (flujo completo + seguridad)
//
// Uso (con el backend encendido):
//     node prueba_api.mjs
// desde la carpeta geronex-backend/pruebas. Necesita Node 18 o más nuevo.
//
// - La cuenta "admin" se lee de login_admin.json (la de la demo).
// - Crea (solo la primera vez) cuentas de prueba con prefijo "qa_" y las reutiliza.
// - Deja el cupo como estaba: lo que confirma lo cancela y lo que no, lo rechaza.
// - Al final dice cuántas pruebas pasaron y cuáles fallaron.
// =====================================================================
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const API = process.env.GERONEX_API || 'http://localhost:8080/api';
const AQUI = dirname(fileURLToPath(import.meta.url));
const ADMIN = JSON.parse(readFileSync(join(AQUI, 'login_admin.json'), 'utf8'));
const CLAVE_QA = 'QaPrueba2026';   // contraseña de las cuentas qa_ (solo para pruebas)

let pasaron = 0;
const fallas = [];

function ok(condicion, nombre, detalle = '') {
    if (condicion) { pasaron++; console.log('  ✔ ' + nombre); }
    else { fallas.push(nombre); console.log('  ✖ ' + nombre + (detalle ? '  → ' + detalle : '')); }
}

async function llamar(metodo, ruta, { token, cuerpo, crudo } = {}) {
    const cabeceras = { 'Content-Type': 'application/json' };
    if (token) cabeceras.Authorization = 'Bearer ' + token;
    const r = await fetch(API + ruta, {
        method: metodo, headers: cabeceras,
        body: crudo !== undefined ? crudo : cuerpo === undefined ? undefined : JSON.stringify(cuerpo)
    });
    const texto = await r.text();
    let datos = null;
    try { datos = texto ? JSON.parse(texto) : null; } catch { datos = texto; }
    return { estado: r.status, datos };
}

// Fecha local (no UTC) de hoy + n días, en formato AAAA-MM-DD
const dia = n => {
    const f = new Date(); f.setDate(f.getDate() + n);
    return f.getFullYear() + '-' + String(f.getMonth() + 1).padStart(2, '0') + '-' + String(f.getDate()).padStart(2, '0');
};
const resumen = r => `${r.estado} ${JSON.stringify(r.datos)?.slice(0, 160)}`;
const formatoError = d => d && typeof d.estado === 'number' && typeof d.mensaje === 'string' && d.errores && d.fecha;

async function login(nombreUsuario, contrasena) {
    const r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario, contrasena } });
    if (r.estado !== 200) throw new Error(`No se pudo iniciar sesión con ${nombreUsuario}: ${resumen(r)}`);
    return r.datos.token;
}

/** Crea la cuenta qa_ si no existe (por el admin o por registro público) y devuelve su token. */
async function cuentaQa(tokenAdmin, usuario, rol, nombre, extra = {}) {
    const cuerpo = { nombreUsuario: usuario, correo: usuario + '@prueba.geronex.mx', contrasena: CLAVE_QA, rol, nombre, ...extra };
    const r = rol === 'CLIENTE'
        ? await llamar('POST', '/auth/registro', { cuerpo })
        : await llamar('POST', '/admin/usuarios', { token: tokenAdmin, cuerpo });
    if (r.estado !== 201 && r.estado !== 409) throw new Error(`No se pudo crear ${usuario}: ${resumen(r)}`);
    return login(usuario, CLAVE_QA);
}

async function cupo(idServicio) {
    return (await llamar('GET', '/servicios/' + idServicio)).datos.cupoDisponible;
}

async function principal() {
    console.log('GERONEX · prueba de la API en ' + API + '\n');

    // ------------------------------------------------------------ 1. público
    console.log('1. Catálogo público');
    let r = await llamar('GET', '/salud');
    ok(r.estado === 200 && r.datos.estado === 'OK', 'GET /salud responde OK', resumen(r));
    r = await llamar('GET', '/categorias');
    ok(r.estado === 200 && r.datos.length >= 4, 'GET /categorias trae las 4 categorías', resumen(r));
    r = await llamar('GET', '/servicios');
    const catalogo = r.datos;
    ok(r.estado === 200 && catalogo.length >= 14, 'GET /servicios trae el catálogo (14 o más)', resumen(r));
    ok(catalogo.every(s => s.idServicio && s.nombre && s.destino && s.precio != null && s.cupoDisponible != null),
        'Cada servicio trae id, nombre, destino, precio y cupo');
    r = await llamar('GET', '/servicios?categoria=HOTEL&texto=cancun');
    ok(r.estado === 200 && r.datos.length > 0 && r.datos.every(s => s.categoria === 'HOTEL' && s.destino === 'Cancún'),
        'Filtro categoría + texto sin acento ("cancun" encuentra "Cancún")', resumen(r));
    r = await llamar('GET', '/servicios?texto=CHICHÉN');
    ok(r.estado === 200 && r.datos.length === 1, 'Búsqueda ignora mayúsculas y acentos', resumen(r));
    r = await llamar('GET', '/servicios?texto=zzzz');
    ok(r.estado === 200 && r.datos.length === 0, 'Búsqueda sin resultados devuelve lista vacía');
    r = await llamar('GET', '/servicios/99999');
    ok(r.estado === 404 && formatoError(r.datos), 'Servicio inexistente → 404 con formato de error', resumen(r));
    r = await llamar('GET', '/servicios/abc');
    ok(r.estado === 400, 'Id no numérico → 400', resumen(r));

    // ------------------------------------------------------------ 2. registro y login
    console.log('\n2. Registro e inicio de sesión');
    const sufijo = Date.now().toString(36).slice(-6);
    const nuevo = 'qr' + sufijo;
    r = await llamar('POST', '/auth/registro', { cuerpo: { nombreUsuario: nuevo, correo: nuevo + '@Prueba.Geronex.MX', contrasena: CLAVE_QA, rol: 'ADMINISTRADOR' } });
    ok(r.estado === 201 && r.datos.rol === 'CLIENTE', 'Registro siempre crea CLIENTE aunque mande otro rol', resumen(r));
    ok(r.datos?.correo === nuevo + '@prueba.geronex.mx', 'El correo se guarda en minúsculas');
    ok(r.datos?.nombre === nuevo, 'El nombre toma el nombre de usuario');
    ok(r.datos && !('contrasena' in r.datos), 'La respuesta nunca trae la contraseña');
    r = await llamar('POST', '/auth/registro', { cuerpo: { nombreUsuario: nuevo, correo: 'otro' + sufijo + '@prueba.geronex.mx', contrasena: CLAVE_QA } });
    ok(r.estado === 409, 'Usuario repetido → 409', resumen(r));
    r = await llamar('POST', '/auth/registro', { cuerpo: { nombreUsuario: 'qs' + sufijo, correo: nuevo + '@prueba.geronex.mx', contrasena: CLAVE_QA } });
    ok(r.estado === 409, 'Correo repetido → 409', resumen(r));
    r = await llamar('POST', '/auth/registro', { cuerpo: { nombreUsuario: 'usuario_demasiado_largo', correo: 'malo', contrasena: '123' } });
    ok(r.estado === 400 && r.datos.errores.nombreUsuario && r.datos.errores.correo && r.datos.errores.contrasena,
        'Datos inválidos → 400 con un mensaje por campo', resumen(r));
    r = await llamar('POST', '/auth/registro', { crudo: '{esto no es json' });
    ok(r.estado === 400 && formatoError(r.datos), 'JSON mal formado → 400', resumen(r));
    r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: nuevo, contrasena: 'incorrecta123' } });
    ok(r.estado === 401 && r.datos.mensaje === 'Usuario o contraseña incorrectos', 'Contraseña mala → 401 genérico', resumen(r));
    r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: 'no_existe_' + sufijo.slice(0, 4), contrasena: CLAVE_QA } });
    ok(r.estado === 401 && r.datos.mensaje === 'Usuario o contraseña incorrectos', 'Usuario inexistente → mismo 401 genérico', resumen(r));
    r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: nuevo, contrasena: CLAVE_QA } });
    ok(r.estado === 200 && r.datos.token && r.datos.tipo === 'Bearer' && r.datos.expiraEnSegundos === 3600, 'Login correcto devuelve token de 60 min', resumen(r));
    const tokenNuevo = r.datos.token;

    // ------------------------------------------------------------ 3. mi cuenta
    console.log('\n3. Mi cuenta');
    r = await llamar('GET', '/usuarios/me', { token: tokenNuevo });
    ok(r.estado === 200 && r.datos.nombreUsuario === nuevo, 'GET /usuarios/me', resumen(r));
    r = await llamar('PUT', '/usuarios/me', { token: tokenNuevo, cuerpo: { nombre: 'Prueba Automática', correo: nuevo + '@prueba.geronex.mx' } });
    ok(r.estado === 200 && r.datos.nombre === 'Prueba Automática', 'PUT /usuarios/me cambia el nombre', resumen(r));
    r = await llamar('PUT', '/usuarios/me', { token: tokenNuevo, cuerpo: { nombre: 'x', correo: 'no-es-correo' } });
    ok(r.estado === 400, 'Correo inválido en mi cuenta → 400', resumen(r));
    r = await llamar('PATCH', '/usuarios/me/password', { token: tokenNuevo, cuerpo: { contrasenaActual: 'equivocada1', contrasenaNueva: 'NuevaClave2026' } });
    ok(r.estado === 400, 'Cambiar contraseña con la actual equivocada → 400', resumen(r));
    r = await llamar('PATCH', '/usuarios/me/password', { token: tokenNuevo, cuerpo: { contrasenaActual: CLAVE_QA, contrasenaNueva: CLAVE_QA } });
    ok(r.estado === 400, 'La contraseña nueva igual a la actual → 400', resumen(r));
    r = await llamar('PATCH', '/usuarios/me/password', { token: tokenNuevo, cuerpo: { contrasenaActual: CLAVE_QA, contrasenaNueva: 'NuevaClave2026' } });
    ok(r.estado === 204, 'Cambiar contraseña correcta → 204', resumen(r));
    r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: nuevo, contrasena: 'NuevaClave2026' } });
    ok(r.estado === 200, 'Login con la contraseña nueva funciona', resumen(r));

    // ------------------------------------------------------------ 4. administración
    console.log('\n4. Administración (cuentas y agentes)');
    const tAdmin = await login(ADMIN.nombreUsuario, ADMIN.contrasena);
    const yo = (await llamar('GET', '/usuarios/me', { token: tAdmin })).datos;
    ok(yo.rol === 'ADMINISTRADOR', 'La cuenta admin tiene rol ADMINISTRADOR', JSON.stringify(yo));
    const tAgente = await cuentaQa(tAdmin, 'qa_agente', 'AGENTE', 'Agente de Pruebas', { telefono: '5500000001' });
    const tResp = await cuentaQa(tAdmin, 'qa_reserva', 'RESPONSABLE_RESERVACIONES', 'Responsable de Pruebas');
    const tAuditor = await cuentaQa(tAdmin, 'qa_auditor', 'AUDITOR', 'Auditor de Pruebas');
    const tSuper = await cuentaQa(tAdmin, 'qa_super', 'SUPERVISOR', 'Supervisor de Pruebas');
    const tCli = await cuentaQa(tAdmin, 'qa_cliente', 'CLIENTE', '');
    const tCli2 = await cuentaQa(tAdmin, 'qa_cliente2', 'CLIENTE', '');
    ok(true, 'Cuentas qa_ listas (agente, responsable, auditor, supervisor y 2 clientes)');

    r = await llamar('GET', '/admin/usuarios', { token: tAdmin });
    ok(r.estado === 200 && r.datos.some(u => u.nombreUsuario === 'qa_agente' && u.rol === 'AGENTE'), 'GET /admin/usuarios lista las cuentas con su rol', resumen(r));
    r = await llamar('GET', '/agentes', { token: tAdmin });
    const agenteQa = r.datos.find(a => a.nombreUsuario === 'qa_agente');
    ok(r.estado === 200 && agenteQa, 'Crear cuenta AGENTE también da de alta al agente', resumen(r));
    r = await llamar('GET', '/agentes/' + agenteQa.idAgente, { token: tSuper });
    ok(r.estado === 200, 'El supervisor puede consultar un agente', resumen(r));
    r = await llamar('PUT', '/agentes/' + agenteQa.idAgente, { token: tSuper, cuerpo: { nombre: 'X', correo: 'x@x.mx', telefono: '' } });
    ok(r.estado === 403, 'El supervisor NO puede editar agentes → 403', resumen(r));
    r = await llamar('PUT', '/agentes/' + agenteQa.idAgente, { token: tAdmin, cuerpo: { nombre: 'Agente de Pruebas', correo: 'qa_agente@prueba.geronex.mx', telefono: '123' } });
    ok(r.estado === 400 && r.datos.errores.telefono, 'Teléfono de agente que no es de 10 dígitos → 400', resumen(r));
    r = await llamar('PUT', '/agentes/' + agenteQa.idAgente, { token: tAdmin, cuerpo: { nombre: 'Agente de Pruebas', correo: 'qa_agente@prueba.geronex.mx', telefono: '5500000002' } });
    ok(r.estado === 200 && r.datos.telefono === '5500000002', 'El admin edita al agente', resumen(r));
    r = await llamar('PUT', '/agentes/' + agenteQa.idAgente, { token: tAdmin, cuerpo: { nombre: 'Agente de Pruebas', correo: 'qa_reserva@prueba.geronex.mx', telefono: '' } });
    ok(r.estado === 409, 'Agente con correo de otra cuenta → 409', resumen(r));
    r = await llamar('PATCH', `/admin/usuarios/${yo.idUsuario}/estatus`, { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });
    ok(r.estado === 400, 'El admin no puede desactivar su propia cuenta → 400', resumen(r));
    r = await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: { nombreUsuario: 'qa_x' + sufijo.slice(0, 4), correo: 'qx' + sufijo + '@prueba.geronex.mx', contrasena: CLAVE_QA, rol: 'DIRECTOR' } });
    ok(r.estado === 400, 'Rol inexistente → 400', resumen(r));

    // baja lógica del agente: su sesión deja de servir y no puede entrar
    r = await llamar('PATCH', `/agentes/${agenteQa.idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });
    ok(r.estado === 200 && r.datos.estatus === 'INACTIVO', 'Baja lógica del agente', resumen(r));
    r = await llamar('GET', '/usuarios/me', { token: tAgente });
    ok(r.estado === 401, 'Con la baja, el token del agente ya no sirve → 401', resumen(r));
    r = await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: 'qa_agente', contrasena: CLAVE_QA } });
    ok(r.estado === 401, 'Con la baja, el agente no puede iniciar sesión', resumen(r));
    r = await llamar('PATCH', `/agentes/${agenteQa.idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'ACTIVO' } });
    ok(r.estado === 200 && r.datos.estatus === 'ACTIVO', 'Reactivar al agente', resumen(r));
    const tAg = await login('qa_agente', CLAVE_QA);

    // ------------------------------------------------------------ 5. flujo completo
    console.log('\n5. Flujo: solicitud → cotización → reservación → confirmación → cancelación');
    const sol = { destino: 'Cancún', fechaInicio: dia(30), fechaFin: dia(34), numPersonas: 2, presupuesto: 30000, necesidades: 'Prueba automática' };
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { ...sol, fechaInicio: dia(-1) } });
    ok(r.estado === 400, 'Solicitud con fecha pasada → 400', resumen(r));
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { ...sol, fechaFin: dia(29) } });
    ok(r.estado === 400, 'Regreso antes de la ida → 400', resumen(r));
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { ...sol, numPersonas: 51 } });
    ok(r.estado === 400, 'Más de 50 personas → 400', resumen(r));
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { ...sol, presupuesto: -5 } });
    ok(r.estado === 400, 'Presupuesto negativo → 400', resumen(r));
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: sol });
    ok(r.estado === 201 && r.datos.estatus === 'NUEVA', 'Crear solicitud → 201 NUEVA', resumen(r));
    const idSol = r.datos.idSolicitud;

    r = await llamar('GET', '/solicitudes/mias', { token: tCli });
    ok(r.estado === 200 && r.datos[0].idSolicitud === idSol, 'GET /solicitudes/mias (la más reciente primero)', resumen(r));
    r = await llamar('GET', '/solicitudes/' + idSol, { token: tCli2 });
    ok(r.estado === 404, 'Otro cliente no ve la solicitud ajena → 404', resumen(r));
    r = await llamar('GET', '/solicitudes?estatus=NUEVA', { token: tAg });
    ok(r.estado === 200 && r.datos.some(s => s.idSolicitud === idSol), 'El agente la ve en su bandeja de NUEVAS', resumen(r));
    r = await llamar('GET', '/solicitudes?estatus=PATITO', { token: tAg });
    ok(r.estado === 400, 'Estatus inventado en el filtro → 400', resumen(r));

    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, detalles: [{ idServicio: 1, cantidad: 1 }, { idServicio: 1, cantidad: 2 }] } });
    ok(r.estado === 400, 'Cotización con servicio repetido → 400', resumen(r));
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, detalles: [{ idServicio: 99999, cantidad: 1 }] } });
    ok(r.estado === 400, 'Cotización con servicio inexistente → 400', resumen(r));
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, detalles: [{ idServicio: 5, cantidad: 99 }] } });
    ok(r.estado === 409, 'Cotización con más cantidad que el cupo → 409', resumen(r));
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, detalles: [] } });
    ok(r.estado === 400, 'Cotización sin servicios → 400', resumen(r));
    r = await llamar('POST', '/cotizaciones', { token: tCli, cuerpo: { idSolicitud: idSol, detalles: [{ idServicio: 1, cantidad: 1 }] } });
    ok(r.estado === 403, 'Un cliente no puede cotizar → 403', resumen(r));

    // primera cotización: el cliente la rechaza
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, notas: 'Opción 1', detalles: [{ idServicio: 2, cantidad: 4 }] } });
    ok(r.estado === 201 && Number(r.datos.total) === 5520, 'Cotización 1: total calculado por el backend (4 × 1380 = 5520)', resumen(r));
    const idCot1 = r.datos.idCotizacion;
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, detalles: [{ idServicio: 2, cantidad: 1 }] } });
    ok(r.estado === 409, 'No se puede cotizar dos veces mientras hay una pendiente → 409', resumen(r));
    r = await llamar('POST', `/cotizaciones/${idCot1}/aceptar`, { token: tCli2 });
    ok(r.estado === 404, 'Otro cliente no puede aceptar la cotización ajena → 404', resumen(r));
    r = await llamar('POST', `/cotizaciones/${idCot1}/rechazar`, { token: tCli });
    ok(r.estado === 200 && r.datos.estatus === 'RECHAZADA', 'El cliente rechaza la cotización 1', resumen(r));
    r = await llamar('GET', '/solicitudes/' + idSol, { token: tCli });
    ok(r.datos.estatus === 'NUEVA', 'Al rechazar, la solicitud vuelve a NUEVA', resumen(r));

    // segunda cotización: el cliente la acepta
    const antes = { 1: await cupo(1), 6: await cupo(6), 12: await cupo(12) };
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSol, notas: 'Opción 2', detalles: [{ idServicio: 1, cantidad: 4 }, { idServicio: 6, cantidad: 2 }, { idServicio: 12, cantidad: 2 }] } });
    ok(r.estado === 201 && Number(r.datos.total) === 19500, 'Cotización 2: total 19,500 (4×2450 + 2×3200 + 2×1650)', resumen(r));
    const idCot2 = r.datos.idCotizacion;
    r = await llamar('POST', `/cotizaciones/${idCot2}/aceptar`, { token: tCli });
    ok(r.estado === 201 && r.datos.estatus === 'POR_VALIDAR' && /^GX-\d{6}-[A-Z2-9]{4}$/.test(r.datos.folio),
        'Aceptar crea reservación POR_VALIDAR con folio GX-AAMMDD-XXXX', resumen(r));
    const idRes = r.datos.idReservacion;
    ok(r.datos.puedeCancelar === true && r.datos.fechaLimiteCancelacion === dia(27), 'Comprobante trae puedeCancelar y la fecha límite (3 días antes)', resumen(r));
    r = await llamar('POST', `/cotizaciones/${idCot2}/aceptar`, { token: tCli });
    ok(r.estado === 409, 'Aceptar dos veces → 409', resumen(r));
    r = await llamar('GET', '/solicitudes/mias', { token: tCli });
    const conRes = r.datos.find(s => s.idSolicitud === idSol);
    ok(conRes.estatus === 'ACEPTADA' && conRes.cotizaciones.some(c => c.folio && c.estatusReservacion === 'POR_VALIDAR'),
        'La solicitud queda ACEPTADA y su cotización trae folio y estatus de la reservación');

    r = await llamar('GET', '/reservaciones/mias', { token: tCli });
    ok(r.estado === 200 && r.datos.some(x => x.idReservacion === idRes), 'GET /reservaciones/mias', resumen(r));
    r = await llamar('GET', '/reservaciones/' + idRes, { token: tCli2 });
    ok(r.estado === 404, 'Otro cliente no ve el comprobante ajeno → 404', resumen(r));
    r = await llamar('GET', '/reservaciones?estatus=POR_VALIDAR', { token: tResp });
    ok(r.estado === 200 && r.datos.some(x => x.idReservacion === idRes), 'El responsable la ve en su bandeja POR_VALIDAR', resumen(r));
    r = await llamar('POST', `/reservaciones/${idRes}/confirmar`, { token: tAuditor });
    ok(r.estado === 403, 'El auditor NO puede confirmar → 403', resumen(r));
    r = await llamar('POST', `/reservaciones/${idRes}/confirmar`, { token: tResp });
    ok(r.estado === 200 && r.datos.estatus === 'CONFIRMADA' && r.datos.responsable, 'El responsable confirma', resumen(r));
    ok(await cupo(1) === antes[1] - 4 && await cupo(6) === antes[6] - 2 && await cupo(12) === antes[12] - 2,
        'Al confirmar se descuenta el cupo de cada servicio');
    r = await llamar('POST', `/reservaciones/${idRes}/confirmar`, { token: tResp });
    ok(r.estado === 409, 'Confirmar dos veces → 409', resumen(r));
    r = await llamar('POST', `/reservaciones/${idRes}/cancelar`, { token: tCli2 });
    ok(r.estado === 404, 'Otro cliente no puede cancelar → 404', resumen(r));
    r = await llamar('POST', `/reservaciones/${idRes}/cancelar`, { token: tCli });
    ok(r.estado === 200 && r.datos.estatus === 'CANCELADA' && Number(r.datos.cargoCancelacion) === 975,
        'Cancelar una CONFIRMADA registra cargo del 5% (975.00)', resumen(r));
    ok(await cupo(1) === antes[1] && await cupo(6) === antes[6] && await cupo(12) === antes[12],
        'Al cancelar regresa el cupo');
    r = await llamar('POST', `/reservaciones/${idRes}/cancelar`, { token: tCli });
    ok(r.estado === 409, 'Cancelar dos veces → 409', resumen(r));

    // ------------------------------------------------------------ 6. rechazo y regla de 3 días
    console.log('\n6. Rechazo del responsable y regla de 3 días');
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { destino: 'Oaxaca', fechaInicio: dia(1), fechaFin: dia(3), numPersonas: 1 } });
    ok(r.estado === 201, 'Solicitud para mañana (sin presupuesto ni necesidades)', resumen(r));
    const idSolM = r.datos.idSolicitud;
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSolM, detalles: [{ idServicio: 4, cantidad: 2 }] } });
    r = await llamar('POST', `/cotizaciones/${r.datos.idCotizacion}/aceptar`, { token: tCli });
    const idResM = r.datos.idReservacion;
    ok(r.estado === 201 && r.datos.puedeCancelar === false, 'Viaje de mañana: puedeCancelar = false', resumen(r));
    r = await llamar('POST', `/reservaciones/${idResM}/cancelar`, { token: tCli });
    ok(r.estado === 409 && r.datos.mensaje.includes('3 días'), 'Cancelar con menos de 3 días → 409 con explicación', resumen(r));
    r = await llamar('POST', `/reservaciones/${idResM}/rechazar`, { token: tResp, cuerpo: { motivo: '   ' } });
    ok(r.estado === 400, 'Rechazar sin motivo → 400', resumen(r));
    r = await llamar('POST', `/reservaciones/${idResM}/rechazar`, { token: tSuper, cuerpo: { motivo: 'Prueba: el hotel no tiene cuartos' } });
    ok(r.estado === 200 && r.datos.estatus === 'RECHAZADA' && r.datos.motivoRechazo, 'El supervisor rechaza con motivo', resumen(r));
    r = await llamar('GET', '/solicitudes/' + idSolM, { token: tAg });
    ok(r.datos.estatus === 'NUEVA', 'Al rechazar la reservación, la solicitud regresa a NUEVA', resumen(r));

    // cancelar una POR_VALIDAR no tiene cargo
    r = await llamar('POST', '/solicitudes', { token: tCli, cuerpo: { ...sol, destino: 'CDMX' } });
    const idSolC = r.datos.idSolicitud;
    r = await llamar('POST', '/cotizaciones', { token: tAg, cuerpo: { idSolicitud: idSolC, detalles: [{ idServicio: 14, cantidad: 2 }] } });
    r = await llamar('POST', `/cotizaciones/${r.datos.idCotizacion}/aceptar`, { token: tCli });
    r = await llamar('POST', `/reservaciones/${r.datos.idReservacion}/cancelar`, { token: tCli });
    ok(r.estado === 200 && Number(r.datos.cargoCancelacion) === 0, 'Cancelar una POR_VALIDAR no tiene cargo', resumen(r));

    // ------------------------------------------------------------ 7. seguridad
    console.log('\n7. Seguridad (todo esto debe fallar)');
    r = await llamar('POST', '/solicitudes', { cuerpo: sol });
    ok(r.estado === 401 && formatoError(r.datos), 'Sin token → 401 en JSON', resumen(r));
    r = await llamar('GET', '/usuarios/me', { token: 'esto.no.sirve' });
    ok(r.estado === 401, 'Token falso → 401', resumen(r));
    r = await llamar('GET', '/admin/usuarios', { token: tAg });
    ok(r.estado === 403 && formatoError(r.datos), 'Agente en administración → 403 en JSON', resumen(r));
    r = await llamar('GET', '/solicitudes', { token: tCli });
    ok(r.estado === 403, 'Cliente en la bandeja de empleados → 403', resumen(r));
    r = await llamar('GET', '/reservaciones', { token: tCli });
    ok(r.estado === 403, 'Cliente en la bandeja de reservaciones → 403', resumen(r));
    r = await llamar('POST', '/solicitudes', { token: tAg, cuerpo: sol });
    ok(r.estado === 403, 'Un agente no puede crear solicitudes → 403', resumen(r));
    r = await llamar('POST', '/admin/usuarios', { token: tCli, cuerpo: {} });
    ok(r.estado === 403, 'Un cliente no puede crear cuentas → 403', resumen(r));
    r = await llamar('GET', '/agentes', { token: tResp });
    ok(r.estado === 403, 'El responsable no consulta agentes → 403', resumen(r));

    // ------------------------------------------------------------ resultado
    console.log(`\nResultado: ${pasaron} pasaron, ${fallas.length} fallaron.`);
    if (fallas.length) { console.log('Fallaron:\n - ' + fallas.join('\n - ')); process.exitCode = 1; }
}

principal().catch(e => {
    console.error('\nLa prueba se detuvo: ' + e.message);
    if (e.cause?.code === 'ECONNREFUSED') console.error('¿Está encendido el backend en ' + API + '?');
    process.exitCode = 2;
});
