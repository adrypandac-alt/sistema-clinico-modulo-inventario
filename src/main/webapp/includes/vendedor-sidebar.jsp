<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    Usuario scUsuario = (Usuario) session.getAttribute("usuario");
    String scPagina = (String) request.getAttribute("scPagina");
    if (scPagina == null) scPagina = "";
    Integer scAlertas = (Integer) request.getAttribute("alertasCount");
    if (scAlertas == null) {
        java.util.List productosCtx = (java.util.List) application.getAttribute("productos");
        scAlertas = DashboardService.contarAlertas(productosCtx);
    }
    boolean verVenta = DashboardService.puedeVerPanel(scUsuario, "venta");
    boolean verMisVentas = DashboardService.puedeVerPanel(scUsuario, "mis-ventas");
    boolean verAlertasVenta = DashboardService.puedeVerPanel(scUsuario, "alertas-venta");
    boolean verFactura = DashboardService.puedeVerPanel(scUsuario, "factura");
%>
<aside class="sc-sidebar">
    <div class="sc-brand">
        <div class="sc-brand-icon">📦</div>
        <div class="sc-brand-text">
            Sistema Clínico<br><span class="sc-brand-sub">Farmacia</span>
        </div>
    </div>

    <nav class="sc-nav">
        <% if (verVenta) { %>
        <a href="venta" class="<%= "venta".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">🛒</span> Panel de Venta
        </a>
        <% } %>
        <% if (verMisVentas) { %>
        <a href="mis-ventas" class="<%= "mis-ventas".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">$</span> Mis Ventas
        </a>
        <% } %>
        <% if (verAlertasVenta) { %>
        <a href="alertas-venta" class="<%= "alertas-venta".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">⚠</span> Alertas médicas
            <% if (scAlertas > 0) { %>
                <span class="sc-badge-nav" data-alert-badge><%= scAlertas %></span>
            <% } %>
        </a>
        <% } %>
        <% if (verFactura) { %>
        <a href="factura" class="<%= "factura".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">📄</span> Factura
        </a>
        <% } %>
        <a href="logout" class="sc-nav-logout">
            <span class="sc-nav-icon">⏻</span> Cerrar sesión
        </a>
    </nav>

    <div class="sc-user-card">
        <div class="sc-user-avatar"><%= scUsuario.getIniciales() %></div>
        <div class="sc-user-info">
            <div class="sc-user-name"><%= scUsuario.getNombre() %></div>
            <div class="sc-user-role">Farmacia</div>
        </div>
        <a href="logout" class="sc-logout" title="Cerrar sesión">⏻</a>
    </div>
</aside>
