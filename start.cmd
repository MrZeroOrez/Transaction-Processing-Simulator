@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (echo Install JDK 21 and add Java to PATH. & pause & exit /b 1)
where npm >nul 2>nul
if errorlevel 1 (echo Install Node 20.19 or newer and reopen this terminal. & pause & exit /b 1)
echo Starting backend and frontend in separate windows. First start downloads dependencies.
start "Backend" "%ComSpec%" /k call "%~dp0run-backend.cmd"
start "Frontend" "%ComSpec%" /k call "%~dp0run-frontend.cmd"
echo Read the Frontend window for the local browser URL. Stop BOTH windows with Ctrl+C.
pause
