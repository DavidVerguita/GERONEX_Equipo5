// Lógica de index.html (las utilidades compartidas están en geronex.js)
pintarNav();
let catalogo = [];
let categoriaActual = '';
const $ = id => document.getElementById(id);
const capital = t => t.charAt(0) + t.slice(1).toLowerCase();

// Solo los clientes solicitan viajes. Sin sesión, se les invita a entrar.
function irASolicitar(destino) {
    const rol = Sesion.token() ? Sesion.rol() : null;
    if (!rol) {
        toast('Necesitas iniciar sesión o crear una cuenta para solicitar un viaje.', 'info');
        setTimeout(() => { location.href = 'login.html' + (destino ? '?destino=' + encodeURIComponent(destino) : ''); }, 1200);
    } else if (rol !== 'CLIENTE') {
        toast('Esta opción es para clientes. Tu cuenta es de ' + (NOMBRE_ROL[rol] || rol) + '.', 'error');
    } else {
        location.href = 'cliente.html' + (destino ? '?destino=' + encodeURIComponent(destino) : '');
    }
}
$('btn-solicitar').addEventListener('click', ev => { ev.preventDefault(); irASolicitar(); });

// ------------------------------------------------ destinos populares
function pintarDestinos() {
    const porDestino = new Map();
    for (const s of catalogo) {
        const d = porDestino.get(s.destino) || { destino: s.destino, servicios: 0, desde: Infinity };
        d.servicios++;
        d.desde = Math.min(d.desde, Number(s.precio));
        porDestino.set(s.destino, d);
    }
    const lista = [...porDestino.values()].sort((a, b) => b.servicios - a.servicios);
    $('lista-destinos').innerHTML = lista.map(d => `
        <button class="destino" data-ir-destino="${esc(d.destino)}" aria-label="Ver servicios en ${esc(d.destino)}">
            <img src="${imagenDestino(d.destino)}" alt="" loading="lazy">
            <div><h3>${esc(d.destino)}</h3><span>${d.servicios} servicio(s) · desde ${dinero(d.desde)}</span></div>
        </button>`).join('');
    $('hero-datos').innerHTML = `<div><b>${lista.length}</b>destinos</div><div><b>${catalogo.length}</b>servicios</div>
        <div><b>${new Set(catalogo.map(s => s.proveedor)).size}</b>proveedores</div>`;
    for (const sel of [$('f-destino'), $('buscar-destino')]) {
        sel.insertAdjacentHTML('beforeend', lista.map(d => `<option value="${esc(d.destino)}">${esc(d.destino)}</option>`).join(''));
    }
}

$('lista-destinos').addEventListener('click', ev => {
    const b = ev.target.closest('[data-ir-destino]');
    if (!b) return;
    $('f-destino').value = b.dataset.irDestino;
    pintarCatalogo();
    $('catalogo').scrollIntoView({ behavior: 'smooth' });
});

// ------------------------------------------------ categorías
async function cargarCategorias() {
    try {
        const categorias = await api('/categorias', { publica: true });
        const chips = $('chips');
        chips.innerHTML = '<button class="chip activo" data-cat="">🌎 Todos</button>' + categorias.map(c =>
            `<button class="chip" data-cat="${esc(c.nombre)}">${ICONO_CATEGORIA[c.nombre] || ''} ${esc(NOMBRE_CATEGORIA[c.nombre] || capital(c.nombre))}</button>`).join('');
        $('buscar-categoria').insertAdjacentHTML('beforeend', categorias.map(c =>
            `<option value="${esc(c.nombre)}">${esc(NOMBRE_CATEGORIA[c.nombre] || capital(c.nombre))}</option>`).join(''));
    } catch (e) { /* si falla, el catálogo mostrará el error */ }
}
$('chips').addEventListener('click', ev => {
    const chip = ev.target.closest('.chip');
    if (!chip) return;
    categoriaActual = chip.dataset.cat;
    pintarCatalogo();
});

// ------------------------------------------------ catálogo
function filtrados() {
    const texto = normalizar($('f-texto').value);
    const destino = $('f-destino').value;
    const precioMax = Number($('f-precio').value) || Infinity;
    const soloDisponibles = $('f-disponibles').checked;
    const lista = catalogo.filter(s => (!categoriaActual || s.categoria === categoriaActual)
        && (!destino || s.destino === destino)
        && Number(s.precio) <= precioMax
        && (!soloDisponibles || s.cupoDisponible > 0)
        && (!texto || normalizar(s.nombre + ' ' + s.destino + ' ' + s.proveedor + ' ' + (s.descripcion || '')).includes(texto)));
    const orden = $('f-orden').value;
    const comparar = {
        'precio-asc': (a, b) => a.precio - b.precio, 'precio-desc': (a, b) => b.precio - a.precio,
        'nombre': (a, b) => a.nombre.localeCompare(b.nombre, 'es'), 'cupo': (a, b) => b.cupoDisponible - a.cupoDisponible
    }[orden];
    return comparar ? lista.sort(comparar) : lista;
}

function etiquetaCupo(s) {
    if (s.cupoDisponible <= 0) return '<span class="cupo agotado">Agotado</span>';
    if (s.cupoDisponible <= 5) return `<span class="cupo pocos">¡Quedan ${s.cupoDisponible}!</span>`;
    return `<span class="cupo">${s.cupoDisponible} disponibles</span>`;
}

function pintarCatalogo() {
    document.querySelectorAll('.chip').forEach(c => c.classList.toggle('activo', c.dataset.cat === categoriaActual));
    const lista = filtrados();
    $('contador').textContent = `${lista.length} de ${catalogo.length} servicios`;
    if (!lista.length) {
        $('grid').innerHTML = '<div class="vacio" style="grid-column:1/-1">No encontramos servicios con esos filtros. Prueba con otros o <a href="#" data-solicitar>solicita un viaje</a> y un agente te ayuda.</div>';
        return;
    }
    const puedeSolicitar = !Sesion.token() || Sesion.rol() === 'CLIENTE';
    $('grid').innerHTML = lista.map(s => `
        <article class="card">
            <div class="card-imagen"><img src="${imagenDestino(s.destino)}" alt="${esc(s.destino)}" loading="lazy">
                <span class="etiqueta">${ICONO_CATEGORIA[s.categoria] || ''} ${esc(s.categoria)}</span>${etiquetaCupo(s)}</div>
            <div class="card-content">
                <h3>${esc(s.nombre)}</h3>
                <div class="meta" style="margin-bottom:.4rem">📍 ${esc(s.destino)} · ${esc(s.proveedor)}</div>
                <p>${esc(s.descripcion || '')}</p>
                <div class="precio">${dinero(s.precio)} <small>${esc(s.unidad)}</small></div>
                <div class="acciones">
                    <button class="btn btn-claro" data-detalle="${s.idServicio}">Ver detalle</button>
                    ${puedeSolicitar ? `<button class="btn btn-azul" data-destino="${esc(s.destino)}">Solicitar</button>` : ''}
                </div>
            </div>
        </article>`).join('');
}

async function cargarServicios() {
    try {
        catalogo = await api('/servicios', { publica: true });
        ocultarMensaje('msg-catalogo');
        pintarDestinos();
        pintarCatalogo();
    } catch (e) {
        $('grid').innerHTML = '';
        $('lista-destinos').innerHTML = '';
        mostrarMensaje('msg-catalogo', textoError(e), 'error');
    }
}

// Detalle de un servicio en una ventana
function abrirDetalle(id) {
    const s = catalogo.find(x => x.idServicio === id);
    if (!s) return;
    const puedeSolicitar = !Sesion.token() || Sesion.rol() === 'CLIENTE';
    const d = document.createElement('dialog');
    d.className = 'dialogo detalle-servicio';
    d.innerHTML = `<img src="${imagenDestino(s.destino)}" alt="${esc(s.destino)}">
        <div class="cuerpo">
            <div class="fila-titulo"><h3>${esc(s.nombre)}</h3><span class="badge">${ICONO_CATEGORIA[s.categoria] || ''} ${esc(s.categoria)}</span></div>
            <p style="margin:.4rem 0 1rem">${esc(s.descripcion || 'Sin descripción.')}</p>
            <div class="datos-grid">
                <div><span>Destino</span>${esc(s.destino)}</div>
                <div><span>Proveedor</span>${esc(s.proveedor)}</div>
                <div><span>Precio</span><strong>${dinero(s.precio)}</strong> ${esc(s.unidad)}</div>
                <div><span>Disponibilidad</span>${s.cupoDisponible > 0 ? s.cupoDisponible + ' lugares' : 'Agotado'}</div>
            </div>
            <p class="meta" style="margin-top:1rem">El precio final lo confirma tu agente en la cotización, según tus fechas y número de personas.</p>
            <div class="acciones" style="justify-content:flex-end">
                <button class="btn btn-claro" data-cerrar>Cerrar</button>
                ${puedeSolicitar ? `<button class="btn btn-primario" data-destino="${esc(s.destino)}">Solicitar viaje a ${esc(s.destino)}</button>` : ''}
            </div>
        </div>`;
    document.body.appendChild(d);
    const cerrar = () => { d.close(); d.remove(); };
    d.addEventListener('cancel', ev => { ev.preventDefault(); cerrar(); });
    d.addEventListener('click', ev => {
        if (ev.target === d || ev.target.closest('[data-cerrar]')) return cerrar();
        const b = ev.target.closest('[data-destino]');
        if (b) { cerrar(); irASolicitar(b.dataset.destino); }
    });
    d.showModal();
}

$('grid').addEventListener('click', ev => {
    const detalle = ev.target.closest('[data-detalle]');
    if (detalle) return abrirDetalle(Number(detalle.dataset.detalle));
    const boton = ev.target.closest('[data-destino]');
    if (boton) return irASolicitar(boton.dataset.destino);
    if (ev.target.closest('[data-solicitar]')) { ev.preventDefault(); irASolicitar($('f-destino').value); }
});

for (const id of ['f-texto', 'f-precio']) $(id).addEventListener('input', pintarCatalogo);
for (const id of ['f-destino', 'f-orden', 'f-disponibles']) $(id).addEventListener('change', pintarCatalogo);
$('f-limpiar').addEventListener('click', () => {
    $('f-texto').value = ''; $('f-destino').value = ''; $('f-precio').value = ''; $('f-orden').value = '';
    $('f-disponibles').checked = false; categoriaActual = '';
    pintarCatalogo();
});

// El buscador de la portada llena los filtros del catálogo
$('form-buscar').addEventListener('submit', ev => {
    ev.preventDefault();
    categoriaActual = $('buscar-categoria').value;
    $('f-texto').value = $('buscar-texto').value;
    $('f-destino').value = $('buscar-destino').value;
    pintarCatalogo();
    $('catalogo').scrollIntoView({ behavior: 'smooth' });
});

cargarCategorias().then(cargarServicios);
