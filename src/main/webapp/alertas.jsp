<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<Producto> alertas = (List<Producto>) request.getAttribute("alertas");
    Integer criticos = (Integer) request.getAttribute("criticos");
    Integer bajos = (Integer) request.getAttribute("bajos");
    Integer normales = (Integer) request.getAttribute("normales");
    Integer caducados = (Integer) request.getAttribute("caducados");
    Integer proximosCaducar = (Integer) request.getAttribute("proximosCaducar");
    Integer alertasCount = (Integer) request.getAttribute("alertasCount");
    request.setAttribute("scPagina", "alertas");
    List<Producto> todosProductos = (List<Producto>) application.getAttribute("productos");
    List<Map<String,String>> recomendaciones = DashboardService.generarRecomendaciones(
            todosProductos != null ? todosProductos : new java.util.ArrayList<>());
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Alertas médicas - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div><h1 class="sc-page-title">Alertas médicas</h1><p class="sc-page-sub"><%= alertasCount == null ? 0 : alertasCount %> productos requieren atención</p></div>
        </div>

        <div class="sc-alert-summary sc-alert-summary-medical">
            <button type="button" class="sc-alert-sum-card critico" onclick="filtrarAlertaResumen(this,'caducado')"><div class="sc-alert-sum-num"><%= caducados == null ? 0 : caducados %></div><div class="sc-alert-sum-label">CADUCADOS</div></button>
            <button type="button" class="sc-alert-sum-card bajo" onclick="filtrarAlertaResumen(this,'proximo')"><div class="sc-alert-sum-num"><%= proximosCaducar == null ? 0 : proximosCaducar %></div><div class="sc-alert-sum-label">PRÓXIMOS A CADUCAR</div></button>
            <button type="button" class="sc-alert-sum-card critico" onclick="filtrarAlertaResumen(this,'stock-critico')"><div class="sc-alert-sum-num"><%= criticos == null ? 0 : criticos %></div><div class="sc-alert-sum-label">STOCK CRÍTICO</div></button>
            <button type="button" class="sc-alert-sum-card bajo" onclick="filtrarAlertaResumen(this,'stock-bajo')"><div class="sc-alert-sum-num"><%= bajos == null ? 0 : bajos %></div><div class="sc-alert-sum-label">STOCK BAJO</div></button>
            <button type="button" class="sc-alert-sum-card normal" onclick="filtrarAlertaResumen(this,'stock-normal')"><div class="sc-alert-sum-num"><%= normales == null ? 0 : normales %></div><div class="sc-alert-sum-label">STOCK NORMAL</div></button>
        </div>

        <div class="sc-filters" id="filtros-alerta-medica">
            <button class="sc-filter-btn activo" onclick="filtrarAlerta(this,'')">Todas</button>
            <button class="sc-filter-btn" onclick="filtrarAlerta(this,'caducado')">Caducados</button>
            <button class="sc-filter-btn" onclick="filtrarAlerta(this,'proximo')">Próximos a caducar</button>
            <button class="sc-filter-btn" onclick="filtrarAlerta(this,'stock')">Stock</button>
        </div>

        <div id="alertas-detalle">
            <% if (alertas != null) { for (Producto producto : alertas) {
                String tipoAlerta = producto.isCaducado() ? "caducado" : (producto.isProximoCaducar() ? "proximo" : "stock");
                String tipoStock = producto.esStockCritico() ? "stock-critico" : (producto.esStockBajo() ? "stock-bajo" : "stock-normal");
                boolean stockBajo = producto.getStockMinimo() > 0 && producto.getStock() < producto.getStockMinimo();
                String nivel = producto.isCaducado() || producto.esStockCritico() ? "critico" : "bajo";
                int porcentaje = producto.getStockMinimo() > 0 ? (int) Math.min(100, (producto.getStock() * 100) / producto.getStockMinimo()) : 0;
            %>
            <div class="sc-alert-detail" data-alerta="<%= tipoAlerta %>" data-stock="<%= tipoStock %>">
                <div class="sc-alert-detail-header">
                    <div>
                        <div class="sc-alert-name"><%= producto.getNombre() %></div>
                        <div class="sc-alert-meta"><%= producto.getSku() %> · <%= producto.getCategoria() %> · Lote <%= producto.getLote() %></div>
                        <div class="sc-alert-meta">Registro sanitario: <%= producto.getRegistroSanitario() %> · Ubicación: <%= producto.getUbicacion() %></div>
                    </div>
                    <div class="sc-alert-tags">
                        <% if (producto.isCaducado()) { %><span class="sc-expiry-badge expired">Caducado hace <%= Math.abs(producto.getDiasParaCaducar()) %> días</span><% } %>
                        <% if (producto.isProximoCaducar()) { %><span class="sc-expiry-badge soon">Caduca en <%= producto.getDiasParaCaducar() %> días</span><% } %>
                        <% if (stockBajo) { %><span class="sc-expiry-badge <%= nivel %>">Stock <%= producto.getStock() %> / <%= producto.getStockMinimo() %></span><% } %>
                    </div>
                </div>
                <div class="sc-alert-meta sc-expiry-date">Fecha de caducidad: <strong><%= producto.getFechaCaducidad() %></strong></div>
                <% if (stockBajo) { %><div class="sc-progress"><div class="sc-progress-fill <%= producto.esStockCritico() ? "red" : "yellow" %>" style="width:<%= porcentaje %>%"></div></div><% } %>
            </div>
            <% } } %>
        </div>

        <div class="sc-card" style="margin-top:20px;">
            <h2 class="sc-card-title">Protocolo recomendado</h2>
            <p class="sc-card-sub">Aplicar FIFO: el primer lote que ingresa es el primero que sale. Los lotes caducados deben bloquearse y retirarse del inventario disponible.</p>
            <div id="live-recomendaciones">
                <% if (recomendaciones != null) { for (Map<String,String> rec : recomendaciones) { %>
                <div class="sc-rec-item <%= rec.get("nivel") %>">
                    <div class="sc-rec-title"><%= rec.get("titulo") %> <span style="color:#4b5563;font-size:11px;font-weight:400;margin-left:6px;"><%= rec.get("ref") %></span></div>
                    <div class="sc-rec-text"><%= rec.get("texto") %></div>
                </div>
                <% } } %>
            </div>
        </div>
    </main>
</div>
<script>
function filtrarAlerta(btn, tipo) {
    document.querySelectorAll('#filtros-alerta-medica .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('.sc-alert-sum-card').forEach(card => card.classList.remove('activo'));
    document.querySelectorAll('#alertas-detalle .sc-alert-detail').forEach(item => {
        item.style.display = !tipo || item.dataset.alerta === tipo ? '' : 'none';
    });
}
function filtrarAlertaResumen(card, tipo) {
    document.querySelectorAll('.sc-alert-sum-card').forEach(c => c.classList.remove('activo'));
    card.classList.add('activo');
    document.querySelectorAll('#filtros-alerta-medica .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    document.querySelectorAll('#alertas-detalle .sc-alert-detail').forEach(item => {
        item.style.display = item.dataset.alerta === tipo || item.dataset.stock === tipo ? '' : 'none';
    });
}
</script>
<script src="js/dashboard.js"></script>
</body>
</html>
