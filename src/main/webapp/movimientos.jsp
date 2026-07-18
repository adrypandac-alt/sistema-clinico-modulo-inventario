<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Movimiento" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Movimiento> movimientos = (List<Movimiento>) request.getAttribute("movimientos");
    List<Producto> productos = (List<Producto>) application.getAttribute("productos");
    Integer alertasCount = (Integer) request.getAttribute("alertasCount");
    request.setAttribute("scPagina", "movimientos");
    request.setAttribute("alertasCount", alertasCount);
    Set<String> usuariosMovimiento = (Set<String>) request.getAttribute("usuariosMovimiento");
    Map<String, String> rolesMovimiento = (Map<String, String>) request.getAttribute("rolesMovimiento");
    String usuarioFiltro = (String) request.getAttribute("usuarioFiltro");
    Boolean puedeVerTodosUsuarios = (Boolean) request.getAttribute("puedeVerTodosUsuarios");
    Boolean puedeRegistrarMovimiento = (Boolean) request.getAttribute("puedeRegistrarMovimiento");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Movimientos - Sistema Clínico</title>
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
                <h1 class="sc-page-title">Movimientos</h1>
                <p class="sc-page-sub"><%= movimientos != null ? movimientos.size() : 0 %> registros del usuario seleccionado</p>
            </div>
            <% if (Boolean.TRUE.equals(puedeRegistrarMovimiento)) { %>
            <button class="sc-btn-primary" onclick="document.getElementById('modal-mov').classList.remove('sc-hidden')">+ Ajuste manual de stock</button>
            <% } else { %><span class="sc-readonly-pill">Solo lectura</span><% } %>
        </div>

        <% if (!Boolean.TRUE.equals(puedeVerTodosUsuarios)) { %>
        <div class="sc-info-box" style="margin-bottom:12px;">
            Mostrando únicamente los movimientos registrados por tu usuario.
        </div>
        <% } %>

        <form action="movimientos" method="get" class="sc-user-filter-form">
            <label for="usuario-mov">Seleccionar por usuario</label>
            <select id="usuario-mov" name="usuario" onchange="this.form.submit()">
                <% if (Boolean.TRUE.equals(puedeVerTodosUsuarios)) { %>
                <option value="">Todos los usuarios</option>
                <% } %>
                <% if (usuariosMovimiento != null) { for (String nombreUsuario : usuariosMovimiento) { %>
                <option value="<%= nombreUsuario %>" <%= nombreUsuario.equalsIgnoreCase(usuarioFiltro) ? "selected" : "" %>>
                    <%= nombreUsuario %><%= rolesMovimiento != null && rolesMovimiento.get(nombreUsuario) != null ? " - " + rolesMovimiento.get(nombreUsuario) : "" %>
                </option>
                <% } } %>
            </select>
        </form>

        <div class="sc-filters" id="filtros-tipo">
            <button class="sc-filter-btn activo" onclick="setFiltroTipo(this,'')">Todos</button>
            <button class="sc-filter-btn" onclick="setFiltroTipo(this,'ENTRADA')">Entradas</button>
            <button class="sc-filter-btn" onclick="setFiltroTipo(this,'SALIDA')">Salidas</button>
            <button class="sc-filter-btn" onclick="setFiltroTipo(this,'PEDIDO')">Pedidos</button>
            <button class="sc-filter-btn" onclick="setFiltroTipo(this,'AJUSTE')">Ajustes</button>
            <button class="sc-filter-btn" onclick="setFiltroTipo(this,'ELIMINACION')">Eliminaciones</button>
        </div>

        <div class="sc-card" id="lista-movimientos">
            <% if (movimientos != null) {
                for (int i = movimientos.size() - 1; i >= 0; i--) {
                    Movimiento m = movimientos.get(i);
                    String iconCls = "ENTRADA".equals(m.getTipo()) ? "entrada" : ("SALIDA".equals(m.getTipo()) ? "salida" : ("PEDIDO".equals(m.getTipo()) ? "pedido" : ("ELIMINACION".equals(m.getTipo()) ? "eliminacion" : "ajuste")));
                    String arrow = "ENTRADA".equals(m.getTipo()) ? "↑" : ("SALIDA".equals(m.getTipo()) ? "↓" : ("PEDIDO".equals(m.getTipo()) ? "↗" : ("ELIMINACION".equals(m.getTipo()) ? "×" : "↔")));
                    String qtyCls = "ENTRADA".equals(m.getTipo()) ? "up" : ("ELIMINACION".equals(m.getTipo()) ? "danger" : "down");
                    String signo = "ENTRADA".equals(m.getTipo()) ? "+" : ("SALIDA".equals(m.getTipo()) ? "-" : "");
                    String usuarioMov = m.getUsuario() == null ? "Sin usuario" : m.getUsuario();
                    String rolMov = "No registrado";
                    int rolInicio = usuarioMov.lastIndexOf("(");
                    int rolFin = usuarioMov.lastIndexOf(")");
                    if (rolInicio >= 0 && rolFin > rolInicio) {
                        rolMov = usuarioMov.substring(rolInicio + 1, rolFin);
                        usuarioMov = usuarioMov.substring(0, rolInicio).trim();
                    } else if (rolesMovimiento != null && rolesMovimiento.get(usuarioMov) != null) {
                        rolMov = rolesMovimiento.get(usuarioMov);
                    }
            %>
            <div class="sc-mov-item" data-tipo="<%= m.getTipo() %>">
                <div class="sc-mov-icon <%= iconCls %>"><%= arrow %></div>
                <div class="sc-mov-info">
                    <div class="sc-mov-name">
                        <%= m.getProductoNombre() %>
                        <span class="sc-tipo-pill <%= iconCls %>"><%= m.getTipo() %></span>
                    </div>
                    <div class="sc-mov-desc">
                        <span><strong>Usuario:</strong> <%= usuarioMov %></span>
                        <span><strong>Rol:</strong> <%= rolMov %></span>
                        <span><strong>Observación:</strong> <%= m.getMotivo() %></span>
                    </div>
                </div>
                <div>
                    <div class="sc-mov-qty <%= qtyCls %>"><%= signo %><%= m.getCantidad() %></div>
                    <div class="sc-mov-stock"><%= m.getStockAntes() %> → <%= m.getStockDespues() %></div>
                </div>
                <div class="sc-mov-date"><%= m.getFecha() %></div>
            </div>
            <%  }
            } %>
        </div>
    </main>
</div>

<% if (Boolean.TRUE.equals(puedeRegistrarMovimiento)) { %>
<div id="modal-mov" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal">
        <h3>Ajuste manual de stock</h3>
        <p class="sc-card-sub">Los despachos y las altas se registran automáticamente. Use este formulario solo para correcciones de inventario que no correspondan a un despacho o pedido.</p>
        <form action="movimientos" method="post">
            <div class="sc-form-group">
                <label>Producto</label>
                <select name="productoId" required>
                    <% if (productos != null) {
                        for (Producto p : productos) { %>
                    <option value="<%= p.getId() %>"><%= p.getSku() %> — <%= p.getNombre() %> (stock: <%= p.getStock() %>)</option>
                    <%  }
                    } %>
                </select>
            </div>
            <div class="sc-form-group">
                <label>Tipo</label>
                <select name="tipo" required>
                    <option value="ENTRADA">Entrada</option>
                    <option value="SALIDA">Salida</option>
                    <option value="AJUSTE">Ajuste</option>
                </select>
            </div>
            <div class="sc-form-group">
                <label>Cantidad</label>
                <input type="number" name="cantidad" min="1" required>
            </div>
            <div class="sc-form-group">
                <label>Motivo</label>
                <input type="text" name="motivo" placeholder="Motivo clínico o ajuste de emergencia" required>
            </div>
            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Registrar</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-mov').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>
<% } %>

<script>
function setFiltroTipo(btn, tipo) {
    document.querySelectorAll('#filtros-tipo .sc-filter-btn').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('#lista-movimientos .sc-mov-item').forEach(item => {
        item.style.display = !tipo || item.dataset.tipo === tipo ? '' : 'none';
    });
}
</script>
<script src="js/dashboard.js"></script>
</body>
</html>

