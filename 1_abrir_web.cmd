@echo off
rem GERONEX - Abre la web en http://localhost:8090 (el backend debe estar encendido en IntelliJ).
rem Para cerrarla: cierra esta ventana o presiona Ctrl+C.
cd /d "%~dp0GERONEX_Equipo5"
where node >nul 2>nul || (echo No se encontro Node.js. Instalalo desde https://nodejs.org & pause & exit /b 1)
start "" http://localhost:8090
node servidor_web.mjs
pause
