<%@ page import="com.nexodist.model.Venta" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<Venta> ventas = (List<Venta>) request.getAttribute("ventas");
    Double ventasTotal = (Double) request.getAttribute("ventasTotal");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ventas médicas - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <link rel="stylesheet" href="css/accessibility.css">
</head>
<body class="sc-body">
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main" id="contenido-principal" tabindex="-1">
        <jsp:include page="includes/admin-topbar.jsp"/>
        <div class="sc-page-header">
            <div><h1 class="sc-page-title">Ventas médicas</h1><p class="sc-page-sub">Supervisión de ventas registradas por farmacia</p></div>
        </div>
        <div class="sc-kpi-grid sc-kpi-grid-compact">
            <div class="sc-kpi"><div class="sc-kpi-label">Ventas registradas</div><div class="sc-kpi-value"><%= ventas == null ? 0 : ventas.size() %></div><div class="sc-kpi-sub">Operaciones auditadas</div></div>
            <div class="sc-kpi"><div class="sc-kpi-label">Valor vendido</div><div class="sc-kpi-value">$<%= String.format(Locale.US, "%,.2f", ventasTotal == null ? 0 : ventasTotal) %></div><div class="sc-kpi-sub">Dólares americanos</div></div>
        </div>
        <div class="sc-card sc-table-wrap">
            <table class="sc-table">
                <thead><tr><th>#</th><th>Paciente / institución</th><th>Farmacia</th><th>Productos</th><th>Total</th><th>Fecha</th><th>Estado</th></tr></thead>
                <tbody>
                <% if (ventas != null) { for (Venta venta : ventas) { %>
                    <tr>
                        <td><%= venta.getId() %></td>
                        <td><div class="sc-prod-name"><%= venta.getClienteNombre() %></div><div class="sc-prod-sub"><%= venta.getClienteRuc() %></div></td>
                        <td><%= venta.getVendedorNombre() %></td>
                        <td><%= venta.getItems() == null ? 0 : venta.getItems().size() %> ítems</td>
                        <td class="sc-prod-name">$<%= String.format(Locale.US, "%,.2f", venta.getTotal()) %></td>
                        <td><%= venta.getFecha() %></td>
                        <td><span class="sc-status <%= venta.isFacturada() ? "activo" : "inactivo" %>"><%= venta.isFacturada() ? "Facturada" : "Registrada" %></span></td>
                    </tr>
                <% } } %>
                </tbody>
            </table>
        </div>
    </main>
</div>
</body>
</html>

