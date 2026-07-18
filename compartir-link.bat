@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo   COMPARTIR SISTEMA CLINICO POR LINK
echo ========================================
echo.

if not exist ".tools" mkdir ".tools"

if not exist ".tools\cloudflared.exe" (
    echo Descargando Cloudflare Tunnel...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile '.tools\cloudflared.exe'"
    if errorlevel 1 (
        echo.
        echo No se pudo descargar cloudflared. Revise su conexion a internet.
        pause
        exit /b 1
    )
)

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo Payara no esta activo. Abriendo servidor local...
    start "Payara - Sistema Clinico" cmd /k ""%~dp0ejecutar-payara.bat""
    echo Esperando a que la aplicacion responda...
    powershell -NoProfile -Command "$ok=$false; for($i=0;$i -lt 45;$i++){ try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/SistemaClinico/' -TimeoutSec 2; if($r.StatusCode -eq 200){$ok=$true; break} } catch {}; Start-Sleep -Seconds 2 }; if($ok){exit 0}else{exit 1}"
    if errorlevel 1 (
        echo.
        echo La aplicacion no respondio en http://localhost:8081/SistemaClinico/
        echo Revise que MySQL este iniciado y que Payara no haya mostrado errores.
        pause
        exit /b 1
    )
) else (
    echo Payara ya esta activo en el puerto 8081.
)

echo.
echo Cuando aparezca el enlace de trycloudflare.com, comparta este formato:
echo   https://xxxxx.trycloudflare.com/SistemaClinico/
echo.
echo Mantenga esta ventana abierta mientras sus companeros usen el sistema.
echo Para cerrar el link publico, presione Ctrl+C en esta ventana.
echo.

".tools\cloudflared.exe" tunnel --url http://localhost:8081
pause
