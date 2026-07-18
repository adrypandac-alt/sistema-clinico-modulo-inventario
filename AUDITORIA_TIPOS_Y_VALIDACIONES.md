# Auditoría de tipos y validaciones

## Motor real

El código usa **MySQL 8/MariaDB**, no Oracle: dependencia `mysql-connector-j`, URL `jdbc:mysql`, `AUTO_INCREMENT`, `BOOLEAN` e `InnoDB`. Una conversión a Oracle requiere un proyecto de migración separado y no fue ejecutada.

| Elemento | Campo | Tipo actual | Recomendado | Validación actual/faltante | Riesgo | Cambio aplicado / pendiente |
|---|---|---|---|---|---|---|
| usuario | email | `VARCHAR(512)` cifrado | Mantener; hash auxiliar en evolución | correo backend | Medio | Cifrado aplicado; índice hash pendiente |
| usuario | password | `VARCHAR(255)` BCrypt | Mantener | mínimo 8 y hash | Bajo | Aplicado |
| usuario | teléfono | `VARCHAR(255)` cifrado | Mantener | caracteres/longitud | Bajo | Aplicado |
| producto | código/SKU | `VARCHAR(50)` | Mantener | obligatorio/único | Bajo | Aplicado |
| producto | precio | `DECIMAL(10,2)` / Java `double` | SQL `DECIMAL(12,2)`, Java `BigDecimal` | no negativo | Alto | Migración Java pendiente |
| producto | stock | `INT` | Mantener | >= 0 | Medio | Backend aplicado; CHECK recomendado |
| lote | fecha vencimiento | `DATE` | Mantener | parseo `LocalDate` | Bajo | Aplicado |
| lote | número | `VARCHAR(60)` | Mantener | requerido/longitud | Medio | Longitud HTML pendiente |
| proveedor | RUC | `VARCHAR(13)` | Mantener | 10/13 dígitos | Bajo | Aplicado |
| proveedor | teléfono | `VARCHAR(20)` | Mantener | 7–10 dígitos | Bajo | Cambiar HTML a `tel` aplicado pendiente |
| movimiento | cantidad | `INT` | Mantener | entero > 0 | Bajo | `RequestValidator`/backend aplicado |
| pedido | estado | `VARCHAR(30)` | `VARCHAR` + CHECK/catálogo | transición backend | Medio | CHECK pendiente |
| venta | totales | Java `double` | `BigDecimal` | recalculado | Alto | Pendiente por migración coordinada |
| intentos_login | fecha | `TIMESTAMP` | Mantener | ventana 3 minutos | Bajo | Aplicado |

## Coherencia y consultas

- Las consultas con entradas usan `PreparedStatement`; no encontré SQL formado directamente con parámetros HTTP.
- `asegurarColumna` concatena únicamente nombres/definiciones internas controladas por el código, no valores del usuario.
- Encontré operaciones compuestas sin una transacción JDBC que abarque producto + movimiento + pedido. Se documenta como pendiente de refactor porque actualmente memoria y SQL se actualizan en métodos separados.
- No ejecuté cambios destructivos de tipo ni conversión de motor.

