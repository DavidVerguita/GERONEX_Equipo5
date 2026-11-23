@echo off
rem GERONEX - Conecta el telefono por USB al backend (adb reverse) y, si quieres, instala la app.
rem Repitelo cada vez que desconectes y vuelvas a conectar el cable.
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if not exist "%ADB%" (echo No se encontro adb en "%ADB%". Abre Android Studio una vez para instalar el SDK. & pause & exit /b 1)

echo Telefonos conectados:
"%ADB%" devices
"%ADB%" reverse tcp:8080 tcp:8080 || (echo. & echo No hay telefono listo. Revisa el cable, la depuracion USB y acepta el aviso en el telefono. & pause & exit /b 1)
echo Listo: en el telefono, localhost:8080 ahora llega a esta computadora.

set APKS=%~dp0GeronexAndroid\app\build\outputs\apk
echo.
echo Que app quieres instalar?
echo   1 = Local: usa el backend de esta laptop por el cable (app-debug.apk)
echo   2 = Azure: usa la VM de Azure por internet (app-azure.apk)
echo   3 = Ninguna, ya esta instalada
choice /c 123 /m "Elige"
if errorlevel 3 goto fin
if errorlevel 2 (set APK=%APKS%\azure\app-azure.apk) else (set APK=%APKS%\debug\app-debug.apk)
if not exist "%APK%" (echo No existe "%APK%". Compilalo en Android Studio: Build ^> Build APK. & goto fin)
"%ADB%" install -r "%APK%"
:fin
pause
