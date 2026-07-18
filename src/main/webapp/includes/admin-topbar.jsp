<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.nexodist.storage.DatosStorage" %>
<%
    Integer topAlertas = (Integer) request.getAttribute("alertasCount");
    if (topAlertas == null) topAlertas = 0;
    boolean scDbConectada = DatosStorage.verificarConexion();
%>
<div class="sc-topbar">
    <div class="sc-db-status <%= scDbConectada ? "sc-db-online" : "sc-db-offline" %>" role="status"><span></span><%= scDbConectada ? "BD conectada" : "BD desconectada" %></div>
    <div class="sc-live">
        <span class="sc-live-dot"></span>
        <span id="live-time">En vivo</span>
    </div>
    <a href="alertas" class="sc-alert-pill" id="top-alert-text">
        ⚠ <%= topAlertas %> alertas médicas
    </a>
    <span class="sc-date" id="top-date"><%= DashboardService.fechaCorta() %></span>
    <button type="button" class="a11y-control a11y-mobile-toggle" data-a11y="menu"
            aria-label="Abrir controles de accesibilidad" aria-controls="a11y-controls"
            aria-expanded="false" title="Abrir controles de accesibilidad">Accesibilidad</button>
    <div class="a11y-controls" id="a11y-controls" aria-label="Controles de accesibilidad">
        <button type="button" class="a11y-control" data-a11y="decrease"
                aria-label="Reducir tamaño del texto" title="Reducir texto">A− Reducir texto</button>
        <button type="button" class="a11y-control" data-a11y="increase"
                aria-label="Aumentar tamaño del texto" title="Aumentar texto">A+ Aumentar texto</button>
        <button type="button" class="a11y-control" data-a11y="dark" aria-pressed="false"
                aria-label="Activar o desactivar modo oscuro" title="Activar modo oscuro">🌙 Modo oscuro</button>
        <button type="button" class="a11y-control" data-a11y="colorblind" aria-pressed="false"
                aria-label="Activar o desactivar modo daltonismo" title="Activar modo daltonismo">◉ Modo daltonismo</button>
    </div>
    <a href="inicio" class="sc-topbar-logout">Inicio</a>
    <a href="logout" class="sc-topbar-logout">Cerrar sesión</a>
</div>
<script src="js/accessibility.js" defer></script>
<% if (Boolean.FALSE.equals(request.getAttribute("puedeInteractuar"))) { %>
<div class="sc-readonly-notice">Modo solo lectura: las acciones están deshabilitadas para tu rol.</div>
<script>
document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('main button, main input:not([type="search"]), main select, main textarea').forEach(function (el) {
    el.disabled = true; el.title = 'Acción no permitida para un rol de solo lectura';
  });
  document.querySelectorAll('main form').forEach(function (form) { form.addEventListener('submit', function (e) { e.preventDefault(); }); });
  document.body.classList.add('sc-readonly');
});
</script>
<% } %>
