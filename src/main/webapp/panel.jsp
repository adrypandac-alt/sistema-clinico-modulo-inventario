<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.service.DashboardService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Producto> productos = (List<Producto>) application.getAttribute("productos");
    int alertasCount = DashboardService.contarAlertas(productos);
    request.setAttribute("scPagina", "panel");
    request.setAttribute("alertasCount", alertasCount);
    String fechaResumen = (String) request.getAttribute("fechaResumen");
    if (fechaResumen == null) fechaResumen = DashboardService.fechaHoyFormateada();
    Integer caducadosCount = (Integer) request.getAttribute("caducadosCount");
    Integer proximosCaducarCount = (Integer) request.getAttribute("proximosCaducarCount");
    boolean puedeEditar = DashboardService.puedeModificarInventario(usuario);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Panel médico - Sistema Clínico</title>
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
                <h1 class="sc-page-title">Dashboard</h1>
                <p class="sc-page-sub">Resumen del inventario médico - <%= fechaResumen %></p>
            </div>
            <% if (puedeEditar) { %>
            <div class="sc-header-actions">
                <a class="sc-btn-primary" href="inventario?nuevo=1">+ Ingreso de producto</a>
                <a class="sc-btn-secondary-action" href="movimientos">Ingreso de mercadería</a>
            </div>
            <% } %>
        </div>

        <div class="sc-kpi-grid">
            <div class="sc-kpi">
                <div class="sc-kpi-icon blue">📦</div>
                <div class="sc-kpi-label">Productos médicos</div>
                <div class="sc-kpi-value" id="kpi-productos"><%= productos.size() %></div>
                <div class="sc-kpi-sub">Medicamentos e insumos</div>
            </div>
            <div class="sc-kpi">
                <div class="sc-kpi-icon green">📈</div>
                <div class="sc-kpi-label">Unidades en Stock</div>
                <div class="sc-kpi-value" id="kpi-unidades"><%= DashboardService.totalUnidades(productos) %></div>
                <div class="sc-kpi-sub">Entre todos los depósitos</div>
            </div>
            <div class="sc-kpi">
                <div class="sc-kpi-icon purple">💲</div>
                <div class="sc-kpi-label">Valor Total</div>
                <div class="sc-kpi-value" id="kpi-valor">$<%= String.format("%.0f", DashboardService.valorTotal(productos) / 1000) %>K</div>
                <div class="sc-kpi-sub">Dólares americanos</div>
            </div>
            <div class="sc-kpi">
                <div class="sc-kpi-icon yellow">⚠</div>
                <div class="sc-kpi-label">Alertas médicas</div>
                <div class="sc-kpi-value" id="kpi-bajo"><%= alertasCount %></div>
                <div class="sc-kpi-sub">Stock o caducidad</div>
            </div>
        </div>

        <div class="sc-grid-2" style="margin-bottom:14px;">
            <div class="sc-card sc-med-summary">
                <div><h2 class="sc-card-title">Control de caducidad</h2><p class="sc-card-sub">Seguimiento de lotes y vencimientos</p></div>
                <div class="sc-expiry-counts">
                    <span class="sc-expiry-badge expired"><%= caducadosCount == null ? 0 : caducadosCount %> caducados</span>
                    <span class="sc-expiry-badge soon"><%= proximosCaducarCount == null ? 0 : proximosCaducarCount %> próximos</span>
                </div>
                <a href="alertas" class="sc-btn-sm">Revisar alertas</a>
            </div>
        </div>

        <div class="sc-grid-2" style="margin-bottom:14px;">
            <div class="sc-card">
                <h2 class="sc-card-title">Movimientos semanales</h2>
                <p class="sc-card-sub">Entradas y salidas últimos 7 días</p>
                <div class="sc-chart-wrap">
                    <canvas id="chartMovimientos"></canvas>
                </div>
            </div>
            <div class="sc-card">
                <h2 class="sc-card-title">Stock por categoría</h2>
                <p class="sc-card-sub">Distribución de unidades</p>
                <div class="sc-chart-wrap">
                    <canvas id="chartCategorias"></canvas>
                </div>
            </div>
        </div>

        <div class="sc-grid-3">
            <div class="sc-card">
                <h2 class="sc-card-title">Últimos movimientos</h2>
                <p class="sc-card-sub">Actualización en tiempo real</p>
                <div id="live-movimientos"></div>
            </div>
            <div class="sc-card">
                <h2 class="sc-card-title">Alertas médicas</h2>
                <p class="sc-card-sub">Stock bajo y fechas de caducidad</p>
                <div id="live-alertas-stock"></div>
            </div>
            <div class="sc-card">
                <h2 class="sc-card-title">Recomendaciones</h2>
                <p class="sc-card-sub">Sugerencias inteligentes de reposición</p>
                <div id="live-recomendaciones"></div>
            </div>
        </div>
    </main>
</div>
<script src="js/dashboard.js"></script>
</body>
</html>

