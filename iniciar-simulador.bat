@echo off
REM Inicia el sistema con el simulador del SRI y la ANT.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1" -Mock
pause
