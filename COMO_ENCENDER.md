# Cómo encender GERONEX (backend, web y Android)

Siempre en este orden: **MySQL → backend → web → teléfono**. Si el backend no está encendido, la web y la app no cargan nada.

Cuentas de demo (contraseña `Demo12345`): `admin`, `agente1`, `reserva1` y `cliente1`.

---

## Paso 1. MySQL
MySQL arranca solo con Windows (servicio `MySQL80`, inicio automático).

**Para comprobarlo:** en PowerShell, corre esto:
```powershell
Get-Service MySQL80
```
Debe decir `Running`. Si dice `Stopped`, abre PowerShell **como administrador** y corre:
```powershell
Start-Service MySQL80
```

## Paso 2. Backend (IntelliJ)
1. Abre IntelliJ con el proyecto `GERONEX_Equipo5`.
2. Arriba a la derecha, elige la configuración **GeronexApplication** y pulsa el botón verde **Run ▶**.
3. Espera a que en la consola de abajo salga `Tomcat started on port 8080` y `Started GeronexApplication`.

**Para comprobarlo:** abre http://localhost:8080/api/salud en el navegador. Debe verse:
`{"estado":"OK","baseDeDatos":"geronex",...,"rolesRegistrados":7}`

> Las variables `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y `CORS_ORIGENES` ya están guardadas en esa configuración de IntelliJ. No hay que tocarlas.

## Paso 3. Web
Elige **una** de las dos formas.

**Forma A (la más fácil):** doble clic en **`1_abrir_web.cmd`**, que está en la carpeta `Geronex2`.
- Se abre una ventana negra que dice `GERONEX web en http://localhost:8090` y el navegador se abre solo en esa dirección.
- **No cierres la ventana negra** mientras presentas. Al cerrarla, se apaga la web.

**Forma B (desde IntelliJ):** abre `index.html`, haz clic derecho y elige **Open In → Browser**. Se abre en `http://localhost:63342/...`.
(En la versión gratuita de IntelliJ no se puede cambiar ese puerto. Si quieres otro, usa la forma A.)

**Para comprobarlo:** debe verse la portada con los contadores de destinos, servicios y proveedores (por ejemplo 5 / 14 / 5). Si salen en 0 o aparece un error, el backend no está encendido (vuelve al paso 2).

**Varios roles en la misma laptop:** cada pestaña guarda su propia sesión. Abre cada pestaña con Ctrl+T y pega la dirección; no uses "abrir en pestaña nueva" sobre un enlace.

## Paso 4. App Android (teléfono por USB)
1. En el teléfono debe estar activada la **depuración USB** (Opciones de desarrollador).
2. Conecta el cable. Si el teléfono pregunta "¿Permitir depuración USB?", pulsa **Permitir**.
3. Doble clic en **`2_conectar_telefono.cmd`** (carpeta `Geronex2`).
   - Debe aparecer tu teléfono en la lista y el mensaje `Listo: en el telefono, localhost:8080 ahora llega a esta computadora`.
   - Te pregunta si quieres instalar la app: escribe **S** la primera vez (o si cambiaste el código). Debe terminar en `Success`.
4. Abre la app **GERONEX** en el teléfono e inicia sesión con `cliente1`.

> **Cada vez que desconectes y vuelvas a conectar el cable, repite el paso 3** (el script). Si no lo haces, la app dice que no puede conectarse.

**Otra forma de instalar:** abre `GeronexAndroid` en Android Studio, elige tu teléfono arriba y pulsa **Run ▶**. Aun así, corre `2_conectar_telefono.cmd` para el `adb reverse`.

**Para comprobarlo:** después de iniciar sesión, la pantalla de inicio de la app muestra "Hola, …" con el nombre de la cuenta. Si sale "No se pudo conectar con el servidor", corre otra vez `2_conectar_telefono.cmd`.

---

## Comprobación completa (opcional, 1 minuto)
Con el backend encendido, en PowerShell corre:
```powershell
cd "D:\A S U S\batiz2\GERONEX\geronexes de claude\Geronex2\GERONEX_Equipo5\geronex-backend\pruebas"
```
```powershell
node prueba_api.mjs
```
Al final debe decir `Resultado: 92 pasaron, 0 fallaron.` (Crea datos de prueba con cuentas `qa_...`: no afecta a la demo.)

## Si algo falla
| Qué ves | Qué pasa | Qué hacer |
|---|---|---|
| IntelliJ: `Port 8080 was already in use` | El backend ya está encendido (otra ventana o una corrida anterior) | Detén la corrida anterior con el cuadro rojo ■ o simplemente usa la que ya está |
| IntelliJ: error de conexión a MySQL (`Communications link failure`) | MySQL apagado | Paso 1 |
| Web: portada sin datos o "El backend no responde" | Backend apagado | Paso 2 |
| Web por 63342: error de CORS en la consola (F12) | El origen no está permitido | Usa la forma A (8090), que no necesita CORS |
| La sesión "se cierra sola" | El token dura 60 minutos | Vuelve a iniciar sesión |
| App: "No se pudo conectar" | Falta `adb reverse` o el backend está apagado | Corre `2_conectar_telefono.cmd` y revisa el paso 2 |
| Script del teléfono: `no devices/emulators found` | El teléfono no está autorizado | Revisa el cable, activa la depuración USB y acepta el aviso en el teléfono |
| `1_abrir_web.cmd`: "No se encontró Node.js" | Falta Node (pasaría en otra PC) | Instálalo desde nodejs.org, o usa la forma B |
