// Lógica de cotizacion.html (las utilidades compartidas están en geronex.js)
// Comprobante de cotización (RF-05.17): ?solicitud=ID&cotizacion=ID
// Lo pueden ver el cliente dueño y los empleados (usa GET /solicitudes/{id}).
if (requerirRol()) {
    pintarNav();
    const params = new URLSearchParams(location.search);
    const idSolicitud = params.get('solicitud');
    const idCotizacion = Number(params.get('cotizacion'));
    const esCliente = Sesion.rol() === 'CLIENTE';
    const volver = esCliente ? 'cliente.html' : inicioPorRol(Sesion.rol());

    function aviso(c) {
        if (c.estatus === 'ENVIADA') return '<div class="aviso aviso-ambar">Cotización por responder. El cliente puede aceptarla o rechazarla en “Mis viajes”.</div>';
        if (c.estatus === 'ACEPTADA') return `<div class="aviso aviso-verde">Cotización aceptada el ${fechaHora(c.fechaRespuesta)}${c.folio ? ` · Reservación <strong>${esc(c.folio)}</strong> (${esc(TEXTO_ESTATUS[c.estatusReservacion] || c.estatusReservacion)})` : ''}.</div>`;
        return `<div class="aviso aviso-rojo">Cotización rechazada por el cliente el ${fechaHora(c.fechaRespuesta)}.</div>`;
    }

    function pintar(s, c) {
        document.title = 'GERONEX - Cotización #' + c.idCotizacion;
        const comparacion = s.presupuesto == null ? '' : Number(c.total) <= Number(s.presupuesto)
            ? `<p class="meta" style="color:var(--verde)">Dentro del presupuesto del cliente (${dinero(s.presupuesto)}).</p>`
            : `<p class="meta" style="color:#b3282a">Supera el presupuesto del cliente (${dinero(s.presupuesto)}) por ${dinero(c.total - s.presupuesto)}.</p>`;
        document.getElementById('comprobante').innerHTML = `
        <article class="comprobante">
            <div class="comprobante-cabecera">
                <div class="marca"><img src="img/logo.svg" alt=""><div><div class="meta" style="color:#cfd8e3">GERONEX · Comprobante de cotización</div><div class="folio">Cotización #${c.idCotizacion}</div></div></div>
                <div style="text-align:right">${badge(c.estatus)}<div class="meta" style="color:#cfd8e3;margin-top:.3rem">Emitida ${fechaHora(c.fechaCreacion)}</div></div>
            </div>
            <div class="comprobante-foto" style="background-image:url('${imagenDestino(s.destino)}')" role="img" aria-label="${esc(s.destino)}"></div>
            <div class="comprobante-cuerpo">
                ${aviso(c)}
                <h3>Cliente</h3>
                <div class="datos-grid">
                    <div><span>Nombre</span>${esc(s.clienteNombre)}</div>
                    <div><span>Usuario</span>${esc(s.clienteUsuario)}</div>
                    <div><span>Correo</span>${esc(s.clienteCorreo)}</div>
                </div>
                <h3>Viaje solicitado</h3>
                <div class="datos-grid">
                    <div><span>Solicitud</span>#${s.idSolicitud}</div>
                    <div><span>Destino</span>${esc(s.destino)}</div>
                    <div><span>Fechas</span>${fecha(s.fechaInicio)} al ${fecha(s.fechaFin)} (${nochesEntre(s.fechaInicio, s.fechaFin)} noches)</div>
                    <div><span>Personas</span>${s.numPersonas}</div>
                    <div><span>Agente</span>${esc(c.agente)}</div>
                </div>
                ${c.notas ? `<h3>Notas del agente</h3><p>“${esc(c.notas)}”</p>` : ''}
                <h3>Servicios cotizados</h3>
                <div class="tabla-wrap"><table class="tabla">
                    <thead><tr><th>Servicio</th><th>Proveedor</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Subtotal</th></tr></thead>
                    <tbody>${c.detalles.map(d => `<tr>
                        <td>${esc(d.servicio)}</td>
                        <td>${esc(d.proveedor)}</td>
                        <td class="num">${d.cantidad}</td>
                        <td class="num">${dinero(d.precioUnitario)}<div class="meta">${esc(d.unidad)}</div></td>
                        <td class="num">${dinero(d.subtotal)}</td></tr>`).join('')}</tbody>
                    <tfoot><tr><td colspan="4" class="num">Total</td><td class="num total">${dinero(c.total)}</td></tr></tfoot>
                </table></div>
                ${comparacion}
                <p class="meta" style="margin-top:1rem">Los precios de esta cotización se guardaron al emitirla: si el catálogo cambia después, el importe no se altera.
                   La reservación se confirma cuando el equipo valida la disponibilidad con los proveedores.</p>
                <div class="acciones no-imprimir">
                    <a class="btn btn-claro" href="${volver}">← Volver</a>
                    <button class="btn btn-azul" onclick="window.print()">Imprimir / guardar PDF</button>
                    ${c.idReservacion ? `<a class="btn btn-primario" href="comprobante.html?id=${c.idReservacion}">Ver la reservación ${esc(c.folio)}</a>` : ''}
                </div>
            </div>
        </article>`;
    }

    (async () => {
        if (!idSolicitud || !idCotizacion) { mostrarMensaje('msg', 'Faltan el número de solicitud o de cotización.', 'error'); document.getElementById('comprobante').innerHTML = ''; return; }
        try {
            const s = await api('/solicitudes/' + encodeURIComponent(idSolicitud));
            const c = s.cotizaciones.find(x => x.idCotizacion === idCotizacion);
            if (!c) throw { estado: 404, mensaje: 'La cotización no existe en esa solicitud.', errores: {} };
            pintar(s, c);
        } catch (e) {
            document.getElementById('comprobante').innerHTML = '';
            manejarError(e, 'msg');
        }
    })();
}
