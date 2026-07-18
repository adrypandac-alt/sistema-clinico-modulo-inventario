<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Producto> alertas = (List<Producto>) request.getAttribute("alertas");
    Integer criticos = (Integer) request.getAttribute("criticos");
    Integer bajos = (Integer) request.getAttribute("bajos");
    Integer normales = (Integer) request.getAttribute("normales");
    Integer caducados = (Integer) request.getAttribute("caducados");
    Integer proximosCaducar = (Integer) request.getAttribute("proximosCaducar");
    Integer alertasCount = (Integer) request.getAttribute("alertasCount");
    request.setAttribute("scPagina", "alertas-venta");
    request.setAttribute("alertasCount", alertasCount);
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Alertas médicas - Sistema Clínico</title>
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
                <h1 class="sc-page-title">Alertas médicas</h1>
                <p class="sc-page-sub"><%= alertasCount %> productos con restricciones de stock o caducidad</p>
            </div>
        </div>

        <div class="sc-filters" id="filtros-alert-cat">
            <button class="sc-filter-btn activo" onclick="setFiltroAlerta(this,'')">Todas</button>
            <button class="sc-filter-btn" onclick="setFiltroAlerta(this,'Medicamentos')">Medicamentos</button>
            <button class="sc-filter-btn" onclick="setFiltroAlerta(this,'Vacunas')">Vacunas</button>
            <button class="sc-filter-btn" onclick="setFiltroAlerta(this,'Insumos médicos')">Insumos</button>
            <button class="sc-filter-btn" onclick="setFiltroAlerta(this,'Equipos médicos')">Equipos</button>
        </div>

        <div class="sc-alert-summary sc-alert-summary-medical">
            <div class="sc-alert-sum-card critico"><div class="sc-alert-sum-num"><%= caducados %></div><div class="sc-alert-sum-label">CADUCADOS</div></div>
            <div class="sc-alert-sum-card bajo"><div class="sc-alert-sum-num"><%= proximosCaducar %></div><div class="sc-alert-sum-label">PRÓXIMOS</div></div>
            <div class="sc-alert-sum-card critico">
                <div class="sc-alert-sum-num" id="sum-criticos"><%= criticos %></div>
                <div class="sc-alert-sum-label">STOCK CRÍTICO</div>
            </div>
            <div class="sc-alert-sum-card bajo">
                <div class="sc-alert-sum-num" id="sum-bajos"><%= bajos %></div>
                <div class="sc-alert-sum-label">STOCK BAJO</div>
            </div>
            <div class="sc-alert-sum-card normal">
                <div class="sc-alert-sum-num" id="sum-normales"><%= normales %></div>
                <div class="sc-alert-sum-label">STOCK NORMAL</div>
            </div>
        </div>

        <div id="alertas-detalle">
            <% boolean mostroCritico = false; boolean mostroBajo = false;
            if (alertas != null) {
                for (Producto p : alertas) {
                    if (p.esStockCritico() && !mostroCritico) {
                        mostroCritico = true;
            %>
            <div class="sc-section-title critico">CRÍTICO — STOCK ≤ 50% DEL MÍNIMO</div>
            <%  }
                    if (p.esStockBajo() && !mostroBajo) {
                        mostroBajo = true;
            %>
            <div class="sc-section-title bajo">BAJO — POR DEBAJO DEL MÍNIMO</div>
            <%  }
                    int pct = p.getStockMinimo() > 0 ? (int) Math.min(100, (p.getStock() * 100) / p.getStockMinimo()) : 0;
                    String barColor = p.esStockCritico() ? "red" : "yellow";
            %>
            <div class="sc-alert-detail" data-cat="<%= p.getCategoria() %>">
                <div class="sc-alert-detail-header">
                    <div>
                        <div class="sc-alert-name"><%= p.getNombre() %></div>
                        <div class="sc-alert-meta"><%= p.getSku() %> · <%= p.getCategoria() %> · Lote <%= p.getLote() %></div>
                        <div class="sc-alert-meta">Proveedor: <%= p.getProveedor() %> · Ubicación: <%= p.getUbicacion() %></div>
                    </div>
                    <div class="sc-alert-tags">
                        <% if (p.isCaducado()) { %><span class="sc-expiry-badge expired">CADUCADO</span><% } %>
                        <% if (p.isProximoCaducar()) { %><span class="sc-expiry-badge soon"><%= p.getDiasParaCaducar() %> días</span><% } %>
                        <span class="sc-alert-nums"><%= p.getStock() %> / <%= p.getStockMinimo() %> mín.</span>
                    </div>
                </div>
                <div class="sc-alert-meta">Caducidad: <strong><%= p.getFechaCaducidad() %></strong></div>
                <div class="sc-progress">
                    <div class="sc-progress-fill <%= barColor %>" style="width:<%= pct %>%"></div>
                </div>
            </div>
            <%  }
            } %>
        </div>

        <div class="sc-card" style="margin-top:20px;">
            <h2 class="sc-card-title">Recomendaciones</h2>
            <p class="sc-card-sub">Los lotes caducados no pueden agregarse a una venta. Priorice próximos vencimientos.</p>
            <div id="live-recomendaciones"></div>
        </div>
    </main>
</div>
<script>
function setFiltroAlerta(btn, cat) {
    document.querySelectorAll('#filtros-alert-cat .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('#alertas-detalle .sc-alert-detail').forEach(el => {
        el.style.display = !cat || el.dataset.cat === cat ? '' : 'none';
    });
}
</script>
<script src="js/dashboard.js"></script>
</body>
</html>

