# =====================================================================
# GERONEX - Sube el proyecto a la VM de Azure y lo instala (se corre en la laptop).
#
# Uso (PowerShell, desde la carpeta Geronex2):
#     powershell -ExecutionPolicy Bypass -File azure\subir_a_azure.ps1
#
# Pasos: compila el JAR del backend, junta web + scripts SQL, los copia a la VM
# con scp y ejecuta instalar_en_vm.sh por ssh. Se puede correr las veces que sea.
# =====================================================================
param(
    [string]$Ip = "20.69.91.86",
    [string]$Usuario = "azureuser",
    [string]$Llave = "D:\A S U S\batiz2\GERONEX\cosas de azure\DavidVerguita_key.pem",
    [switch]$SoloPreparar,         # solo compila y junta los archivos, sin conectarse a la VM
    # Contraseña del usuario geronex_app de MySQL en la VM. Si no se da, se pregunta (Enter = una al azar).
    # Nunca se guarda en el repositorio: viaja en un archivo temporal que la VM borra al usarlo.
    [string]$ClaveBD = $env:GERONEX_DB_PASSWORD
)
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot           # carpeta Geronex2
$web = Join-Path $raiz "GERONEX_Equipo5"
$backend = Join-Path $web "geronex-backend"
$subida = Join-Path $PSScriptRoot "_subida"        # carpeta temporal (está en .gitignore)

function Paso($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }

if (-not $SoloPreparar) {
Paso "1/5 Revisando la llave y la conexión con $Ip"
if (-not (Test-Path $Llave)) { throw "No encuentro la llave en: $Llave" }
# OpenSSH de Windows rechaza llaves que otros usuarios pueden leer: se deja solo para ti.
icacls $Llave /inheritance:r /grant:r "$($env:USERNAME):R" | Out-Null
$ssh = @("-i", $Llave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15", "-o", "BatchMode=yes")
& ssh @ssh "$Usuario@$Ip" "echo conectado a `$(hostname)"
if ($LASTEXITCODE -ne 0) { throw "No hay conexión SSH. ¿La VM está encendida y el puerto 22 abierto en el NSG?" }

}

Paso "2/5 Compilando el backend (JAR)"
# Se usa el Maven que trae IntelliJ (no hace falta instalar otro)
$mvn = Get-Item "D:\Program Files\IntelliJ*\plugins\maven-plugin\lib\maven3\bin\mvn.cmd",
                "C:\Program Files\JetBrains\*\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue |
       Select-Object -First 1
if (-not $mvn) { throw "No encontré Maven. Abre el proyecto en IntelliJ y usa Maven > package." }
# El backend pide Java 21 o más nuevo; el JAVA_HOME del sistema puede ser Java 11, así que se elige aquí.
$java = @("C:\Program Files\Java\jdk-23") + (Get-Item "D:\Program Files\IntelliJ*\jbr" -ErrorAction SilentlyContinue).FullName |
        Where-Object { $_ -and (Test-Path "$_\bin\java.exe") } | Select-Object -First 1
if (-not $java) { throw "No encontré Java 21+ (jdk-23 o el JBR de IntelliJ)." }
$env:JAVA_HOME = $java
Push-Location $backend
& $mvn.FullName -q -DskipTests package
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "Falló la compilación del backend." }
$jar = Get-ChildItem (Join-Path $backend "target") -Filter "geronex-backend-*.jar" | Select-Object -First 1

Paso "3/5 Juntando archivos"
if (Test-Path $subida) { Remove-Item $subida -Recurse -Force }
New-Item -ItemType Directory -Force "$subida\web", "$subida\db", "$subida\semilla" | Out-Null
Copy-Item "$web\*.html" "$subida\web"
Copy-Item "$web\css", "$web\js", "$web\img" "$subida\web" -Recurse
Copy-Item (Join-Path $raiz "db\0*.sql") "$subida\db"
foreach ($f in "registro_admin", "registro_cliente", "login_admin", "crear_agente", "crear_responsable") {
    Copy-Item "$backend\pruebas\$f.json" "$subida\semilla"
}
Copy-Item $jar.FullName "$subida\geronex-backend.jar"
Copy-Item "$PSScriptRoot\instalar_en_vm.sh" $subida

if ($SoloPreparar) { Write-Host "Archivos listos en $subida"; exit 0 }

# Contraseña de MySQL: la VM la usa y borra el archivo; aquí se borra al terminar la copia.
if (-not $ClaveBD -and [Environment]::UserInteractive) {
    $segura = Read-Host "Contraseña para MySQL en la VM (Enter = generar una al azar)" -AsSecureString
    $ClaveBD = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
}
$archivoClave = Join-Path $subida "clave_bd.txt"
if ($ClaveBD) { [IO.File]::WriteAllText($archivoClave, $ClaveBD) }

Paso "4/5 Copiando a la VM (el JAR pesa ~60 MB, puede tardar)"
& ssh @ssh "$Usuario@$Ip" "rm -rf ~/geronex_subida"
& scp @ssh -r -q $subida "$Usuario@${Ip}:geronex_subida"
$copia = $LASTEXITCODE
Remove-Item $archivoClave -Force -ErrorAction SilentlyContinue
if ($copia -ne 0) { throw "Falló la copia con scp." }

Paso "5/5 Instalando en la VM"
& ssh @ssh "$Usuario@$Ip" "sed -i 's/\r$//' ~/geronex_subida/instalar_en_vm.sh && bash ~/geronex_subida/instalar_en_vm.sh $Ip"
if ($LASTEXITCODE -ne 0) { throw "La instalación falló: lee el mensaje de arriba." }

Write-Host "`nListo: abre http://$Ip en el navegador." -ForegroundColor Green
