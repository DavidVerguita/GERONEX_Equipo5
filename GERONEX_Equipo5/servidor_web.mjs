// =====================================================================
// GERONEX - Servidor local para abrir la web SIN IntelliJ (opcional)
//
// Uso (con el backend encendido):
//     node servidor_web.mjs
// y abre http://localhost:8090 en el navegador.
//
// Sirve las páginas de esta carpeta y reenvía /api/* al backend (puerto 8080).
// Como la página y la API salen del mismo origen, no hace falta tocar CORS_ORIGENES.
// Útil para probar en otra computadora (por ejemplo, la de la escuela).
// =====================================================================
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const PUERTO = Number(process.env.PUERTO || 8090);
const BACKEND = process.env.BACKEND || 'http://localhost:8080';
const RAIZ = dirname(fileURLToPath(import.meta.url));
const TIPOS = {
    '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
    '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp',
    '.ico': 'image/x-icon', '.json': 'application/json'
};
// Esta línea hace que geronex.js use /api (mismo origen) en lugar de http://localhost:8080/api
const AJUSTE_API = '<script>window.GERONEX_API = "/api";</script>';

async function reenviarApi(req, res) {
    const cuerpo = [];
    for await (const parte of req) cuerpo.push(parte);
    const cabeceras = {};
    if (req.headers.authorization) cabeceras.authorization = req.headers.authorization;
    if (req.headers['content-type']) cabeceras['content-type'] = req.headers['content-type'];
    try {
        const r = await fetch(BACKEND + req.url, {
            method: req.method, headers: cabeceras,
            body: ['GET', 'HEAD'].includes(req.method) ? undefined : Buffer.concat(cuerpo)
        });
        const datos = Buffer.from(await r.arrayBuffer());
        res.writeHead(r.status, { 'content-type': r.headers.get('content-type') || 'application/json' });
        res.end(datos);
    } catch {
        res.writeHead(502, { 'content-type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ estado: 502, mensaje: 'El backend no responde en ' + BACKEND, errores: {}, fecha: new Date() }));
    }
}

async function servirArchivo(req, res) {
    let ruta = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    if (ruta === '/') ruta = '/index.html';
    const archivo = normalize(join(RAIZ, ruta));
    // No se sirve nada fuera de esta carpeta, ni el backend ni archivos ocultos
    if (!archivo.startsWith(RAIZ) || /[\\/](\.|geronex-backend)/.test(archivo.slice(RAIZ.length))) {
        res.writeHead(404); res.end('No encontrado'); return;
    }
    try {
        let contenido = await readFile(archivo);
        const tipo = TIPOS[extname(archivo).toLowerCase()] || 'application/octet-stream';
        if (tipo.startsWith('text/html')) {
            contenido = contenido.toString('utf8').replace('<script src="js/geronex.js">', AJUSTE_API + '<script src="js/geronex.js">');
        }
        res.writeHead(200, { 'content-type': tipo, 'cache-control': 'no-store' });
        res.end(contenido);
    } catch {
        res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' }); res.end('No encontrado');
    }
}

createServer((req, res) => (req.url.startsWith('/api/') ? reenviarApi(req, res) : servirArchivo(req, res)))
    .listen(PUERTO, () => console.log(`GERONEX web en http://localhost:${PUERTO}  (API → ${BACKEND})`));
