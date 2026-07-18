Sistema Clinico Inventario - MySQL

1. Inicie MySQL local.
2. Ejecute mysql-configuracion.sql con un usuario administrador.
3. Configure estas variables antes de ejecutar Payara:

   set SISTEMA_DB_URL=jdbc:mysql://localhost:3306/ase_proyecto_integrador?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   set SISTEMA_DB_USER=root
   set SISTEMA_DB_PASSWORD=

4. Ejecute:

   ejecutar-payara.bat

La aplicacion usa la estructura ASE: rol, modulo, usuario, producto, movimiento, movimiento_detalle, lote_producto, venta y venta_detalle.
