# Auditoría de navegación por teclado

| Área | Tab/Shift+Tab | Enter/Espacio | Escape | Foco/restauración | Estado |
|---|---|---|---|---|---|
| Login | Orden lógico | botón nativo | No aplica | foco visible | Conforme |
| Menú lateral | enlaces secuenciales | enlaces nativos | No aplica | indicador visible | Conforme |
| Accesibilidad escritorio | botones secuenciales | botones nativos | No aplica | `aria-pressed` | Conforme |
| Accesibilidad móvil | botón y panel | abre con botón | cierra panel | restauración pendiente | Parcial |
| Modales | foco limitado al diálogo | controles nativos | cierra modal | apertura sí; restauración al disparador pendiente | Parcial |
| Formularios | orden DOM | submit nativo | modal cerrable | errores globales | Parcial: falta mensaje por campo en varias vistas |
| Tablas | acciones reciben Tab | botones/enlaces nativos | No aplica | foco visible | Conforme |
| Filtros | botones/select nativos | funcionan | No aplica | visible | Conforme |
| Gráficos | no bloquean el foco | no requieren interacción | No aplica | información duplicada en texto | Conforme |

No se utilizan `tabindex` positivos ni atajos globales de una sola letra. El foco del modal se mantiene hasta cerrarlo, lo cual es la única retención intencional.

