# GERONEX: contexto para Claude Code

> Proyecto escolar. Entrega y presentación: **21 de noviembre de 2026**. El equipo quiere terminar antes.
> Este archivo resume todo lo hecho al 17 de noviembre de 2026 (madrugada). Detalle de cambios en CAMBIOS.md. Si algo aquí contradice el código, manda el código.

## 1. Qué es
GERONEX es la empresa escolar (equipo de 3) que moderniza el sistema de **iamonos**, una agencia de viajes.
El sistema tiene una **web** (HTML/CSS/JS), una **app Android** (Java) y un **backend Spring Boot**, todo sobre **una sola base MySQL**.
Web y Android **nunca** se conectan directo a MySQL: todo pasa por la API REST.

- Nombre visible para el usuario: **GERONEX** (ya no "Travia" ni "IAMONOS").
- Equipo: una persona en base de datos, una en backend + web (David, el usuario, que en la práctica hace de todo) y una en Android.
- Todo corre **en local**, en la laptop de David. No hay servidor ni HTTPS.
- La presentación será en esa laptop. Si hace falta, se instalará el Android Studio nuevo en una PC de la escuela.

### Modelo de negocio (agencia asistida, no reserva directa)
1. El **cliente** consulta el catálogo (sin cuenta) y crea una **solicitud de viaje** (requiere cuenta).
2. Un **agente** arma una **cotización** con servicios del catálogo. El total lo calcula el backend.
3. El cliente **acepta**, y se crea una **reservación** POR_VALIDAR con folio, o **rechaza**, y la solicitud vuelve a NUEVA.
4. El **responsable de reservaciones** (o el supervisor) valida disponibilidad: **confirma** y se descuenta el cupo, o **rechaza** con motivo y la solicitud vuelve a NUEVA.
5. El cliente ve su **comprobante** y puede **cancelar**.

**Regla de cancelación:** se puede cancelar si faltan al menos **3 días naturales** para `fecha_inicio`. Si estaba CONFIRMADA se registra un **cargo del 5% del total** (solo se guarda, no hay cobro real) y se devuelve el cupo. Si estaba POR_VALIDAR, no hay cargo.

## 2. Entorno del usuario
- Windows, PowerShell, IntelliJ (backend y web), MySQL 8.0 + Workbench, Android Studio (versión más nueva).
- JDK 23 (el pom apunta a Java 21). Prefiere `curl.exe` con archivos `.json` (PowerShell rompe las comillas del JSON en línea).
- Teléfono físico con **Android 16** por USB: `adb reverse tcp:8080 tcp:8080`, que se repite cada vez que se reconecta el cable. No usa emulador.
- La web se abre con el servidor integrado de IntelliJ (puerto 63342), nunca con doble clic, porque CORS bloquea `file://`.
  Alternativa: `node servidor_web.mjs` en `GERONEX_Equipo5` → http://localhost:8090 (reenvía /api al backend; define `window.GERONEX_API`).
- **El disco C: estaba lleno (0 GB libres) el 17 de noviembre.** Compilar o guardar temporales en C: falla; usar D:.
- Variables de entorno en la configuración de ejecución de IntelliJ: `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (32 caracteres o más) y `CORS_ORIGENES` (incluye `http://localhost:63342`).
- **Nunca pidas ni muestres** la contraseña de MySQL, el valor de `JWT_SECRET` ni tokens reales.

## 3. Estructura
```
Geronex/
├── db/                      01_esquema_cuentas.sql, 02_usuario_aplicacion.sql,
│                            03_esquema_negocio.sql, 04_datos_ejemplo.sql, 05_primer_administrador.sql
├── GERONEX_Equipo5/         (repositorio git) web + backend
│   ├── index.html login.html cuenta.html cliente.html comprobante.html
│   ├── agente.html reservaciones.html admin.html
│   ├── js/geronex.js  css/geronex.css  img/ (logo, portada y destinos en SVG)  servidor_web.mjs
│   └── geronex-backend/     Spring Boot (Maven) + pruebas/ (JSON, PRUEBAS.md y prueba_api.mjs: 92 pruebas)
├── GeronexAndroid/          proyecto Android Studio (paquete com.example.geronex)
├── CAMBIOS.md  GUIA_DEMO.md
```

## 4. Backend (`com.geronex.backend`)
- Spring Boot **4.1.0**, Web MVC, Data JPA/Hibernate (`ddl-auto=validate`, **no** modifica el esquema), Security, validación, Maven.
- **JWT** HS256 sin sesiones, 60 minutos. Claims: `sub` = idUsuario y `rol`, que se convierte en `ROLE_<rol>`. Contraseñas con **BCrypt**.
- El rol va dentro del token: si cambia el rol en la BD, hay que volver a iniciar sesión.
- Errores siempre con el formato `{estado, mensaje, errores{campo:mensaje}, fecha}`, también los 401 y 403 (los escribe `SecurityConfig`).
- Usuario MySQL `geronex_app` con SELECT, INSERT y UPDATE solamente. **No hay DELETE**: las bajas son lógicas (estatus).
- Estatus guardados como `String` con constantes, **no** `@Enumerated`, porque Hibernate esperaría un ENUM nativo y fallaría `validate`.
- Concurrencia: `SELECT … FOR UPDATE` (`@Lock PESSIMISTIC_WRITE`) en disponibilidad, solicitud y reservación. Las filas de disponibilidad se bloquean en orden de `id_servicio`.
- Paquetes: `auth`, `usuario` (incluye `UsuarioActual`), `rol` (`Roles`), `catalogo`, `agente`, `admin`, `operacion` (solicitudes, cotizaciones, reservaciones, `Mapeador`, `ReglasCancelacion`, `FolioGenerador`, `Acceso`), `dto`, `error`, `config`, `util`, `salud`.

### Base de datos (MySQL `geronex`)
- **Cuentas:** `rol` (7 roles), `usuario` (nombre_usuario de 15 como máximo y único, correo único, hash, id_rol, estatus ACTIVO/INACTIVO/BLOQUEADO) y `perfil` (1 a 1, `nombre`; al registrarse toma el nombre de usuario).
- **Negocio:** `categoria_servicio` (HOTEL, VUELO, TRANSPORTE, PAQUETE), `proveedor`, `servicio` (precio y unidad: "por noche", "por persona" o "por viaje"), `disponibilidad` (1 a 1 con servicio: cupo_total y cupo_disponible).
- `agente` (id_usuario 1 a 1: **opción A**, el agente es una cuenta con rol AGENTE; nombre, correo único, teléfono de 10 dígitos, estatus ACTIVO/INACTIVO).
- `solicitud_viaje` (NUEVA → COTIZADA → ACEPTADA).
- `cotizacion` (ENVIADA → ACEPTADA o RECHAZADA) y `detalle_cotizacion`, que guarda el **precio del momento**.
- `reservacion` (folio único `GX-AAMMDD-XXXX`, una por cotización; POR_VALIDAR → CONFIRMADA, RECHAZADA o CANCELADA; motivo_rechazo, cargo_cancelacion, id_responsable).
- Tablas del diagrama ER que **no** se hicieron (siguiente fase): solicitud_cambio, reasignacion, seguimiento_comercial, seguimiento_proveedor, auditoria, ajuste_cotizacion, publico_objetivo, detalle_reservacion, permiso y rol_permiso.
- El primer administrador se crea así: se registra la cuenta `admin` (queda como CLIENTE) y se corre `05_primer_administrador.sql`. Después, el admin crea a los empleados desde la API o la web.

### API (base `http://localhost:8080/api`)
| Ruta | Quién | Qué hace |
|---|---|---|
| POST `/auth/registro`, `/auth/login` | público | El registro siempre crea CLIENTE; el login devuelve `{token, tipo, expiraEnSegundos, usuario}` |
| GET `/salud`, `/categorias`, `/servicios[?categoria=&texto=]`, `/servicios/{id}` | público | Catálogo; la búsqueda ignora acentos |
| GET/PUT `/usuarios/me`, PATCH `/usuarios/me/password` | cualquier sesión | Mi cuenta (si es agente, también actualiza su ficha) |
| GET/POST `/admin/usuarios`, PATCH `/admin/usuarios/{id}/estatus` | ADMINISTRADOR | Crear cuentas de cualquier rol (con rol AGENTE crea la fila en `agente`); el admin no puede cambiar su propio estatus |
| GET `/agentes`, `/agentes/{id}` | ADMINISTRADOR, SUPERVISOR | Consultar agentes |
| PUT `/agentes/{id}`, PATCH `/agentes/{id}/estatus` | ADMINISTRADOR | Cambio y baja lógica (INACTIVO también desactiva la cuenta) |
| POST `/solicitudes`; GET `/solicitudes/mias` | CLIENTE | Crear (fechaInicio en el futuro, fechaFin no antes del inicio, de 1 a 50 personas) y listar las propias |
| GET `/solicitudes[?estatus=]` | AGENTE, SUPERVISOR, ADMINISTRADOR, AUDITOR | Bandeja |
| GET `/solicitudes/{id}` | dueño o empleado | Si un cliente pide una ajena, recibe 404 |
| POST `/cotizaciones` `{idSolicitud, notas, detalles:[{idServicio,cantidad}]}` | AGENTE con ficha ACTIVA | Solo si la solicitud está NUEVA; valida cupo; sin servicios repetidos |
| POST `/cotizaciones/{id}/aceptar` / `rechazar` | CLIENTE dueño | Aceptar devuelve la reservación (201) |
| GET `/reservaciones/mias` | CLIENTE | Sus reservaciones |
| GET `/reservaciones[?estatus=]` | RESP_RESERVACIONES, SUPERVISOR, ADMIN, AUDITOR | Bandeja |
| GET `/reservaciones/{id}` | dueño o empleado | Comprobante (incluye `fechaLimiteCancelacion` y `puedeCancelar`) |
| POST `/reservaciones/{id}/confirmar`, `/rechazar` `{motivo}` | RESP_RESERVACIONES, SUPERVISOR | Validar |
| POST `/reservaciones/{id}/cancelar` | CLIENTE dueño | Regla de 3 días y 5% |

Las respuestas de solicitud traen sus `cotizaciones[]` con `detalles[]`, y si hay reservación, `idReservacion`, `folio` y `estatusReservacion`. Los formatos exactos están en `dto/OperacionDto.java`.

## 5. Web
- `js/geronex.js`: `API`, `Sesion` (token en **sessionStorage**, que es por pestaña; así se demuestran varios roles en una sola PC), `api()` (con `{publica:true}` no manda token), `enviar()`, `requerirRol()`, `inicioPorRol()`, `pintarNav()` por rol, formato (`dinero`, `fecha`, `badge`, `esc` contra XSS) y `refrescarCada(10, fn)`, que **consulta la API cada 10 s** (no es tiempo real; así se decidió).
- Páginas: `index` (catálogo), `login` (redirige según el rol), `cuenta`, `cliente` (Mis viajes), `comprobante?id=` (imprimible y para cancelar), `agente` (bandeja y armado de cotización con cantidades sugeridas), `reservaciones` (cupo por servicio, confirmar o rechazar), `admin`.
- Utilidades nuevas en geronex.js: `toast()`, `preguntar()` (diálogo propio en lugar de confirm), `botonDe(ev)`, `imagenDestino()`, `normalizar()`, `nochesEntre()`, `diasPara()`, `htmlResumen()`, `pintarPie()`.
- Paleta (no cambiar): crema `#F9F5EB`, coral `#EA5455`, naranja `#F07B3F`, pizarra `#2D4059`, azul marino `#002B5B`. Tipografía Segoe UI.

## 6. Android (`GeronexAndroid`, paquete `com.example.geronex`)
- Java 11, minSdk 24, compileSdk/targetSdk **37**, AGP **9.3.3**, Gradle **9.5**, OkHttp 4.12.0 (se usa porque `HttpURLConnection` no admite PATCH). Los layouts, drawables y colores son del equipo: consérvalos.
- Clases base: `Config.BASE_URL = "http://localhost:8080/api"` (teléfono por USB con `adb reverse tcp:8080 tcp:8080`), `ApiClient` (`llamar` para objetos y `llamarLista` para arreglos; callbacks en el hilo principal; un 401 cierra la sesión), `SesionStore` (token cifrado con AES-GCM en Android Keystore), `Validador`, `Formato` (dinero, fechas con Calendar porque minSdk 24, textos y colores de estatus), `Destinos` (imagen por destino), `BaseActivity` (exigirCliente, encabezado, badges, chips, diálogos y refresco automático en onResume).
- Pantallas: `LoginActivity` (inicio), `RegisterActivity`, `MainActivity` (resumen, aviso de cotización por responder, accesos, buscador y destinos populares; las cuentas que no son CLIENTE ven un aviso), `CatalogoActivity` (búsqueda, chips por tipo, destino, orden, solo con lugares y detalle), `NuevaSolicitudActivity` (DatePicker con ida desde mañana, personas de 1 a 50, presupuesto y necesidades), `MisViajesActivity` (filtros, progreso en 4 pasos, aceptar o rechazar, cotizaciones anteriores; refresco cada 15 s), `ReservacionesActivity` (búsqueda y filtros por estatus), `ComprobanteActivity` (cancelar solo si `puedeCancelar`, compartir y copiar folio), `CuentaActivity`. Todas declaradas en el manifest.
- Recursos: vectores `dest_*.xml` (los mismos destinos que la web), `bg_chip`, `bg_chip_activo`, `bg_destacado`, `bg_boton_verde`, `bg_boton_claro`, `bg_imagen`, `encabezado.xml` (include) y colores de estatus en `colors.xml`. Tema Material3 claro, también en modo oscuro.
- `network_security_config` permite HTTP solo hacia localhost, 127.0.0.1 y 10.0.2.2 (solo para desarrollo).
- Compilar desde terminal: JAVA_HOME = JBR de Android Studio y temporales en D: (el disco C: llegó a llenarse).

## 7. Estado real (17 de noviembre de 2026)
| Parte | Estado |
|---|---|
| Esquema y datos (scripts 01 a 05) | Ejecutado por el usuario |
| Backend | **Probado**: `prueba_api.mjs` 92 de 92. Las validaciones nuevas (no cotizar, aceptar ni confirmar un viaje cuya fecha ya llegó) están **compiladas, pero falta reiniciar el backend** para que tomen efecto |
| Web | **Probada con el backend real** (todos los roles, flujo completo y tamaño de celular). Versión final rediseñada |
| Android | **Todas las pantallas del cliente escritas y compiladas (APK debug generado). No probadas en un teléfono** |
| Pruebas automatizadas | `pruebas/prueba_api.mjs` (Node, contra la API encendida) |

### Actualización del 23 de noviembre (madrugada)
- Web: CSS y JS de cada página separados en `css/<página>.css` y `js/<página>.js` (idéntica píxel por píxel; 92/92 y 24/24).
- Azure: `azure/subir_a_azure.ps1` (laptop) + `azure/instalar_en_vm.sh` (VM). **Montado y probado el 23 nov**: http://20.69.91.86, 92/92 y 24/24 contra Azure. La VM ya tenía Apache (detenido), Java 17 (se dejó; el servicio usa Java 21 con ruta completa), MySQL con contraseña en root (se entra con `/etc/mysql/debian.cnf`) y política de contraseñas MEDIUM. Falta probar el APK azure en el teléfono.
- Android: variantes `debug` (localhost) y `azure` (http://20.69.91.86/api). `Config.BASE_URL = BuildConfig.BASE_URL`.
- Documentos: `docs/Presentacion_GERONEX_Final.pptx`, GUIA_PROYECTO.md, GUIA_CODIGO.md, GUIA_AZURE.md, COMO_ENCENDER.md.

### Actualización del 24 de noviembre
- Equipo real de 3: Omar Yoel Cárdenas (BD, parte 1), David Vega (backend, web y Azure, parte 2), Yael Sebastián Portillo (Android, parte 3).
- Web y Android sin emojis y con menos redondeo (pedido del usuario: "que no parezca hecha con IA"). Ya no existe `ICONO_CATEGORIA` (web) ni `Formato.iconoCategoria` (Android).
- Presentación v2 (27 diapositivas, 3 partes, transiciones) y `docs/Guia_del_Equipo_GERONEX.docx` (guía fácil + guion). Los generadores viven en el scratchpad de Claude, no en el repo.

## 8. Lo que sigue
1. Reiniciar el backend en IntelliJ y probar la app en el teléfono (adb reverse) con el guion de GUIA_DEMO.md.
2. Prueba cruzada: una cuenta creada en la web se usa en Android y al revés.
3. **(Hecho)** Documentación escolar en `docs/Documentacion_Final_GERONEX.html` (objetivos, requerimientos con estado real, casos de uso, ER, diccionario de datos) y diagramas SVG en `docs/diagramas`. Falta revisarla y pasarla a Word.
4. **(Hecho, falta sacar capturas)** Tarea de David: 36 casos MVC de **Agente** en `docs/Casos_de_Prueba_Agente.html` (36/36 pasaron). Automáticos: `pruebas/prueba_agente.mjs`.
5. Opcional: bajar AGP, Gradle y compileSdk si hay que abrir el proyecto en el Android Studio viejo de la escuela.

## 9. Cómo trabajar con el usuario
- Es estudiante y escribe en español. Quiere instrucciones **paso a paso**: qué archivo, en qué carpeta, qué comando, qué debe verse y cómo comprobarlo.
- Avanza por etapas pequeñas y compruébalas antes de seguir. Distingue siempre entre **generado**, **compilado** y **probado**, y no digas que algo funciona sin comprobarlo.
- No rediseñes los diagramas ni la arquitectura sin preguntar. No agregues funciones, APIs externas ni pagos reales por tu cuenta. No borres trabajo existente sin justificarlo.
- Si encuentras una contradicción, presenta opciones breves y pregunta antes de decidir.
