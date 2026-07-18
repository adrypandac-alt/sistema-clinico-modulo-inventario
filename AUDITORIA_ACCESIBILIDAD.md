# Auditoría de accesibilidad

| Pantalla/área | Texto | Oscuro | Daltonismo | Teclado/formularios | Cambios realizados | Pendiente |
|---|---|---|---|---|---|---|
| Login | >=14 px | Sí | Sí | etiquetas y foco | CSS/JS central | Prueba con lector real |
| Dashboard | Escala global | Sí | Sí, también gráficos | enlaces y controles nativos | leyendas 14 px, puntos/formas | Prueba WCAG automatizada |
| Productos | Escala global | Sí | estados con icono/texto | modal y campos | stock/caducidad no dependen del color | Revisión con zoom 400% |
| Proveedores | Escala global | Sí | Sí | modal accesible | foco, Escape y etiquetas | mensajes por campo |
| Movimientos | Fechas >=14 px | Sí | entrada/salida textual | filtros/botones | tipo, icono, texto y forma | flechas de filtros no aplican |
| Despachos | Escala global | Sí | estados visibles | formularios y confirmación | responsive | prueba completa sin ratón |
| Alertas | Escala global | Sí | icono + texto + borde | filtros nativos | crítico/bajo/caducidad explícitos | lector real |
| Usuarios | Escala global | Sí | Sí | modales con foco | contraseña y formularios | mensajes específicos por campo |
| Tablas | 15/16 px | Sí | no solo color | controles internos accesibles | scroll horizontal | agregar `scope` a todas las legadas |

Los controles guardan `inventario_font_size`, `inventario_dark_mode` e `inventario_colorblind_mode`. La comprobación manual verificó persistencia después de recargar y menú móvil accesible.

