# Estructura real del proyecto

```text
src/
├─ main/java/com/nexodist/
│  ├─ filters/
│  ├─ listeners/
│  ├─ model/
│  ├─ service/
│  ├─ servlet/
│  ├─ storage/
│  └─ util/
├─ main/webapp/
│  ├─ css/
│  ├─ includes/
│  ├─ js/
│  ├─ WEB-INF/
│  └─ *.jsp
└─ test/java/com/nexodist/
```

- En `servlet` recibo solicitudes, verifico sesión/permisos, valido parámetros y selecciono la vista o redirección.
- En `service` almaceno reglas reutilizables de dashboard, actividad y ventas. El proyecto no posee servicios para todas las entidades.
- En `storage` ejecuto JDBC contra MySQL. Aquí persisto productos, usuarios, movimientos, pedidos, ventas y seguridad.
- En `model` represento la información transportada entre control, negocio, persistencia y vista.
- En `util` concentro hash de contraseñas, cifrado de datos y validaciones de solicitudes.
- En `filters` aplico conteo de solicitudes y modo de solo lectura.
- En `listeners` inicializo el contexto, cargo datos y observo sesiones.
- En `webapp` presento la interfaz JSP del monolito.
- En `includes` reutilizo menú lateral y barra superior.
- En `css` mantengo el diseño general y la accesibilidad centralizada.
- En `js` actualizo el dashboard y gestiono preferencias accesibles.
- En `WEB-INF` configuro el despliegue web.
- En `test` compruebo reglas de caja blanca y caja negra.
- En `Entregables/SQL` mantengo el esquema MySQL de 24 tablas.
- En `target` Maven genera clases y WAR; no es código fuente.

No encuentro carpetas DAO, DTO o repository, por lo que no las atribuyo a la arquitectura.

