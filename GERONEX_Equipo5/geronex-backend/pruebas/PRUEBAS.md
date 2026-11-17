# Pruebas de la API de negocio (PowerShell + curl.exe)

> **Atajo: prueba automática.** Con el backend encendido, en esta carpeta:
> `node prueba_api.mjs`
> Corre 92 pruebas (todo lo de abajo y más) y al final dice cuántas pasaron.
> Usa la cuenta `admin` de `login_admin.json` y crea cuentas `qa_...` la primera vez.

Abre PowerShell **en esta carpeta** (`geronex-backend/pruebas`) con el backend encendido.
Las cuentas de aquí son de demostración (contraseña `Demo12345`); cámbiala si quieres.

## 0. Preparación (una sola vez)

1. En Workbench ejecuta `db/03_esquema_negocio.sql` y `db/04_datos_ejemplo.sql`.
2. Registra la cuenta `admin`: desde la web, o con
   `curl.exe -s -X POST http://localhost:8080/api/auth/registro -H "Content-Type: application/json" -d "@registro_admin.json"`
3. Ejecuta `db/05_primer_administrador.sql`. Debe salir `admin` con rol ADMINISTRADOR.
4. Reinicia el backend en IntelliJ.

## 1. Catálogo público (sin token)

```powershell
curl.exe -s http://localhost:8080/api/servicios
curl.exe -s "http://localhost:8080/api/servicios?categoria=HOTEL&texto=cancun"
```
Esperado: lista JSON con 14 servicios; el segundo, solo los hoteles de Cancún.

## 2. El administrador crea al agente y al responsable

```powershell
$ADMIN = (curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "@login_admin.json" | ConvertFrom-Json).token
curl.exe -s -X POST http://localhost:8080/api/admin/usuarios -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" -d "@crear_agente.json"
curl.exe -s -X POST http://localhost:8080/api/admin/usuarios -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" -d "@crear_responsable.json"
curl.exe -s http://localhost:8080/api/agentes -H "Authorization: Bearer $ADMIN"
```
Esperado: dos cuentas creadas (rol AGENTE y RESPONSABLE_RESERVACIONES) y la lista de agentes con Laura Méndez.

## 3. El cliente pide un viaje

```powershell
curl.exe -s -X POST http://localhost:8080/api/auth/registro -H "Content-Type: application/json" -d "@registro_cliente.json"
$CLIENTE = (curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "@login_cliente.json" | ConvertFrom-Json).token
curl.exe -s -X POST http://localhost:8080/api/solicitudes -H "Authorization: Bearer $CLIENTE" -H "Content-Type: application/json" -d "@solicitud.json"
```
Esperado: `201` con `"estatus":"NUEVA"`. Anota el `idSolicitud` (si no es 1, cámbialo en `cotizacion.json`).

## 4. El agente cotiza

```powershell
$AGENTE = (curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "@login_agente.json" | ConvertFrom-Json).token
curl.exe -s "http://localhost:8080/api/solicitudes?estatus=NUEVA" -H "Authorization: Bearer $AGENTE"
curl.exe -s -X POST http://localhost:8080/api/cotizaciones -H "Authorization: Bearer $AGENTE" -H "Content-Type: application/json" -d "@cotizacion.json"
```
Esperado: total = 4×2450 + 2×3200 + 2×1650 = **19,500.00**. Anota el `idCotizacion`.

## 5. El cliente acepta (cambia 1 por tu idCotizacion)

```powershell
curl.exe -s -X POST http://localhost:8080/api/cotizaciones/1/aceptar -H "Authorization: Bearer $CLIENTE"
```
Esperado: reservación con `"estatus":"POR_VALIDAR"` y un folio como `GX-261116-K7PQ`. Anota el `idReservacion`.

## 6. El responsable confirma (cambia 1 por tu idReservacion)

```powershell
$RESP = (curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "@login_responsable.json" | ConvertFrom-Json).token
curl.exe -s "http://localhost:8080/api/reservaciones?estatus=POR_VALIDAR" -H "Authorization: Bearer $RESP"
curl.exe -s -X POST http://localhost:8080/api/reservaciones/1/confirmar -H "Authorization: Bearer $RESP"
```
Esperado: `"estatus":"CONFIRMADA"`. En Workbench, `SELECT * FROM disponibilidad WHERE id_servicio IN (1,6,12);` debe mostrar 16, 38 y 28.

## 7. El cliente ve su comprobante y cancela

```powershell
curl.exe -s http://localhost:8080/api/reservaciones/1 -H "Authorization: Bearer $CLIENTE"
curl.exe -s -X POST http://localhost:8080/api/reservaciones/1/cancelar -H "Authorization: Bearer $CLIENTE"
```
Esperado: `"estatus":"CANCELADA"`, `"cargoCancelacion":975.00` (5% de 19,500) y la disponibilidad regresa a 20, 40 y 30.

## 8. Pruebas de seguridad (deben FALLAR)

```powershell
# Un cliente no puede cotizar -> 403
curl.exe -s -X POST http://localhost:8080/api/cotizaciones -H "Authorization: Bearer $CLIENTE" -H "Content-Type: application/json" -d "@cotizacion.json"
# Un agente no puede entrar a la administración -> 403
curl.exe -s http://localhost:8080/api/admin/usuarios -H "Authorization: Bearer $AGENTE"
# Sin token no se pueden crear solicitudes -> 401
curl.exe -s -X POST http://localhost:8080/api/solicitudes -H "Content-Type: application/json" -d "@solicitud.json"
# Confirmar dos veces la misma reservación -> 409
curl.exe -s -X POST http://localhost:8080/api/reservaciones/1/confirmar -H "Authorization: Bearer $RESP"
```

Si algo sale distinto, copia la respuesta y la consola de IntelliJ (sin contraseñas ni tokens).
