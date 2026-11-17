// =====================================================================
// GERONEX - Genera docs/Casos_de_Prueba_Agente.html (36 casos MVC de la entidad Agente)
// con la evidencia de:
//   - GERONEX_Equipo5/geronex-backend/pruebas/resultados_agente.json  (Controlador y Modelo, automáticos)
//   - docs/resultados_vista_agente.json                               (Vista, ejecutados en admin.html)
// Uso:  node generar_casos_agente.mjs   (en la carpeta docs)
// =====================================================================
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const AQUI = dirname(fileURLToPath(import.meta.url));
const auto = JSON.parse(readFileSync(join(AQUI, '..', 'GERONEX_Equipo5', 'geronex-backend', 'pruebas', 'resultados_agente.json'), 'utf8'));
const vista = JSON.parse(readFileSync(join(AQUI, 'resultados_vista_agente.json'), 'utf8'));
const porId = Object.fromEntries(auto.resultados.map(r => [r.id, r]));
const esc = t => String(t ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// [id, operación, caso, precondición, pasos y datos, resultado esperado]
const casos = [
    // ------------------------------------------------ VISTA (admin.html)
    ['V-01', 'Alta', 'El campo Teléfono solo aparece para el rol Agente', 'Sesión de administrador en admin.html', 'Elegir el rol Supervisor y después Agente en "Crear cuenta".', 'Con Supervisor el campo queda oculto; con Agente se muestra.'],
    ['V-02', 'Alta', 'Alta exitosa de un agente desde el formulario', 'Sesión de administrador', 'Rol Agente, usuario nuevo, nombre, correo, contraseña de 8+ caracteres y teléfono de 10 dígitos; "Crear cuenta".', 'Aviso "Cuenta … creada con rol Agente" y el agente aparece en la tabla de agentes.'],
    ['V-03', 'Alta', 'Mensaje de error con usuario repetido', 'Existe el usuario que se va a capturar', 'Capturar el mismo nombre de usuario de V-02 y enviar.', 'Mensaje "Ese nombre de usuario ya está en uso"; el formulario conserva los datos.'],
    ['V-04', 'Consulta', 'La tabla de agentes muestra sus datos', 'Hay al menos un agente', 'Revisar la tabla "Agentes".', 'Columnas Nombre, Usuario, Contacto (correo y teléfono), Estatus y botones Editar / Dar de baja.'],
    ['V-05', 'Consulta', 'El resumen cuenta los agentes activos', 'Sesión de administrador', 'Comparar la tarjeta "Agentes activos" antes y después del alta.', 'El número aumenta en 1.'],
    ['V-06', 'Consulta', 'Filtrar cuentas por rol Agente', 'Sesión de administrador', 'En "Todas las cuentas" elegir el filtro de rol Agente.', 'Solo aparecen cuentas con rol Agente y el contador lo indica.'],
    ['V-07', 'Cambio', 'Editar abre el formulario con los datos actuales', 'Hay al menos un agente', 'Elegir "Editar" en la fila del agente.', 'Se abre el panel "Editar agente" con nombre, correo y teléfono actuales.'],
    ['V-08', 'Cambio', 'Guardar cambios válidos', 'Panel de edición abierto', 'Cambiar nombre, correo y teléfono; "Guardar cambios".', 'Aviso de confirmación, el panel se cierra y la tabla muestra los datos nuevos.'],
    ['V-09', 'Cambio', 'Validación del formulario de edición', 'Panel de edición abierto', 'Escribir teléfono "12AB" y correo "sin-arroba".', 'El navegador no permite enviar e indica el formato correcto.'],
    ['V-10', 'Baja', 'La baja pide confirmación', 'Agente activo', 'Elegir "Dar de baja" y luego "Cancelar" en la ventana.', 'Aparece la ventana de confirmación; al cancelar el agente sigue Activo.'],
    ['V-11', 'Baja', 'Confirmar la baja', 'Agente activo', 'Elegir "Dar de baja" y confirmar.', 'La fila muestra Inactivo, el botón cambia a "Reactivar" y el resumen baja en 1.'],
    ['V-12', 'Baja', 'Reactivar al agente', 'Agente inactivo', 'Elegir "Reactivar".', 'La fila vuelve a Activo con el botón "Dar de baja".'],
    // ------------------------------------------------ CONTROLADOR (API REST)
    ['C-01', 'Alta', 'POST /admin/usuarios con rol AGENTE y datos válidos', 'Token de administrador', 'nombreUsuario, correo, contrasena, rol AGENTE, nombre, telefono 5511112222', '201 Created con rol AGENTE.'],
    ['C-02', 'Alta', 'POST /admin/usuarios con teléfono de 9 dígitos', 'Token de administrador', 'telefono = 551111222', '400 con el error en el campo telefono.'],
    ['C-03', 'Alta', 'POST /admin/usuarios sin token y con token de agente', '—', 'Misma petición sin cabecera Authorization y con el token de un agente.', '401 sin sesión y 403 con sesión de agente.'],
    ['C-04', 'Consulta', 'GET /agentes como administrador', 'Token de administrador', '—', '200 con la lista de agentes.'],
    ['C-05', 'Consulta', 'GET /agentes/{id} como supervisor; id inexistente', 'Token de supervisor', 'id del agente creado y id 999999', '200 con el agente; 404 para el inexistente.'],
    ['C-06', 'Consulta', 'GET /agentes como responsable de reservaciones y como cliente', 'Tokens de esos roles', '—', '403 en ambos casos.'],
    ['C-07', 'Cambio', 'PUT /agentes/{id} con datos válidos', 'Token de administrador', 'nombre, correo y teléfono nuevos', '200 con los datos actualizados.'],
    ['C-08', 'Cambio', 'PUT /agentes/{id} con nombre vacío', 'Token de administrador', 'nombre = "  "', '400 con el error en el campo nombre.'],
    ['C-09', 'Cambio', 'PUT /agentes/{id} como supervisor', 'Token de supervisor', 'Datos válidos', '403 (solo el administrador cambia agentes).'],
    ['C-10', 'Baja', 'PATCH /agentes/{id}/estatus a INACTIVO', 'Token de administrador', 'estatus = INACTIVO', '200 con estatus INACTIVO.'],
    ['C-11', 'Baja', 'PATCH /agentes/{id}/estatus con un estatus inválido', 'Token de administrador', 'estatus = BORRADO', '400 con el error en el campo estatus.'],
    ['C-12', 'Baja', 'PATCH /agentes/{id}/estatus de un agente inexistente', 'Token de administrador', 'id 999999', '404.'],
    // ------------------------------------------------ MODELO (entidad y base de datos)
    ['M-01', 'Alta', 'Crear una cuenta AGENTE crea la fila en la tabla agente', 'Alta de C-01', 'Consultar los agentes y buscar el id de la cuenta.', 'Existe la fila ligada a la cuenta (id_usuario) con estatus ACTIVO.'],
    ['M-02', 'Alta', 'Valores por omisión del agente', 'Token de administrador', 'Alta con nombre y teléfono vacíos.', 'agente.nombre toma el nombre de usuario y telefono queda NULL.'],
    ['M-03', 'Alta', 'Correo único (UNIQUE) sin registros a medias', 'Existe un agente con ese correo', 'Alta con otro usuario y el mismo correo.', '409 y el número de agentes no cambia (la transacción no deja nada).'],
    ['M-04', 'Consulta', 'Orden de la consulta', 'Varios agentes', 'GET /agentes', 'Los agentes vienen ordenados por nombre.'],
    ['M-05', 'Consulta', 'Relación 1 a 1 agente–usuario', 'Varios agentes', 'GET /agentes', 'Cada agente trae idUsuario y nombreUsuario, y ningún usuario se repite.'],
    ['M-06', 'Consulta', 'Coherencia entre agente, usuario y perfil', 'Alta de C-01', 'Comparar /agentes con /admin/usuarios.', 'Correo y nombre iguales en agente, usuario y perfil.'],
    ['M-07', 'Cambio', 'El cambio se propaga a usuario y perfil', 'Cambio de C-07', 'Consultar la cuenta en /admin/usuarios.', 'usuario.correo y perfil.nombre tienen los valores nuevos.'],
    ['M-08', 'Cambio', 'Correo de otra cuenta', 'Existe la cuenta qa_reserva', 'PUT con el correo de esa cuenta.', '409 y el correo del agente no cambia.'],
    ['M-09', 'Cambio', 'Teléfono vacío se guarda como NULL', 'Agente con teléfono', 'PUT con telefono vacío.', '200 y telefono = null.'],
    ['M-10', 'Baja', 'La baja desactiva también la cuenta', 'Baja de C-10', 'Consultar la cuenta e intentar iniciar sesión con el agente.', 'usuario.estatus = INACTIVO y el inicio de sesión responde 401.'],
    ['M-11', 'Baja', 'La baja es lógica', 'Baja de C-10', 'GET /agentes', 'La fila sigue existiendo con estatus INACTIVO (no hay DELETE).'],
    ['M-12', 'Baja', 'Reactivación', 'Agente inactivo', 'PATCH estatus ACTIVO e iniciar sesión.', 'Agente y cuenta en ACTIVO; el inicio de sesión responde 200.']
];

const capas = { V: 'Vista (web: admin.html)', C: 'Controlador (API REST de Spring Boot)', M: 'Modelo (entidad Agente, MySQL)' };
let filas = '';
let pasaron = 0;
for (const capa of ['V', 'C', 'M']) {
    filas += `<tr><th colspan="7" class="capa">${capas[capa]}</th></tr>`;
    for (const [id, op, nombre, pre, pasos, esperado] of casos.filter(c => c[0][0] === capa)) {
        let obtenido, ok;
        if (capa === 'V') { ok = vista.resultados[id]?.ok; obtenido = vista.resultados[id]?.obtenido; }
        else { ok = porId[id]?.resultado === 'PASÓ'; obtenido = porId[id]?.obtenido; }
        if (ok) pasaron++;
        filas += `<tr><td class="id">${id}</td><td>${op}</td><td><strong>${esc(nombre)}</strong><div class="pre">Precondición: ${esc(pre)}</div></td>
<td>${esc(pasos)}</td><td>${esc(esperado)}</td><td class="obt">${esc(obtenido || 'Sin ejecutar')}</td><td class="${ok ? 'si' : 'no'}">${ok ? 'Pasó' : 'No pasó'}</td></tr>\n`;
    }
}

const html = `<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>GERONEX - Casos de prueba de Agente</title>
<style>
  body { font-family:'Segoe UI', Arial, sans-serif; color:#222; margin:0; font-size:10pt; }
  main { max-width:1100px; margin:0 auto; padding:24px; }
  h1 { color:#002B5B; margin-bottom:4px; } h2 { color:#002B5B; border-bottom:3px solid #F07B3F; padding-bottom:4px; margin-top:28px; }
  table { width:100%; border-collapse:collapse; margin:10px 0; }
  th { background:#002B5B; color:#fff; text-align:left; padding:6px; font-size:9pt; }
  th.capa { background:#2D4059; font-size:10.5pt; }
  td { border:1px solid #d8d2c4; padding:5px 6px; vertical-align:top; font-size:9pt; }
  tr { page-break-inside:avoid; }
  .id { font-weight:700; color:#F07B3F; white-space:nowrap; } .pre { color:#6b7280; font-size:8.5pt; margin-top:3px; }
  .obt { font-family:Consolas, monospace; font-size:8pt; color:#2D4059; word-break:break-word; }
  .si { color:#1f7a4d; font-weight:700; } .no { color:#b3282a; font-weight:700; }
  .resumen { display:flex; gap:12px; flex-wrap:wrap; margin:14px 0; }
  .resumen div { background:#F9F5EB; border-top:4px solid #002B5B; border-radius:8px; padding:10px 14px; min-width:150px; }
  .resumen b { display:block; font-size:20pt; color:#002B5B; }
  .nota { background:#F9F5EB; border-left:4px solid #F07B3F; padding:10px 14px; }
  @media print { main { padding:0; } }
</style></head><body><main>
<h1>Casos de prueba MVC: entidad Agente</h1>
<p>GERONEX · Laboratorio de Desarrollo de Software III · 5IM8 · Responsable: Vega Beid David</p>
<div class="resumen">
  <div><b>36</b>casos (12 por capa)</div>
  <div><b>${pasaron}/36</b>pasaron</div>
  <div><b>4</b>operaciones: altas, bajas, cambios y consultas</div>
</div>
<div class="nota">
  <strong>Qué es la entidad Agente:</strong> la ficha del agente de viajes (tabla <code>agente</code>), ligada 1 a 1 con su cuenta de acceso con rol AGENTE.
  El alta se hace al crear una cuenta con rol AGENTE (<code>POST /api/admin/usuarios</code>); la consulta, el cambio y la baja lógica, en <code>/api/agentes</code>.
  La vista es la página <code>admin.html</code>.<br>
  <strong>Cómo se ejecutaron:</strong> los casos de Controlador y Modelo con <code>node prueba_agente.mjs</code> (carpeta <code>geronex-backend/pruebas</code>), el ${esc(new Date(auto.fecha).toLocaleString('es-MX'))}, con el agente de prueba <code>${esc(auto.agenteDePrueba)}</code>.
  Los de Vista, en <code>admin.html</code> con sesión de administrador contra el backend real, el ${esc(new Date(vista.fecha).toLocaleString('es-MX'))}, con el agente <code>${esc(vista.agenteDePrueba)}</code>.
  La columna "Resultado obtenido" es la respuesta real que se registró.
</div>
<h2>Matriz de casos</h2>
<table>
<tr><th style="width:5%">ID</th><th style="width:7%">Operación</th><th style="width:20%">Caso</th><th style="width:18%">Pasos / datos</th><th style="width:18%">Resultado esperado</th><th>Resultado obtenido</th><th style="width:6%">Estado</th></tr>
${filas}
</table>
<h2>Evidencia para capturas</h2>
<ol>
  <li>Vista: abre <code>admin.html</code> como administrador y sigue los pasos de V-01 a V-12; toma una captura de cada resultado.</li>
  <li>Controlador y Modelo: en PowerShell, dentro de <code>geronex-backend/pruebas</code>, ejecuta <code>node prueba_agente.mjs</code> y toma una captura de la salida (debe decir "24 de 24 pasaron"). El detalle queda en <code>resultados_agente.json</code>.</li>
  <li>Para regenerar este documento con los resultados nuevos: <code>node generar_casos_agente.mjs</code> en la carpeta <code>docs</code>.</li>
</ol>
</main></body></html>`;
writeFileSync(join(AQUI, 'Casos_de_Prueba_Agente.html'), html);
console.log(`Listo: Casos_de_Prueba_Agente.html (${pasaron}/36 pasaron)`);
