@echo off
REM Inicia el sistema contra el SRI y la ANT reales.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1"
pause
