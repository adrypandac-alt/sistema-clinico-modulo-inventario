# Flujos del módulo de inventario

| Flujo | Inicio / controlador | Servicio o persistencia | Tablas principales | Vista / resultado | Validaciones y errores |
|---|---|---|---|---|---|
| Inicio de sesión | `index.jsp` -> `LoginServlet` | `SeguridadStorage`, `DatosStorage` | `usuario`, `intentos_login`, `sesion_usuario` | Redirección a `/panel` | Campos, BCrypt, usuario activo, bloqueo 3 min |
| Dashboard | `PanelServlet` | `DashboardService`, `VentaService` | Datos cargados de producto/movimiento/venta | `panel.jsp` y `/api/dashboard` | Sesión y panel permitido |
| Registrar producto | `inventario.jsp` -> `InventarioServlet` | `DatosStorage.guardarProductos` | `producto`, `lote_producto`, cat./proveedor | `/inventario` | Obligatorios, numéricos, fecha, SKU duplicado |
| Modificar producto | `InventarioServlet` acción editar | `DatosStorage` | `producto`, `lote_producto` | `/inventario` | ID existente, duplicado, rangos y permisos |
| Ingreso de mercadería | `movimientos.jsp` -> `MovimientosServlet` | `DashboardService`, `DatosStorage` | `movimiento`, detalle, producto | `/movimientos` | Cantidad positiva, producto, tipo y motivo |
| Crear lote | Formulario de producto | `DatosStorage.guardarProductos` | `lote_producto` | Producto actualizado | Lote obligatorio y fecha válida |
| Actualizar existencias | Movimiento/venta/despacho | `DashboardService` | producto, movimiento y detalle | Dashboard/inventario | Stock no negativo e identificación existente |
| Despacho | `pedidos.jsp` -> `PedidoServlet` | `DashboardService`, `DatosStorage` | pedido, detalle, producto, movimiento | `/pedidos` | Estado, stock, receptor y permisos |
| Registrar movimiento | `MovimientosServlet` | `DashboardService.registrarMovimiento` | movimiento y detalle | `movimientos.jsp` | Tipo cerrado, cantidad positiva, stock suficiente |
| Alertas | `AlertasServlet` | `DashboardService` | producto y lote cargados | `alertas.jsp` | Caducidad y stock mínimo |
| Proveedores | `ProveedorServlet` | Estado de contexto; proveedor base en SQL | `proveedor` y relacionadas | `proveedor.jsp` | RUC, correo, teléfono, obligatorios |
| Usuarios | `UsuariosServlet` | `DatosStorage`, `PasswordUtil` | usuario y rol | `usuarios.jsp` | correo, contraseña, teléfono, rol y permisos |
| Roles/permisos | `UsuariosServlet` | `DatosStorage.guardarRol` | rol y usuario | `usuarios.jsp` | nombre, panel permitido y rol en uso |
| Modo oscuro | Barra superior | `accessibility.js` | `localStorage` | Todas las JSP | Botón y preferencia disponibles |
| Modo daltonismo | Barra superior | `accessibility.js`, evento de gráficos | `localStorage` | CSS + Chart.js | Color acompañado de texto/icono/borde |
| Tamaño de texto | A−/A+ | `accessibility.js` | `localStorage` | 16/18/20 px | Límites normal y extra grande |
| Teclado | Todas las vistas | `dashboard.js`, HTML nativo | No aplica | Foco, menús y modales | Tab, Shift+Tab, Enter, espacio y Escape |

No existe un DAO por flujo. Cuando la tabla indica `DatosStorage`, esa clase es el gateway JDBC real.

