// Lógica de cuenta.html (las utilidades compartidas están en geronex.js)
if (!Sesion.token()) location.href = 'login.html';
pintarNav();

function pintarCuenta(u) {
    document.getElementById('d-usuario').textContent = u.nombreUsuario;
    document.getElementById('d-rol').textContent = NOMBRE_ROL[u.rol] || u.rol;
    const f = new Date(u.fechaCreacion);
    document.getElementById('d-fecha').textContent = isNaN(f) ? '' : f.toLocaleDateString('es-MX', { dateStyle: 'long' });
    document.getElementById('c-nombre').value = u.nombre || '';
    document.getElementById('c-correo').value = u.correo;
}

async function cargar() {
    try {
        const u = await api('/usuarios/me');
        Sesion.guardar(Sesion.token(), u);
        pintarCuenta(u);
    } catch (e) {
        if (e.estado === 401) location.href = 'login.html';
        else mostrarMensaje('msg-cuenta', textoError(e), 'error');
    }
}

document.getElementById('form-cuenta').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const boton = botonDe(ev); boton.disabled = true;
    try {
        const u = await api('/usuarios/me', { method: 'PUT', body: JSON.stringify({
            nombre: document.getElementById('c-nombre').value,
            correo: document.getElementById('c-correo').value }) });
        Sesion.guardar(Sesion.token(), u);
        pintarCuenta(u);
        mostrarMensaje('msg-cuenta', 'Tus datos se guardaron.', 'ok');
        pintarNav();
    } catch (e) {
        if (e.estado === 401) location.href = 'login.html';
        else mostrarMensaje('msg-cuenta', textoError(e), 'error');
    } finally { boton.disabled = false; }
});

document.getElementById('form-pass').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const nueva = document.getElementById('p-nueva').value;
    if (nueva !== document.getElementById('p-confirma').value) {
        mostrarMensaje('msg-pass', 'Las contraseñas nuevas no coinciden.', 'error');
        return;
    }
    const boton = botonDe(ev); boton.disabled = true;
    try {
        await api('/usuarios/me/password', { method: 'PATCH', body: JSON.stringify({
            contrasenaActual: document.getElementById('p-actual').value, contrasenaNueva: nueva }) });
        ev.target.reset();
        mostrarMensaje('msg-pass', 'Tu contraseña se cambió.', 'ok');
    } catch (e) {
        if (e.estado === 401) location.href = 'login.html';
        else mostrarMensaje('msg-pass', textoError(e), 'error');
    } finally { boton.disabled = false; }
});

cargar();
