// =====================================================================
// GERONEX - Casos de prueba de la entidad AGENTE (capas Controlador y Modelo)
//
// Uso (con el backend encendido), en esta carpeta:
//     node prueba_agente.mjs
//
// Ejecuta los 24 casos automáticos (C-01 a C-12 y M-01 a M-12) del documento
// docs/Casos_de_Prueba_Agente.html y guarda la evidencia en resultados_agente.json.
// Los 12 casos de Vista (V-01 a V-12) se ejecutan en admin.html (ver el documento).
// Cada corrida crea un agente nuevo "qag..." para no depender de datos anteriores.
// =====================================================================
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const API = process.env.GERONEX_API || 'http://localhost:8080/api';
const AQUI = dirname(fileURLToPath(import.meta.url));
const ADMIN = JSON.parse(readFileSync(join(AQUI, 'login_admin.json'), 'utf8'));
const CLAVE = 'QaPrueba2026';
const resultados = [];

async function llamar(metodo, ruta, { token, cuerpo } = {}) {
    const h = { 'Content-Type': 'application/json' };
    if (token) h.Authorization = 'Bearer ' + token;
    const r = await fetch(API + ruta, { method: metodo, headers: h, body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo) });
    const t = await r.text();
    let datos = null;
    try { datos = t ? JSON.parse(t) : null; } catch { datos = t; }
    return { estado: r.status, datos };
}
const login = async (u, c) => (await llamar('POST', '/auth/login', { cuerpo: { nombreUsuario: u, contrasena: c } }));
async function token(u, c) {
    const r = await login(u, c);
    if (r.estado !== 200) throw new Error('No se pudo iniciar sesión con ' + u + ': ' + r.estado);
    return r.datos.token;
}
async function asegurar(tAdmin, usuario, rol) {
    await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: { nombreUsuario: usuario, correo: usuario + '@prueba.geronex.mx', contrasena: CLAVE, rol, nombre: usuario } });
    return token(usuario, CLAVE);
}

function caso(id, descripcion, esperado, r, paso) {
    const obtenido = `${r.estado} ${typeof r.datos === 'object' ? JSON.stringify(r.datos).slice(0, 140) : String(r.datos).slice(0, 140)}`;
    resultados.push({ id, descripcion, esperado, obtenido, resultado: paso ? 'PASÓ' : 'FALLÓ' });
    console.log(`${paso ? '  ✔' : '  ✖'} ${id}  ${descripcion}${paso ? '' : '\n       esperado: ' + esperado + '\n       obtenido: ' + obtenido}`);
}

async function principal() {
    console.log('GERONEX · Casos de prueba de AGENTE (Controlador y Modelo) en ' + API + '\n');
    const tAdmin = await token(ADMIN.nombreUsuario, ADMIN.contrasena);
    const tSuper = await asegurar(tAdmin, 'qa_super', 'SUPERVISOR');
    const tResp = await asegurar(tAdmin, 'qa_reserva', 'RESPONSABLE_RESERVACIONES');
    const sufijo = Date.now().toString(36).slice(-6);
    const usuario = 'qag' + sufijo;                    // agente de esta corrida
    const correo = usuario + '@prueba.geronex.mx';
    const nuevo = { nombreUsuario: usuario, correo, contrasena: CLAVE, rol: 'AGENTE', nombre: 'Agente Prueba ' + sufijo, telefono: '5511112222' };

    // -------------------------------------------------------------- ALTA
    console.log('ALTA');
    let r = await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: nuevo });
    caso('C-01', 'Alta de agente con datos válidos', '201 y rol AGENTE', r, r.estado === 201 && r.datos.rol === 'AGENTE');
    const idUsuario = r.datos?.idUsuario;

    r = await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: { ...nuevo, nombreUsuario: 'qat' + sufijo, correo: 'qat' + correo, telefono: '551111222' } });
    caso('C-02', 'Alta con teléfono de 9 dígitos', '400 con error en el campo telefono', r, r.estado === 400 && !!r.datos.errores?.telefono);

    const sinToken = await llamar('POST', '/admin/usuarios', { cuerpo: nuevo });
    const conAgente = await llamar('POST', '/admin/usuarios', { token: await token(usuario, CLAVE), cuerpo: nuevo });
    caso('C-03', 'Alta sin sesión y con sesión de agente', '401 sin sesión y 403 con sesión de agente',
        { estado: `${sinToken.estado}/${conAgente.estado}`, datos: conAgente.datos }, sinToken.estado === 401 && conAgente.estado === 403);

    r = await llamar('GET', '/agentes', { token: tAdmin });
    const agente = r.datos.find(a => a.idUsuario === idUsuario);
    caso('M-01', 'El alta crea la fila en agente ligada a la cuenta, ACTIVO', 'fila con idUsuario de la cuenta y estatus ACTIVO',
        { estado: r.estado, datos: agente }, !!agente && agente.estatus === 'ACTIVO' && agente.nombreUsuario === usuario);
    const idAgente = agente?.idAgente;

    const sinNombre = await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: { nombreUsuario: 'qan' + sufijo, correo: 'qan' + correo, contrasena: CLAVE, rol: 'AGENTE', nombre: '', telefono: '' } });
    const lista2 = await llamar('GET', '/agentes', { token: tAdmin });
    const agenteSinNombre = lista2.datos.find(a => a.nombreUsuario === 'qan' + sufijo);
    caso('M-02', 'Nombre del agente: el enviado, o el nombre de usuario si va vacío; teléfono vacío = null',
        'nombre = "' + nuevo.nombre + '" y nombre = "qan' + sufijo + '" con telefono null',
        { estado: sinNombre.estado, datos: { conNombre: agente?.nombre, sinNombre: agenteSinNombre?.nombre, telefono: agenteSinNombre?.telefono } },
        agente?.nombre === nuevo.nombre && agenteSinNombre?.nombre === 'qan' + sufijo && agenteSinNombre?.telefono == null);
    // ese agente auxiliar se da de baja para no dejarlo activo
    if (agenteSinNombre) await llamar('PATCH', `/agentes/${agenteSinNombre.idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });

    const antes = (await llamar('GET', '/agentes', { token: tAdmin })).datos.length;
    r = await llamar('POST', '/admin/usuarios', { token: tAdmin, cuerpo: { ...nuevo, nombreUsuario: 'qad' + sufijo } });
    const despues = (await llamar('GET', '/agentes', { token: tAdmin })).datos.length;
    caso('M-03', 'Correo repetido: no se crea cuenta ni agente', '409 y el número de agentes no cambia',
        { estado: r.estado, datos: { antes, despues, mensaje: r.datos?.mensaje } }, r.estado === 409 && antes === despues);

    // -------------------------------------------------------------- CONSULTA
    console.log('\nCONSULTA');
    r = await llamar('GET', '/agentes', { token: tAdmin });
    caso('C-04', 'Consultar la lista de agentes como administrador', '200 y una lista', r, r.estado === 200 && Array.isArray(r.datos));
    const lista = r.datos;

    const uno = await llamar('GET', `/agentes/${idAgente}`, { token: tSuper });
    const noExiste = await llamar('GET', '/agentes/999999', { token: tSuper });
    caso('C-05', 'Consultar un agente como supervisor; id inexistente', '200 con el agente y 404 para el inexistente',
        { estado: `${uno.estado}/${noExiste.estado}`, datos: uno.datos }, uno.estado === 200 && uno.datos.idAgente === idAgente && noExiste.estado === 404);

    const comoResp = await llamar('GET', '/agentes', { token: tResp });
    const tCliente = (await llamar('POST', '/auth/registro', { cuerpo: { nombreUsuario: 'qa_cliente', correo: 'qa_cliente@prueba.geronex.mx', contrasena: CLAVE } }), await token('qa_cliente', CLAVE));
    const comoCliente = await llamar('GET', '/agentes', { token: tCliente });
    caso('C-06', 'Consultar agentes como responsable y como cliente', '403 en ambos casos',
        { estado: `${comoResp.estado}/${comoCliente.estado}`, datos: comoCliente.datos }, comoResp.estado === 403 && comoCliente.estado === 403);

    const nombres = lista.map(a => a.nombre);
    const ordenada = [...nombres].sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }));
    caso('M-04', 'La lista viene ordenada por nombre', 'orden alfabético por nombre',
        { estado: 200, datos: nombres.slice(0, 6) }, JSON.stringify(nombres.map(n => n.toLowerCase())) === JSON.stringify(ordenada.map(n => n.toLowerCase())) || nombres.every((n, i) => i === 0 || nombres[i - 1].localeCompare(n, 'en') <= 0));

    caso('M-05', 'Cada agente trae los datos de su cuenta (relación 1 a 1)', 'todos con idUsuario y nombreUsuario, sin repetirse',
        { estado: 200, datos: lista.slice(0, 2) }, lista.every(a => a.idUsuario && a.nombreUsuario) && new Set(lista.map(a => a.idUsuario)).size === lista.length);

    const cuentas = (await llamar('GET', '/admin/usuarios', { token: tAdmin })).datos;
    const cuenta = cuentas.find(u => u.idUsuario === idUsuario);
    caso('M-06', 'Ficha de agente y cuenta coinciden en correo y nombre', 'correo y nombre iguales en agente y usuario/perfil',
        { estado: 200, datos: { agente: [agente?.correo, agente?.nombre], cuenta: [cuenta?.correo, cuenta?.nombre] } },
        cuenta && cuenta.correo === agente.correo && cuenta.nombre === agente.nombre);

    // -------------------------------------------------------------- CAMBIO
    console.log('\nCAMBIO');
    const cambio = { nombre: 'Agente Editado ' + sufijo, correo: 'e' + correo, telefono: '5533334444' };
    r = await llamar('PUT', `/agentes/${idAgente}`, { token: tAdmin, cuerpo: cambio });
    caso('C-07', 'Cambiar nombre, correo y teléfono del agente', '200 con los datos nuevos', r,
        r.estado === 200 && r.datos.nombre === cambio.nombre && r.datos.correo === cambio.correo && r.datos.telefono === cambio.telefono);

    r = await llamar('PUT', `/agentes/${idAgente}`, { token: tAdmin, cuerpo: { ...cambio, nombre: '  ' } });
    caso('C-08', 'Cambio con nombre vacío', '400 con error en el campo nombre', r, r.estado === 400 && !!r.datos.errores?.nombre);

    r = await llamar('PUT', `/agentes/${idAgente}`, { token: tSuper, cuerpo: cambio });
    caso('C-09', 'Cambio hecho por un supervisor', '403', r, r.estado === 403);

    const cuenta2 = (await llamar('GET', '/admin/usuarios', { token: tAdmin })).datos.find(u => u.idUsuario === idUsuario);
    caso('M-07', 'El cambio también actualiza la cuenta y el perfil', 'usuario.correo y perfil.nombre iguales a los nuevos',
        { estado: 200, datos: cuenta2 }, cuenta2?.correo === cambio.correo && cuenta2?.nombre === cambio.nombre);

    r = await llamar('PUT', `/agentes/${idAgente}`, { token: tAdmin, cuerpo: { ...cambio, correo: 'qa_reserva@prueba.geronex.mx' } });
    const sigue = await llamar('GET', `/agentes/${idAgente}`, { token: tAdmin });
    caso('M-08', 'Correo de otra cuenta: se rechaza y no cambia nada', '409 y el correo sigue igual',
        { estado: r.estado, datos: { mensaje: r.datos?.mensaje, correoActual: sigue.datos.correo } }, r.estado === 409 && sigue.datos.correo === cambio.correo);

    r = await llamar('PUT', `/agentes/${idAgente}`, { token: tAdmin, cuerpo: { ...cambio, telefono: '' } });
    caso('M-09', 'Teléfono vacío se guarda como nulo', '200 y telefono null', r, r.estado === 200 && r.datos.telefono == null);

    // -------------------------------------------------------------- BAJA
    console.log('\nBAJA');
    r = await llamar('PATCH', `/agentes/${idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });
    caso('C-10', 'Baja lógica del agente', '200 y estatus INACTIVO', r, r.estado === 200 && r.datos.estatus === 'INACTIVO');

    r = await llamar('PATCH', `/agentes/${idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'BORRADO' } });
    caso('C-11', 'Estatus inválido', '400 con error en el campo estatus', r, r.estado === 400 && !!r.datos.errores?.estatus);

    r = await llamar('PATCH', '/agentes/999999/estatus', { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });
    caso('C-12', 'Baja de un agente que no existe', '404', r, r.estado === 404);

    const cuenta3 = (await llamar('GET', '/admin/usuarios', { token: tAdmin })).datos.find(u => u.idUsuario === idUsuario);
    const loginBaja = await login(usuario, CLAVE);
    caso('M-10', 'Con la baja la cuenta queda INACTIVO y no puede iniciar sesión', 'cuenta INACTIVO y login 401',
        { estado: loginBaja.estado, datos: { estatusCuenta: cuenta3?.estatus } }, cuenta3?.estatus === 'INACTIVO' && loginBaja.estado === 401);

    const conBaja = (await llamar('GET', '/agentes', { token: tAdmin })).datos.find(a => a.idAgente === idAgente);
    caso('M-11', 'La baja es lógica: la fila sigue existiendo', 'el agente aparece en la lista con estatus INACTIVO',
        { estado: 200, datos: conBaja }, conBaja?.estatus === 'INACTIVO');

    r = await llamar('PATCH', `/agentes/${idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'ACTIVO' } });
    const loginOtraVez = await login(usuario, CLAVE);
    caso('M-12', 'Reactivar: agente y cuenta vuelven a ACTIVO y puede iniciar sesión', '200 ACTIVO y login 200',
        { estado: `${r.estado}/${loginOtraVez.estado}`, datos: r.datos }, r.estado === 200 && r.datos.estatus === 'ACTIVO' && loginOtraVez.estado === 200);

    // El agente de esta corrida se deja inactivo para no llenar la lista de agentes activos
    await llamar('PATCH', `/agentes/${idAgente}/estatus`, { token: tAdmin, cuerpo: { estatus: 'INACTIVO' } });

    const pasaron = resultados.filter(x => x.resultado === 'PASÓ').length;
    writeFileSync(join(AQUI, 'resultados_agente.json'), JSON.stringify({ fecha: new Date().toISOString(), api: API, agenteDePrueba: usuario, pasaron, total: resultados.length, resultados }, null, 2));
    console.log(`\nResultado: ${pasaron} de ${resultados.length} pasaron. Evidencia: resultados_agente.json`);
    if (pasaron !== resultados.length) process.exitCode = 1;
}

principal().catch(e => { console.error('\nLa prueba se detuvo: ' + e.message); process.exitCode = 2; });
