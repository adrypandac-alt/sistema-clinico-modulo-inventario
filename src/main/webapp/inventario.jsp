<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String js(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
%>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    List<String> categorias = (List<String>) request.getAttribute("categorias");
    Integer alertasCount = (Integer) request.getAttribute("alertasCount");
    request.setAttribute("scPagina", "inventario");
    request.setAttribute("alertasCount", alertasCount);
    boolean puedeEditar = DashboardService.puedeModificarInventario(usuario);
    boolean esAdministrador = DashboardService.esAdministrador(usuario);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Inventario médico - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Inventario</h1>
                <p class="sc-page-sub"><%= productos != null ? productos.size() : 0 %> medicamentos, insumos y equipos</p>
            </div>
            <% if (puedeEditar) { %>
            <div class="sc-header-actions">
                <button class="sc-btn-primary" onclick="abrirNuevoProducto()">+ Ingreso de producto</button>
                <button class="sc-btn-secondary-action" onclick="document.getElementById('modal-categoria').classList.remove('sc-hidden')">+ Categoría</button>
                <a class="sc-btn-cart" href="pedidos">Despachos</a>
            </div>
            <% } else { %><span class="sc-readonly-pill">Solo lectura</span><% } %>
        </div>

        <% if ("1".equals(request.getParameter("categoriaCreada"))) { %>
            <div class="sc-success-box">Categoría creada correctamente</div>
        <% } %>
        <% if ("duplicada".equals(request.getParameter("categoriaError"))) { %>
            <div class="sc-error-box">La categoría ya existe</div>
        <% } else if ("vacia".equals(request.getParameter("categoriaError"))) { %>
            <div class="sc-error-box">El nombre de categoría es obligatorio</div>
        <% } %>
        <%
            String errorParam = request.getParameter("error");
            if ("camposObligatorios".equals(errorParam)) { %>
            <div class="sc-error-box">Complete todos los campos obligatorios antes de guardar.</div>
        <% } else if ("valoresNegativos".equals(errorParam)) { %>
            <div class="sc-error-box">El precio y el stock deben ser mayores a cero. El stock mínimo debe ser al menos 1.</div>
        <% } else if ("formatoNumerico".equals(errorParam)) { %>
            <div class="sc-error-box">Verifique que precio, stock y stock mínimo sean números válidos.</div>
        <% } else if ("fechaPasada".equals(errorParam)) { %>
            <div class="sc-error-box">La fecha de caducidad no puede ser anterior a hoy al registrar un nuevo producto.</div>
        <% } else if ("fechaInvalida".equals(errorParam)) { %>
            <div class="sc-error-box">El formato de fecha de caducidad no es válido.</div>
        <% } else if ("skuDuplicado".equals(errorParam)) { %>
            <div class="sc-error-box">El SKU ingresado ya está registrado. Use un código único para cada producto.</div>
        <% } else if ("producto".equals(errorParam)) { %>
            <div class="sc-error-box">No se encontró el producto. Recargue la página e intente nuevamente.</div>
        <% } %>

        <div class="sc-search">
            <span>🔍</span>
            <input type="text" id="buscar-inventario" placeholder="Buscar por nombre o SKU..." oninput="filtrarInventario()">
        </div>

        <div class="sc-filters" id="filtros-cat">
            <button class="sc-filter-btn activo" data-cat="" onclick="setFiltroCat(this,'')">Todas</button>
            <% if (categorias != null) {
                for (String categoriaFiltro : categorias) { %>
            <button class="sc-filter-btn" data-cat="<%= categoriaFiltro %>" onclick="setFiltroCat(this,'<%= js(categoriaFiltro) %>')"><%= categoriaFiltro %></button>
            <%  }
            } %>
        </div>
        <div id="sin-productos-categoria" class="sc-empty-category sc-hidden">No existen productos en esta categoría</div>

        <div class="sc-card sc-table-wrap">
            <table class="sc-table" id="tabla-inventario">
                <thead>
                <tr>
                    <th>SKU</th>
                    <th>Producto</th>
                    <th>Categoría</th>
                    <th>Lote</th>
                    <th>Caducidad</th>
                    <th>Stock</th>
                    <th>Stock mín.</th>
                    <th>Precio</th>
                    <th>Acciones</th>
                </tr>
                </thead>
                <tbody>
                <% if (productos != null) {
                    for (Producto p : productos) {
                        boolean bajo = p.getStockMinimo() > 0 && p.getStock() < p.getStockMinimo();
                %>
                <tr data-cat="<%= p.getCategoria() %>" data-nombre="<%= p.getNombre().toLowerCase() %>" data-sku="<%= p.getSku().toLowerCase() %>">
                    <td><%= p.getSku() %></td>
                    <td>
                        <div class="sc-prod-name"><%= p.getNombre() %></div>
                        <div class="sc-prod-sub"><%= p.getProveedor() %></div>
                    </td>
                    <td><span class="sc-cat-pill"><%= p.getCategoria() %></span></td>
                    <td><%= p.getLote() %></td>
                    <td>
                        <span class="sc-expiry-badge <%= p.isCaducado() ? "expired" : (p.isProximoCaducar() ? "soon" : "ok") %>">
                            <%= p.getFechaCaducidad() == null || p.getFechaCaducidad().isBlank() ? "Sin fecha" : p.getFechaCaducidad() %>
                        </span>
                    </td>
                    <td class="<%= bajo ? "sc-stock-warn" : "" %>"><%= p.getStock() %></td>
                    <td><%= p.getStockMinimo() %></td>
                    <td>$<%= String.format(Locale.US, "%,.0f", p.getPrecio()) %></td>
                    <td>
                        <div class="sc-action-buttons">
                            <% if (puedeEditar) { %>
                            <button type="button" class="sc-action-btn edit" onclick="abrirEditarProducto('<%= p.getId() %>','<%= js(p.getNombre()) %>','<%= js(p.getSku()) %>','<%= js(p.getCategoria()) %>','<%= js(p.getProveedor()) %>','<%= p.getPrecio() %>','<%= p.getStock() %>','<%= p.getStockMinimo() %>','<%= js(p.getLote()) %>','<%= js(p.getFechaCaducidad()) %>','<%= js(p.getRegistroSanitario()) %>')">Modificar</button>
                            <% } %>
                            <% if (esAdministrador) { %>
                            <span class="sc-action-sep"></span>
                            <form action="inventario" method="post" class="sc-inline-form" onsubmit="return confirm('¿Está seguro de eliminar «<%= js(p.getNombre()) %>»? Esta acción no se puede deshacer.');">
                                <input type="hidden" name="accion" value="eliminar">
                                <input type="hidden" name="id" value="<%= p.getId() %>">
                                <button type="submit" class="sc-action-btn delete">Eliminar</button>
                            </form>
                            <% } %>
                            <% if (!puedeEditar && !esAdministrador) { %>
                            <span class="sc-readonly-pill">Consulta</span>
                            <% } %>
                        </div>
                    </td>
                </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </main>
</div>

<% if (puedeEditar) { %>
<div id="modal-producto" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal">
        <h3 id="modal-producto-titulo">Nuevo producto médico</h3>
        <form action="inventario" method="post" id="form-producto">
            <input type="hidden" name="accion" value="crear">
            <input type="hidden" name="id" value="">

            <p class="sc-modal-section-label">Identificación</p>
            <div class="sc-form-row-2">
                <div class="sc-form-group">
                    <label>Nombre del producto</label>
                    <input type="text" name="nombre" required placeholder="Ej. Paracetamol 500 mg">
                </div>
                <div class="sc-form-group">
                    <label>SKU <span class="sc-field-hint">(único)</span></label>
                    <input type="text" name="sku" placeholder="MED-099">
                </div>
            </div>
            <div class="sc-form-row-2">
                <div class="sc-form-group">
                    <label>Categoría médica</label>
                    <select name="categoria" required>
                        <% if (categorias != null) {
                            for (String categoriaOption : categorias) { %>
                        <option value="<%= categoriaOption %>"><%= categoriaOption %></option>
                        <%  }
                        } %>
                    </select>
                </div>
                <div class="sc-form-group">
                    <label>Proveedor</label>
                    <input type="text" name="proveedor" required placeholder="Nombre del proveedor">
                </div>
            </div>

            <p class="sc-modal-section-label">Stock y precio</p>
            <div class="sc-form-row-3">
                <div class="sc-form-group">
                    <label>Precio (USD)</label>
                    <input type="number" name="precio" step="0.01" min="0" required placeholder="0.00">
                </div>
                <div class="sc-form-group">
                    <label>Stock inicial</label>
                    <input type="number" name="stock" id="campo-stock" min="0" required placeholder="0">
                </div>
                <div class="sc-form-group">
                    <label>Stock mínimo</label>
                    <input type="number" name="stockMinimo" min="1" required placeholder="10">
                </div>
            </div>
            <div id="aviso-ajuste-stock" class="sc-info-box sc-hidden">
                Al cambiar el stock se registrará automáticamente un movimiento de ajuste en el historial.
            </div>

            <p class="sc-modal-section-label">Trazabilidad clínica</p>
            <div class="sc-form-row-2">
                <div class="sc-form-group">
                    <label>Número de lote</label>
                    <input type="text" name="lote" placeholder="MED-2601" required>
                </div>
                <div class="sc-form-group">
                    <label>Fecha de caducidad</label>
                    <input type="date" name="fechaCaducidad" id="campo-fecha-caducidad" required>
                </div>
            </div>
            <div id="aviso-fecha-pasada" class="sc-error-box sc-hidden" style="margin-bottom:8px;">
                La fecha de caducidad es anterior a hoy. Verifique antes de guardar.
            </div>
            <div class="sc-form-group">
                <label>Registro sanitario</label>
                <input type="text" name="registroSanitario" placeholder="RS-EC-2026-001" required>
            </div>

            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Guardar</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-producto').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>

<div id="modal-categoria" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal sc-modal-small">
        <h3>Nueva categoría</h3>
        <form action="inventario" method="post">
            <input type="hidden" name="accion" value="crearCategoria">
            <div class="sc-form-group">
                <label>Nombre de categoría</label>
                <input type="text" name="nombreCategoria" required>
            </div>
            <div class="sc-form-group">
                <label>Descripción opcional</label>
                <textarea name="descripcionCategoria" rows="3" placeholder="Uso interno de la categoría"></textarea>
            </div>
            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Guardar</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-categoria').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>

<% } %>

<script>
let filtroCatActual = '';
let stockOriginalEdicion = null;
<% if (puedeEditar) { %>
function formProducto() {
    return document.getElementById('form-producto');
}
function abrirNuevoProducto() {
    const form = formProducto();
    form.reset();
    form.elements.accion.value = 'crear';
    form.elements.id.value = '';
    stockOriginalEdicion = null;
    document.getElementById('aviso-ajuste-stock').classList.add('sc-hidden');
    document.getElementById('aviso-fecha-pasada').classList.add('sc-hidden');
    // Fecha mínima = hoy
    document.getElementById('campo-fecha-caducidad').min = new Date().toISOString().split('T')[0];
    document.getElementById('modal-producto-titulo').textContent = 'Nuevo producto médico';
    document.getElementById('modal-producto').classList.remove('sc-hidden');
}
function abrirEditarProducto(id, nombre, sku, categoria, proveedor, precio, stock, stockMinimo, lote, fechaCaducidad, registroSanitario) {
    const form = formProducto();
    form.elements.accion.value = 'editar';
    form.elements.id.value = id;
    form.elements.nombre.value = nombre;
    form.elements.sku.value = sku;
    form.elements.categoria.value = categoria;
    form.elements.proveedor.value = proveedor;
    form.elements.precio.value = precio;
    form.elements.stock.value = stock;
    stockOriginalEdicion = parseInt(stock, 10);
    form.elements.stockMinimo.value = stockMinimo;
    form.elements.lote.value = lote;
    form.elements.fechaCaducidad.value = fechaCaducidad;
    form.elements.registroSanitario.value = registroSanitario;
    document.getElementById('aviso-ajuste-stock').classList.add('sc-hidden');
    document.getElementById('aviso-fecha-pasada').classList.add('sc-hidden');
    document.getElementById('campo-fecha-caducidad').min = ''; // Al editar se permite cualquier fecha
    document.getElementById('modal-producto-titulo').textContent = 'Modificar producto';
    document.getElementById('modal-producto').classList.remove('sc-hidden');
}
document.addEventListener('DOMContentLoaded', function() {
    // Aviso de ajuste de stock al editar
    const campoStock = document.getElementById('campo-stock');
    if (campoStock) {
        campoStock.addEventListener('input', function() {
            const aviso = document.getElementById('aviso-ajuste-stock');
            const form = formProducto();
            if (form.elements.accion.value === 'editar' && stockOriginalEdicion !== null) {
                const nuevo = parseInt(this.value, 10);
                aviso.classList.toggle('sc-hidden', isNaN(nuevo) || nuevo === stockOriginalEdicion);
            } else {
                aviso.classList.add('sc-hidden');
            }
        });
    }
    // Validación de fecha pasada en tiempo real
    const campoFecha = document.getElementById('campo-fecha-caducidad');
    if (campoFecha) {
        campoFecha.addEventListener('change', function() {
            const form = formProducto();
            const esNuevo = form.elements.accion.value === 'crear';
            const hoy = new Date().toISOString().split('T')[0];
            const aviso = document.getElementById('aviso-fecha-pasada');
            aviso.classList.toggle('sc-hidden', !esNuevo || !this.value || this.value >= hoy);
        });
    }
});
<% } %>
function setFiltroCat(btn, cat) {
    filtroCatActual = cat;
    document.querySelectorAll('#filtros-cat .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    filtrarInventario();
}
function filtrarInventario() {
    const q = document.getElementById('buscar-inventario').value.toLowerCase();
    let visibles = 0;
    document.querySelectorAll('#tabla-inventario tbody tr').forEach(row => {
        const matchQ = !q || row.dataset.nombre.includes(q) || row.dataset.sku.includes(q);
        const matchCat = !filtroCatActual || row.dataset.cat === filtroCatActual;
        const mostrar = matchQ && matchCat;
        row.style.display = mostrar ? '' : 'none';
        if (mostrar) visibles++;
    });
    const vacio = document.getElementById('sin-productos-categoria');
    if (vacio) {
        vacio.classList.toggle('sc-hidden', !filtroCatActual || visibles > 0);
    }
}
<% if (puedeEditar && "1".equals(request.getParameter("nuevo"))) { %>
document.addEventListener('DOMContentLoaded', abrirNuevoProducto);
<% } %>
</script>
<script src="js/dashboard.js"></script>
</body>
</html>
