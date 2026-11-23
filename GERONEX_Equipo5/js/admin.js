// Lógica de admin.html (las utilidades compartidas están en geronex.js)
if (requerirRol('ADMINISTRADOR')) {
    pintarNav();
    const yo = Sesion.usuario().idUsuario;
    let agentes = [];
    let cuentas = [];
    document.getElementById('f-rol').insertAdjacentHTML('beforeend', Object.entries(NOMBRE_ROL).map(([v, t]) => `<option value="${v}">${esc(t)}</option>`).join(''));
    for (const id of ['f-texto', 'f-rol', 'f-estatus']) document.getElementById(id).addEventListener(id === 'f-texto' ? 'input' : 'change', pintarCuentas);

    const selRol = document.getElementById('n-rol');
    const grupoTel = document.getElementById('grupo-telefono');
    selRol.addEventListener('change', () => { grupoTel.style.visibility = selRol.value === 'AGENTE' ? 'visible' : 'hidden'; });

    document.getElementById('form-crear').addEventListener('submit', async ev => {
        ev.preventDefault();
        const boton = botonDe(ev); boton.disabled = true;
        try {
            const u = await enviar('/admin/usuarios', 'POST', {
                nombreUsuario: document.getElementById('n-usuario').value.trim(),
                correo: document.getElementById('n-correo').value.trim(),
                contrasena: document.getElementById('n-pass').value,
                rol: selRol.value,
                nombre: document.getElementById('n-nombre').value.trim(),
                telefono: selRol.value === 'AGENTE' ? document.getElementById('n-telefono').value.trim() : ''
            });
            ev.target.reset();
            grupoTel.style.visibility = 'visible';
            ocultarMensaje('msg-crear');
            toast(`Cuenta "${u.nombreUsuario}" creada con rol ${NOMBRE_ROL[u.rol] || u.rol}.`);
            cargar();
        } catch (e) { manejarError(e, 'msg-crear'); }
        finally { boton.disabled = false; }
    });

    function pintarAgentes() {
        const cont = document.getElementById('agentes');
        if (!agentes.length) { cont.innerHTML = '<p class="meta">Aún no hay agentes. Crea una cuenta con rol Agente.</p>'; return; }
        cont.innerHTML = `<div class="tabla-wrap"><table class="tabla">
            <thead><tr><th>Nombre</th><th>Usuario</th><th>Contacto</th><th>Estatus</th><th></th></tr></thead>
            <tbody>${agentes.map(a => `<tr>
                <td><strong>${esc(a.nombre)}</strong></td>
                <td>${esc(a.nombreUsuario)}</td>
                <td>${esc(a.correo)}<div class="meta">${esc(a.telefono || 'Sin teléfono')}</div></td>
                <td>${badge(a.estatus)}</td>
                <td class="num" style="white-space:nowrap">
                    <button class="btn btn-claro btn-chico" data-editar="${a.idAgente}">Editar</button>
                    ${a.estatus === 'ACTIVO'
                        ? `<button class="btn btn-peligro btn-chico" data-estatus-agente="${a.idAgente}" data-valor="INACTIVO">Dar de baja</button>`
                        : `<button class="btn btn-verde btn-chico" data-estatus-agente="${a.idAgente}" data-valor="ACTIVO">Reactivar</button>`}
                </td></tr>`).join('')}</tbody></table></div>`;
    }

    function pintarCuentas() {
        const texto = normalizar(document.getElementById('f-texto').value);
        const rol = document.getElementById('f-rol').value;
        const est = document.getElementById('f-estatus').value;
        const lista = cuentas.filter(u => (!rol || u.rol === rol) && (!est || u.estatus === est)
            && (!texto || normalizar(u.nombreUsuario + ' ' + (u.nombre || '') + ' ' + u.correo).includes(texto)));
        document.getElementById('contador').textContent = `${lista.length} de ${cuentas.length} cuentas`;
        if (!lista.length) { document.getElementById('cuentas').innerHTML = '<p class="meta">Ninguna cuenta coincide con los filtros.</p>'; return; }
        document.getElementById('cuentas').innerHTML = `<div class="tabla-wrap"><table class="tabla">
            <thead><tr><th>#</th><th>Usuario</th><th>Nombre</th><th>Correo</th><th>Rol</th><th>Alta</th><th>Estatus</th></tr></thead>
            <tbody>${lista.map(u => `<tr>
                <td>${u.idUsuario}</td>
                <td><strong>${esc(u.nombreUsuario)}</strong></td>
                <td>${esc(u.nombre || '')}</td>
                <td>${esc(u.correo)}</td>
                <td>${esc(NOMBRE_ROL[u.rol] || u.rol)}</td>
                <td>${fecha(u.fechaCreacion)}</td>
                <td>${u.idUsuario === yo ? badge(u.estatus) + ' <span class="meta">(tú)</span>'
                    : `<select data-estatus-cuenta="${u.idUsuario}" aria-label="Estatus de ${esc(u.nombreUsuario)}">
                        ${['ACTIVO', 'INACTIVO', 'BLOQUEADO'].map(e => `<option value="${e}" ${e === u.estatus ? 'selected' : ''}>${TEXTO_ESTATUS[e]}</option>`).join('')}
                       </select>`}</td></tr>`).join('')}</tbody></table></div>`;
    }

    function pintarResumen() {
        document.getElementById('resumen').innerHTML = htmlResumen([
            { valor: cuentas.length, texto: 'Cuentas registradas', tono: 'azul' },
            { valor: cuentas.filter(u => u.rol === 'CLIENTE').length, texto: 'Clientes', tono: 'naranja' },
            { valor: agentes.filter(a => a.estatus === 'ACTIVO').length, texto: 'Agentes activos', tono: 'verde' },
            { valor: cuentas.filter(u => u.estatus !== 'ACTIVO').length, texto: 'Cuentas inactivas o bloqueadas', tono: 'rojo' }
        ]) + htmlResumen([
            { valor: operacion.servicios, texto: 'Servicios en el catálogo', tono: 'azul' },
            { valor: operacion.porCotizar, texto: 'Solicitudes por cotizar', tono: 'naranja' },
            { valor: operacion.porValidar, texto: 'Reservaciones por validar', tono: 'ambar' },
            { valor: dinero(operacion.vendido), texto: 'Vendido (confirmadas)', tono: 'verde' }
        ]);
    }

    // Panel administrativo (RF-08.1): además de las cuentas, un resumen de la operación
    let operacion = { servicios: '—', porCotizar: '—', porValidar: '—', vendido: 0 };
    async function cargarOperacion() {
        try {
            const [servicios, solicitudes, reservaciones] = await Promise.all([
                api('/servicios', { publica: true }), api('/solicitudes'), api('/reservaciones')]);
            operacion = {
                servicios: servicios.length,
                porCotizar: solicitudes.filter(s => s.estatus === 'NUEVA').length,
                porValidar: reservaciones.filter(r => r.estatus === 'POR_VALIDAR').length,
                vendido: reservaciones.filter(r => r.estatus === 'CONFIRMADA').reduce((t, r) => t + Number(r.total), 0)
            };
        } catch (e) { /* el resumen de operación es informativo; si falla, se quedan los guiones */ }
    }

    async function cargar() {
        try {
            const [listaAgentes, listaCuentas] = await Promise.all([api('/agentes'), api('/admin/usuarios'), cargarOperacion()]);
            agentes = listaAgentes;
            cuentas = listaCuentas;
            pintarAgentes();
            pintarCuentas();
            pintarResumen();
        } catch (e) { manejarError(e, 'msg-general'); }
    }

    // Editar agente
    const panelEditar = document.getElementById('panel-editar');
    document.getElementById('agentes').addEventListener('click', async ev => {
        const editar = ev.target.closest('[data-editar]');
        if (editar) {
            const a = agentes.find(x => x.idAgente === Number(editar.dataset.editar));
            document.getElementById('e-id').value = a.idAgente;
            document.getElementById('e-nombre').value = a.nombre;
            document.getElementById('e-correo').value = a.correo;
            document.getElementById('e-telefono').value = a.telefono || '';
            ocultarMensaje('msg-editar');
            panelEditar.classList.add('visible');
            document.getElementById('e-nombre').focus();
            return;
        }
        const cambiar = ev.target.closest('[data-estatus-agente]');
        if (!cambiar) return;
        const baja = cambiar.dataset.valor === 'INACTIVO';
        if (baja && !(await preguntar('¿Dar de baja a este agente?', 'Su cuenta quedará inactiva y no podrá iniciar sesión. Puedes reactivarlo después.', { aceptar: 'Dar de baja', peligro: true }))) return;
        cambiar.disabled = true;
        try {
            const a = await enviar(`/agentes/${cambiar.dataset.estatusAgente}/estatus`, 'PATCH', { estatus: cambiar.dataset.valor });
            toast(`${a.nombre} ahora está ${baja ? 'dado de baja' : 'activo'}.`);
            cargar();
        } catch (e) { manejarError(e, 'msg-general'); cambiar.disabled = false; }
    });

    document.getElementById('btn-cancelar-edicion').addEventListener('click', () => panelEditar.classList.remove('visible'));

    document.getElementById('form-editar').addEventListener('submit', async ev => {
        ev.preventDefault();
        const boton = botonDe(ev); boton.disabled = true;
        try {
            const a = await enviar('/agentes/' + document.getElementById('e-id').value, 'PUT', {
                nombre: document.getElementById('e-nombre').value.trim(),
                correo: document.getElementById('e-correo').value.trim(),
                telefono: document.getElementById('e-telefono').value.trim()
            });
            panelEditar.classList.remove('visible');
            toast(`Se guardaron los datos de ${a.nombre}.`);
            cargar();
        } catch (e) { manejarError(e, 'msg-editar'); }
        finally { boton.disabled = false; }
    });

    // Estatus de cuentas
    document.getElementById('cuentas').addEventListener('change', async ev => {
        const sel = ev.target.closest('[data-estatus-cuenta]');
        if (!sel) return;
        sel.disabled = true;
        try {
            const u = await enviar(`/admin/usuarios/${sel.dataset.estatusCuenta}/estatus`, 'PATCH', { estatus: sel.value });
            toast(`La cuenta "${u.nombreUsuario}" ahora está ${TEXTO_ESTATUS[u.estatus].toLowerCase()}.`);
            cargar();
        } catch (e) { manejarError(e, 'msg-general'); cargar(); }
    });

    cargar();
}
