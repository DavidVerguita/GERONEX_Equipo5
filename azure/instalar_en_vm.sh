#!/usr/bin/env bash
# =====================================================================
# GERONEX - Instala o actualiza todo dentro de la VM Ubuntu de Azure.
#
# Lo sube y lo ejecuta subir_a_azure.ps1 (desde la laptop). A mano sería:
#     bash ~/geronex_subida/instalar_en_vm.sh 20.69.91.86
#
# Qué deja funcionando:
#   - MySQL con la base "geronex" (scripts 01, 03 y 04) y el usuario geronex_app
#   - El backend (JAR de Spring Boot) como servicio "geronex" en 127.0.0.1:8080
#   - nginx en el puerto 80: sirve la web y reenvía /api/ al backend
#   - Cuentas de demo (solo la primera vez): admin, agente1, reserva1, cliente1
# Se puede correr varias veces: la segunda vez solo actualiza el JAR y la web.
# =====================================================================
set -euo pipefail

IP_PUBLICA="${1:-}"
DIR="$(cd "$(dirname "$0")" && pwd)"     # carpeta con lo que subió subir_a_azure.ps1
ENV_FILE=/etc/geronex/geronex.env         # secretos: solo root los puede leer
WEB=/var/www/geronex

paso() { echo; echo "== $*"; }

# ---------------------------------------------------------------------
paso "1/7 Memoria de intercambio (las VM pequeñas de Azure tienen 1 GB de RAM)"
if [ "$(free -m | awk '/Mem:/ {print $2}')" -lt 2000 ] && ! swapon --show | grep -q /swapfile; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile >/dev/null
    sudo swapon /swapfile
    grep -q /swapfile /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
    echo "Swap de 2 GB creada."
else
    echo "No hace falta."
fi

# ---------------------------------------------------------------------
paso "2/7 Paquetes: Java 21, MySQL, nginx"
JAVA21=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
FALTAN=""
[ -x "$JAVA21" ] || FALTAN="$FALTAN openjdk-21-jre-headless"
command -v mysql >/dev/null || FALTAN="$FALTAN mysql-server"
command -v nginx >/dev/null || FALTAN="$FALTAN nginx"
for p in curl python3 openssl; do command -v $p >/dev/null || FALTAN="$FALTAN $p"; done
# Apache ocupa el puerto 80 (en esta VM solo mostraba la página por defecto): se detiene, no se desinstala
if systemctl is-active --quiet apache2; then
    sudo systemctl disable --now apache2
    echo "Apache detenido (el puerto 80 ahora es de nginx). Para regresarlo: sudo systemctl enable --now apache2"
fi
if [ -n "$FALTAN" ]; then
    # Si otro repositorio de la VM falla (p. ej. una llave vencida), se sigue con los de Ubuntu
    sudo apt-get update -y || echo "Aviso: algún repositorio no se pudo actualizar; se sigue con los demás."
    if ! apt-cache show openjdk-21-jre-headless >/dev/null 2>&1; then
        echo "ERROR: esta versión de Ubuntu no tiene Java 21 (se necesita Ubuntu 22.04 o más nueva)."
        exit 1
    fi
    JAVA_ANTES="$(readlink -f /usr/bin/java 2>/dev/null || true)"
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y $FALTAN
    # El "java" por defecto de la VM se deja como estaba; el servicio usa Java 21 con ruta completa
    if [ -n "$JAVA_ANTES" ] && [ "$JAVA_ANTES" != "$(readlink -f /usr/bin/java)" ]; then
        sudo update-alternatives --set java "$JAVA_ANTES"
    fi
fi
"$JAVA21" -version 2>&1 | head -1

# ---------------------------------------------------------------------
paso "3/7 Secretos (se generan una sola vez, nadie los escribe a mano)"
if ! sudo test -f "$ENV_FILE"; then
    sudo mkdir -p /etc/geronex
    printf 'DB_USER=geronex_app\nDB_PASSWORD=%s\nJWT_SECRET=%s\n' "$(openssl rand -hex 16)" "$(openssl rand -hex 32)" \
        | sudo tee "$ENV_FILE" >/dev/null
    sudo chmod 600 "$ENV_FILE"
    echo "Creado $ENV_FILE"
fi
# Contraseña elegida por el equipo (si subir_a_azure.ps1 la mandó): reemplaza la generada y se borra el archivo
if [ -f "$DIR/clave_bd.txt" ]; then
    CLAVE_ELEGIDA="$(cat "$DIR/clave_bd.txt")"
    shred -u "$DIR/clave_bd.txt" 2>/dev/null || rm -f "$DIR/clave_bd.txt"
    case "$CLAVE_ELEGIDA" in
        *[\'\"\\\$\`\ ]*|"") echo "ERROR: la contraseña no puede estar vacía ni tener comillas, \\, \$, \` o espacios."; exit 1 ;;
    esac
    sudo sed -i '/^DB_PASSWORD=/d' "$ENV_FILE"
    printf 'DB_PASSWORD=%s\n' "$CLAVE_ELEGIDA" | sudo tee -a "$ENV_FILE" >/dev/null
    echo "Se usará la contraseña elegida para geronex_app."
fi
# Orígenes web permitidos para CORS (la web por nginx no lo necesita, pero no estorba)
sudo sed -i '/^CORS_ORIGENES=/d' "$ENV_FILE"
echo "CORS_ORIGENES=http://${IP_PUBLICA:-localhost},http://localhost:63342,http://localhost:8090" | sudo tee -a "$ENV_FILE" >/dev/null
CLAVE_DB="$(sudo grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"

# ---------------------------------------------------------------------
paso "4/7 Base de datos"
sudo systemctl enable --now mysql
# Entrar a MySQL como administrador: sin contraseña si root usa auth_socket; si root tiene
# contraseña, con la cuenta de mantenimiento de Ubuntu (debian-sys-maint)
if sudo mysql -e "SELECT 1" >/dev/null 2>&1; then
    mysql_admin() { sudo mysql "$@"; }
elif sudo test -f /etc/mysql/debian.cnf; then
    mysql_admin() { sudo mysql --defaults-file=/etc/mysql/debian.cnf "$@"; }
else
    echo "ERROR: no puedo entrar a MySQL como administrador (root tiene contraseña y no hay /etc/mysql/debian.cnf)."
    exit 1
fi
if ! mysql_admin -N -e "SHOW DATABASES LIKE 'geronex'" | grep -q geronex; then
    for f in 01_esquema_cuentas.sql 03_esquema_negocio.sql 04_datos_ejemplo.sql; do
        echo "Ejecutando $f"
        mysql_admin < "$DIR/db/$f"
    done
else
    echo "La base geronex ya existe: no se toca."
fi
# Mismo permiso que 02_usuario_aplicacion.sql: SELECT, INSERT y UPDATE (sin DELETE)
mysql_admin -e "CREATE USER IF NOT EXISTS 'geronex_app'@'localhost' IDENTIFIED BY '$CLAVE_DB';
               ALTER USER 'geronex_app'@'localhost' IDENTIFIED BY '$CLAVE_DB';
               GRANT SELECT, INSERT, UPDATE ON geronex.* TO 'geronex_app'@'localhost';
               FLUSH PRIVILEGES;"

# ---------------------------------------------------------------------
paso "5/7 Backend (servicio geronex)"
sudo mkdir -p /opt/geronex
sudo cp "$DIR/geronex-backend.jar" /opt/geronex/geronex-backend.jar
sudo tee /etc/systemd/system/geronex.service >/dev/null <<'EOF'
[Unit]
Description=GERONEX backend (Spring Boot)
After=network.target mysql.service
Requires=mysql.service

[Service]
EnvironmentFile=/etc/geronex/geronex.env
ExecStart=/usr/lib/jvm/java-21-openjdk-amd64/bin/java -Xmx400m -jar /opt/geronex/geronex-backend.jar --server.address=127.0.0.1
DynamicUser=yes
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable geronex >/dev/null 2>&1
sudo systemctl restart geronex
echo -n "Esperando al backend"
for i in $(seq 1 90); do
    if curl -s -m 2 http://127.0.0.1:8080/api/salud | grep -q '"OK"'; then echo " listo."; break; fi
    echo -n "."; sleep 2
    if [ "$i" = 90 ]; then echo; echo "ERROR: el backend no arrancó. Revisa: sudo journalctl -u geronex -n 80"; exit 1; fi
done

# ---------------------------------------------------------------------
paso "6/7 Web (nginx en el puerto 80)"
sudo mkdir -p "$WEB"
sudo rm -rf "${WEB:?}"/*
sudo cp -r "$DIR/web/." "$WEB/"
sudo tee /etc/nginx/sites-available/geronex >/dev/null <<'EOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    root /var/www/geronex;
    index index.html;

    # La API: nginx la reenvía al backend, así la web y la app usan http://IP/api
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header Origin "";        # mismo origen: no hace falta CORS
        proxy_read_timeout 60s;
    }

    location / {
        try_files $uri $uri/ =404;
        add_header Cache-Control "no-store";
    }
}
EOF
sudo rm -f /etc/nginx/sites-enabled/default
sudo ln -sf /etc/nginx/sites-available/geronex /etc/nginx/sites-enabled/geronex
sudo nginx -t
sudo systemctl enable --now nginx >/dev/null 2>&1
sudo systemctl reload nginx
# Si el firewall de Ubuntu está activo, se abre el 80 (en Azure suele estar apagado)
if sudo ufw status 2>/dev/null | grep -q "Status: active"; then sudo ufw allow 80/tcp; fi

# ---------------------------------------------------------------------
paso "7/7 Cuentas de demo"
# Se crean si todavía no hay ninguna cuenta (también si una corrida anterior se interrumpió)
if [ "$(mysql_admin -N -e "SELECT COUNT(*) FROM geronex.usuario")" = 0 ]; then
    API=http://127.0.0.1:8080/api
    post() { curl -s -m 10 -H 'Content-Type: application/json' "$@"; }
    post -d @"$DIR/semilla/registro_admin.json" "$API/auth/registro" >/dev/null
    post -d @"$DIR/semilla/registro_cliente.json" "$API/auth/registro" >/dev/null
    mysql_admin < "$DIR/db/05_primer_administrador.sql" >/dev/null
    TOKEN="$(post -d @"$DIR/semilla/login_admin.json" "$API/auth/login" | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')"
    post -H "Authorization: Bearer $TOKEN" -d @"$DIR/semilla/crear_agente.json" "$API/admin/usuarios" >/dev/null
    post -H "Authorization: Bearer $TOKEN" -d @"$DIR/semilla/crear_responsable.json" "$API/admin/usuarios" >/dev/null
    mysql_admin -e "SELECT u.nombre_usuario, r.nombre AS rol FROM geronex.usuario u JOIN geronex.rol r ON r.id_rol = u.id_rol"
else
    echo "Ya existían: no se crean de nuevo."
fi

echo
echo "====================================================="
curl -s http://127.0.0.1/api/salud; echo
echo "LISTO. Abre en el navegador:  http://${IP_PUBLICA:-<IP pública>}"
echo "====================================================="
