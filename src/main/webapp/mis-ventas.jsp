<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Venta" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Venta> ventas = (List<Venta>) request.getAttribute("ventas");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mis Ventas — Sistema Clínico</title>
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
                <h1 class="sc-page-title">Mis Ventas</h1>
                <p class="sc-page-sub"><%= ventas != null ? ventas.size() : 0 %> ventas registradas</p>
            </div>
        </div>

        <% if ("1".equals(request.getParameter("ok"))) { %>
            <div class="sc-success-box">Venta registrada correctamente.</div>
        <% } %>

        <div class="sc-card">
            <% if (ventas == null || ventas.isEmpty()) { %>
                <div class="sc-empty-state">
                    <div class="sc-empty-icon">📦</div>
                    <p>No hay ventas registradas aún. Registre una venta en el Panel de Venta.</p>
                    <a href="venta" class="sc-btn-primary" style="margin-top:14px;">Ir al Panel de Venta</a>
                </div>
            <% } else { %>
            <table class="sc-table">
                <thead>
                <tr>
                    <th>#</th>
                    <th>Cliente</th>
                    <th>Productos</th>
                    <th>Total</th>
                    <th>Fecha</th>
                    <th>Estado</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <% for (Venta v : ventas) { %>
                <tr>
                    <td><%= v.getId() %></td>
                    <td>
                        <div class="sc-prod-name"><%= v.getClienteNombre() %></div>
                        <div class="sc-prod-sub"><%= v.getClienteRuc() %></div>
                    </td>
                    <td><%= v.getItems() != null ? v.getItems().size() : 0 %> ítems</td>
                    <td class="sc-prod-name">$<%= String.format(Locale.US, "%,.2f", v.getTotal()) %></td>
                    <td><%= v.getFecha() %></td>
                    <td>
                        <span class="sc-status <%= v.isFacturada() ? "activo" : "inactivo" %>">
                            <%= v.isFacturada() ? "Facturada" : "Registrada" %>
                        </span>
                    </td>
                    <td><a href="factura?id=<%= v.getId() %>" class="sc-btn-sm">Factura</a></td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <% } %>
        </div>
    </main>
</div>
<script src="js/dashboard.js"></script>
</body>
</html>

