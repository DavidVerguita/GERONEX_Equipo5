@echo off
rem GERONEX - Conecta el telefono por USB al backend (adb reverse) y, si quieres, instala la app.
rem Repitelo cada vez que desconectes y vuelvas a conectar el cable.
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if not exist "%ADB%" (echo No se encontro adb en "%ADB%". Abre Android Studio una vez para instalar el SDK. & pause & exit /b 1)

echo Telefonos conectados:
"%ADB%" devices
"%ADB%" reverse tcp:8080 tcp:8080 || (echo. & echo No hay telefono listo. Revisa el cable, la depuracion USB y acepta el aviso en el telefono. & pause & exit /b 1)
echo Listo: en el telefono, localhost:8080 ahora llega a esta computadora.

set APK=%~dp0GeronexAndroid\app\build\outputs\apk\debug\app-debug.apk
echo.
choice /m "Instalar o actualizar la app GERONEX en el telefono"
if errorlevel 2 goto fin
if not exist "%APK%" (echo No existe el APK. Compilalo en Android Studio con Build ^> Build APK. & goto fin)
"%ADB%" install -r "%APK%"
:fin
pause
