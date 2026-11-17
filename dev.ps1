# Levanta todo el sistema en segundo plano (sin abrir varias ventanas de PowerShell).
#   .\dev.ps1          -> servicios reales (SRI/ANT)
#   .\dev.ps1 -Mock    -> con el simulador local del SRI/ANT
#   .\dev.ps1 -Stop    -> detiene todo
param([switch]$Mock, [switch]$Stop)

$root    = $PSScriptRoot
$pidFile = Join-Path $root '.dev-pids'
$logDir  = Join-Path $root 'logs'

function Stop-Dev {
    if (Test-Path $pidFile) {
        Get-Content $pidFile | ForEach-Object {
            if ($_ -match '^\d+$') { cmd /c "taskkill /PID $_ /T /F >nul 2>&1" }
        }
        Remove-Item $pidFile -Force
        Write-Host 'Servicios detenidos.' -ForegroundColor Yellow
    } else {
        Write-Host 'No hay servicios registrados.'
    }
}

if ($Stop) { Stop-Dev; return }
if (Test-Path $pidFile) { Stop-Dev }   # limpia una ejecución anterior

# --- Cargar .env ---
$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) { throw 'Falta el archivo .env. Copia .env.example a .env y pon tu REDIS_URL de Upstash.' }
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*?)\s*$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim('"'), 'Process')
    }
}
if (-not $env:REDIS_URL -or $env:REDIS_URL -like '*CAMBIAR*') { throw 'Configura REDIS_URL en el archivo .env' }

# --- Java 21 (el proyecto compila con release 21) ---
# Si JAVA_HOME apunta a otra versión, se busca un JDK 21 instalado y se usa solo para esta ejecución.
function Test-Jdk21($dir) {
    $rel = Join-Path $dir 'release'
    return (Test-Path $rel) -and ((Get-Content $rel -Raw) -match 'JAVA_VERSION="21')
}
if (-not ($env:JAVA_HOME -and (Test-Jdk21 $env:JAVA_HOME))) {
    $jdk = Get-ChildItem 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft' `
               -Directory -ErrorAction SilentlyContinue |
           Where-Object { Test-Jdk21 $_.FullName } | Select-Object -First 1
    if (-not $jdk) { throw 'No se encontró un JDK 21. Instálalo o configura JAVA_HOME con un JDK 21.' }
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = (Join-Path $jdk.FullName 'bin') + ';' + $env:Path
}
Write-Host "Usando Java: $env:JAVA_HOME"

# --- Modo simulador ---
if ($Mock) {
    $env:SRI_CATASTRO_URL = 'http://localhost:9000/sri/catastro'
    $env:SRI_VEHICULO_URL = 'http://localhost:9000/sri/vehiculo'
    $env:ANT_URL          = 'http://localhost:9000/ant/clp_grid_citaciones.jsp'
    $env:SRI_CACHE_FRESH  = '0s'
    $env:ANT_CACHE_FRESH  = '0s'
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Start-Svc($name, $workDir, $cmd) {
    $log = Join-Path $logDir "$name.log"
    $p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "$cmd > `"$log`" 2>&1" `
         -WorkingDirectory $workDir -WindowStyle Hidden -PassThru
    Add-Content -Path $pidFile -Value $p.Id
    Write-Host "Iniciando $name  (log: logs\$name.log)"
}

function Wait-Port($port, $name, $timeoutSec) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $timeoutSec) {
        # Vite escucha en ::1 (IPv6) y Spring en IPv4: se prueban ambas direcciones.
        foreach ($ip in '127.0.0.1', '::1') {
            try {
                $c = New-Object Net.Sockets.TcpClient([Net.IPAddress]::Parse($ip).AddressFamily)
                $c.Connect($ip, $port)
                $c.Close()
                Write-Host "  OK  $name (puerto $port)" -ForegroundColor Green
                return
            } catch { }
        }
        Start-Sleep -Seconds 3
    }
    Write-Warning "$name no respondió en $timeoutSec s. Revisa logs\ para ver el error."
}

if ($Mock) { Start-Svc 'mock'        (Join-Path $root 'tools\mock-upstream') 'node server.js' }
Start-Svc 'sri-service' (Join-Path $root 'backend')  'mvn -q -pl sri-service spring-boot:run'
Start-Svc 'ant-service' (Join-Path $root 'backend')  'mvn -q -pl ant-service spring-boot:run'
Start-Svc 'frontend'    (Join-Path $root 'frontend') 'npm install && npm run dev'

Write-Host 'Esperando a que arranquen (la primera vez Maven descarga dependencias y puede tardar varios minutos)...'
Wait-Port 8081 'sri-service' 300
Wait-Port 8082 'ant-service' 300
Wait-Port 5173 'frontend'    300

Write-Host ''
Write-Host 'Listo: http://localhost:5173' -ForegroundColor Green
Write-Host 'Para detener todo: .\dev.ps1 -Stop   (o doble clic en detener.bat)'
Start-Process 'http://localhost:5173'
