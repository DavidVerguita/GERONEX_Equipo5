// Lógica de agente.html (las utilidades compartidas están en geronex.js)
if (requerirRol('AGENTE', 'SUPERVISOR', 'AUDITOR')) {
    pintarNav();
    const $ = id => document.getElementById(id);
    const puedeCotizar = Sesion.rol() === 'AGENTE';
    if (!puedeCotizar) $('subtitulo').textContent = 'Consulta de solicitudes (solo los agentes pueden enviar cotizaciones).';

    let todas = [];            // todas las solicitudes (se filtran aquí)
    let estatus = 'NUEVA';     // pestaña activa
    let seleccionada = null;   // solicitud abierta en el panel derecho
    let catalogo = [];         // servicios del catálogo
    let partidas = new Map();  // idServicio -> cantidad (la cotización en construcción)
    let nuevasVistas = null;   // para avisar cuando llega una solicitud nueva

    const PESTANAS = [['NUEVA', 'Por cotizar'], ['COTIZADA', 'Esperando cliente'], ['ACEPTADA', 'Aceptadas'], ['', 'Todas']];

    // ------------------------------------------------ lista de solicitudes
    async function cargarLista() {
        try {
            todas = await api('/solicitudes');
            const nuevas = new Set(todas.filter(s => s.estatus === 'NUEVA').map(s => s.idSolicitud));
            if (nuevasVistas && [...nuevas].some(id => !nuevasVistas.has(id))) toast('Llegó una nueva solicitud de viaje.', 'info');
            nuevasVistas = nuevas;
            pintarResumen();
            pintarLista();
            ocultarMensaje('msg-general');
            marcarActualizado('actualizado');
            // Si otro agente ya cotizó la que tengo abierta, aviso
            if (seleccionada && seleccionada.estatus === 'NUEVA') {
                const actual = todas.find(s => s.idSolicitud === seleccionada.idSolicitud);
                if (!actual || actual.estatus !== 'NUEVA') {
                    mostrarMensaje('msg-cotizacion', 'Esta solicitud ya no está disponible: otro agente la cotizó o cambió de estatus.', 'error');
                }
            }
        } catch (e) { manejarError(e, 'msg-general'); }
    }

    function pintarResumen() {
        const hoy = diaISO(0);
        $('resumen').innerHTML = htmlResumen([
            { valor: todas.filter(s => s.estatus === 'NUEVA').length, texto: 'Por cotizar', tono: 'naranja' },
            { valor: todas.filter(s => s.estatus === 'COTIZADA').length, texto: 'Esperando al cliente', tono: 'azul' },
            { valor: todas.filter(s => s.estatus === 'ACEPTADA').length, texto: 'Aceptadas', tono: 'verde' },
            { valor: todas.filter(s => String(s.fechaCreacion).slice(0, 10) === hoy).length, texto: 'Recibidas hoy', tono: 'ambar' }
        ]);
    }

    function visibles() {
        const texto = normalizar($('buscar').value);
        const lista = todas.filter(s => (!estatus || s.estatus === estatus)
            && (!texto || normalizar(`#${s.idSolicitud} ${s.destino} ${s.clienteNombre} ${s.clienteUsuario} ${s.agente || ''}`).includes(texto)));
        const orden = {
            antiguas: (a, b) => String(a.fechaCreacion).localeCompare(String(b.fechaCreacion)),
            recientes: (a, b) => String(b.fechaCreacion).localeCompare(String(a.fechaCreacion)),
            viaje: (a, b) => a.fechaInicio.localeCompare(b.fechaInicio),
            presupuesto: (a, b) => (b.presupuesto ?? -1) - (a.presupuesto ?? -1)
        }[$('orden').value];
        return lista.sort(orden);
    }

    function pintarLista() {
        $('pestanas').innerHTML = PESTANAS.map(([v, t]) =>
            `<button type="button" data-estatus="${v}" class="${v === estatus ? 'activo' : ''}">${t}<span class="n">${v ? todas.filter(s => s.estatus === v).length : todas.length}</span></button>`).join('');
        const lista = visibles();
        const cont = $('lista');
        if (!lista.length) { cont.innerHTML = '<div class="vacio">No hay solicitudes con esos filtros. La lista se actualiza sola cada 10 segundos.</div>'; return; }
        cont.innerHTML = lista.map(s => {
            const faltan = diasPara(s.fechaInicio);
            return `<article class="panel ${seleccionada && seleccionada.idSolicitud === s.idSolicitud ? 'seleccionada' : ''}" data-id="${s.idSolicitud}" tabindex="0">
                <div class="fila-con-imagen">
                    <img class="miniatura" src="${imagenDestino(s.destino)}" alt="">
                    <div>
                        <div class="fila-titulo"><h3>${esc(s.destino)}</h3>${badge(s.estatus)}</div>
                        <div class="meta">#${s.idSolicitud} · <strong>${esc(s.clienteNombre)}</strong> · ${fecha(s.fechaInicio)} al ${fecha(s.fechaFin)} · ${s.numPersonas} pers.</div>
                        <div class="meta">${s.presupuesto != null ? 'Presupuesto ' + dinero(s.presupuesto) + ' · ' : ''}Recibida ${fechaHora(s.fechaCreacion)}${s.agente ? ' · Agente: ' + esc(s.agente) : ''}</div>
                        ${s.estatus === 'NUEVA' && faltan <= 0 ? `<div class="edad" style="color:#b3282a">Fecha vencida: ya no se puede cotizar</div>`
                            : s.estatus === 'NUEVA' && faltan <= 7 ? `<div class="edad">El viaje es en ${faltan} día(s)</div>` : ''}
                    </div>
                </div>
            </article>`;
        }).join('');
    }

    function abrir(id) {
        const s = todas.find(x => x.idSolicitud === id);
        if (!s) return;
        seleccionada = s;
        partidas = new Map();
        pintarLista();
        pintarCotizador();
        if (window.innerWidth < 900) $('cotizador').scrollIntoView({ behavior: 'smooth' });
    }

    $('lista').addEventListener('click', ev => {
        const tarjeta = ev.target.closest('[data-id]');
        if (tarjeta) abrir(Number(tarjeta.dataset.id));
    });
    $('lista').addEventListener('keydown', ev => {
        const tarjeta = ev.target.closest('[data-id]');
        if (tarjeta && (ev.key === 'Enter' || ev.key === ' ')) { ev.preventDefault(); abrir(Number(tarjeta.dataset.id)); }
    });
    $('pestanas').addEventListener('click', ev => {
        const b = ev.target.closest('[data-estatus]');
        if (b) { estatus = b.dataset.estatus; pintarLista(); }
    });
    $('buscar').addEventListener('input', pintarLista);
    $('orden').addEventListener('change', pintarLista);

    // ------------------------------------------------ cotizador
    // Cantidad sugerida según cómo se cobra el servicio
    function cantidadSugerida(servicio) {
        if (servicio.unidad.includes('noche')) return nochesEntre(seleccionada.fechaInicio, seleccionada.fechaFin);
        if (servicio.unidad.includes('persona')) return seleccionada.numPersonas;
        return 1;
    }

    function htmlDetalleSolicitud(s) {
        return `<div class="banda-destino" style="background-image:url('${imagenDestino(s.destino)}')"></div>
            <div class="fila-titulo"><h3>Solicitud #${s.idSolicitud}: ${esc(s.destino)}</h3>${badge(s.estatus)}</div>
            <div class="datos-grid" style="margin:.6rem 0">
                <div><span>Cliente</span>${esc(s.clienteNombre)} (${esc(s.clienteUsuario)})</div>
                <div><span>Correo</span><a href="mailto:${esc(s.clienteCorreo)}">${esc(s.clienteCorreo)}</a></div>
                <div><span>Fechas</span>${fecha(s.fechaInicio)} al ${fecha(s.fechaFin)} (${nochesEntre(s.fechaInicio, s.fechaFin)} noches)</div>
                <div><span>Personas</span>${s.numPersonas}</div>
                <div><span>Presupuesto</span>${s.presupuesto != null ? dinero(s.presupuesto) : 'No indicó'}</div>
            </div>
            ${s.necesidades ? `<p class="aviso aviso-ambar" style="margin-top:0"><strong>Necesidades:</strong> ${esc(s.necesidades)}</p>` : ''}`;
    }

    function htmlHistorial(s) {
        if (!s.cotizaciones.length) return '';
        return `<h3 style="margin-top:1.2rem">Cotizaciones enviadas</h3>` + s.cotizaciones.map(c => `
            <div class="fila-titulo meta" style="border-top:1px solid var(--borde);padding-top:.5rem">
                <span>#${c.idCotizacion} · ${esc(c.agente)} · ${fechaHora(c.fechaCreacion)} · <strong>${dinero(c.total)}</strong>${c.folio ? ' · Folio ' + esc(c.folio) : ''}
                <br>${c.detalles.map(d => esc(d.servicio) + ' ×' + d.cantidad).join(', ')}
                · <a href="cotizacion.html?solicitud=${s.idSolicitud}&cotizacion=${c.idCotizacion}">Ver comprobante</a></span>
                ${badge(c.estatusReservacion || c.estatus)}</div>`).join('');
    }

    async function pintarCotizador() {
        const s = seleccionada;
        const panel = $('cotizador');
        const vencida = diasPara(s.fechaInicio) <= 0; // el backend no deja cotizar un viaje cuya fecha ya llegó
        const editable = puedeCotizar && s.estatus === 'NUEVA' && !vencida;
        panel.innerHTML = `<div class="panel">${htmlDetalleSolicitud(s)}
            ${s.estatus === 'NUEVA' && vencida ? `<p class="aviso aviso-rojo">La fecha de inicio de este viaje ya llegó: no se puede cotizar. El cliente debe crear una nueva solicitud.</p>` : ''}
            ${editable ? `
            <h3 style="margin-top:1.2rem">Armar cotización</h3>
            <div class="filtros">
                <input type="search" id="cat-texto" placeholder="Buscar servicio" aria-label="Buscar servicio en el catálogo">
                <select id="cat-filtro" aria-label="Tipo de servicio"><option value="">Todos los tipos</option>
                    <option value="HOTEL">Hoteles</option><option value="VUELO">Vuelos</option>
                    <option value="TRANSPORTE">Transporte</option><option value="PAQUETE">Paquetes</option></select>
                <label><input type="checkbox" id="solo-destino" checked> Solo “${esc(s.destino)}”</label>
            </div>
            <div class="catalogo-mini" id="catalogo-mini"><div class="vacio">Cargando catálogo…</div></div>
            <div class="fila-titulo" style="margin-top:1.2rem;align-items:center"><h3>Servicios en la cotización</h3>
                <button type="button" class="btn btn-claro btn-chico" id="btn-limpiar">Quitar todos</button></div>
            <div id="partidas"></div>
            <div class="form-group" style="margin-top:1rem"><label for="notas">Notas para el cliente (opcional)</label>
                <textarea id="notas" maxlength="500" placeholder="Ej. Incluye 4 noches todo incluido y traslado"></textarea></div>
            <div class="resumen"><div><div class="meta">Total estimado</div><div class="total" id="total">${dinero(0)}</div>
                <div class="meta" id="vs-presupuesto"></div><div class="barra-presupuesto" id="barra-presupuesto" hidden><div></div></div></div>
                <button class="btn btn-primario" id="btn-enviar">Enviar cotización al cliente</button></div>
            <div id="msg-cotizacion" class="msg"></div>` : `<div id="msg-cotizacion" class="msg"></div>`}
            ${htmlHistorial(s)}
        </div>`;
        if (!editable) return;

        if (!catalogo.length) {
            try { catalogo = await api('/servicios', { publica: true }); }
            catch (e) { manejarError(e, 'msg-cotizacion'); return; }
        }
        $('cat-texto').addEventListener('input', pintarCatalogoMini);
        $('cat-filtro').addEventListener('change', pintarCatalogoMini);
        $('solo-destino').addEventListener('change', pintarCatalogoMini);
        $('catalogo-mini').addEventListener('click', ev => {
            const b = ev.target.closest('[data-agregar]');
            if (!b) return;
            const servicio = catalogo.find(x => x.idServicio === Number(b.dataset.agregar));
            partidas.set(servicio.idServicio, Math.min(cantidadSugerida(servicio), servicio.cupoDisponible));
            pintarPartidas(); pintarCatalogoMini();
        });
        $('partidas').addEventListener('input', ev => {
            if (!ev.target.dataset.cantidad) return;
            const n = Math.max(1, Math.min(100, Number(ev.target.value) || 1));
            partidas.set(Number(ev.target.dataset.cantidad), n);
            pintarTotal();
        });
        $('partidas').addEventListener('click', ev => {
            const b = ev.target.closest('[data-quitar]');
            if (!b) return;
            partidas.delete(Number(b.dataset.quitar));
            pintarPartidas(); pintarCatalogoMini();
        });
        $('btn-limpiar').addEventListener('click', () => { partidas.clear(); pintarPartidas(); pintarCatalogoMini(); });
        $('btn-enviar').addEventListener('click', enviarCotizacion);
        pintarCatalogoMini();
        pintarPartidas();
    }

    function pintarCatalogoMini() {
        const cat = $('cat-filtro').value;
        const texto = normalizar($('cat-texto').value);
        const soloDestino = $('solo-destino').checked;
        const destino = normalizar(seleccionada.destino);
        const lista = catalogo.filter(s => (!cat || s.categoria === cat)
            && (!texto || normalizar(s.nombre + ' ' + s.proveedor + ' ' + s.destino).includes(texto))
            && (!soloDestino || normalizar(s.destino).includes(destino) || destino.includes(normalizar(s.destino))));
        const cont = $('catalogo-mini');
        if (!lista.length) {
            cont.innerHTML = `<div class="vacio" style="border:none">No hay servicios con esos filtros. Quita la marca de “Solo…” para ver todo el catálogo.</div>`;
            return;
        }
        cont.innerHTML = `<table class="tabla"><tbody>${lista.map(s => `<tr>
            <td><strong>${esc(s.nombre)}</strong><div class="meta">${esc(s.destino)} · ${esc(s.proveedor)} · ${s.cupoDisponible} disp. · sugerido ×${cantidadSugerida(s)}</div></td>
            <td class="num">${dinero(s.precio)}<div class="meta">${esc(s.unidad)}</div></td>
            <td class="num">${partidas.has(s.idServicio) ? '<span class="meta">✔ Agregado</span>'
                : `<button class="btn btn-claro btn-chico" data-agregar="${s.idServicio}" ${s.cupoDisponible < 1 ? 'disabled title="Agotado"' : ''}>+ Agregar</button>`}</td>
        </tr>`).join('')}</tbody></table>`;
    }

    function pintarPartidas() {
        const cont = $('partidas');
        if (!partidas.size) { cont.innerHTML = '<p class="meta">Agrega servicios desde el catálogo de arriba.</p>'; pintarTotal(); return; }
        cont.innerHTML = `<div class="tabla-wrap"><table class="tabla">
            <thead><tr><th>Servicio</th><th class="num">Precio</th><th class="num">Cantidad</th><th class="num">Subtotal</th><th></th></tr></thead>
            <tbody>${[...partidas].map(([id, cant]) => {
                const s = catalogo.find(x => x.idServicio === id);
                return `<tr><td>${esc(s.nombre)}<div class="meta">Máx. ${s.cupoDisponible} disponibles</div></td>
                    <td class="num">${dinero(s.precio)}<div class="meta">${esc(s.unidad)}</div></td>
                    <td class="num"><input type="number" min="1" max="${Math.min(100, s.cupoDisponible)}" value="${cant}" data-cantidad="${id}" aria-label="Cantidad de ${esc(s.nombre)}"></td>
                    <td class="num" data-subtotal="${id}">${dinero(s.precio * cant)}</td>
                    <td class="num"><button class="btn btn-peligro btn-chico" data-quitar="${id}" aria-label="Quitar ${esc(s.nombre)}">✕</button></td></tr>`;
            }).join('')}</tbody></table></div>`;
        pintarTotal();
    }

    // El total aquí es solo una vista previa: el backend lo vuelve a calcular con los precios oficiales.
    function pintarTotal() {
        let total = 0;
        for (const [id, cant] of partidas) {
            const s = catalogo.find(x => x.idServicio === id);
            total += s.precio * cant;
            const celda = document.querySelector(`[data-subtotal="${id}"]`);
            if (celda) celda.textContent = dinero(s.precio * cant);
        }
        $('total').textContent = dinero(total);
        const vs = $('vs-presupuesto');
        const barra = $('barra-presupuesto');
        const p = seleccionada.presupuesto;
        if (p == null || !partidas.size) { vs.textContent = ''; barra.hidden = true; return; }
        const dentro = total <= p;
        vs.textContent = dentro ? `Dentro del presupuesto (sobran ${dinero(p - total)})` : `Excede el presupuesto por ${dinero(total - p)}`;
        vs.style.color = dentro ? 'var(--verde)' : '#b3282a';
        barra.hidden = false;
        barra.firstElementChild.style.width = Math.min(100, p > 0 ? total / p * 100 : 100) + '%';
        barra.firstElementChild.style.background = dentro ? 'var(--verde)' : 'var(--coral)';
    }

    async function enviarCotizacion(ev) {
        if (!partidas.size) { mostrarMensaje('msg-cotizacion', 'Agrega al menos un servicio.', 'error'); return; }
        const boton = ev.target;
        const si = await preguntar('¿Enviar la cotización?', `Se enviará a ${seleccionada.clienteNombre} con ${partidas.size} servicio(s) por ${$('total').textContent}.`, { aceptar: 'Enviar' });
        if (!si) return;
        boton.disabled = true;
        try {
            const c = await enviar('/cotizaciones', 'POST', {
                idSolicitud: seleccionada.idSolicitud,
                notas: $('notas').value.trim(),
                detalles: [...partidas].map(([idServicio, cantidad]) => ({ idServicio, cantidad }))
            });
            seleccionada = null;
            catalogo = []; // se vuelve a pedir para ver el cupo actualizado
            $('cotizador').innerHTML =
                `<div class="panel"><p class="aviso aviso-verde" style="margin-top:0">Cotización #${c.idCotizacion} enviada por ${dinero(c.total)}. El cliente ya puede verla en “Mis viajes”.</p></div>`;
            toast('Cotización enviada al cliente.');
            cargarLista();
        } catch (e) { manejarError(e, 'msg-cotizacion'); boton.disabled = false; }
    }

    cargarLista();
    refrescarCada(10, cargarLista);
}
