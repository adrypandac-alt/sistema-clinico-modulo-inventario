# Pruebas de navegación mediante teclado

| Pantalla | Acción/tecla | Esperado | Obtenido | Estado |
|---|---|---|---|---|
| Login | Tab + Enter | recorrer campos e iniciar | orden nativo y foco visible | Pasa |
| Menú | Tab + Enter | abrir cada ruta | enlaces nativos | Pasa |
| Dashboard | Tab | acceder a acciones | controles accesibles | Pasa |
| Producto | Enter en botón | abrir modal | abre y enfoca campo | Pasa |
| Modal | Tab/Shift+Tab | mantener foco | ciclo dentro del modal | Pasa |
| Modal | Escape | cerrar | cierra | Pasa |
| Modal | cerrar | volver al disparador | no siempre restaurado | Pendiente |
| Select | flechas | cambiar opción | control nativo | Pasa |
| Tabla | Tab | recorrer acciones | botones/enlaces accesibles | Pasa |
| Accesibilidad | Enter/espacio | cambiar preferencia | `aria-pressed` actualizado | Pasa |
| Menú móvil | Enter | abrir panel | `aria-expanded=true` | Pasa |
| Menú móvil | Escape | cerrar panel | cierra; foco no siempre restaurado | Parcial |
| Cerrar sesión | Tab + Enter | terminar sesión | enlace nativo | Pasa |

No detecté una trampa permanente de teclado. La retención del foco en modales se libera con Escape.

