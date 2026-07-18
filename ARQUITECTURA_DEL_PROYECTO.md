# Arquitectura del proyecto

## Arquitectura identificada

Identifico una aplicación web **monolítica Jakarta EE**, organizada como un MVC práctico con capas parciales. No encuentro frontend separado, repositorios, DTO ni una capa DAO formal.

- **Vista:** almaceno las JSP en `src/main/webapp` y los recursos CSS/JavaScript en `css` y `js`.
- **Control:** recibo HTTP mediante clases `HttpServlet` anotadas con `@WebServlet`.
- **Negocio:** concentro parte de las reglas en `DashboardService`, `VentaService` y `ActividadService`; otras reglas permanecen dentro de los servlets.
- **Persistencia:** concentro JDBC en `DatosStorage` y `SeguridadStorage`. Estas clases cumplen una función similar a un gateway/DAO, aunque el proyecto no las denomina DAO.
- **Modelo:** transporto estado con POJO serializables como `Producto`, `Movimiento`, `Pedido`, `Usuario` y `Venta`.
- **Infraestructura:** inicializo datos y contexto en `AppContextListener`; aplico controles transversales con filtros.

## Evidencias reales

- `LoginServlet` recibe credenciales, valida sesión y delega persistencia de seguridad.
- `InventarioServlet` procesa los formularios de productos y presenta `inventario.jsp`.
- `DashboardService` calcula indicadores, alertas y movimientos.
- `DatosStorage` ejecuta SQL mediante JDBC y `PreparedStatement`.
- `PanelServlet` prepara atributos y reenvía a `panel.jsp`.
- `ApiDashboardServlet` expone JSON para la actualización de gráficos.
- `SoloLecturaFilter` y comprobaciones de `DashboardService` aplican permisos.

## Comunicación entre capas

```text
Navegador -> Servlet -> validación/regla -> Servicio (cuando existe)
          -> DatosStorage/SeguridadStorage -> MySQL
          -> atributos request/session -> JSP -> HTML/CSS/JavaScript
```

## Ejemplo real: registro de producto

1. `inventario.jsp` envía POST a `/inventario`.
2. `InventarioServlet.doPost` valida sesión, permiso, campos, números, fecha y duplicados.
3. El servlet actualiza el modelo `Producto` mantenido en el contexto.
4. `DatosStorage.guardarProductos` utiliza sentencias parametrizadas para persistir producto y lote.
5. La respuesta redirige nuevamente a `/inventario`.

## Ventajas actuales

- Despliegue sencillo en un único WAR.
- Rutas y responsabilidades de presentación fáciles de localizar.
- Consultas parametrizadas centralizadas en dos clases.
- Modelos reutilizados por vistas, servicios y persistencia.
- Control de sesión, roles y permisos existente.

## Acoplamientos y riesgos encontrados

- Algunos servlets contienen reglas de negocio que deberían estar en servicios.
- `DatosStorage` concentra demasiadas entidades y responsabilidades.
- El contexto de Servlet funciona como memoria compartida y se sincroniza manualmente.
- Varias operaciones actualizan memoria y luego varias tablas sin una transacción JDBC única.
- Los importes usan `double`; recomiendo `BigDecimal` mediante una migración coordinada.
- El esquema y el código reales son MySQL, no Oracle.
- Existen pantallas legadas (`productos.jsp`, `carrito.jsp`) además del flujo principal.

## Recomendaciones

1. Extraer gateways por entidad sin cambiar primero las rutas.
2. Mover despacho, movimiento y producto a servicios transaccionales.
3. Migrar dinero a `BigDecimal` con respaldo y pruebas de compatibilidad.
4. Definir migraciones versionadas en lugar de ejecutar `ALTER TABLE` al iniciar.
5. Mantener el validador compartido y ampliar pruebas de integración.

## Archivos principales por responsabilidad

| Responsabilidad | Archivos reales |
|---|---|
| Control HTTP | `servlet/*.java` |
| Negocio | `service/DashboardService.java`, `VentaService.java`, `ActividadService.java` |
| Persistencia | `storage/DatosStorage.java`, `SeguridadStorage.java` |
| Modelo | `model/*.java` |
| Validación y seguridad | `util/RequestValidator.java`, `PasswordUtil.java`, `DataProtectionUtil.java` |
| Presentación | `webapp/*.jsp`, `includes/*.jsp` |
| Accesibilidad | `css/accessibility.css`, `js/accessibility.js` |
| Ciclo de vida | `listeners/AppContextListener.java` |
| Filtros | `filters/*.java` |

