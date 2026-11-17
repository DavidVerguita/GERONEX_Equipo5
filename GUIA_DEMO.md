# Guía de la demostración (una sola computadora + teléfono)

Cada pestaña del navegador guarda su propia sesión, así que puedes tener a varias personas
conectadas al mismo tiempo en la misma laptop. **Importante:** abre cada pestaña nueva desde cero
(Ctrl+T y pega la dirección). No uses "abrir en pestaña nueva" sobre un enlace, porque eso copia la sesión.

## Antes de presentar
1. Enciende MySQL y el backend en IntelliJ (debe decir "Started GeronexApplication").
2. Abre la web: con el navegador integrado de IntelliJ, o con `node servidor_web.mjs` (en `GERONEX_Equipo5`)
   y luego http://localhost:8090.
3. Ten listas las cuentas: `admin`, `agente1`, `reserva1` y `cliente1` (contraseña de demo).
4. Comprueba que todo responde: en `geronex-backend/pruebas` corre `node prueba_api.mjs` (debe decir 92 pasaron).
5. Teléfono (Android 16, depuración USB activada):
   - Conecta el cable y ejecuta `adb reverse tcp:8080 tcp:8080` (se repite cada vez que reconectas el cable).
   - Instala la app desde Android Studio con **Run ▶** (o `adb install -r app\build\outputs\apk\debug\app-debug.apk`).
   - Opcional: proyecta la pantalla del teléfono con scrcpy.

## Guion sugerido (unos 10 minutos)
| Paso | Dónde | Qué se hace | Qué se ve |
|---|---|---|---|
| 1 | Web, sin sesión | Portada, "Destinos populares", filtros por tipo, precio y orden; detalle de un servicio | Cualquiera consulta sin cuenta; imágenes propias sin internet |
| 2 | Web, admin | Crea la cuenta de un agente | Aparece en la tabla de agentes y en el resumen |
| 3 | **Teléfono**, cliente | "Pedir un viaje" → destino Cancún, fechas con el calendario, personas | "¡Solicitud enviada!" |
| 4 | Web, agente | La solicitud aparece sola (aviso "Llegó una nueva solicitud"); arma la cotización | Cantidades sugeridas y barra contra el presupuesto |
| 5 | **Teléfono**, cliente | Inicio muestra "Tienes 1 cotización por responder"; en Mis viajes la acepta | Folio GX-… y progreso en 4 pasos |
| 6 | Web, responsable | Ve la disponibilidad de cada servicio y confirma | Se descuenta el cupo |
| 7 | **Teléfono**, cliente | Mis reservaciones → comprobante: cuenta regresiva, compartir, copiar folio | Estatus Confirmada |
| 8 | Web, cliente | Abre el comprobante; imprímelo o guárdalo en PDF | Comprobante con logo e imagen del destino |
| 9 | Teléfono o web | Cancela | Cargo del 5% y el cupo regresa |

Extras para lucirse:
- Abre `admin.html` con la sesión del cliente: te regresa a su página.
- Rechaza una reservación con un motivo frecuente (un clic) y muestra que la solicitud regresa al agente.
- Entra a la app con una cuenta de empleado: la app avisa que es para clientes.
- Una solicitud con fecha vencida: la web no deja cotizarla (y el backend tampoco).
