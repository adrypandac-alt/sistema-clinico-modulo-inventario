<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.ActividadCuenta" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Collections" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }

    List<ActividadCuenta> actividad = (List<ActividadCuenta>) request.getAttribute("actividad");
    if (actividad == null) actividad = new java.util.ArrayList<>();

    // Mostrar más reciente primero
    List<ActividadCuenta> actividadDesc = new java.util.ArrayList<>(actividad);
    Collections.reverse(actividadDesc);

    // Contadores para el resumen
    int totalLogin = 0, totalLogout = 0, totalAccion = 0, totalError = 0;
    for (ActividadCuenta a : actividad) {
        if (a.getTipo() == ActividadCuenta.Tipo.LOGIN)  totalLogin++;
        else if (a.getTipo() == ActividadCuenta.Tipo.LOGOUT) totalLogout++;
        else if (a.getTipo() == ActividadCuenta.Tipo.ACCION) totalAccion++;
        else if (a.getTipo() == ActividadCuenta.Tipo.ERROR)  totalError++;
    }

    request.setAttribute("scPagina", "actividad");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Actividad de cuentas - Sistema Clínico</title>
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
                <h1 class="sc-page-title">Actividad de cuentas</h1>
                <p class="sc-page-sub"><%= actividadDesc.size() %> eventos registrados — actualización automática cada 5 s</p>
            </div>
        </div>

        <!-- Resumen estadístico -->
        <div class="sc-act-stats">
            <div class="sc-act-stat">
                <div class="sc-act-stat-num" style="color:#4ade80;"><%= totalLogin %></div>
                <div class="sc-act-stat-lbl">Inicios de sesión</div>
            </div>
            <div class="sc-act-stat">
                <div class="sc-act-stat-num" style="color:#60a5fa;"><%= totalLogout %></div>
                <div class="sc-act-stat-lbl">Cierres de sesión</div>
            </div>
            <div class="sc-act-stat">
                <div class="sc-act-stat-num" style="color:#fbbf24;"><%= totalAccion %></div>
                <div class="sc-act-stat-lbl">Acciones registradas</div>
            </div>
            <div class="sc-act-stat">
                <div class="sc-act-stat-num" style="color:#f87171;"><%= totalError %></div>
                <div class="sc-act-stat-lbl">Intentos fallidos</div>
            </div>
        </div>

        <!-- Filtros -->
        <div class="sc-filters" id="filtros-actividad">
            <button class="sc-filter-btn activo" onclick="filtrarActividad(this,'')">Todos</button>
            <button class="sc-filter-btn" onclick="filtrarActividad(this,'login')">Login</button>
            <button class="sc-filter-btn" onclick="filtrarActividad(this,'logout')">Logout</button>
            <button class="sc-filter-btn" onclick="filtrarActividad(this,'accion')">Acciones</button>
            <button class="sc-filter-btn" onclick="filtrarActividad(this,'error')">Errores</button>
        </div>

        <!-- Búsqueda por usuario -->
        <div class="sc-search">
            <span>🔍</span>
            <input type="text" id="busqueda-actividad" placeholder="Buscar por usuario, correo o descripción..." oninput="buscarActividad(this.value)">
        </div>

        <!-- Lista de actividad -->
        <div class="sc-card sc-card-scanline" id="lista-actividad">
            <% if (actividadDesc.isEmpty()) { %>
            <div class="sc-empty-state">
                <div class="sc-empty-icon">📋</div>
                <p>No hay actividad registrada aún.</p>
                <p class="sc-empty-hint">Los eventos de login, logout y acciones aparecerán aquí en tiempo real.</p>
            </div>
            <% } else {
                for (ActividadCuenta a : actividadDesc) {
                    String tipoCss = a.getTipoCss();
            %>
            <div class="sc-activity-item" data-tipo="<%= tipoCss %>"
                 data-search="<%= (a.getUsuarioNombre() + " " + a.getUsuarioCorreo() + " " + a.getDescripcion()).toLowerCase().replace("\"","") %>">
                <div class="sc-activity-icon <%= tipoCss %>"><%= a.getTipoIcono() %></div>
                <div class="sc-activity-info">
                    <div class="sc-activity-title">
                        <%= a.getUsuarioNombre() %>
                        <span class="sc-activity-tipo <%= tipoCss %>"><%= a.getTipo() %></span>
                    </div>
                    <div class="sc-activity-sub"><%= a.getDescripcion() %></div>
                    <div class="sc-activity-ip"><%= a.getIp() %> — <%= a.getUsuarioRol() %> — <%= a.getUsuarioCorreo() %></div>
                </div>
                <div class="sc-activity-time"><%= a.getFecha() %></div>
            </div>
            <%  }
            } %>
        </div>
    </main>
</div>

<script>
function filtrarActividad(btn, tipo) {
    document.querySelectorAll('#filtros-actividad .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('#lista-actividad .sc-activity-item').forEach(item => {
        const visible = !tipo || item.dataset.tipo === tipo;
        item.style.display = visible ? '' : 'none';
    });
}

function buscarActividad(texto) {
    const q = texto.toLowerCase();
    document.querySelectorAll('#lista-actividad .sc-activity-item').forEach(item => {
        item.style.display = item.dataset.search.includes(q) ? '' : 'none';
    });
}

// Auto-refresh cada 5 segundos
setInterval(() => {
    fetch('actividad', { cache: 'no-store' })
        .then(r => r.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const nuevaLista = doc.getElementById('lista-actividad');
            const nuevaStats = doc.querySelector('.sc-act-stats');
            if (nuevaLista) {
                document.getElementById('lista-actividad').innerHTML = nuevaLista.innerHTML;
                // Re-aplicar filtro activo
                const filtroActivo = document.querySelector('#filtros-actividad .sc-filter-btn.activo');
                if (filtroActivo) {
                    const texto = filtroActivo.textContent.trim().toLowerCase();
                    const tipo = texto === 'todos' ? '' :
                                 texto === 'login'   ? 'login'  :
                                 texto === 'logout'  ? 'logout' :
                                 texto === 'acciones'? 'accion' :
                                 texto === 'errores' ? 'error'  : '';
                    document.querySelectorAll('#lista-actividad .sc-activity-item').forEach(item => {
                        item.style.display = !tipo || item.dataset.tipo === tipo ? '' : 'none';
                    });
                }
            }
            if (nuevaStats) {
                document.querySelector('.sc-act-stats').innerHTML = nuevaStats.innerHTML;
            }
        })
        .catch(() => {});
}, 5000);
</script>
<script src="js/dashboard.js"></script>
</body>
</html>

