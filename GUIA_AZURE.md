# GERONEX en Azure: guía paso a paso

> **Estado (23 de noviembre):** **montado y probado en la VM** `20.69.91.86` (Ubuntu 22.04).
> - http://20.69.91.86 muestra la web y http://20.69.91.86/api/salud responde `"estado":"OK"`.
> - Pruebas automáticas contra Azure: `prueba_api.mjs` **92 de 92** y `prueba_agente.mjs` **24 de 24**.
> - Falta: probar la app Android versión **azure** en el teléfono.
>
> **Para la exposición:** la VM debe estar **encendida** (portal → Iniciar). Al encenderla, todo
> (MySQL, backend y nginx) arranca solo; espera 1 o 2 minutos y abre http://20.69.91.86.

### Lo que ya tenía esta VM (y qué se hizo con eso)
La VM no estaba vacía. No se borró nada:
- **Apache** mostraba la página por defecto de Ubuntu en el puerto 80: se **detuvo** (no se desinstaló)
  para que nginx use el 80. Para regresarlo: `sudo systemctl enable --now apache2` (y detener nginx).
- **Java 17** era el `java` por defecto: se dejó así. Se instaló Java 21 aparte y el servicio `geronex`
  lo usa con ruta completa (`/usr/lib/jvm/java-21-openjdk-amd64/bin/java`).
- **MySQL** ya estaba instalado y `root` tiene contraseña: el script entra como administrador con la
  cuenta de mantenimiento de Ubuntu (`/etc/mysql/debian.cnf`), sin usar la contraseña de root.
  MySQL tiene activa la regla de contraseñas **MEDIUM** (8 caracteres, mayúscula, minúscula, número y
  símbolo): por eso la contraseña de `geronex_app` es la que elegiste **con la primera letra en mayúscula**.
- El repositorio de paquetes de MySQL (`repo.mysql.com`) tiene la llave vencida: el script lo ignora y
  usa los de Ubuntu. No se modificó.
- La carpeta `~/Workspace/Web-1` (otro proyecto) no se tocó.

---

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
- Los **secretos** viven solo en la VM, en `/etc/geronex/geronex.env` (solo root los lee):
  la contraseña de `geronex_app` (la que elija el equipo, o una al azar) y `JWT_SECRET` (siempre al azar).
  Para administrar MySQL desde la VM: `sudo mysql --defaults-file=/etc/mysql/debian.cnf` (en esta VM `root` tiene contraseña).
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
3. Antes de copiar, te pide la **contraseña para MySQL** (no se ve al escribirla). Escribe la que elegiste
   para la base, o pulsa Enter para que genere una al azar. No se guarda en el repositorio: viaja en un
   archivo temporal que la VM borra en cuanto la usa.
4. Qué va a pasar (tarda entre 5 y 10 minutos la primera vez):
   - `1/5` revisa la llave y se conecta por SSH (la primera vez acepta solo la huella del servidor).
   - `2/5` compila el backend (JAR).
   - `3/5` junta la web, los scripts SQL y las cuentas de demo.
   - `4/5` copia todo a la VM (unos 60 MB).
   - `5/5` en la VM: crea swap si hay poca RAM, instala Java 21, MySQL y nginx, crea la base, el servicio
     `geronex`, configura nginx y crea las cuentas de demo.
5. Al final debe decir: `LISTO. Abre en el navegador: http://20.69.91.86`.

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
| `ERROR 1819 … password does not satisfy the current policy` | La contraseña de MySQL no cumple la regla MEDIUM: usa una con mayúscula, minúscula, número y símbolo |
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

---

## Cómo explicar la conexión a Azure (para la exposición)

### La idea en 30 segundos
> "Rentamos una computadora en la nube de Microsoft (una máquina virtual con Linux). Tiene una dirección
> pública, `20.69.91.86`, que cualquiera en internet puede alcanzar. Un firewall de Azure solo deja pasar
> dos puertas: la 80, para la web, y la 22, para administrarla nosotros con una llave. Adentro, un programa
> llamado nginx recibe todo lo que llega por la 80: si piden una página, la entrega; si piden algo de `/api`,
> se lo pasa a nuestro backend de Spring Boot, que consulta MySQL en la misma máquina. Así la web y la app
> usan una sola dirección, y la base de datos nunca está expuesta a internet."

### Qué pasa cuando alguien abre http://20.69.91.86 (paso a paso)
1. **El navegador** se conecta a la IP `20.69.91.86` por el **puerto 80** (HTTP).
2. **Azure** recibe el paquete y lo revisa con el **NSG** (*Network Security Group*, el firewall de Azure):
   hay una regla que permite el 80, así que lo deja pasar a la VM. Si alguien intenta el 3306 (MySQL) o el
   8080, no hay regla y Azure lo bloquea antes de que llegue.
3. **nginx** (dentro de la VM, escuchando en el 80) ve la ruta:
   - `/index.html`, `/css/…`, `/js/…`, `/img/…` → entrega el archivo de `/var/www/geronex`.
   - `/api/…` → actúa como **proxy inverso**: reenvía la petición a `http://127.0.0.1:8080`.
4. **Spring Boot** escucha solo en `127.0.0.1:8080` (*localhost*: solo acepta conexiones de la misma
   máquina). Procesa la petición igual que en la laptop: revisa el token, aplica las reglas y consulta MySQL.
5. **MySQL** escucha en `localhost:3306`. Solo el backend lo usa, con el usuario `geronex_app`.
6. La respuesta (JSON) regresa por el mismo camino: Spring Boot → nginx → internet → navegador.

### Y la app Android
Es lo mismo: la versión **azure** del APK tiene `BASE_URL = http://20.69.91.86/api`. El teléfono (con datos
o Wi-Fi) manda sus peticiones a esa dirección; nginx las pasa al backend. No necesita cable ni `adb reverse`.
Android bloquea HTTP sin cifrar por defecto; el archivo `network_security_config.xml` lo permite solo para
esa IP (y localhost para pruebas).

### Por qué la web y la API comparten dirección
Como la web y la API salen del mismo sitio (`http://20.69.91.86`), el navegador no tiene que hacer peticiones
a "otro origen", así que no hace falta CORS. En el código, `js/geronex.js` usa `/api` cuando no está en
IntelliJ: funciona igual en la laptop (servidor del 8090) y en Azure.

### Cómo administramos la VM (SSH con llave)
- Nos conectamos con **SSH** (puerto 22) usando una **llave privada** (`.pem`) en lugar de contraseña. Azure
  guardó la llave pública en la VM al crearla; solo quien tiene la privada puede entrar.
- El script `subir_a_azure.ps1` usa **`scp`** (copia por SSH) para subir el JAR, la web y los scripts SQL, y
  **`ssh`** para ejecutar `instalar_en_vm.sh` dentro de la VM.

### Qué hace que siga funcionando sola
- El backend es un **servicio de systemd** (`geronex.service`): arranca al encender la VM y, si se cae, se
  reinicia a los 5 segundos.
- nginx y MySQL también son servicios que arrancan solos.
- Los secretos se leen de `/etc/geronex/geronex.env` (como las variables de entorno de IntelliJ en la laptop).

### Comparación laptop vs. Azure (para responder rápido)
| | Laptop | Azure |
|---|---|---|
| Web | IntelliJ (63342) o `servidor_web.mjs` (8090) | nginx en el 80 |
| Backend | IntelliJ, puerto 8080 | servicio systemd, 127.0.0.1:8080 |
| MySQL | Windows, localhost:3306 | Ubuntu, localhost:3306 |
| Variables | Configuración de ejecución de IntelliJ | `/etc/geronex/geronex.env` |
| Teléfono | Cable USB + `adb reverse` (APK debug) | Internet (APK azure) |
| Quién puede entrar | Solo la laptop | Cualquiera por el 80; nosotros por el 22 con llave |

### Preguntas posibles
- **¿Por qué no abrir el 8080 directamente?** Porque así el backend y la base no quedan expuestos; nginx es la
  única puerta. Además, nginx podría servir HTTPS o repartir carga entre varios backends en el futuro.
- **¿Qué es el NSG?** El firewall de Azure: una lista de reglas de qué puertos pueden entrar a la VM.
- **¿Qué pasa si se reinicia la VM?** systemd vuelve a arrancar MySQL, el backend y nginx. Si la IP es dinámica,
  podría cambiar; por eso conviene hacerla estática.
- **¿Es seguro?** Falta HTTPS (sin dominio no hay certificado); todo lo demás está cerrado: solo 22 con llave y 80.
  Las contraseñas de usuarios van en BCrypt y la base no es accesible desde fuera.
