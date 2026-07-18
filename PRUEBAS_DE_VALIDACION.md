# Pruebas de validación

| Campo/caso | Valor | Esperado | Obtenido/capa | Estado |
|---|---|---|---|---|
| Login vacío | `""` | rechazar | Servlet: complete credenciales | Pasa |
| Texto con espacios | `"   "` | rechazar | `isBlank`/RequestValidator | Pasa |
| Cantidad con letras | `abc` | rechazar sin error 500 | RequestValidator/Servlet | Pasa |
| Cantidad negativa | `-1` | rechazar | backend y HTML | Pasa |
| Cantidad cero | `0` | rechazar movimiento/pedido | backend | Pasa |
| Precio negativo | `-2.50` | rechazar | InventarioServlet | Pasa |
| Precio con >2 decimales | `1.999` | rechazar/redondeo definido | falta regla backend explícita | Pendiente |
| Fecha inválida | `2026-02-30` | rechazar | parseo fecha | Pasa |
| Caducidad pasada | fecha anterior | rechazar alta | InventarioServlet | Pasa |
| Correo incorrecto | `a@` | rechazar | usuario/proveedor/venta | Pasa |
| Cédula/RUC incorrecto | `123` | rechazar | RequestValidator/proveedor | Pasa |
| Teléfono incorrecto | letras | rechazar | proveedor/usuario | Pasa |
| Código duplicado | SKU existente | rechazar | InventarioServlet | Pasa |
| Identificador inexistente | `999999` | rechazar | búsqueda backend | Pasa |
| Stock insuficiente | cantidad > stock | no despachar/vender | servicios/servlet | Pasa |
| Usuario sin permiso | rol lectura | 403/no interacción | filtro y control | Pasa |
| Inyección SQL | `' OR 1=1 --` | tratar como dato | PreparedStatement | Pasa por inspección |
| Clave foránea inválida | ID inexistente | no persistir | búsqueda/BD | Pasa |
| Opcional vacío | observación/dirección | permitir | backend | Pasa |
| Excede columna | > máximo | rechazar | parcial según formulario | Pendiente global |

Las pruebas “Pasa por inspección” deben complementarse con integración contra una base de prueba antes de producción.

