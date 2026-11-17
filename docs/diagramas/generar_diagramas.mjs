// =====================================================================
// GERONEX - Genera los diagramas de la documentación en SVG:
//   er.svg            Diagrama entidad-relación (las 12 tablas implementadas)
//   casos_de_uso.svg  Diagrama de casos de uso
//   estados.svg       Ciclo de vida de solicitud, cotización y reservación
// Uso:  node generar_diagramas.mjs   (en esta carpeta)
// Los SVG se abren en cualquier navegador y se pueden insertar en Word (Insertar > Imágenes).
// =====================================================================
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const AQUI = dirname(fileURLToPath(import.meta.url));
const C = { navy: '#002B5B', slate: '#2D4059', orange: '#F07B3F', coral: '#EA5455', crema: '#F9F5EB', gris: '#6b7280', verde: '#1f7a4d', ambar: '#a15c07' };
const FUENTE = "font-family='Segoe UI, Arial, sans-serif'";
const esc = t => String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function svg(ancho, alto, contenido, titulo) {
    return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${ancho} ${alto}" width="${ancho}" height="${alto}" ${FUENTE}>
<title>${esc(titulo)}</title>
<defs>
  <marker id="flecha" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="${C.slate}"/></marker>
  <marker id="triangulo" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto"><path d="M0,0 L12,6 L0,12 z" fill="#fff" stroke="${C.slate}" stroke-width="1.2"/></marker>
</defs>
<rect width="${ancho}" height="${alto}" fill="#ffffff"/>
${contenido}
</svg>
`;
}

// ------------------------------------------------------------------ ER
function diagramaER() {
    const tablas = {
        rol: { x: 30, y: 60, cols: [['id_rol', 'PK'], ['nombre', 'UK'], ['descripcion', '']] },
        usuario: { x: 300, y: 60, cols: [['id_usuario', 'PK'], ['nombre_usuario', 'UK'], ['contrasena (hash)', ''], ['correo', 'UK'], ['id_rol', 'FK'], ['estatus', ''], ['fecha_creacion', ''], ['fecha_actualizacion', '']] },
        perfil: { x: 590, y: 60, cols: [['id_perfil', 'PK'], ['id_usuario', 'FK UK'], ['nombre', '']] },
        reservacion: { x: 1160, y: 60, cols: [['id_reservacion', 'PK'], ['folio', 'UK'], ['id_cotizacion', 'FK UK'], ['id_usuario (cliente)', 'FK'], ['id_responsable', 'FK'], ['total', ''], ['estatus', ''], ['motivo_rechazo', ''], ['cargo_cancelacion', ''], ['fecha_creacion', ''], ['fecha_validacion', ''], ['fecha_cancelacion', '']] },
        agente: { x: 300, y: 360, cols: [['id_agente', 'PK'], ['id_usuario', 'FK UK'], ['nombre', ''], ['correo', 'UK'], ['telefono', ''], ['estatus', '']] },
        solicitud_viaje: { x: 590, y: 360, cols: [['id_solicitud', 'PK'], ['id_usuario (cliente)', 'FK'], ['id_agente', 'FK'], ['destino', ''], ['fecha_inicio', ''], ['fecha_fin', ''], ['num_personas', ''], ['presupuesto', ''], ['necesidades', ''], ['estatus', ''], ['fecha_creacion', ''], ['fecha_actualizacion', '']] },
        cotizacion: { x: 880, y: 360, cols: [['id_cotizacion', 'PK'], ['id_solicitud', 'FK'], ['id_agente', 'FK'], ['total', ''], ['notas', ''], ['estatus', ''], ['fecha_creacion', ''], ['fecha_respuesta', '']] },
        categoria_servicio: { x: 30, y: 690, cols: [['id_categoria', 'PK'], ['nombre', 'UK'], ['descripcion', '']] },
        proveedor: { x: 300, y: 690, cols: [['id_proveedor', 'PK'], ['nombre', 'UK'], ['telefono', ''], ['correo', ''], ['estatus', '']] },
        servicio: { x: 590, y: 690, cols: [['id_servicio', 'PK'], ['id_categoria', 'FK'], ['id_proveedor', 'FK'], ['nombre', ''], ['descripcion', ''], ['destino', ''], ['precio', ''], ['unidad', ''], ['estatus', '']] },
        detalle_cotizacion: { x: 880, y: 690, cols: [['id_detalle', 'PK'], ['id_cotizacion', 'FK'], ['id_servicio', 'FK'], ['cantidad', ''], ['precio_unitario', ''], ['subtotal', '']] },
        disponibilidad: { x: 1160, y: 690, cols: [['id_disponibilidad', 'PK'], ['id_servicio', 'FK UK'], ['cupo_total', ''], ['cupo_disponible', '']] }
    };
    const W = 240, HEAD = 34, FILA = 20;
    let cuerpo = '';
    for (const [nombre, t] of Object.entries(tablas)) {
        const h = HEAD + t.cols.length * FILA + 8;
        t.h = h;
        cuerpo += `<g>
  <rect x="${t.x}" y="${t.y}" width="${W}" height="${h}" rx="8" fill="#fff" stroke="${C.slate}" stroke-width="1.5"/>
  <path d="M${t.x},${t.y + 8} a8,8 0 0 1 8,-8 h${W - 16} a8,8 0 0 1 8,8 v${HEAD - 8} h-${W} z" fill="${C.navy}"/>
  <text x="${t.x + W / 2}" y="${t.y + 22}" text-anchor="middle" fill="#fff" font-size="15" font-weight="700">${nombre.toUpperCase()}</text>
${t.cols.map(([c, k], i) => {
            const y = t.y + HEAD + 16 + i * FILA;
            const color = k.startsWith('PK') ? C.orange : k.startsWith('FK') ? C.slate : '#333';
            return `  <text x="${t.x + 12}" y="${y}" font-size="13" fill="${color}" ${k.startsWith('PK') ? 'font-weight="700"' : ''} ${k.startsWith('FK') ? 'font-style="italic"' : ''}>${esc(c)}</text>
  <text x="${t.x + W - 12}" y="${y}" font-size="11" text-anchor="end" fill="${C.gris}" font-weight="700">${k}</text>`;
        }).join('\n')}
</g>
`;
    }
    // Relaciones: [ruta, etiqueta en el lado "1", etiqueta en el lado "N", posiciones de etiquetas]
    const lineas = [
        { d: 'M270,95 H300', l: [[276, 88, '1'], [290, 88, 'N']] },                                  // rol - usuario
        { d: 'M540,95 H590', l: [[546, 88, '1'], [580, 88, '1']] },                                  // usuario - perfil
        { d: 'M420,262 V360', l: [[428, 278, '1'], [428, 352, '0..1']] },                            // usuario - agente
        { d: 'M540,200 H560 V420 H590', l: [[546, 193, '1'], [578, 412, 'N']] },                     // usuario - solicitud
        { d: 'M540,410 H590', l: [[546, 403, '1'], [580, 403, 'N']] },                               // agente - solicitud
        { d: 'M830,410 H880', l: [[836, 403, '1'], [870, 403, 'N']] },                               // solicitud - cotizacion
        { d: 'M460,522 V650 H860 V520 H880', l: [[468, 540, '1'], [870, 512, 'N']] },                // agente - cotizacion
        { d: 'M1000,562 V690', l: [[1008, 584, '1'], [1008, 682, 'N']] },                            // cotizacion - detalle
        { d: 'M830,760 H880', l: [[836, 753, '1'], [870, 753, 'N']] },                               // servicio - detalle
        { d: 'M540,740 H590', l: [[546, 733, '1'], [580, 733, 'N']] },                               // proveedor - servicio
        { d: 'M150,792 V930 H650 V912', l: [[158, 810, '1'], [658, 912, 'N']] },                     // categoria - servicio
        { d: 'M760,912 V950 H1280 V812', l: [[768, 935, '1'], [1288, 830, '1']] },                   // servicio - disponibilidad
        { d: 'M1120,390 H1140 V300 H1160', l: [[1126, 383, '1'], [1148, 292, '0..1']] },             // cotizacion - reservacion
        { d: 'M480,60 V30 H1300 V60', l: [[488, 50, '1'], [1308, 52, 'N']] }                          // usuario - reservacion
    ];
    for (const r of lineas) {
        cuerpo += `<path d="${r.d}" fill="none" stroke="${C.slate}" stroke-width="1.6"/>\n`;
        for (const [x, y, t] of r.l) cuerpo += `<text x="${x}" y="${y}" font-size="12" font-weight="700" fill="${C.coral}">${t}</text>\n`;
    }
    cuerpo += `<text x="700" y="22" font-size="12" fill="${C.gris}" text-anchor="middle">usuario 1 — N reservación (como cliente y como responsable que valida)</text>
<text x="30" y="985" font-size="12" fill="${C.gris}">PK = llave primaria · FK = llave foránea · UK = valor único · Las cardinalidades se leen de la tabla "1" a la tabla "N". Base de datos MySQL 8 "geronex".</text>`;
    return svg(1430, 1000, cuerpo, 'GERONEX - Diagrama entidad-relación');
}

// ------------------------------------------------------------------ Casos de uso
function actor(x, y, nombre) {
    const lineas = nombre.split('\n');
    return `<g stroke="${C.navy}" stroke-width="2.2" fill="none">
  <circle cx="${x}" cy="${y - 34}" r="11" fill="${C.crema}"/>
  <path d="M${x},${y - 23} V${y + 6} M${x - 18},${y - 12} H${x + 18} M${x},${y + 6} L${x - 14},${y + 30} M${x},${y + 6} L${x + 14},${y + 30}"/>
</g>
${lineas.map((l, i) => `<text x="${x}" y="${y + 48 + i * 16}" text-anchor="middle" font-size="14" font-weight="700" fill="${C.navy}">${esc(l)}</text>`).join('')}`;
}

function diagramaCasosDeUso() {
    const casos = {
        CU01: [450, 90, 'CU-01 Consultar catálogo'], CU02: [450, 150, 'CU-02 Registrarse'],
        CU03: [450, 210, 'CU-03 Iniciar / cerrar sesión'], CU04: [450, 270, 'CU-04 Configurar mi cuenta'],
        CU05: [450, 350, 'CU-05 Crear solicitud de viaje'], CU06: [450, 410, 'CU-06 Consultar mis viajes'],
        CU07: [450, 470, 'CU-07 Responder cotización'], CU08: [450, 550, 'CU-08 Consultar comprobante'],
        CU09: [450, 630, 'CU-09 Cancelar reservación'],
        CU10: [790, 150, 'CU-10 Consultar solicitudes'], CU11: [790, 230, 'CU-11 Elaborar cotización'],
        CU12: [790, 370, 'CU-12 Validar reservación'], CU13: [790, 450, 'CU-13 Consultar reservaciones'],
        CU14: [790, 600, 'CU-14 Gestionar cuentas'], CU15: [790, 670, 'CU-15 Gestionar agentes'],
        CU16: [790, 740, 'CU-16 Consultar agentes']
    };
    const actores = {
        Invitado: [110, 160], Cliente: [110, 470],
        Agente: [1130, 150], Responsable: [1130, 360], Supervisor: [1130, 520], Administrador: [1130, 680], Auditor: [1130, 830]
    };
    const nombresActor = { Responsable: 'Responsable de\nreservaciones' };
    const RX = 135, RY = 24;
    let cuerpo = `<rect x="270" y="40" width="720" height="800" rx="14" fill="${C.crema}" stroke="${C.slate}" stroke-width="1.5"/>
<text x="630" y="30" text-anchor="middle" font-size="16" font-weight="700" fill="${C.navy}">Sistema GERONEX (web + Android + API)</text>\n`;
    const borde = (cu, haciaX) => { const [x, y] = casos[cu]; return [haciaX < x ? x - RX : x + RX, y]; };
    const conexiones = [
        ['Invitado', ['CU01', 'CU02']],
        ['Cliente', ['CU01', 'CU03', 'CU04', 'CU05', 'CU06', 'CU07', 'CU08', 'CU09']],
        ['Agente', ['CU10', 'CU11']],
        ['Responsable', ['CU12', 'CU13']],
        ['Supervisor', ['CU10', 'CU12', 'CU13', 'CU16']],
        ['Administrador', ['CU13', 'CU14', 'CU15', 'CU16']],
        ['Auditor', ['CU10', 'CU13']]
    ];
    for (const [a, lista] of conexiones) {
        const [ax, ay] = actores[a];
        for (const cu of lista) {
            const [x, y] = borde(cu, ax);
            cuerpo += `<line x1="${ax < 630 ? ax + 26 : ax - 26}" y1="${ay - 6}" x2="${x}" y2="${y}" stroke="${C.slate}" stroke-width="1.3"/>\n`;
        }
    }
    // Generalización: el cliente es un invitado con cuenta
    cuerpo += `<line x1="110" y1="395" x2="110" y2="240" stroke="${C.slate}" stroke-width="1.5" marker-end="url(#triangulo)"/>
<text x="118" y="320" font-size="11" fill="${C.gris}">es un</text>\n`;
    // extend / include
    cuerpo += `<path d="M450,606 V574" stroke="${C.orange}" stroke-width="1.5" stroke-dasharray="6 4" fill="none" marker-end="url(#flecha)"/>
<text x="458" y="595" font-size="12" fill="${C.orange}">«extend»</text>
<path d="M655,230 C620,230 610,100 585,95" stroke="${C.orange}" stroke-width="1.5" stroke-dasharray="6 4" fill="none" marker-end="url(#flecha)"/>
<text x="600" y="168" font-size="12" fill="${C.orange}">«include»</text>
<path d="M790,394 V426" stroke="${C.orange}" stroke-width="1.5" stroke-dasharray="6 4" fill="none" marker-end="url(#flecha)"/>
<text x="798" y="416" font-size="12" fill="${C.orange}">«include»</text>\n`;
    for (const [, [x, y, t]] of Object.entries(casos)) {
        cuerpo += `<ellipse cx="${x}" cy="${y}" rx="${RX}" ry="${RY}" fill="#fff" stroke="${C.navy}" stroke-width="1.6"/>
<text x="${x}" y="${y + 5}" text-anchor="middle" font-size="13" fill="${C.navy}">${esc(t)}</text>\n`;
    }
    for (const [a, [x, y]] of Object.entries(actores)) cuerpo += actor(x, y, nombresActor[a] || a) + '\n';
    cuerpo += `<text x="290" y="820" font-size="12" fill="${C.gris}">Todos los usuarios registrados (empleados incluidos) pueden iniciar sesión, cerrar sesión y configurar su cuenta (CU-03, CU-04).</text>`;
    return svg(1260, 900, cuerpo, 'GERONEX - Diagrama de casos de uso');
}

// ------------------------------------------------------------------ Estados
function caja(x, y, texto, color, ancho = 150) {
    return `<rect x="${x}" y="${y}" width="${ancho}" height="44" rx="22" fill="${color}" stroke="${C.slate}" stroke-width="1.2"/>
<text x="${x + ancho / 2}" y="${y + 27}" text-anchor="middle" font-size="14" font-weight="700" fill="#fff">${texto}</text>`;
}
function flecha(d, texto, tx, ty, color = C.slate) {
    return `<path d="${d}" fill="none" stroke="${color}" stroke-width="1.6" marker-end="url(#flecha)"/>
<text x="${tx}" y="${ty}" font-size="12" fill="${C.slate}">${esc(texto)}</text>`;
}
function diagramaEstados() {
    let c = '';
    const titulo = (y, t) => `<text x="30" y="${y}" font-size="17" font-weight="700" fill="${C.navy}">${t}</text><line x1="30" y1="${y + 8}" x2="1150" y2="${y + 8}" stroke="${C.orange}" stroke-width="2"/>`;
    // Solicitud
    c += titulo(40, 'Solicitud de viaje');
    c += caja(80, 90, 'NUEVA', C.ambar) + caja(480, 90, 'COTIZADA', C.navy) + caja(880, 90, 'ACEPTADA', C.verde);
    c += flecha('M230,104 H478', 'el agente envía una cotización', 255, 96);
    c += flecha('M630,104 H878', 'el cliente acepta la cotización', 655, 96);
    c += flecha('M555,136 C555,190 155,190 155,138', 'el cliente rechaza la cotización', 260, 200);
    c += flecha('M955,136 C955,240 155,240 155,138', 'el responsable rechaza la reservación (sin disponibilidad)', 400, 252);
    // Cotizacion
    c += titulo(310, 'Cotización');
    c += caja(80, 350, 'ENVIADA', C.ambar) + caja(480, 330, 'ACEPTADA', C.verde) + caja(480, 400, 'RECHAZADA', C.coral);
    c += flecha('M230,366 C350,366 350,352 478,352', 'el cliente acepta', 300, 345);
    c += flecha('M230,380 C350,380 350,422 478,422', 'el cliente rechaza', 300, 418);
    c += `<text x="660" y="360" font-size="12" fill="${C.gris}">Al aceptar nace la reservación (folio GX-AAMMDD-XXXX).</text>
<text x="660" y="428" font-size="12" fill="${C.gris}">El agente puede enviar una nueva cotización para la misma solicitud.</text>`;
    // Reservacion
    c += titulo(490, 'Reservación');
    c += caja(80, 590, 'POR VALIDAR', C.ambar, 160) + caja(500, 540, 'CONFIRMADA', C.verde, 160) + caja(500, 650, 'RECHAZADA', C.coral, 160) + caja(920, 590, 'CANCELADA', C.slate, 160);
    c += flecha('M240,604 C360,604 360,562 498,562', 'el responsable confirma (se descuenta el cupo)', 250, 552);
    c += flecha('M240,624 C360,624 360,672 498,672', 'el responsable rechaza con motivo', 270, 690);
    c += flecha('M660,562 C800,562 800,604 918,604', 'el cliente cancela: cargo 5%, regresa el cupo', 690, 552);
    c += flecha('M160,636 C160,760 1000,760 1000,636', 'el cliente cancela mientras sigue por validar: sin cargo', 380, 775);
    c += `<text x="30" y="820" font-size="12" fill="${C.gris}">Regla de cancelación: solo con al menos 3 días naturales antes de la fecha de inicio. No se puede cotizar, aceptar ni confirmar un viaje cuya fecha de inicio ya llegó.</text>`;
    return svg(1180, 840, c, 'GERONEX - Ciclo de vida de solicitud, cotización y reservación');
}

writeFileSync(join(AQUI, 'er.svg'), diagramaER());
writeFileSync(join(AQUI, 'casos_de_uso.svg'), diagramaCasosDeUso());
writeFileSync(join(AQUI, 'estados.svg'), diagramaEstados());
console.log('Listo: er.svg, casos_de_uso.svg y estados.svg');
