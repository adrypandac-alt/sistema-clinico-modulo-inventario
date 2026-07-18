@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo   SISTEMA CLINICO - PAYARA MICRO
echo ========================================
echo.

if not exist "target\SistemaClinico.war" (
    echo Generando aplicacion web...
    call mvn clean package
    if errorlevel 1 (
        echo.
        echo No se pudo compilar el proyecto.
        pause
        exit /b 1
    )
)

if not exist "ejecutable-payara\payara-micro.jar" (
    echo No se encontro ejecutable-payara\payara-micro.jar
    echo Vuelva a generar la distribucion Payara.
    pause
    exit /b 1
)

echo Aplicacion disponible en:
echo http://localhost:8081/SistemaClinico/
echo.
if "%SISTEMA_DB_URL%"=="" set "SISTEMA_DB_URL=jdbc:mysql://localhost:3306/ase_proyecto_integrador?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
if "%SISTEMA_DB_USER%"=="" set "SISTEMA_DB_USER=root"
if "%SISTEMA_DB_PASSWORD%"=="" set "SISTEMA_DB_PASSWORD="
echo Base de datos MySQL: %SISTEMA_DB_URL%
echo.
echo Para detener Payara presione Ctrl+C.
echo.

java -Dsistema.db.url="%SISTEMA_DB_URL%" -Dsistema.db.user="%SISTEMA_DB_USER%" -Dsistema.db.password="%SISTEMA_DB_PASSWORD%" -jar "ejecutable-payara\payara-micro.jar" --deploy "target\SistemaClinico.war" --contextroot SistemaClinico --port 8081 --noCluster
pause
