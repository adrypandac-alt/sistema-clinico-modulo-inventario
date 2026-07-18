<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Venta" %>
<%@ page import="com.nexodist.model.VentaItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Venta> ventas = (List<Venta>) request.getAttribute("ventas");
    Venta seleccionada = (Venta) request.getAttribute("ventaSeleccionada");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Factura — Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/vendedor-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Panel de Factura</h1>
                <p class="sc-page-sub">Seleccione una venta para generar e imprimir la factura</p>
            </div>
        </div>

        <div class="sc-card">
            <div class="sc-card-header-row">
                <h2 class="sc-card-title">📄 Ventas registradas</h2>
                <span class="sc-cat-pill"><%= ventas != null ? ventas.size() : 0 %> ventas</span>
            </div>

            <% if (ventas == null || ventas.isEmpty()) { %>
                <div class="sc-empty-state">
                    <div class="sc-empty-icon">📦</div>
                    <p>No hay ventas registradas aún. Registre una venta en el Panel de Venta.</p>
                </div>
            <% } else { %>
            <div class="sc-factura-list">
                <% for (Venta v : ventas) { %>
                <a href="factura?id=<%= v.getId() %>" class="sc-factura-item <%= seleccionada != null && seleccionada.getId() == v.getId() ? "activo" : "" %>">
                    <div>
                        <div class="sc-prod-name">Venta #<%= v.getId() %> — <%= v.getClienteNombre() %></div>
                        <div class="sc-prod-sub"><%= v.getFecha() %> · $<%= String.format(Locale.US, "%,.2f", v.getTotal()) %></div>
                    </div>
                    <span class="sc-status <%= v.isFacturada() ? "activo" : "inactivo" %>">
                        <%= v.isFacturada() ? "Facturada" : "Pendiente" %>
                    </span>
                </a>
                <% } %>
            </div>
            <% } %>
        </div>

        <% if (seleccionada != null) { %>
        <div class="sc-card sc-factura-print" id="factura-detalle">
            <% if ("1".equals(request.getParameter("impreso"))) { %>
                <div class="sc-success-box">Factura generada correctamente.</div>
            <% } %>
            <div class="sc-factura-header">
                <div>
                    <h2 class="sc-card-title">Sistema Clínico</h2>
                    <p class="sc-card-sub">Factura #<%= seleccionada.getId() %></p>
                </div>
                <div class="sc-factura-meta">
                    <div><%= seleccionada.getFecha() %></div>
                    <div>Farmacia: <%= seleccionada.getVendedorNombre() %></div>
                </div>
            </div>
            <div class="sc-factura-cliente">
                <strong>Cliente:</strong> <%= seleccionada.getClienteNombre() %><br>
                <span class="sc-prod-sub">RUC/Cédula: <%= seleccionada.getClienteRuc() %></span><br>
                <% if (seleccionada.getClienteTelefono() != null && !seleccionada.getClienteTelefono().isBlank()) { %>
                    Tel: <%= seleccionada.getClienteTelefono() %><br>
                <% } %>
                <% if (seleccionada.getClienteDireccion() != null && !seleccionada.getClienteDireccion().isBlank()) { %>
                    <%= seleccionada.getClienteDireccion() %>
                <% } %>
            </div>
            <table class="sc-table">
                <thead>
                <tr><th>SKU</th><th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr>
                </thead>
                <tbody>
                <% if (seleccionada.getItems() != null) {
                    for (VentaItem item : seleccionada.getItems()) { %>
                <tr>
                    <td><%= item.getSku() %></td>
                    <td><%= item.getNombre() %></td>
                    <td><%= item.getCantidad() %></td>
                    <td>$<%= String.format(Locale.US, "%,.2f", item.getPrecioUnitario()) %></td>
                    <td>$<%= String.format(Locale.US, "%,.2f", item.getSubtotal()) %></td>
                </tr>
                <%  }
                } %>
                </tbody>
                <tfoot>
                <tr>
                    <td colspan="4" style="text-align:right;font-weight:700;">TOTAL</td>
                    <td style="font-weight:700;">$<%= String.format(Locale.US, "%,.2f", seleccionada.getTotal()) %></td>
                </tr>
                </tfoot>
            </table>
            <div class="sc-form-actions" style="margin-top:16px;">
                <button type="button" class="sc-btn-primary" onclick="window.print()">Imprimir factura</button>
                <% if (!seleccionada.isFacturada()) { %>
                <form action="factura" method="post" style="display:inline;">
                    <input type="hidden" name="ventaId" value="<%= seleccionada.getId() %>">
                    <button type="submit" class="sc-btn-sm">Marcar como facturada</button>
                </form>
                <% } %>
            </div>
        </div>
        <% } %>
    </main>
</div>
<script src="js/dashboard.js"></script>
</body>
</html>

