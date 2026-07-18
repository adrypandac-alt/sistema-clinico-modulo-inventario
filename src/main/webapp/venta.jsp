<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.model.VentaItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    List<VentaItem> carrito = (List<VentaItem>) request.getAttribute("carrito");
    List<String> errores = (List<String>) request.getAttribute("errores");
    double total = 0;
    if (carrito != null) for (VentaItem it : carrito) total += it.getSubtotal();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dispensación y venta - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/vendedor-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Dispensación y venta</h1>
                <p class="sc-page-sub">Registre medicamentos por paciente o institución. Los lotes caducados están bloqueados.</p>
            </div>
        </div>

        <% if (errores != null && !errores.isEmpty()) { %>
        <div class="sc-error-box">
            <% for (String err : errores) { %>
                <div><%= err %></div>
            <% } %>
        </div>
        <% } %>

        <div class="sc-venta-grid">
            <form action="venta" method="post" id="form-venta" class="sc-card">
                <input type="hidden" name="accion" value="registrar">
                <h2 class="sc-card-title">Paciente o institución</h2>
                <div class="sc-form-group">
                    <label>Nombre / Razón social</label>
                    <input type="text" name="clienteNombre" placeholder="Ej: Juan Pérez / Empresa S.A."
                           value="<%= request.getAttribute("clienteNombre") != null ? request.getAttribute("clienteNombre") : "" %>" required>
                </div>
                <div class="sc-form-group">
                    <label>RUC / Cédula</label>
                    <input type="text" name="clienteRuc" placeholder="Ej: 1712345678001"
                           value="<%= request.getAttribute("clienteRuc") != null ? request.getAttribute("clienteRuc") : "" %>" required>
                </div>
                <div class="sc-form-group">
                    <label>Teléfono</label>
                    <input type="text" name="clienteTelefono" placeholder="Ej: 0998765432"
                           value="<%= request.getAttribute("clienteTelefono") != null ? request.getAttribute("clienteTelefono") : "" %>">
                </div>
                <div class="sc-form-group">
                    <label>Correo electrónico</label>
                    <input type="email" name="clienteCorreo" placeholder="cliente@email.com"
                           value="<%= request.getAttribute("clienteCorreo") != null ? request.getAttribute("clienteCorreo") : "" %>">
                </div>
                <div class="sc-form-group">
                    <label>Dirección</label>
                    <input type="text" name="clienteDireccion" placeholder="Ej: Av. Amazonas N23-45, Quito"
                           value="<%= request.getAttribute("clienteDireccion") != null ? request.getAttribute("clienteDireccion") : "" %>">
                </div>
            </form>

            <div class="sc-card">
                <h2 class="sc-card-title">Agregar productos médicos</h2>
                <form action="venta" method="post" class="sc-add-producto-form">
                    <input type="hidden" name="accion" value="agregar">
                    <div class="sc-form-group">
                        <label>Producto</label>
                        <select name="productoId" required>
                            <% if (productos != null) {
                                for (Producto p : productos) { %>
                            <option value="<%= p.getId() %>"><%= p.getSku() %> — <%= p.getNombre() %> ($<%= String.format(Locale.US, "%,.2f", p.getPrecio()) %>)</option>
                            <%  }
                            } %>
                        </select>
                    </div>
                    <div class="sc-add-row">
                        <div class="sc-form-group">
                            <label>Cantidad</label>
                            <input type="number" name="cantidad" value="1" min="1" required>
                        </div>
                        <button type="submit" class="sc-btn-add">+</button>
                    </div>
                </form>

                <div class="sc-carrito-list">
                        <% if (carrito == null || carrito.isEmpty()) { %>
                            <p class="sc-empty-hint">Agregue productos a la venta</p>
                        <% } else {
                            for (VentaItem item : carrito) { %>
                        <div class="sc-carrito-item">
                            <div class="sc-carrito-info">
                                <div class="sc-prod-name"><%= item.getNombre() %></div>
                                <div class="sc-prod-sub"><%= item.getSku() %> · $<%= String.format(Locale.US, "%,.2f", item.getPrecioUnitario()) %> c/u</div>
                            </div>
                            <form action="venta" method="post" class="sc-carrito-qty-form">
                                <input type="hidden" name="accion" value="actualizar">
                                <input type="hidden" name="productoId" value="<%= item.getProductoId() %>">
                                <input type="number" name="cantidad" value="<%= item.getCantidad() %>" min="1"
                                       onchange="this.form.submit()" class="sc-qty-input">
                            </form>
                            <div class="sc-carrito-total">$<%= String.format(Locale.US, "%,.2f", item.getSubtotal()) %></div>
                            <form action="venta" method="post">
                                <input type="hidden" name="accion" value="eliminar">
                                <input type="hidden" name="productoId" value="<%= item.getProductoId() %>">
                                <button type="submit" class="sc-btn-icon" title="Eliminar">🗑</button>
                            </form>
                        </div>
                        <%  }
                        } %>
                </div>
            </div>
        </div>

        <div class="sc-venta-footer">
            <div class="sc-venta-total">
                Total: <strong>$<%= String.format(Locale.US, "%,.2f", total) %></strong>
            </div>
            <button type="submit" form="form-venta" class="sc-btn-primary" <%= (carrito == null || carrito.isEmpty()) ? "disabled" : "" %>>
                Registrar venta
            </button>
        </div>
    </main>
</div>
<script src="js/dashboard.js"></script>
</body>
</html>

