<%@ page import="com.nexodist.model.Pedido" %>
<%@ page import="com.nexodist.model.PedidoItem" %>
<%@ page import="com.nexodist.model.Producto" %>
<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String js(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
%>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    List<Pedido> pedidos = (List<Pedido>) request.getAttribute("pedidos");
    List<PedidoItem> carrito = (List<PedidoItem>) request.getAttribute("carritoPedido");
    String pedidoNombres = (String) request.getAttribute("pedidoNombres");
    String pedidoApellidos = (String) request.getAttribute("pedidoApellidos");
    request.setAttribute("scPagina", "inventario");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pedidos - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <link rel="stylesheet" href="css/accessibility.css">
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Panel de despachos</h1>
                <p class="sc-page-sub">Registro, preparación y entrega de medicamentos</p>
            </div>
            <a class="sc-btn-sm" href="inventario">Volver a productos</a>
        </div>

        <% if ("1".equals(request.getParameter("ok"))) { %>
            <div class="sc-success-box">Pedido registrado en la bandeja de despachos.</div>
        <% } %>
        <% if ("vacio".equals(request.getParameter("error"))) { %>
            <div class="sc-error-box">Agregue al menos un producto antes de revisar el pedido.</div>
        <% } else if ("producto".equals(request.getParameter("error"))) { %>
            <div class="sc-error-box">Seleccione un producto registrado y una cantidad válida.</div>
        <% } %>

        <div class="sc-pedidos-layout">
            <section class="sc-card sc-pedido-panel <%= carrito == null || carrito.isEmpty() ? "sc-registro-cerrado" : "" %>" id="registro-pedido">
                <div class="sc-section-heading">
                    <div>
                        <h2>Registrar pedido</h2>
                        <p>Ingrese la persona solicitante y agregue los productos con sus cantidades.</p>
                    </div>
                    <span class="sc-cat-pill"><%= carrito != null ? carrito.size() : 0 %> productos agregados</span>
                </div>
                <form action="pedidos" method="post" class="sc-cancelar-registro" onsubmit="return confirm('¿Cancelar el registro actual?');">
                    <input type="hidden" name="accion" value="cancelar">
                    <button type="submit" class="sc-btn-sm">Cancelar registro</button>
                </form>

                <form action="pedidos" method="post" class="sc-pedido-form" onsubmit="return validarProductoPedido()">
                    <input type="hidden" name="accion" value="agregar">
                    <input type="hidden" name="id" value="">
                    <div class="sc-solicitante-grid">
                        <div class="sc-form-group"><label>Nombre de quien solicita</label><input type="text" name="nombresSolicitante" required maxlength="60" value="<%= pedidoNombres == null ? "" : pedidoNombres %>" placeholder="Nombre"></div>
                        <div class="sc-form-group"><label>Apellido de quien solicita</label><input type="text" name="apellidosSolicitante" required maxlength="60" value="<%= pedidoApellidos == null ? "" : pedidoApellidos %>" placeholder="Apellido"></div>
                    </div>
                    <div class="sc-form-group">
                        <label>Producto</label>
                        <input type="text" id="pedido-producto" list="productos-pedido"
                               placeholder="Escriba SKU o nombre del producto" autocomplete="off"
                               oninput="seleccionarProductoPedido()">
                        <datalist id="productos-pedido">
                            <% if (productos != null) {
                                for (Producto p : productos) { %>
                            <option value="<%= p.getSku() %> - <%= p.getNombre() %>" data-id="<%= p.getId() %>" data-stock="<%= p.getStock() %>" data-proveedor="<%= p.getProveedor() %>"></option>
                            <%  }
                            } %>
                        </datalist>
                        <div id="pedido-producto-info" class="sc-form-hint">Seleccione un producto registrado.</div>
                    </div>
                    <div class="sc-pedido-form-row">
                        <div class="sc-form-group">
                            <label>Cantidad solicitada</label>
                            <input type="number" name="cantidadPedido" min="1" required>
                        </div>
                        <button type="submit" class="sc-btn-primary">Agregar producto</button>
                    </div>
                    <p class="sc-form-hint">Puede repetir este paso para agregar todos los productos necesarios. Si agrega el mismo producto otra vez, su cantidad se acumulará.</p>
                    <div class="sc-form-group">
                        <label>Observaciones</label>
                        <textarea name="observacionPedido" rows="3" placeholder="Motivo o detalle del pedido"></textarea>
                    </div>
                </form>

                <div class="sc-cart-title">
                    <h3>Productos a despachar</h3>
                    <% if (carrito != null && !carrito.isEmpty()) { %>
                    <form action="pedidos" method="post">
                        <input type="hidden" name="accion" value="revisar">
                        <button type="submit" class="sc-btn-cart">Revisar y registrar pedido</button>
                    </form>
                    <% } %>
                </div>

                <% if (carrito == null || carrito.isEmpty()) { %>
                    <div class="sc-empty-state">Todavía no hay productos agregados al despacho.</div>
                <% } else {
                    for (PedidoItem item : carrito) { %>
                    <div class="sc-pedido-item">
                        <div>
                            <strong><%= item.getNombre() %></strong>
                            <span><%= item.getSku() %> · <%= item.getProveedor() %></span>
                            <% if (item.getObservacion() != null && !item.getObservacion().isBlank()) { %>
                                <small>Observación: <%= item.getObservacion() %></small>
                            <% } %>
                        </div>
                        <div class="sc-pedido-item-actions">
                            <span class="sc-pedido-qty"><%= item.getCantidad() %></span>
                            <form action="pedidos" method="post">
                                <input type="hidden" name="accion" value="quitar">
                                <input type="hidden" name="id" value="<%= item.getProductoId() %>">
                                <button type="submit" class="sc-btn-danger-outline">Quitar</button>
                            </form>
                        </div>
                    </div>
                <%  }
                } %>
            </section>

            <section class="sc-card sc-pedido-panel">
                <div class="sc-section-heading">
                    <div>
                        <h2>Bandeja de despachos</h2>
                        <p>Se actualiza automáticamente cada 30 segundos.</p>
                    </div>
                    <button type="button" id="btn-registrar-pedido" class="sc-btn-primary <%= carrito != null && !carrito.isEmpty() ? "sc-hidden" : "" %>" onclick="abrirRegistroPedido()">+ Registrar pedido</button>
                </div>

                <% if (pedidos == null || pedidos.isEmpty()) { %>
                    <div class="sc-empty-state">No existen pedidos registrados todavía.</div>
                <% } else {
                    for (Pedido pedido : pedidos) { %>
                    <div class="sc-pedido-history">
                        <div class="sc-pedido-history-head">
                            <strong>Pedido #<%= pedido.getId() %></strong>
                            <span class="sc-cat-pill"><%= pedido.getEstado() %></span>
                        </div>
                        <p><strong>Solicitado por: <%= pedido.getSolicitante() %></strong><br><%= pedido.getFecha() %><% if (pedido.getDespachadoPor() != null) { %> · Responsable de despacho: <%= pedido.getDespachadoPor() %><% } %><% if (pedido.getEntregadoA() != null && !pedido.getEntregadoA().isBlank()) { %><br>Entregado a: <strong><%= pedido.getEntregadoA() %></strong><% } %></p>
                        <ul>
                            <% for (PedidoItem item : pedido.getItems()) { %>
                                <li><%= item.getCantidad() %> x <%= item.getNombre() %></li>
                            <% } %>
                        </ul>
                        <% if ("Pendiente".equalsIgnoreCase(pedido.getEstado())) { %><form action="pedidos" method="post"><input type="hidden" name="accion" value="iniciarDespacho"><input type="hidden" name="id" value="<%=pedido.getId()%>"><button class="sc-btn-primary" type="submit">Empezar despacho</button></form><% } %>
                        <% if ("Preparando".equalsIgnoreCase(pedido.getEstado())) { %><form action="pedidos" method="post" class="sc-entrega-form" onsubmit="return confirm('¿Confirmar la entrega y descontar los productos del inventario?');"><input type="hidden" name="accion" value="completarDespacho"><input type="hidden" name="id" value="<%=pedido.getId()%>"><label>Persona que recibe la medicación<input type="text" name="entregadoA" maxlength="120" required placeholder="Nombre y apellido"></label><button class="sc-btn-cart" type="submit">Despachar</button></form><% } %>
                    </div>
                <%  }
                } %>
            </section>
        </div>
    </main>
</div>

<script>
function abrirRegistroPedido() {
    const panel = document.getElementById('registro-pedido');
    panel.classList.remove('sc-registro-cerrado');
    document.getElementById('btn-registrar-pedido').classList.add('sc-hidden');
    panel.scrollIntoView({behavior: 'smooth', block: 'start'});
    const primerCampo = panel.querySelector('input[name="nombresSolicitante"]');
    if (primerCampo) setTimeout(() => primerCampo.focus(), 350);
}
setTimeout(function(){
    const registroAbierto = !document.getElementById('registro-pedido').classList.contains('sc-registro-cerrado');
    if (!registroAbierto && !document.querySelector('.sc-modal-bg:not(.sc-hidden)')) location.reload();
}, 30000);
function seleccionarProductoPedido() {
    const input = document.getElementById('pedido-producto');
    const form = document.querySelector('.sc-pedido-form');
    const info = document.getElementById('pedido-producto-info');
    const opcion = Array.from(document.querySelectorAll('#productos-pedido option'))
        .find(opt => opt.value.toLowerCase() === input.value.trim().toLowerCase());
    if (opcion) {
        form.elements.id.value = opcion.dataset.id;
        info.textContent = 'Stock actual: ' + opcion.dataset.stock + ' · Proveedor: ' + opcion.dataset.proveedor;
    } else {
        form.elements.id.value = '';
        info.textContent = 'Escriba y seleccione un producto registrado.';
    }
}

function validarProductoPedido() {
    seleccionarProductoPedido();
    const form = document.querySelector('.sc-pedido-form');
    if (!form.elements.id.value) {
        document.getElementById('pedido-producto-info').textContent = 'Seleccione un producto válido de la lista.';
        document.getElementById('pedido-producto').focus();
        return false;
    }
    return true;
}
</script>
<script src="js/dashboard.js"></script>
</body>
</html>

