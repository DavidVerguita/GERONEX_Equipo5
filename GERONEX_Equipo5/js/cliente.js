// Lógica de cliente.html (las utilidades compartidas están en geronex.js)
if (requerirRol('CLIENTE')) {
    pintarNav();
    const $ = id => document.getElementById(id);
    const u = Sesion.usuario();
    $('saludo').textContent = 'Hola, ' + (u.nombre || u.nombreUsuario);

    let solicitudes = [], reservaciones = [];
    let pestana = 'todas';
    let cotizacionesVistas = null; // para avisar cuando llega una cotización nueva

    // ------------------------------------------------ formulario
    const inicio = $('s-inicio');
    const fin = $('s-fin');
    inicio.min = diaISO(1);
    fin.min = diaISO(1);
    inicio.addEventListener('change', () => {
        fin.min = inicio.value || diaISO(1);
        if (fin.value && fin.value < inicio.value) fin.value = inicio.value;
        pintarResumenViaje();
    });
    fin.addEventListener('change', pintarResumenViaje);
    $('s-personas').addEventListener('input', pintarResumenViaje);
    $('s-necesidades').addEventListener('input', ev => { $('contador-necesidades').textContent = ev.target.value.length + ' / 500'; });

    function pintarResumenViaje() {
        if (!inicio.value || !fin.value) { $('resumen-viaje').textContent = 'Elige tus fechas para ver la duración del viaje.'; return; }
        const n = nochesEntre(inicio.value, fin.value);
        const p = Number($('s-personas').value) || 1;
        $('resumen-viaje').innerHTML = `<strong>${n} noche(s)</strong> · ${p} persona(s) · sales en ${diasPara(inicio.value)} día(s)`
            + (diasPara(inicio.value) < 3 ? '<br><span style="color:#b3282a">Ojo: con menos de 3 días de anticipación ya no podrás cancelar.</span>' : '');
    }

    const destinoUrl = new URLSearchParams(location.search).get('destino');
    if (destinoUrl) $('s-destino').value = destinoUrl;

    // Destinos del catálogo como sugerencias rápidas
    api('/servicios', { publica: true }).then(servicios => {
        const destinos = [...new Set(servicios.map(s => s.destino))];
        $('destinos-catalogo').innerHTML = destinos.map(d => `<option value="${esc(d)}">`).join('');
        $('sugerencias').innerHTML = destinos.map(d => `<button type="button" data-sugerencia="${esc(d)}">${esc(d)}</button>`).join('');
    }).catch(() => {});
    $('sugerencias').addEventListener('click', ev => {
        const b = ev.target.closest('[data-sugerencia]');
        if (b) { $('s-destino').value = b.dataset.sugerencia; inicio.focus(); }
    });

    $('form-solicitud').addEventListener('submit', async ev => {
        ev.preventDefault();
        const boton = botonDe(ev); boton.disabled = true;
        const presupuesto = $('s-presupuesto').value;
        try {
            await enviar('/solicitudes', 'POST', {
                destino: $('s-destino').value.trim(),
                fechaInicio: inicio.value,
                fechaFin: fin.value,
                numPersonas: Number($('s-personas').value),
                presupuesto: presupuesto === '' ? null : Number(presupuesto),
                necesidades: $('s-necesidades').value.trim()
            });
            ev.target.reset();
            $('s-personas').value = 2;
            $('contador-necesidades').textContent = '0 / 500';
            pintarResumenViaje();
            ocultarMensaje('msg-solicitud');
            toast('Solicitud enviada. Te avisaremos aquí cuando tu agente mande la cotización.');
            pestana = 'todas';
            cargar();
        } catch (e) { manejarError(e, 'msg-solicitud'); }
        finally { boton.disabled = false; }
    });

    // ------------------------------------------------ dibujo de solicitudes
    function tablaDetalles(detalles, total) {
        return `<div class="tabla-wrap"><table class="tabla">
            <thead><tr><th>Servicio</th><th>Tipo</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Subtotal</th></tr></thead>
            <tbody>${detalles.map(d => `<tr>
                <td>${esc(d.servicio)}<div class="meta">${esc(d.proveedor)}</div></td>
                <td>${esc(d.categoria)}</td>
                <td class="num">${d.cantidad}</td>
                <td class="num">${dinero(d.precioUnitario)}<div class="meta">${esc(d.unidad)}</div></td>
                <td class="num">${dinero(d.subtotal)}</td></tr>`).join('')}</tbody>
            <tfoot><tr><td colspan="4" class="num">Total</td><td class="num">${dinero(total)}</td></tr></tfoot>
        </table></div>`;
    }

    function htmlCotizacion(c, s) {
        let pie = '';
        if (c.estatus === 'ENVIADA' && diasPara(s.fechaInicio) <= 0) {
            pie = '<p class="aviso aviso-rojo">La fecha de inicio de este viaje ya llegó, así que ya no se puede aceptar. Crea una nueva solicitud con otras fechas.</p>';
        } else if (c.estatus === 'ENVIADA') {
            const p = s.presupuesto;
            const comparacion = p == null ? '' : Number(c.total) <= Number(p)
                ? `<span style="color:var(--verde)">✔ Dentro de tu presupuesto (${dinero(p)})</span>`
                : `<span style="color:#b3282a">Supera tu presupuesto por ${dinero(c.total - p)}</span>`;
            pie = `<p class="meta" style="margin-top:.6rem">${comparacion}</p><div class="acciones">
                <button class="btn btn-verde" data-aceptar="${c.idCotizacion}" data-total="${c.total}">Aceptar y reservar</button>
                <button class="btn btn-peligro" data-rechazar="${c.idCotizacion}">Rechazar</button></div>`;
        } else if (c.folio) {
            pie = `<div class="acciones"><a class="btn btn-azul btn-chico" href="comprobante.html?id=${c.idReservacion}">Ver comprobante ${esc(c.folio)}</a> ${badge(c.estatusReservacion)}</div>`;
        }
        return `<div class="fila-titulo"><strong>Cotización #${c.idCotizacion} · ${esc(c.agente)}</strong>${badge(c.estatus)}</div>
            <div class="meta" style="margin-bottom:.5rem">Enviada ${fechaHora(c.fechaCreacion)} · <a href="cotizacion.html?solicitud=${s.idSolicitud}&cotizacion=${c.idCotizacion}">Ver / imprimir cotización</a></div>
            ${c.notas ? `<p class="meta" style="margin-bottom:.6rem">“${esc(c.notas)}”</p>` : ''}
            ${tablaDetalles(c.detalles, c.total)}${pie}`;
    }

    // Pasos: 1 Solicitud, 2 Cotización, 3 Reservación, 4 Confirmada
    function htmlPasos(s) {
        const ultima = s.cotizaciones[0];
        const res = ultima && ultima.estatusReservacion;
        let estados, nombres = ['Solicitud', 'Cotización', 'Reservación', 'Confirmada'];
        if (s.estatus === 'NUEVA') estados = ['hecho', 'actual', '', ''];
        else if (s.estatus === 'COTIZADA') { estados = ['hecho', 'actual', '', '']; nombres[1] = 'Por responder'; }
        else if (res === 'CONFIRMADA') estados = ['hecho', 'hecho', 'hecho', 'hecho'];
        else if (res === 'CANCELADA') { estados = ['hecho', 'hecho', 'hecho', 'fallo']; nombres[3] = 'Cancelada'; }
        else { estados = ['hecho', 'hecho', 'hecho', 'actual']; nombres[3] = 'Validando'; }
        return `<ol class="pasos-viaje">${nombres.map((n, i) => `<li class="${estados[i]}">${n}</li>`).join('')}</ol>`;
    }

    function htmlSolicitud(s) {
        const pendiente = s.cotizaciones.find(c => c.estatus === 'ENVIADA');
        const vigente = pendiente || s.cotizaciones.find(c => c.estatus === 'ACEPTADA');
        const anteriores = s.cotizaciones.filter(c => c !== vigente);
        return `<article class="panel">
            <div class="fila-con-imagen">
                <img class="miniatura" src="${imagenDestino(s.destino)}" alt="">
                <div>
                    <div class="fila-titulo"><h3>${esc(s.destino)}</h3>${badge(s.estatus)}</div>
                    <div class="meta">Solicitud #${s.idSolicitud} · <strong>${fecha(s.fechaInicio)}</strong> al <strong>${fecha(s.fechaFin)}</strong>
                        (${nochesEntre(s.fechaInicio, s.fechaFin)} noches) · ${s.numPersonas} persona(s)${s.presupuesto != null ? ' · Presupuesto ' + dinero(s.presupuesto) : ''}</div>
                </div>
            </div>
            ${htmlPasos(s)}
            ${s.necesidades ? `<p class="meta" style="margin-top:.4rem"><strong>Necesidades:</strong> ${esc(s.necesidades)}</p>` : ''}
            ${s.estatus === 'NUEVA' ? `<p class="aviso aviso-ambar">${s.cotizaciones.length ? 'Tu agente está preparando una nueva propuesta.' : 'Un agente está preparando tu cotización.'}</p>` : ''}
            ${vigente ? `<div class="${pendiente ? 'cotizacion-nueva' : ''}" style="${pendiente ? '' : 'border-top:1px solid var(--borde);margin-top:1rem;padding-top:1rem'}">${htmlCotizacion(vigente, s)}</div>` : ''}
            ${anteriores.length ? `<details class="anterior"><summary>Ver ${anteriores.length} cotización(es) anterior(es)</summary>
                ${anteriores.map(c => `<div style="margin-top:.8rem">${htmlCotizacion(c, s)}</div>`).join('')}</details>` : ''}
        </article>`;
    }

    const FILTROS_SOLICITUD = {
        todas: ['Todas', () => true],
        curso: ['En curso', s => s.estatus === 'NUEVA'],
        responder: ['Por responder', s => s.estatus === 'COTIZADA'],
        aceptadas: ['Aceptadas', s => s.estatus === 'ACEPTADA']
    };

    function pintarSolicitudes() {
        $('pestanas-solicitudes').innerHTML = Object.entries(FILTROS_SOLICITUD).map(([k, [t, f]]) =>
            `<button type="button" data-pestana="${k}" class="${k === pestana ? 'activo' : ''}">${t}<span class="n">${solicitudes.filter(f).length}</span></button>`).join('');
        const lista = solicitudes.filter(FILTROS_SOLICITUD[pestana][1]);
        $('solicitudes').innerHTML = !solicitudes.length
            ? '<div class="vacio">Aún no has pedido ningún viaje. Usa el formulario para crear tu primera solicitud.</div>'
            : lista.length ? lista.map(htmlSolicitud).join('') : '<div class="vacio">No hay solicitudes en esta pestaña.</div>';
    }
    $('pestanas-solicitudes').addEventListener('click', ev => {
        const b = ev.target.closest('[data-pestana]');
        if (b) { pestana = b.dataset.pestana; pintarSolicitudes(); }
    });

    // ------------------------------------------------ reservaciones
    function pintarReservaciones() {
        const texto = normalizar($('buscar-reservacion').value);
        const estatus = $('filtro-reservacion').value;
        const lista = reservaciones.filter(r => (!estatus || r.estatus === estatus)
            && (!texto || normalizar(r.folio + ' ' + r.destino).includes(texto)));
        $('contador-reservaciones').textContent = `${lista.length} de ${reservaciones.length}`;
        if (!reservaciones.length) { $('reservaciones').innerHTML = '<div class="vacio">Todavía no tienes reservaciones. Acepta una cotización para crear una.</div>'; return; }
        if (!lista.length) { $('reservaciones').innerHTML = '<div class="vacio">Ninguna reservación coincide con la búsqueda.</div>'; return; }
        $('reservaciones').innerHTML = `<div class="panel tabla-wrap"><table class="tabla">
            <thead><tr><th>Folio</th><th>Destino</th><th>Fechas</th><th class="num">Total</th><th>Estatus</th><th></th></tr></thead>
            <tbody>${lista.map(r => {
                const faltan = diasPara(r.fechaInicio);
                const activa = r.estatus === 'POR_VALIDAR' || r.estatus === 'CONFIRMADA';
                return `<tr>
                <td><strong>${esc(r.folio)}</strong></td>
                <td><div class="fila-con-imagen" style="align-items:center"><img class="miniatura" style="width:40px;height:40px" src="${imagenDestino(r.destino)}" alt=""><div>${esc(r.destino)}</div></div></td>
                <td>${fecha(r.fechaInicio)} – ${fecha(r.fechaFin)}${activa && faltan >= 0 ? `<div class="faltan ${faltan <= 7 ? 'pronto' : ''}">${faltan === 0 ? '¡Hoy sales!' : 'Faltan ' + faltan + ' día(s)'}</div>` : ''}</td>
                <td class="num">${dinero(r.total)}${Number(r.cargoCancelacion) > 0 ? `<div class="meta">Cargo ${dinero(r.cargoCancelacion)}</div>` : ''}</td>
                <td>${badge(r.estatus)}</td>
                <td><a class="btn btn-claro btn-chico" href="comprobante.html?id=${r.idReservacion}">Comprobante</a></td></tr>`;
            }).join('')}</tbody>
        </table></div>`;
    }
    $('buscar-reservacion').addEventListener('input', pintarReservaciones);
    $('filtro-reservacion').addEventListener('change', pintarReservaciones);

    function pintarResumen() {
        const porResponder = solicitudes.filter(s => s.estatus === 'COTIZADA').length;
        $('resumen').innerHTML = htmlResumen([
            { valor: solicitudes.filter(s => s.estatus !== 'ACEPTADA').length, texto: 'Solicitudes en curso', tono: 'azul' },
            { valor: porResponder, texto: 'Cotizaciones por responder', tono: porResponder ? 'naranja' : 'azul' },
            { valor: reservaciones.filter(r => r.estatus === 'CONFIRMADA').length, texto: 'Viajes confirmados', tono: 'verde' },
            { valor: reservaciones.filter(r => r.estatus === 'POR_VALIDAR').length, texto: 'Reservaciones por validar', tono: 'ambar' }
        ]);
    }

    async function cargar() {
        try {
            [solicitudes, reservaciones] = await Promise.all([api('/solicitudes/mias'), api('/reservaciones/mias')]);
            // Aviso cuando aparece una cotización que antes no estaba
            const pendientes = new Set(solicitudes.flatMap(s => s.cotizaciones.filter(c => c.estatus === 'ENVIADA').map(c => c.idCotizacion)));
            if (cotizacionesVistas && [...pendientes].some(id => !cotizacionesVistas.has(id))) toast('¡Tu agente te envió una nueva cotización!', 'info');
            cotizacionesVistas = pendientes;
            pintarResumen();
            pintarSolicitudes();
            pintarReservaciones();
            ocultarMensaje('msg-general');
            marcarActualizado('actualizado');
        } catch (e) { manejarError(e, 'msg-general'); }
    }

    $('solicitudes').addEventListener('click', async ev => {
        const aceptar = ev.target.closest('[data-aceptar]');
        const rechazar = ev.target.closest('[data-rechazar]');
        if (!aceptar && !rechazar) return;
        const boton = aceptar || rechazar;
        const si = aceptar
            ? await preguntar('¿Aceptar esta cotización?', `Se creará tu reservación por ${dinero(aceptar.dataset.total)} y el equipo validará la disponibilidad.`, { aceptar: 'Sí, reservar' })
            : await preguntar('¿Rechazar esta cotización?', 'Tu agente podrá enviarte otra propuesta.', { aceptar: 'Rechazar', peligro: true });
        if (!si) return;
        boton.disabled = true;
        try {
            if (aceptar) {
                const r = await enviar(`/cotizaciones/${aceptar.dataset.aceptar}/aceptar`, 'POST');
                toast(`¡Listo! Tu folio es ${r.folio}. Estamos validando la disponibilidad.`);
            } else {
                await enviar(`/cotizaciones/${rechazar.dataset.rechazar}/rechazar`, 'POST');
                toast('Rechazaste la cotización. Tu agente preparará otra propuesta.', 'info');
            }
            ocultarMensaje('msg-acciones');
            cargar();
        } catch (e) { manejarError(e, 'msg-acciones'); boton.disabled = false; }
    });

    cargar();
    refrescarCada(10, cargar);
}
