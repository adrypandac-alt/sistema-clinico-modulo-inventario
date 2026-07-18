<%@ page import="com.nexodist.model.PedidoItem" %>
<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<PedidoItem> carrito = (List<PedidoItem>) request.getAttribute("carritoPedido");
    String pedidoSolicitante = (String) request.getAttribute("pedidoSolicitante");
    int total = 0;
    if (carrito != null) {
        for (PedidoItem item : carrito) total += item.getCantidad();
    }
    request.setAttribute("scPagina", "inventario");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Confirmar pedido - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Revisar pedido</h1>
                <p class="sc-page-sub">Revise la persona, los productos y las cantidades antes de registrar el pedido</p>
            </div>
            <a class="sc-btn-sm" href="pedidos">Volver a pedidos</a>
        </div>

        <div class="sc-card sc-confirm-card">
            <div class="sc-section-heading">
                <div>
                    <h2>Información del pedido</h2>
                    <p>Usuario: <%= usuario.getNombre() %> · Rol: <%= DashboardService.rolVisible(usuario) %></p>
                </div>
                <span class="sc-pedido-qty"><%= total %> unidades</span>
            </div>
            <div class="sc-pedido-form-row">
                <div class="sc-form-group"><label>Persona que realiza el pedido</label><input type="text" value="<%= pedidoSolicitante == null ? "" : pedidoSolicitante %>" readonly></div>
            </div>

            <table class="sc-table">
                <thead>
                <tr>
                    <th>SKU</th>
                    <th>Producto</th>
                    <th>Proveedor</th>
                    <th>Cantidad</th>
                    <th>Observación</th>
                </tr>
                </thead>
                <tbody>
                <% if (carrito != null) {
                    for (PedidoItem item : carrito) { %>
                <tr>
                    <td><%= item.getSku() %></td>
                    <td><%= item.getNombre() %></td>
                    <td><%= item.getProveedor() %></td>
                    <td><strong><%= item.getCantidad() %></strong></td>
                    <td><%= item.getObservacion() == null || item.getObservacion().isBlank() ? "Sin observación" : item.getObservacion() %></td>
                </tr>
                <%  }
                } %>
                </tbody>
            </table>

            <div class="sc-confirm-actions">
                <form action="pedidos" method="post" id="confirmar-despacho">
                    <input type="hidden" name="accion" value="cancelar">
                    <button type="submit" class="sc-btn-sm">Cancelar pedido</button>
                </form>
                <form action="pedidos" method="post">
                    <input type="hidden" name="accion" value="confirmar">
                    <button type="submit" class="sc-btn-primary">Registrar pedido</button>
                </form>
            </div>
        </div>
    </main>
</div>
<script src="js/dashboard.js"></script>
</body>
</html>

