# GERONEX: preparación final (23 de noviembre de 2026, madrugada)

## Web
- Cada HTML quedó limpio: sus bloques `<style>` y `<script>` pasaron a `css/<página>.css` y `js/<página>.js`;
  los `style="..."` sueltos, a clases en `geronex.css`; los `onsubmit` del login, a `addEventListener`.
  **Comprobado:** las 9 páginas se ven idénticas píxel por píxel a la versión anterior, y las pruebas siguen 92/92 y 24/24.
- `geronex.js` usa `/api` del mismo sitio salvo en IntelliJ (63342): así funciona igual en el 8090 y en Azure.
- `CORS_ORIGENES` ahora acepta por defecto 63342 y 8090 aunque no se defina la variable.

## Azure
- `azure/subir_a_azure.ps1` + `azure/instalar_en_vm.sh`: nginx (80) + backend como servicio (127.0.0.1:8080) + MySQL.
  Probado en la laptop hasta juntar los archivos (`-SoloPreparar`); **falta correrlo en la VM** (estaba apagada). Ver GUIA_AZURE.md.

## Android
- Variante `azure` (`app-azure.apk`, http://20.69.91.86/api) además de `debug` (localhost). Compiladas las dos; **no probadas en teléfono**.

## Documentos
- `docs/Presentacion_GERONEX_Final.pptx` (19 diapositivas, guion en las notas del orador), GUIA_PROYECTO.md,
  GUIA_CODIGO.md, GUIA_AZURE.md, COMO_ENCENDER.md y los scripts `1_abrir_web.cmd` y `2_conectar_telefono.cmd`.

---

# GERONEX: documentación y casos de prueba (17 de noviembre de 2026, madrugada)

## Documentación escolar (carpeta `docs/`)
- `Documentacion_Final_GERONEX.html`: portada, índice, descripción del sistema, objetivos, reglas de negocio,
  **requerimientos funcionales con las claves oficiales (RF-01.1 a RF-10.5) y el estado real de cada uno**,
  no funcionales, del sistema, actores, diagrama y especificación de 13 casos de uso, ciclo de vida de las
  operaciones, diagrama ER y diccionario de datos de las 12 tablas, y anexo con las rutas de la API.
  Se abre en el navegador; para Word: Archivo > Abrir este .html y "Guardar como" .docx (o imprimir a PDF).
- `diagramas/er.svg`, `casos_de_uso.svg`, `estados.svg` (se regeneran con `node generar_diagramas.mjs`).
  Se pueden insertar en Word como imagen.
- `Casos_de_Prueba_Agente.html`: los **36 casos MVC de la entidad Agente** (12 de vista, 12 de controlador y
  12 de modelo; altas, bajas, cambios y consultas), con el resultado real obtenido. **36 de 36 pasaron.**

## Pruebas
- `geronex-backend/pruebas/prueba_agente.mjs`: ejecuta los 24 casos de controlador y modelo (24/24) y
  guarda la evidencia en `resultados_agente.json`. Los 12 de vista se ejecutaron en admin.html (12/12).

## Web
- Nueva `cotizacion.html` (RF-05.17): comprobante imprimible de cada cotización, con enlace desde
  "Mis viajes" (cliente) y desde el historial del agente. Probado con datos reales.
- `admin.html`: el panel administrativo (RF-08.1) ahora también resume la operación: servicios en el catálogo,
  solicitudes por cotizar, reservaciones por validar y lo vendido.

---

# GERONEX: versión final (17 de noviembre de 2026, segunda parte)

## Android: todas las pantallas del cliente (compiladas; sin probar en un teléfono)
- **Inicio** (`MainActivity`): saludo, resumen (viajes en curso, por responder, confirmados), aviso
  "Tienes N cotizaciones por responder", botones a cada sección, buscador con filtro por tipo y
  "Destinos populares" con imagen y precio "desde". Si la cuenta no es de cliente, muestra un aviso
  (los empleados usan la web).
- **Catálogo**: búsqueda sin acentos, chips por tipo, destino, orden por precio, nombre o lugares,
  "solo con lugares", etiqueta de cupo ("¡Quedan 3!", "Agotado") y ventana de detalle con "Solicitar".
- **Pedir un viaje**: autocompletado y chips de destinos, calendario (ida desde mañana, regreso no
  antes de la ida), personas con − y +, presupuesto, necesidades con contador y aviso si faltan menos de 3 días.
- **Mis viajes**: filtros con contador, progreso en 4 pasos, cotización por responder resaltada,
  comparación contra el presupuesto, Aceptar o Rechazar con confirmación, cotizaciones anteriores
  plegables, botón al comprobante y aviso cuando llega una cotización nueva (se actualiza cada 15 s).
- **Mis reservaciones**: búsqueda por folio o destino, filtros por estatus y "faltan N días".
- **Comprobante**: imagen del destino, aviso según el estatus, datos completos, regla de cancelación,
  Cancelar (solo si la API lo permite, explicando el cargo del 5%), Compartir y Copiar folio.
- Imágenes de destinos en vectores de Android (las mismas de la web), tema con la paleta de GERONEX.
- Todo usa `Config.BASE_URL = http://localhost:8080/api` con `adb reverse tcp:8080 tcp:8080`.

## Reglas de negocio reforzadas
- Backend: no se puede **confirmar** una reservación cuyo viaje ya empezó (además de no poder
  cotizarla ni aceptarla). Web y Android avisan y desactivan los botones en ese caso.
- **Falta reiniciar el backend en IntelliJ** para que estas validaciones tomen efecto.

## Pruebas
- `prueba_api.mjs`: 92 de 92 contra el backend encendido. Backend y Android compilan sin errores.

## Disco
- Se liberaron 8.4 GB en C: (versiones viejas de Roblox, una versión vieja de Discord, distribuciones de
  Gradle sin uso, cachés de npm y VS Code, y temporales de más de 2 días).

---

# GERONEX: cambios del 17 de noviembre de 2026 (revisión completa y mejoras)

Rama git: `mejoras/revision-completa` (en `GERONEX_Equipo5`). Nada está en un commit todavía.

## Qué se probó y cómo salió
- **API real:** el script nuevo `geronex-backend/pruebas/prueba_api.mjs` corre **92 pruebas**: catálogo,
  registro, login, mi cuenta, administración, el flujo completo, el rechazo, la regla de 3 días y la seguridad.
  Resultado contra el backend encendido: **92 de 92 pasaron**.
- **Web con el backend real** (ya no con uno simulado): registro, solicitud, cotización, aceptar, confirmar,
  rechazar con motivo, cancelar con cargo del 5%, administración y comprobante. Todo funcionó.
  También se revisó en tamaño de celular (375 px): no hay desbordes y el menú se abre bien.
- **Backend compilado** sin errores con el cambio nuevo. **Falta reiniciarlo en IntelliJ** para que tome ese cambio.

## Errores corregidos
- En los formularios, `ev.submitter` puede venir vacío (navegadores viejos o envíos por código) y eso rompía
  el botón. Ahora se usa `botonDe(ev)` en login, Mis viajes, administración y Mi cuenta.
- La portada usaba una imagen de internet (Unsplash): sin internet, en la escuela, no se veía. Ahora es local.
- Backend: ya no se puede cotizar ni aceptar un viaje cuya fecha de inicio ya llegó (`CotizacionService`, error 409).

## Mejoras de la web (misma paleta y tipografía)
- **Imágenes propias** (SVG, funcionan sin internet): `img/portada.svg`, `img/logo.svg` y una ilustración por
  destino en `img/destinos/` (Cancún, Los Cabos, CDMX, Oaxaca, Chihuahua y una genérica).
- **Compartido** (`js/geronex.js`, `css/geronex.css`): logo en la barra, menú para celular, avisos flotantes
  (`toast`), ventanas de confirmación propias (`preguntar`, en lugar de `confirm()`), tarjetas de resumen,
  pestañas con contadores, barra de filtros, pie de página con enlaces y la política de cancelación.
- **Inicio:** portada nueva con buscador por texto, tipo y destino; sección "Destinos populares" con precio
  "desde"; catálogo con filtros por tipo, destino, precio máximo, "solo con lugares", orden por precio, nombre o
  cupo, contador de resultados y una ventana con el detalle de cada servicio.
- **Login:** una sola tarjeta con pestañas "Iniciar sesión / Crear cuenta", botón para ver la contraseña,
  medidor de seguridad, y si venías del catálogo con un destino, al entrar te lleva a pedir ese viaje.
- **Mis viajes:** saludo, resumen (en curso, por responder, confirmados, por validar), progreso de cada viaje en
  4 pasos, sugerencias de destinos, duración del viaje al elegir fechas, comparación contra el presupuesto,
  cotizaciones anteriores plegadas, aviso cuando llega una cotización nueva, y buscador y filtro de reservaciones
  con "faltan N días".
- **Agente:** resumen, pestañas por estatus, búsqueda (destino, cliente o número), orden, aviso de solicitudes
  nuevas, imagen del destino, búsqueda dentro del catálogo, barra contra el presupuesto, "Quitar todos" y
  confirmación antes de enviar.
- **Reservaciones:** resumen (incluye lo vendido), pestañas, búsqueda, orden, aviso cuando falta cupo (el botón
  Confirmar se desactiva) y ventana de rechazo con motivos frecuentes de un clic.
- **Administración:** resumen de cuentas, búsqueda y filtros por rol y estatus, confirmación propia para dar de baja.
- **Comprobante:** logo, imagen del destino, noches, cuenta regresiva y botón "Copiar folio".

## Herramientas nuevas
- `GERONEX_Equipo5/servidor_web.mjs`: abre la web sin IntelliJ (`node servidor_web.mjs` → http://localhost:8090).
  Reenvía `/api` al backend, así que no hay que tocar `CORS_ORIGENES`. Útil en la PC de la escuela.
- `geronex-backend/pruebas/prueba_api.mjs`: la prueba automática de la API (`node prueba_api.mjs`).
  Crea cuentas de prueba `qa_...` la primera vez y deja el cupo como estaba.

## Pendiente
- Reiniciar el backend en IntelliJ (para la validación de fecha) y revisar la web con tus propios ojos.
- Android: pantallas del cliente (siguiente tarea).

---

# GERONEX: cambios del 16 de noviembre de 2026

Estado: código **revisado pero sin compilar** (se escribió sin acceso a Maven/Gradle).
Lo primero es compilar y correr las pruebas de `geronex-backend/pruebas/PRUEBAS.md`.

## Base de datos (carpeta db)
- `03_esquema_negocio.sql`: 9 tablas nuevas con nombres del diagrama E-R:
  categoria_servicio, proveedor, servicio, disponibilidad, agente (con id_usuario, opción A),
  solicitud_viaje, cotizacion, detalle_cotizacion y reservacion.
- `04_datos_ejemplo.sql`: 4 categorías, 5 proveedores ficticios, 14 servicios con cupo.
- `05_primer_administrador.sql`: convierte la cuenta "admin" en ADMINISTRADOR.

## Backend (geronex-backend)
- Catálogo público: `GET /api/servicios`, `/api/servicios/{id}`, `/api/categorias`.
- Administración (ADMINISTRADOR): crear cuentas de empleados y activar/desactivar cuentas
  (`/api/admin/usuarios`). Crear una cuenta con rol AGENTE da de alta al agente.
- Agentes: consultar, cambiar y dar de baja lógica (`/api/agentes`).
- Flujo de negocio:
  1. CLIENTE crea una solicitud (`POST /api/solicitudes`) → NUEVA.
  2. AGENTE la cotiza con servicios del catálogo (`POST /api/cotizaciones`) → COTIZADA.
     El total lo calcula el backend y guarda el precio del momento.
  3. CLIENTE acepta (`/aceptar`) → nace la reservación POR_VALIDAR con folio,
     o rechaza (`/rechazar`) → la solicitud vuelve a NUEVA.
  4. RESPONSABLE_RESERVACIONES (o SUPERVISOR) confirma → descuenta disponibilidad,
     o rechaza con motivo → la solicitud vuelve a NUEVA.
  5. CLIENTE ve su comprobante y puede cancelar con 3 días de anticipación
     (cargo del 5% si ya estaba confirmada; se devuelve el cupo).
- Seguridad por rol en `SecurityConfig`; los errores 401 y 403 llegan en JSON.
- La disponibilidad se bloquea (SELECT ... FOR UPDATE) para no vender el último lugar dos veces.
- Si un agente edita "Mi cuenta", su ficha de agente se actualiza igual.

## Android (GeronexAndroid)
- "Mi cuenta" se cerraba porque CuentaActivity no estaba en el AndroidManifest: ya está declarada.
- El nombre visible es GERONEX en todas las pantallas (antes TRAVIA / IAMONOS).
- Se borró UserStorage.java (no se usaba).

## Web (segunda entrega)
- `index.html`: catálogo real desde la API, con búsqueda sin acentos y filtros por tipo.
- `login.html`: cada rol entra a su página de trabajo.
- `cliente.html` (Mis viajes): crear solicitudes, aceptar o rechazar cotizaciones, ver reservaciones.
- `comprobante.html`: comprobante completo, imprimible o en PDF; el cliente puede cancelar.
- `agente.html`: bandeja de solicitudes y armado de cotizaciones con cantidades sugeridas.
- `reservaciones.html`: el responsable ve el cupo de cada servicio y confirma o rechaza.
- `admin.html`: crear cuentas, editar y dar de baja agentes, activar o desactivar cuentas.
- Las bandejas se actualizan solas cada 10 segundos (consultando la API).
- Guía de presentación: `GUIA_DEMO.md`.

## Pendiente
- Android: pantallas del cliente (solicitudes, cotizaciones, reservaciones).
