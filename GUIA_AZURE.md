# GERONEX en Azure: guía paso a paso

> **Estado (23 de noviembre, madrugada):** los scripts están listos y probados en la laptop
> (compilan el backend y juntan los archivos), pero **todavía no se han corrido en la VM**:
> la VM `20.69.91.86` no respondía (ni SSH ni web). Lo más probable es que esté **apagada**.
> Sigue los pasos 1 y 2 en el portal y luego el paso 3 lo deja todo montado con un solo comando.

## Cómo queda montado

```
 Navegador ──┐                       VM Ubuntu en Azure (20.69.91.86)
             ├── http://IP:80 ──▶  nginx :80 ──┬── páginas web (/var/www/geronex)
 App azure ──┘                                 └── /api/* ──▶ Spring Boot 127.0.0.1:8080 ──▶ MySQL :3306
```

- **nginx** (puerto 80) sirve la web y reenvía todo lo que empieza con `/api/` al backend.
- **Backend**: el JAR de Spring Boot corre como servicio `geronex` (systemd). Si la VM se reinicia, arranca solo.
- **MySQL**: base `geronex` con los scripts `01`, `03` y `04`, el usuario `geronex_app` (sin DELETE) y las
  cuentas de demo `admin`, `agente1`, `reserva1` y `cliente1` (contraseña `Demo12345`).
- Los **secretos** (contraseña de MySQL y `JWT_SECRET`) se generan al azar **dentro de la VM**
  en `/etc/geronex/geronex.env` (solo root los lee). Nadie tiene que escribirlos ni pegarlos.
- Solo se abren los puertos **22** (SSH) y **80** (web). El 8080 y el 3306 quedan cerrados hacia internet.

---

## Paso 1. Encender la VM (portal de Azure)
1. Entra a https://portal.azure.com e inicia sesión.
2. Busca **Máquinas virtuales** (Virtual machines) y abre la tuya.
3. Si el estado dice **Detenido (desasignado)** o *Stopped*, pulsa **Iniciar** (Start) y espera a que diga **En ejecución** (*Running*).
4. En la página **Información general** (Overview), revisa la **Dirección IP pública**.
   - Si **ya no es `20.69.91.86`**, anota la nueva y mira el apartado [Si cambió la IP](#si-cambió-la-ip).
   - Recomendado: haz la IP **estática** para que no cambie cada vez que apagues la VM:
     clic en la IP → **Configuración** → Asignación: **Estática** → **Guardar**.
5. Revisa también el **sistema operativo**: debe ser **Ubuntu 22.04 o más nuevo** (si es 20.04, avísame: no trae Java 21).

## Paso 2. Abrir los puertos (grupo de seguridad de red, NSG)
1. En tu VM, menú izquierdo: **Redes** → **Configuración de red** (*Networking*).
2. En **Reglas de puerto de entrada** debe existir una regla para **SSH (22)**. Si no está:
   **Agregar regla de puerto de entrada** → Servicio: **SSH** → Prioridad 300 → **Agregar**.
3. Agrega otra regla para la web:
   **Agregar regla de puerto de entrada** → Servicio: **HTTP** (puerto **80**), Protocolo TCP,
   Acción **Permitir**, Prioridad 310, Nombre `HTTP` → **Agregar**.
4. **No** abras el 8080 ni el 3306.

**Para comprobarlo:** en PowerShell, corre esto:
```powershell
Test-NetConnection 20.69.91.86 -Port 22
```
Debe decir `TcpTestSucceeded : True`.

## Paso 3. Subir e instalar todo (un comando, desde la laptop)
1. Abre PowerShell en la carpeta del proyecto:
```powershell
cd "D:\A S U S\batiz2\GERONEX\geronexes de claude\Geronex2"
```
2. Corre el script:
```powershell
powershell -ExecutionPolicy Bypass -File azure\subir_a_azure.ps1
```
3. Qué va a pasar (tarda entre 5 y 10 minutos la primera vez):
   - `1/5` revisa la llave y se conecta por SSH (la primera vez acepta solo la huella del servidor).
   - `2/5` compila el backend (JAR).
   - `3/5` junta la web, los scripts SQL y las cuentas de demo.
   - `4/5` copia todo a la VM (unos 60 MB).
   - `5/5` en la VM: crea swap si hay poca RAM, instala Java 21, MySQL y nginx, crea la base, el servicio
     `geronex`, configura nginx y crea las cuentas de demo.
4. Al final debe decir: `LISTO. Abre en el navegador: http://20.69.91.86`.

> El script usa tu llave `D:\A S U S\batiz2\GERONEX\cosas de azure\DavidVerguita_key.pem` y le quita el
> permiso de lectura a otros usuarios de Windows (OpenSSH lo exige). No la copia a ningún lado.

## Paso 4. Comprobar
1. Abre http://20.69.91.86/api/salud → debe verse `{"estado":"OK","baseDeDatos":"geronex",…,"rolesRegistrados":7}`.
2. Abre http://20.69.91.86 → la portada de GERONEX con el catálogo (5 destinos, 14 servicios).
3. Inicia sesión con `cliente1` / `Demo12345` y luego, en otra pestaña nueva, con `agente1`.

## Paso 5. App Android apuntando a Azure
La app tiene dos versiones:
- **debug**: usa la laptop por el cable (`adb reverse`).
- **azure**: usa `http://20.69.91.86/api` por internet (el teléfono solo necesita datos o Wi-Fi).

Para instalar la de Azure:
1. Conecta el teléfono por USB (depuración USB activada).
2. Doble clic en `2_conectar_telefono.cmd` y elige **2 = Azure**. Debe terminar en `Success`.

Si cambiaste código de Android, primero recompila la versión azure: en Android Studio,
**Build → Select Build Variant… → app: azure** y luego **Build → Build APK(s)**.

---

## Actualizar después de cambiar código
Vuelve a correr el paso 3. La segunda vez es más rápida: solo reemplaza el JAR y la web, reinicia el servicio
y **no toca la base** (los datos se conservan).

## Si algo falla
| Qué ves | Qué hacer |
|---|---|
| `No hay conexión SSH` | La VM está apagada o falta la regla del puerto 22 (pasos 1 y 2) |
| `Permission denied (publickey)` | El usuario no es `azureuser` o la llave no es la de esta VM. Revisa en el portal: VM → **Conectar** |
| `ERROR: esta versión de Ubuntu no tiene Java 21` | La VM es Ubuntu 20.04: hay que crear otra con 22.04 o 24.04 |
| `ERROR: el backend no arrancó` | Mira el detalle (comando de abajo) y avísame qué dice |
| La web abre pero sin datos | El backend no está corriendo: revisa el servicio (comandos de abajo) |
| http://IP no abre nada | Falta la regla del puerto 80 en el NSG (paso 2) |
| La app dice "No se pudo conectar" | Instalaste la versión debug: instala la **azure** (paso 5) |

Comandos útiles (desde PowerShell en la laptop):
```powershell
ssh -i "D:\A S U S\batiz2\GERONEX\cosas de azure\DavidVerguita_key.pem" azureuser@20.69.91.86
```
Ya dentro de la VM:
```bash
sudo systemctl status geronex          # ¿está corriendo el backend?
sudo journalctl -u geronex -n 80       # últimas 80 líneas del backend
sudo systemctl restart geronex         # reiniciarlo
sudo systemctl status nginx mysql      # ¿nginx y MySQL encendidos?
```

## Si cambió la IP
1. Corre el script con la IP nueva:
```powershell
powershell -ExecutionPolicy Bypass -File azure\subir_a_azure.ps1 -Ip 1.2.3.4
```
2. Para la app: cambia `20.69.91.86` por la IP nueva en dos archivos y recompila la versión azure:
   - `GeronexAndroid/app/build.gradle.kts` (línea `buildConfigField` dentro de `create("azure")`).
   - `GeronexAndroid/app/src/main/res/xml/network_security_config.xml`.

## Para no gastar créditos
Cuando no la uses, en el portal pulsa **Detener** en la VM (queda *desasignada* y no cobra cómputo).
**Enciéndela antes de presentar** y comprueba el paso 4. Si la IP no es estática, puede cambiar al encenderla.
