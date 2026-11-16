@echo off
REM Doble clic: levanta todo con el simulador. Para servicios reales, quita la palabra -Mock.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0dev.ps1" -Mock
pause
