@echo off
setlocal
cd /d "%~dp0"

echo Iniciando Sistema Clinico con Payara Micro...
echo URL: http://localhost:8081/SistemaClinico/
echo Base de datos: %SISTEMA_DB_URL%
echo Para detener el servidor presione Ctrl+C.
echo.

if "%SISTEMA_DB_URL%"=="" set "SISTEMA_DB_URL=jdbc:mysql://localhost:3306/ase_proyecto_integrador?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
if "%SISTEMA_DB_USER%"=="" set "SISTEMA_DB_USER=root"
if "%SISTEMA_DB_PASSWORD%"=="" set "SISTEMA_DB_PASSWORD="

java -Dsistema.db.url="%SISTEMA_DB_URL%" -Dsistema.db.user="%SISTEMA_DB_USER%" -Dsistema.db.password="%SISTEMA_DB_PASSWORD%" -jar "payara-micro.jar" --deploy "SistemaClinico.war" --contextroot SistemaClinico --port 8081 --noCluster
pause
