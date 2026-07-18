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
    boolean esAdmin = DashboardService.esAdministrador(scUsuario);
    boolean esVentas = DashboardService.esVendedor(scUsuario);
    boolean verDashboard = DashboardService.puedeVerPanel(scUsuario, "dashboard");
    boolean verProductos = DashboardService.puedeVerPanel(scUsuario, "productos");
    boolean verProveedores = DashboardService.puedeVerPanel(scUsuario, "proveedor");
    boolean verMovimientos = DashboardService.puedeVerPanel(scUsuario, "movimientos");
    boolean verAlertas = DashboardService.puedeVerPanel(scUsuario, "alertas");
    boolean verUsuarios = DashboardService.puedeVerPanel(scUsuario, "usuarios");
    boolean verDespachos = DashboardService.puedeVerPanel(scUsuario, "despachos");
%>
<aside class="sc-sidebar">
    <div class="sc-brand">
        <div class="sc-brand-icon">📦</div>
        <div class="sc-brand-text">
            Inventario<br><span class="sc-brand-sub">Sistema Clínico</span>
        </div>
    </div>

    <nav class="sc-nav">
        <a href="inicio">
            <span class="sc-nav-icon">⌂</span> Inicio
        </a>
        <% if (verDashboard) { %>
        <a href="panel" class="<%= "panel".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">▦</span> Dashboard
        </a>
        <% } %>
        <% if (verProductos) { %>
        <a href="inventario" class="<%= "inventario".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">📋</span> Productos
        </a>
        <% } %>
        <% if (verProveedores) { %>
        <a href="proveedor" class="<%= "proveedor".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">P</span> Proveedores
        </a>
        <% } %>
        <% if (verMovimientos) { %>
        <a href="movimientos" class="<%= "movimientos".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">↕</span> Movimientos
        </a>
        <% } %>
        <% if (verDespachos) { %>
        <a href="pedidos" class="<%= "despachos".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">🚚</span> Despachos
        </a>
        <% } %>
        <% if (verAlertas && !esVentas) { %>
        <a href="alertas" class="<%= "alertas".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">⚠</span> Alertas
            <% if (scAlertas > 0) { %>
                <span class="sc-badge-nav" data-alert-badge><%= scAlertas %></span>
            <% } %>
        </a>
        <% } %>
        <% if (verUsuarios) { %>
        <a href="usuarios" class="<%= "usuarios".equals(scPagina) ? "activo" : "" %>">
            <span class="sc-nav-icon">👥</span> Usuarios
        </a>
        <% } %>
    </nav>

    <a href="logout" class="sc-nav-logout sc-logout-above-user"><span class="sc-nav-icon">⏻</span> Cerrar sesión</a>
    <button type="button" class="sc-user-card sc-user-profile-button" onclick="document.getElementById('modal-mi-perfil').classList.remove('sc-hidden')" title="Ver mis datos">
        <div class="sc-user-avatar"><%= scUsuario.getIniciales() %></div>
        <div class="sc-user-info">
            <div class="sc-user-name"><%= scUsuario.getNombre() %></div>
            <div class="sc-user-role"><%= scUsuario.getArea() %></div>
        </div>
        <span class="sc-profile-chevron">›</span>
    </button>
</aside>
<div id="modal-mi-perfil" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
  <div class="sc-modal sc-profile-modal">
    <h3>Mis datos</h3><p class="sc-card-sub">Información personal registrada en el sistema.</p>
    <div class="sc-profile-data"><span>Nombre</span><strong><%= scUsuario.getNombres() %></strong></div>
    <div class="sc-profile-data"><span>Apellido</span><strong><%= scUsuario.getApellidos().isBlank() ? "No registrado" : scUsuario.getApellidos() %></strong></div>
    <div class="sc-profile-data"><span>Área</span><strong><%= scUsuario.getArea() %></strong></div>
    <div class="sc-profile-data"><span>Número de teléfono</span><strong><%= scUsuario.getTelefono().isBlank() ? "No registrado" : scUsuario.getTelefono() %></strong></div>
    <div class="sc-form-actions"><button type="button" class="sc-btn-primary" onclick="document.getElementById('modal-mi-perfil').classList.add('sc-hidden')">Cerrar</button></div>
  </div>
</div>
