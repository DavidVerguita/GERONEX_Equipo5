// Lógica de comprobante.html (las utilidades compartidas están en geronex.js)
if (requerirRol()) {
    pintarNav();
    const id = new URLSearchParams(location.search).get('id');
    const esCliente = Sesion.rol() === 'CLIENTE';
    const volver = esCliente ? 'cliente.html' : inicioPorRol(Sesion.rol());

    function avisoEstatus(r) {
        switch (r.estatus) {
            case 'POR_VALIDAR': return '<div class="aviso aviso-ambar">Tu reservación está registrada. El equipo de reservaciones está validando la disponibilidad con los proveedores.</div>';
            case 'CONFIRMADA': return `<div class="aviso aviso-verde">¡Reservación confirmada! Validada${r.responsable ? ' por ' + esc(r.responsable) : ''} el ${fechaHora(r.fechaValidacion)}</div>`;
            case 'RECHAZADA': return `<div class="aviso aviso-rojo">No fue posible confirmar: ${esc(r.motivoRechazo || 'sin disponibilidad')}. Tu agente te enviará otra propuesta.</div>`;
            case 'CANCELADA': return `<div class="aviso aviso-rojo">Reservación cancelada el ${fechaHora(r.fechaCancelacion)} · Cargo por cancelación: <strong>${dinero(r.cargoCancelacion)}</strong></div>`;
            default: return '';
        }
    }

    function pintar(r) {
        const cancelar = esCliente && r.puedeCancelar
            ? `<button class="btn btn-peligro" id="btn-cancelar">Cancelar reservación</button>` : '';
        const reglaCancelacion = r.estatus === 'CONFIRMADA' || r.estatus === 'POR_VALIDAR'
            ? `<p class="meta" style="margin-top:1rem">Puedes cancelar hasta el <strong>${fecha(r.fechaLimiteCancelacion)}</strong> (3 días antes del viaje).
               ${r.estatus === 'CONFIRMADA' ? 'Como ya está confirmada, se aplica un cargo del 5% (' + dinero(r.total * 0.05) + ').' : 'Mientras siga por validar, la cancelación no tiene cargo.'}</p>`
            : '';
        document.getElementById('comprobante').innerHTML = `
        <article class="comprobante">
            <div class="comprobante-cabecera">
                <div class="marca"><img src="img/logo.svg" alt=""><div><div class="meta" style="color:#cfd8e3">GERONEX · Comprobante de reservación</div><div class="folio">${esc(r.folio)}</div></div></div>
                <div style="text-align:right">${badge(r.estatus)}<div class="meta" style="color:#cfd8e3;margin-top:.3rem">Emitido ${fechaHora(r.fechaCreacion)}</div></div>
            </div>
            <div class="comprobante-foto" style="background-image:url('${imagenDestino(r.destino)}')" role="img" aria-label="${esc(r.destino)}"></div>
            <div class="comprobante-cuerpo">
                ${avisoEstatus(r)}
                <h3>Cliente</h3>
                <div class="datos-grid">
                    <div><span>Nombre</span>${esc(r.clienteNombre)}</div>
                    <div><span>Usuario</span>${esc(r.clienteUsuario)}</div>
                    <div><span>Correo</span>${esc(r.clienteCorreo)}</div>
                </div>
                <h3>Viaje</h3>
                <div class="datos-grid">
                    <div><span>Destino</span>${esc(r.destino)}${(r.estatus === 'CONFIRMADA' || r.estatus === 'POR_VALIDAR') && diasPara(r.fechaInicio) >= 0 ? `<br><span class="cuenta-regresiva">${diasPara(r.fechaInicio) === 0 ? '¡Hoy sales!' : 'Faltan ' + diasPara(r.fechaInicio) + ' día(s)'}</span>` : ''}</div>
                    <div><span>Fechas</span>${fecha(r.fechaInicio)} al ${fecha(r.fechaFin)} (${nochesEntre(r.fechaInicio, r.fechaFin)} noches)</div>
                    <div><span>Personas</span>${r.numPersonas}</div>
                    <div><span>Agente</span>${esc(r.agente)}</div>
                    <div><span>Solicitud / cotización</span>#${r.idSolicitud} / #${r.idCotizacion}</div>
                </div>
                <h3>Servicios</h3>
                <div class="tabla-wrap"><table class="tabla">
                    <thead><tr><th>Servicio</th><th>Proveedor</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Subtotal</th></tr></thead>
                    <tbody>${r.detalles.map(d => `<tr>
                        <td>${ICONO_CATEGORIA[d.categoria] || ''} ${esc(d.servicio)}</td>
                        <td>${esc(d.proveedor)}</td>
                        <td class="num">${d.cantidad}</td>
                        <td class="num">${dinero(d.precioUnitario)}<div class="meta">${esc(d.unidad)}</div></td>
                        <td class="num">${dinero(d.subtotal)}</td></tr>`).join('')}</tbody>
                    <tfoot><tr><td colspan="4" class="num">Total</td><td class="num total">${dinero(r.total)}</td></tr>
                    ${r.cargoCancelacion != null && Number(r.cargoCancelacion) > 0 ? `<tr><td colspan="4" class="num">Cargo por cancelación (5%)</td><td class="num">${dinero(r.cargoCancelacion)}</td></tr>` : ''}
                    </tfoot>
                </table></div>
                ${reglaCancelacion}
                <div class="acciones no-imprimir">
                    <a class="btn btn-claro" href="${volver}">← Volver</a>
                    <button class="btn btn-azul" onclick="window.print()">Imprimir / guardar PDF</button>
                    <button class="btn btn-claro" id="btn-copiar">Copiar folio</button>
                    ${cancelar}
                </div>
            </div>
        </article>`;
        const btn = document.getElementById('btn-cancelar');
        if (btn) btn.addEventListener('click', () => cancelarReservacion(r, btn));
        document.getElementById('btn-copiar').addEventListener('click', async () => {
            try { await navigator.clipboard.writeText(r.folio); toast('Folio ' + r.folio + ' copiado.'); }
            catch { toast('Tu folio es ' + r.folio, 'info'); }
        });
        document.title = 'GERONEX - Comprobante ' + r.folio;
    }

    async function cancelarReservacion(r, boton) {
        const cargo = r.estatus === 'CONFIRMADA' ? `Se aplicará un cargo de ${dinero(r.total * 0.05)} (5%).` : 'No tiene cargo.';
        if (!(await preguntar('¿Cancelar la reservación ' + r.folio + '?', cargo + ' Esta acción no se puede deshacer.', { aceptar: 'Sí, cancelar', cancelar: 'No, conservarla', peligro: true }))) return;
        boton.disabled = true;
        try {
            pintar(await enviar(`/reservaciones/${r.idReservacion}/cancelar`, 'POST'));
            toast('Tu reservación se canceló.', 'info');
        } catch (e) { manejarError(e, 'msg'); boton.disabled = false; }
    }

    async function cargar() {
        if (!id) { mostrarMensaje('msg', 'Falta el número de reservación.', 'error'); return; }
        try { pintar(await api('/reservaciones/' + encodeURIComponent(id))); }
        catch (e) {
            document.getElementById('comprobante').innerHTML = '';
            manejarError(e, 'msg');
        }
    }
    cargar();
}
