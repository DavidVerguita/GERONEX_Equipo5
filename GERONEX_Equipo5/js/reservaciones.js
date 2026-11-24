// Lógica de reservaciones.html (las utilidades compartidas están en geronex.js)
if (requerirRol('RESPONSABLE_RESERVACIONES', 'SUPERVISOR', 'ADMINISTRADOR', 'AUDITOR')) {
    pintarNav();
    const $ = id => document.getElementById(id);
    const puedeValidar = ['RESPONSABLE_RESERVACIONES', 'SUPERVISOR'].includes(Sesion.rol());
    if (!puedeValidar) $('subtitulo').textContent = 'Consulta de reservaciones (solo el responsable de reservaciones o el supervisor pueden validarlas).';

    let todas = [];
    let cupos = new Map();
    let estatus = 'POR_VALIDAR';
    let ocupado = false; // mientras hay una ventana abierta no se repinta
    const PESTANAS = [['POR_VALIDAR', 'Por validar'], ['CONFIRMADA', 'Confirmadas'], ['RECHAZADA', 'Rechazadas'], ['CANCELADA', 'Canceladas'], ['', 'Todas']];
    const MOTIVOS = ['El hotel no tiene habitaciones para esas fechas', 'El vuelo ya no tiene lugares', 'El proveedor no confirmó la disponibilidad', 'Cambio de precio del proveedor'];

    function htmlReservacion(r) {
        const porValidar = r.estatus === 'POR_VALIDAR';
        const filas = r.detalles.map(d => {
            const cupo = cupos.get(d.idServicio);
            const estadoCupo = !porValidar || cupo == null ? ''
                : cupo >= d.cantidad ? `<span class="alcanza">✔ ${cupo} disp.</span>` : `<span class="falta">✖ solo ${cupo} disp.</span>`;
            return `<tr><td>${esc(d.servicio)}<div class="meta">${esc(d.proveedor)}</div></td>
                <td class="num">${d.cantidad}</td><td class="num">${dinero(d.subtotal)}</td><td class="num">${estadoCupo}</td></tr>`;
        }).join('');
        const todoAlcanza = r.detalles.every(d => (cupos.get(d.idServicio) ?? 0) >= d.cantidad);
        const faltan = diasPara(r.fechaInicio);
        const vencida = faltan <= 0; // el backend no deja confirmar un viaje cuya fecha ya llegó
        const sePuedeConfirmar = todoAlcanza && !vencida;
        let pie = '';
        if (porValidar && puedeValidar) {
            pie = `${todoAlcanza ? '' : '<p class="aviso aviso-rojo">Algún servicio no tiene cupo suficiente: lo correcto es rechazarla con ese motivo.</p>'}
                ${vencida ? '<p class="aviso aviso-rojo">La fecha del viaje ya llegó: no se puede confirmar. Recházala con ese motivo.</p>' : ''}
                <div class="acciones">
                <button class="btn btn-verde" data-confirmar="${r.idReservacion}" data-folio="${esc(r.folio)}" ${sePuedeConfirmar ? '' : 'disabled title="No se puede confirmar"'}>Confirmar reservación</button>
                <button class="btn btn-peligro" data-rechazar="${r.idReservacion}" data-folio="${esc(r.folio)}">Rechazar</button>
                <a class="btn btn-claro" href="comprobante.html?id=${r.idReservacion}">Ver comprobante</a></div>`;
        } else {
            pie = `<div class="acciones"><a class="btn btn-claro btn-chico" href="comprobante.html?id=${r.idReservacion}">Ver comprobante</a></div>`;
        }
        return `<article class="panel ${porValidar && !todoAlcanza ? 'alerta-cupo' : ''}">
            <div class="fila-con-imagen">
                <img class="miniatura" src="${imagenDestino(r.destino)}" alt="">
                <div>
                    <div class="fila-titulo"><h3>${esc(r.folio)} · ${esc(r.destino)}</h3>${badge(r.estatus)}</div>
                    <div class="meta">Cliente <strong>${esc(r.clienteNombre)}</strong> (${esc(r.clienteCorreo)}) · ${fecha(r.fechaInicio)} al ${fecha(r.fechaFin)} · ${r.numPersonas} pers. · Agente ${esc(r.agente)}</div>
                    <div class="meta">Creada ${fechaHora(r.fechaCreacion)}${r.responsable ? ' · Validó ' + esc(r.responsable) + ' el ' + fechaHora(r.fechaValidacion) : ''}
                        ${porValidar && faltan >= 0 ? ` · <strong style="color:${faltan <= 7 ? 'var(--orange)' : 'inherit'}">el viaje es en ${faltan} día(s)</strong>` : ''}</div>
                </div>
            </div>
            ${r.motivoRechazo ? `<p class="aviso aviso-rojo">Motivo: ${esc(r.motivoRechazo)}</p>` : ''}
            ${r.estatus === 'CANCELADA' ? `<p class="aviso aviso-rojo">Cancelada por el cliente el ${fechaHora(r.fechaCancelacion)} · Cargo ${dinero(r.cargoCancelacion)}</p>` : ''}
            <div class="tabla-wrap" style="margin-top:.8rem"><table class="tabla">
                <thead><tr><th>Servicio</th><th class="num">Cant.</th><th class="num">Subtotal</th><th class="num">${porValidar ? 'Disponibilidad' : ''}</th></tr></thead>
                <tbody>${filas}</tbody>
                <tfoot><tr><td colspan="2" class="num">Total</td><td class="num">${dinero(r.total)}</td><td></td></tr></tfoot>
            </table></div>
            ${pie}
        </article>`;
    }

    function pintar() {
        $('resumen').innerHTML = htmlResumen([
            { valor: todas.filter(r => r.estatus === 'POR_VALIDAR').length, texto: 'Por validar', tono: 'naranja' },
            { valor: todas.filter(r => r.estatus === 'CONFIRMADA').length, texto: 'Confirmadas', tono: 'verde' },
            { valor: todas.filter(r => r.estatus === 'RECHAZADA').length, texto: 'Rechazadas', tono: 'rojo' },
            { valor: dinero(todas.filter(r => r.estatus === 'CONFIRMADA').reduce((t, r) => t + Number(r.total), 0)), texto: 'Vendido (confirmadas)', tono: 'azul' }
        ]);
        $('pestanas').innerHTML = PESTANAS.map(([v, t]) =>
            `<button type="button" data-estatus="${v}" class="${v === estatus ? 'activo' : ''}">${t}<span class="n">${v ? todas.filter(r => r.estatus === v).length : todas.length}</span></button>`).join('');
        const texto = normalizar($('buscar').value);
        const orden = {
            viaje: (a, b) => a.fechaInicio.localeCompare(b.fechaInicio),
            antiguas: (a, b) => String(a.fechaCreacion).localeCompare(String(b.fechaCreacion)),
            recientes: (a, b) => String(b.fechaCreacion).localeCompare(String(a.fechaCreacion)),
            total: (a, b) => b.total - a.total
        }[$('orden').value];
        const lista = todas.filter(r => (!estatus || r.estatus === estatus)
            && (!texto || normalizar(`${r.folio} ${r.clienteNombre} ${r.clienteUsuario} ${r.destino} ${r.agente}`).includes(texto))).sort(orden);
        $('contador').textContent = `${lista.length} reservación(es)`;
        $('lista').innerHTML = lista.length ? lista.map(htmlReservacion).join('')
            : '<div class="vacio">No hay reservaciones con esos filtros. La lista se actualiza sola cada 10 segundos.</div>';
    }

    async function cargar() {
        if (ocupado) return;
        try {
            const [reservaciones, servicios] = await Promise.all([api('/reservaciones'), api('/servicios', { publica: true })]);
            todas = reservaciones;
            cupos = new Map(servicios.map(s => [s.idServicio, s.cupoDisponible]));
            pintar();
            marcarActualizado('actualizado');
        } catch (e) { manejarError(e, 'msg'); }
    }

    $('pestanas').addEventListener('click', ev => {
        const b = ev.target.closest('[data-estatus]');
        if (b) { estatus = b.dataset.estatus; pintar(); }
    });
    $('buscar').addEventListener('input', pintar);
    $('orden').addEventListener('change', pintar);

    $('lista').addEventListener('click', async ev => {
        const confirmar = ev.target.closest('[data-confirmar]');
        const rechazar = ev.target.closest('[data-rechazar]');
        if (!confirmar && !rechazar) return;
        const boton = confirmar || rechazar;
        ocupado = true;
        try {
            if (confirmar) {
                const si = await preguntar('¿Confirmar la reservación ' + confirmar.dataset.folio + '?', 'Se descontará el cupo de cada servicio y el cliente verá su viaje confirmado.', { aceptar: 'Confirmar' });
                if (!si) return;
                boton.disabled = true;
                const r = await enviar(`/reservaciones/${confirmar.dataset.confirmar}/confirmar`, 'POST');
                toast(`Reservación ${r.folio} confirmada. Se descontó la disponibilidad.`);
            } else {
                const motivo = await pedirMotivo(rechazar.dataset.folio);
                if (motivo == null) return;
                boton.disabled = true;
                const r = await enviar(`/reservaciones/${rechazar.dataset.rechazar}/rechazar`, 'POST', { motivo });
                toast(`Reservación ${r.folio} rechazada. La solicitud regresó al agente.`, 'info');
            }
            ocultarMensaje('msg');
        } catch (e) { manejarError(e, 'msg'); boton.disabled = false; }
        finally { ocupado = false; cargar(); }
    });

    // Ventana para escribir el motivo, con motivos frecuentes de un clic
    function pedirMotivo(folio) {
        const promesa = preguntar('Rechazar la reservación ' + folio, 'El cliente verá este motivo y la solicitud regresará al agente para otra propuesta.',
            { aceptar: 'Rechazar', peligro: true, campo: 'Motivo del rechazo', obligatorio: true });
        const campo = document.getElementById('dlg-campo');
        if (campo) {
            campo.insertAdjacentHTML('afterend', `<div class="motivos">${MOTIVOS.map(m => `<button type="button">${esc(m)}</button>`).join('')}</div>`);
            campo.parentElement.querySelector('.motivos').addEventListener('click', ev => {
                if (ev.target.tagName === 'BUTTON') { campo.value = ev.target.textContent; campo.focus(); }
            });
        }
        return promesa;
    }

    cargar();
    refrescarCada(10, cargar);
}
