# Guía paso a paso para ejecutar Sistema Clínico con MySQL

Esta guía corresponde al proyecto ubicado en:

```text
C:\Users\adryp\Documents\proyecto integrador ASE
```

La aplicación usa Java 17 o superior, Maven, MySQL 8 y Payara Micro. Se abre en el puerto `8081` y usa la base `ase_proyecto_integrador`.

> Importante: el script SQL elimina y vuelve a crear la base de datos. Si ya contiene información importante, haga una copia antes de ejecutarlo.

## 1. Abrir PowerShell en la carpeta del proyecto

```powershell
cd "C:\Users\adryp\Documents\proyecto integrador ASE"
```

## 2. Comprobar los programas necesarios

Ejecute:

```powershell
java -version
mvn -version
mysql --version
```

Java debe ser versión 17 o superior. Si los tres comandos muestran sus versiones, continúe.

## 3. Comprobar que MySQL esté iniciado

```powershell
Get-Service *mysql*
```

Si el servicio aparece detenido, inícielo. El nombre habitual es `MySQL80`:

```powershell
Start-Service MySQL80
```

Si Windows rechaza el comando, abra PowerShell como administrador o inicie MySQL desde **Servicios**.

## 4. Crear la base de datos y cargar los datos iniciales

Entre al cliente MySQL:

```powershell
mysql -u root -p
```

Escriba la contraseña de `root`. Dentro de MySQL aparecerá el indicador `mysql>`; ejecute esta instrucción usando barras `/`:

```sql
SOURCE C:/Users/adryp/Documents/proyecto integrador ASE/Entregables/SQL/inventario_medico_modelo_relacional.sql;
```

Al terminar debe mostrarse el mensaje `Base de datos de inventario medico creada correctamente` y la lista de tablas.

Compruebe la base:

```sql
USE ase_proyecto_integrador;
SHOW TABLES;
SELECT id_usuario, nombre, email FROM usuario;
EXIT;
```

## 5. Configurar la conexión en la terminal

En la misma ventana de PowerShell, configure estas variables. Reemplace `SU_CLAVE_MYSQL` por la contraseña real de `root`:

```powershell
$env:SISTEMA_DB_URL = "jdbc:mysql://localhost:3306/ase_proyecto_integrador?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:SISTEMA_DB_USER = "root"
$env:SISTEMA_DB_PASSWORD = "SU_CLAVE_MYSQL"
```

Estas variables duran solamente mientras esa ventana de PowerShell permanezca abierta.

## 6. Compilar y ejecutar las pruebas

```powershell
mvn clean test package
```

La compilación es correcta cuando termina con `BUILD SUCCESS`. El archivo generado queda en:

```text
target\SistemaClinico.war
```

## 7. Iniciar Payara Micro y desplegar la aplicación

Sin cerrar la terminal anterior, ejecute:

```powershell
java "-Dsistema.db.url=$env:SISTEMA_DB_URL" "-Dsistema.db.user=$env:SISTEMA_DB_USER" "-Dsistema.db.password=$env:SISTEMA_DB_PASSWORD" -jar ".\ejecutable-payara\payara-micro.jar" --deploy ".\target\SistemaClinico.war" --contextroot SistemaClinico --port 8081 --noCluster
```

No cierre esa ventana. Espere hasta que Payara indique que la aplicación fue desplegada.

Como alternativa, después de configurar las tres variables puede usar:

```powershell
.\ejecutar-payara.bat
```

## 8. Abrir el sistema

Abra en el navegador:

```text
http://localhost:8081/SistemaClinico/
```

Credenciales iniciales:

```text
Administrador:
admin@sistema-clinico.local
admin123

Operativo:
operativo@sistema-clinico.local
operativo123
```

## 9. Confirmar que está usando MySQL

Mientras Payara está ejecutándose, abra una segunda ventana de PowerShell y entre a MySQL:

```powershell
mysql -u root -p
```

Ejecute:

```sql
USE ase_proyecto_integrador;
SELECT COUNT(*) AS usuarios FROM usuario;
SELECT COUNT(*) AS productos FROM producto;
SELECT COUNT(*) AS movimientos FROM movimiento;
EXIT;
```

También puede crear o modificar un registro desde la aplicación, reiniciar Payara y comprobar que el cambio continúa. Eso confirma la persistencia real.

## 10. Detener el proyecto

Vuelva a la ventana donde se está ejecutando Payara y presione:

```text
Ctrl + C
```

## Solución de problemas

### Error `Access denied for user 'root'@'localhost'`

La contraseña de MySQL es incorrecta o no fue configurada en la terminal. Pruebe primero:

```powershell
mysql -u root -p
```

Luego vuelva a asignar `$env:SISTEMA_DB_PASSWORD` con esa misma contraseña.

### La aplicación indica `Memoria local (MySQL no disponible)`

MySQL no está iniciado o la URL, el usuario o la contraseña son incorrectos. Detenga Payara, corrija las variables del paso 5 y vuelva a iniciarlo.

### El puerto 8081 está ocupado

```powershell
Get-NetTCPConnection -LocalPort 8081 | Select-Object LocalAddress,LocalPort,State,OwningProcess
```

Identifique el proceso:

```powershell
Get-Process -Id ID_DEL_PROCESO
```

Si es una ejecución anterior de Payara, deténgala:

```powershell
Stop-Process -Id ID_DEL_PROCESO
```

### Los cambios visuales no aparecen

Presione `Ctrl + F5` en el navegador para hacer una recarga completa.

### Volver a ejecutar sin borrar la base

No repita el paso 4. Configure las variables, compile si hubo cambios y ejecute Payara desde los pasos 5, 6 y 7.
