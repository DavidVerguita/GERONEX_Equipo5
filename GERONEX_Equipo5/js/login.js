// Lógica de login.html (las utilidades compartidas están en geronex.js)
prepararNav();
// Si venía del catálogo con un destino, al entrar como cliente se lleva a "Mis viajes" con ese destino
const destinoPendiente = new URLSearchParams(location.search).get('destino');
function destinoTras(rol) {
    if (rol === 'CLIENTE' && destinoPendiente) return 'cliente.html?destino=' + encodeURIComponent(destinoPendiente);
    return inicioPorRol(rol);
}
if (Sesion.token() && Sesion.usuario()) location.href = destinoTras(Sesion.rol());

// ------------------------------------------------ pestañas
function mostrarPestana(cual) {
    const registro = cual === 'registro';
    document.getElementById('form-login').hidden = registro;
    document.getElementById('form-registro').hidden = !registro;
    document.getElementById('tab-entrar').classList.toggle('activo', !registro);
    document.getElementById('tab-registro').classList.toggle('activo', registro);
    document.getElementById('tab-entrar').setAttribute('aria-selected', String(!registro));
    document.getElementById('tab-registro').setAttribute('aria-selected', String(registro));
    document.getElementById(registro ? 'reg-user' : 'login-user').focus();
}
document.getElementById('tab-entrar').onclick = () => mostrarPestana('entrar');
document.getElementById('tab-registro').onclick = () => mostrarPestana('registro');
document.querySelectorAll('[data-ir]').forEach(a => a.addEventListener('click', ev => { ev.preventDefault(); mostrarPestana(a.dataset.ir); }));
if (location.hash === '#registro') mostrarPestana('registro');

// Mostrar u ocultar la contraseña
document.querySelectorAll('[data-ver]').forEach(b => b.addEventListener('click', () => {
    const input = document.getElementById(b.dataset.ver);
    const ver = input.type === 'password';
    input.type = ver ? 'text' : 'password';
    b.textContent = ver ? 'Ocultar' : 'Ver';
}));

// Indicador de qué tan segura es la contraseña (solo orienta; la regla real es 8 a 50)
document.getElementById('reg-pass').addEventListener('input', ev => {
    const v = ev.target.value;
    let puntos = 0;
    if (v.length >= 8) puntos++;
    if (v.length >= 12) puntos++;
    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) puntos++;
    if (/\d/.test(v)) puntos++;
    if (/[^A-Za-z0-9]/.test(v)) puntos++;
    const niveles = [['0%', '#ccc', 'Usa letras, números y algún símbolo.'], ['25%', '#EA5455', 'Débil'], ['50%', '#F07B3F', 'Regular'],
        ['75%', '#e0a030', 'Buena'], ['100%', '#1f7a4d', 'Muy segura']];
    const [ancho, color, texto] = niveles[Math.min(4, v.length < 8 ? (v ? 1 : 0) : Math.max(1, puntos - 1))];
    const barra = document.getElementById('fuerza-barra');
    barra.style.width = ancho; barra.style.background = color;
    document.getElementById('fuerza-texto').textContent = v && v.length < 8 ? 'Faltan ' + (8 - v.length) + ' caracteres.' : texto;
});

// ------------------------------------------------ envío
async function iniciarSesion(nombreUsuario, contrasena) {
    const r = await api("/auth/login", { method: "POST", body: JSON.stringify({ nombreUsuario, contrasena }) });
    Sesion.guardar(r.token, r.usuario);
    location.href = destinoTras(r.usuario.rol); // cada rol entra a su página de trabajo
}

async function handleLogin(e) {
    e.preventDefault();
    const boton = botonDe(e); boton.disabled = true;
    ocultarMensaje("login-msg");
    try {
        await iniciarSesion(document.getElementById("login-user").value.trim(), document.getElementById("login-pass").value);
    } catch (err) {
        mostrarMensaje("login-msg", textoError(err), "error");
        boton.disabled = false;
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const usuario = document.getElementById("reg-user").value.trim();
    const correo = document.getElementById("reg-correo").value.trim();
    const pass = document.getElementById("reg-pass").value;
    const errorDiv = document.getElementById("pass-error");
    if (pass !== document.getElementById("reg-pass-confirm").value) {
        errorDiv.style.display = "block";
        return;
    }
    errorDiv.style.display = "none";
    const boton = botonDe(e); boton.disabled = true;
    try {
        await api("/auth/registro", { method: "POST", body: JSON.stringify({ nombreUsuario: usuario, correo, contrasena: pass }) });
    } catch (err) {
        mostrarMensaje("reg-msg", textoError(err), "error");
        boton.disabled = false;
        return;
    }
    try {
        await iniciarSesion(usuario, pass); // cuenta creada: entra directo
    } catch (err) {
        mostrarMensaje("reg-msg", "Tu cuenta se creó. Ahora inicia sesión.", "ok");
        boton.disabled = false;
    }
}

// Formularios de entrar y registrarse
document.getElementById('form-login').addEventListener('submit', handleLogin);
document.getElementById('form-registro').addEventListener('submit', handleRegister);
