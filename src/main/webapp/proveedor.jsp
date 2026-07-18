<%@ page import="com.nexodist.model.Proveedor" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String js(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "\\n");
    }
%>
<%
    if (session.getAttribute("usuario") == null) { response.sendRedirect("index.jsp"); return; }
    List<Proveedor> proveedores = (List<Proveedor>) request.getAttribute("proveedores");
    request.setAttribute("scPagina", "proveedor");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Proveedores - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Proveedores</h1>
                <p class="sc-page-sub"><%= proveedores != null ? proveedores.size() : 0 %> proveedores registrados</p>
            </div>
            <button class="sc-btn-primary" onclick="abrirNuevoProveedor()">+ Nuevo proveedor</button>
        </div>

        <% if (request.getAttribute("mensajeError") != null) { %>
            <div class="sc-error-box"><%= request.getAttribute("mensajeError") %></div>
        <% } %>
        <% if ("1".equals(request.getParameter("creado"))) { %><div class="sc-success-box">Proveedor creado correctamente.</div><% } %>
        <% if ("1".equals(request.getParameter("actualizado"))) { %><div class="sc-success-box">Proveedor actualizado correctamente.</div><% } %>
        <% if ("1".equals(request.getParameter("eliminado"))) { %><div class="sc-success-box">Proveedor eliminado correctamente.</div><% } %>

        <div class="sc-search">
            <span>🔍</span>
            <input type="text" id="buscar-proveedor" placeholder="Buscar por RUC, nombre o contacto..." oninput="filtrarProveedores()">
        </div>

        <div class="sc-card sc-table-wrap">
            <table class="sc-table" id="tabla-proveedores">
                <thead>
                <tr>
                    <th>RUC</th>
                    <th>Proveedor</th>
                    <th>Contacto</th>
                    <th>Teléfono</th>
                    <th>Correo</th>
                    <th>Estado</th>
                    <th>Observaciones</th>
                    <th>Acciones</th>
                </tr>
                </thead>
                <tbody>
                <% if (proveedores != null) {
                    for (Proveedor proveedor : proveedores) { %>
                <tr data-search="<%= (proveedor.getRuc() + " " + proveedor.getNombre() + " " + proveedor.getContacto()).toLowerCase() %>">
                    <td><%= proveedor.getRuc() %></td>
                    <td>
                        <div class="sc-prod-name"><%= proveedor.getNombre() %></div>
                        <div class="sc-prod-sub"><%= proveedor.getDireccion() == null || proveedor.getDireccion().isBlank() ? "Sin dirección" : proveedor.getDireccion() %></div>
                    </td>
                    <td><%= proveedor.getContacto() %></td>
                    <td><%= proveedor.getTelefono() %></td>
                    <td><%= proveedor.getCorreo() %></td>
                    <td><span class="sc-status <%= proveedor.isActivo() ? "activo" : "inactivo" %>"><%= proveedor.isActivo() ? "Activo" : "Inactivo" %></span></td>
                    <td><%= proveedor.getObservaciones() == null || proveedor.getObservaciones().isBlank() ? "—" : proveedor.getObservaciones() %></td>
                    <td>
                        <div class="sc-action-buttons">
                            <button type="button" class="sc-action-btn edit" onclick="editarProveedor('<%= js(proveedor.getRuc()) %>','<%= js(proveedor.getNombre()) %>','<%= js(proveedor.getContacto()) %>','<%= js(proveedor.getTelefono()) %>','<%= js(proveedor.getCorreo()) %>','<%= js(proveedor.getDireccion()) %>','<%= proveedor.isActivo() ? "Activo" : "Inactivo" %>','<%= js(proveedor.getObservaciones()) %>')">Modificar</button>
                            <form action="proveedor" method="post" class="sc-inline-form" onsubmit="return confirm('¿Está seguro de eliminar este proveedor?');">
                                <input type="hidden" name="accion" value="eliminar">
                                <input type="hidden" name="ruc" value="<%= proveedor.getRuc() %>">
                                <button type="submit" class="sc-action-btn delete">Eliminar</button>
                            </form>
                        </div>
                    </td>
                </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </main>
</div>

<div id="modal-proveedor" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal">
        <h3 id="modal-proveedor-titulo">Nuevo proveedor</h3>
        <form action="proveedor" method="post" id="form-proveedor">
            <input type="hidden" name="accion" value="crear">
            <div class="sc-form-group"><label>RUC</label><input type="text" name="ruc" maxlength="13" pattern="[0-9]{10,13}" required></div>
            <div class="sc-form-group"><label>Nombre del proveedor</label><input type="text" name="nombre" required></div>
            <div class="sc-form-group"><label>Persona de contacto</label><input type="text" name="contacto" required></div>
            <div class="sc-form-group"><label>Teléfono</label><input type="text" name="telefono" maxlength="10" pattern="[0-9]{7,10}" required></div>
            <div class="sc-form-group"><label>Correo electrónico</label><input type="email" name="correo" required></div>
            <div class="sc-form-group"><label>Dirección opcional</label><input type="text" name="direccion"></div>
            <div class="sc-form-group">
                <label>Estado</label>
                <select name="estado">
                    <option value="Activo">Activo</option>
                    <option value="Inactivo">Inactivo</option>
                </select>
            </div>
            <div class="sc-form-group"><label>Observaciones</label><textarea name="observaciones" rows="3"></textarea></div>
            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Guardar</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-proveedor').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>

<script>
function formProveedor() {
    return document.getElementById('form-proveedor');
}
function abrirNuevoProveedor() {
    const form = formProveedor();
    form.reset();
    form.elements.accion.value = 'crear';
    form.elements.ruc.readOnly = false;
    document.getElementById('modal-proveedor-titulo').textContent = 'Nuevo proveedor';
    document.getElementById('modal-proveedor').classList.remove('sc-hidden');
}
function editarProveedor(ruc, nombre, contacto, telefono, correo, direccion, estado, observaciones) {
    const form = formProveedor();
    form.elements.accion.value = 'editar';
    form.elements.ruc.value = ruc;
    form.elements.ruc.readOnly = true;
    form.elements.nombre.value = nombre;
    form.elements.contacto.value = contacto;
    form.elements.telefono.value = telefono;
    form.elements.correo.value = correo;
    form.elements.direccion.value = direccion;
    form.elements.estado.value = estado;
    form.elements.observaciones.value = observaciones;
    document.getElementById('modal-proveedor-titulo').textContent = 'Modificar proveedor';
    document.getElementById('modal-proveedor').classList.remove('sc-hidden');
}
function filtrarProveedores() {
    const q = document.getElementById('buscar-proveedor').value.toLowerCase();
    document.querySelectorAll('#tabla-proveedores tbody tr').forEach(row => {
        row.style.display = !q || row.dataset.search.includes(q) ? '' : 'none';
    });
}
</script>
</body>
</html>

