<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="com.nexodist.model.Rol" %>
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
    private boolean tienePanel(String paneles, String panel) {
        if (paneles == null || panel == null) return false;
        for (String item : paneles.split(",")) {
            if (item.trim().equalsIgnoreCase(panel)) return true;
        }
        return false;
    }
%>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    if (usuario == null) { response.sendRedirect("index.jsp"); return; }
    List<Usuario> usuariosLista = (List<Usuario>) request.getAttribute("usuariosLista");
    List<String> rolesDisponibles = (List<String>) request.getAttribute("rolesDisponibles");
    List<Rol> roles = (List<Rol>) request.getAttribute("roles");
    Integer alertasCount = (Integer) request.getAttribute("alertasCount");
    request.setAttribute("scPagina", "usuarios");
    request.setAttribute("alertasCount", alertasCount);

    int activos = 0;
    if (usuariosLista != null) {
        for (Usuario u : usuariosLista) if (u.isActivo()) activos++;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Usuarios - Sistema Clínico</title>
    <link rel="stylesheet" href="css/stockcontrol.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body class="sc-body">
<div class="sc-layout">
    <jsp:include page="includes/admin-sidebar.jsp"/>
    <main class="sc-main">
        <jsp:include page="includes/admin-topbar.jsp"/>

        <div class="sc-page-header">
            <div>
                <h1 class="sc-page-title">Usuarios</h1>
                <p class="sc-page-sub"><%= activos %> activos de <%= usuariosLista != null ? usuariosLista.size() : 0 %> total</p>
            </div>
            <div class="sc-header-actions">
                <button class="sc-btn-secondary-action" onclick="abrirModalRol()">+ Crear rol</button>
                <button class="sc-btn-primary" onclick="document.getElementById('modal-usuario').classList.remove('sc-hidden')">+ Crear usuario</button>
            </div>
        </div>

        <% if ("1".equals(request.getParameter("creado"))) { %><div class="sc-success-box">Usuario creado correctamente.</div><% } %>
        <% if ("1".equals(request.getParameter("modificado"))) { %><div class="sc-success-box">Usuario modificado correctamente.</div><% } %>
        <% if ("1".equals(request.getParameter("rolGuardado"))) { %><div class="sc-success-box">Rol guardado correctamente.</div><% } %>
        <% if ("1".equals(request.getParameter("rolEliminado"))) { %><div class="sc-success-box">Rol eliminado correctamente.</div><% } %>
        <% if (request.getParameter("error") != null) { %><div class="sc-error-box"><%= "rolEnUso".equals(request.getParameter("error")) ? "No se puede eliminar: el rol está en uso o es Administrador." : "No se pudo guardar. Revise los campos y datos duplicados." %></div><% } %>

        <div class="sc-role-cards">
            <% if (roles != null) for (Rol rolItem : roles) { %>
            <div class="sc-role-card <%= rolItem.getNombre().equalsIgnoreCase("Administrador") ? "admin" : rolItem.getNombre().equalsIgnoreCase("Farmacia") ? "vendedor" : rolItem.getNombre().equalsIgnoreCase("Visualizador") ? "visualizador" : "operador" %>">
                <div class="sc-role-name"><%= rolItem.getNombre() %></div>
                <ul class="sc-role-perms">
                    <% if (rolItem.getPanelesPermitidos().isBlank()) { %><li>Sin paneles asignados</li><% } else for (String panel : rolItem.getPanelesPermitidos().split(",")) { %><li><%= panel %></li><% } %>
                    <li><%= rolItem.isPuedeInteractuar() ? "Puede interactuar" : "Solo lectura: botones deshabilitados" %></li>
                </ul>
                <div class="sc-role-actions">
                    <button type="button" class="sc-action-btn edit" onclick="abrirModificarRol('<%= js(rolItem.getNombre()) %>','<%= js(rolItem.getPanelesPermitidos()) %>',<%= rolItem.isPuedeInteractuar() %>)">Modificar</button>
                    <% if (!"Administrador".equalsIgnoreCase(rolItem.getNombre())) { %><form action="usuarios" method="post" onsubmit="return confirm('¿Eliminar este rol? Solo será posible si no está asignado.');"><input type="hidden" name="accion" value="eliminarRol"><input type="hidden" name="nombreRol" value="<%= rolItem.getNombre() %>"><button type="submit" class="sc-action-btn delete">Eliminar</button></form><% } %>
                </div>
            </div><% } %>
            <% if (false) { %><div class="sc-role-card admin">
                <div class="sc-role-name">Administrador</div>
                <ul class="sc-role-perms">
                    <li>Ver inventario</li>
                    <li>Editar productos</li>
                    <li>Registrar movimientos</li>
                    <li>Gestionar usuarios</li>
                </ul>
            </div>
            <div class="sc-role-card operador">
                <div class="sc-role-name">Operativo</div>
                <ul class="sc-role-perms">
                    <li>Dashboard</li>
                    <li>Productos</li>
                    <li>Entradas y salidas</li>
                    <li>Movimientos y alertas</li>
                </ul>
            </div>
            <div class="sc-role-card visualizador">
                <div class="sc-role-name">Visualizador</div>
                <ul class="sc-role-perms">
                    <li>Consulta de dashboard</li>
                    <li>Paneles seleccionados</li>
                    <li>Sin edición</li>
                    <li>Acceso controlado</li>
                </ul>
            </div>
            <% } %>
        </div>

        <div class="sc-card sc-table-wrap">
            <table class="sc-table">
                <thead>
                <tr>
                    <th>Usuario</th>
                    <th>Correo</th>
                    <th>Rol</th>
                    <th>Panel y acciones</th>
                    <th>Área</th>
                    <th>Último acceso</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
                </thead>
                <tbody>
                <% if (usuariosLista != null) {
                    for (Usuario u : usuariosLista) {
                        boolean esTu = u.getCorreo().equalsIgnoreCase(usuario.getCorreo());
                %>
                <tr>
                    <td>
                        <span class="sc-user-table-avatar"><%= u.getIniciales() %></span>
                        <%= u.getNombre() %><% if (esTu) { %> <span style="color:#5f7087;font-size:14px;">(tú)</span><% } %>
                    </td>
                    <td><%= u.getCorreo() %></td>
                    <td>
                        <span class="sc-cat-pill"><%= com.nexodist.service.DashboardService.rolVisible(u) %></span>
                    </td>
                    <td><div class="sc-prod-name"><%= u.getPanelPrincipal() %></div><div class="sc-prod-sub"><%= u.getAccionesPermitidas() %></div></td>
                    <td><%= u.getArea() != null ? u.getArea() : "—" %></td>
                    <td><%= u.getUltimoAcceso() != null ? u.getUltimoAcceso() : "—" %></td>
                    <td>
                        <span class="sc-status <%= u.isActivo() ? "activo" : "inactivo" %>">
                            <%= u.isActivo() ? "Activo" : "Inactivo" %>
                        </span>
                    </td>
                    <td>
                        <div class="sc-user-actions">
                        <button type="button" class="sc-action-btn edit sc-user-action-lg" onclick="abrirModificarUsuario('<%= js(u.getNombres()) %>','<%= js(u.getApellidos()) %>','<%= js(u.getTelefono()) %>','<%= js(u.getCorreo()) %>','<%= js(u.getClave()) %>','<%= js(u.getRol()) %>','<%= js(u.getArea()) %>','<%= u.isActivo() %>','<%= js(u.getPanelesPermitidos()) %>')">Modificar</button>
                        <% if (!esTu) { %>
                        <form action="usuarios" method="post" style="display:inline;">
                            <input type="hidden" name="correo" value="<%= u.getCorreo() %>">
                            <% if (u.isActivo()) { %>
                                <input type="hidden" name="accion" value="desactivar">
                                <button type="submit" class="sc-action-btn order sc-user-action-lg">Desactivar</button>
                            <% } else { %>
                                <input type="hidden" name="accion" value="activar">
                                <button type="submit" class="sc-action-btn order sc-user-action-lg">Activar</button>
                            <% } %>
                        </form>
                        <form action="usuarios" method="post" style="display:inline;" onsubmit="return confirm('¿Está seguro de eliminar este usuario?');">
                            <input type="hidden" name="accion" value="eliminar">
                            <input type="hidden" name="correo" value="<%= u.getCorreo() %>">
                            <button type="submit" class="sc-action-btn delete sc-user-action-lg">Eliminar</button>
                        </form>
                        <% } %>
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

<div id="modal-rol" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal sc-modal-small">
        <h3 id="titulo-modal-rol">Crear rol</h3>
        <p class="sc-card-sub">Defina un nombre de rol y marque los paneles que tendrá por defecto.</p>
        <form action="usuarios" method="post" id="form-rol">
        <input type="hidden" name="accion" value="crearRol"><input type="hidden" name="rolOriginal" value="">
        <div class="sc-form-group">
            <label>Nombre del rol</label>
            <input type="text" name="nombreRol" id="nuevo-rol-nombre" maxlength="80" required placeholder="Ej. Coordinador de bodega">
        </div>
        <label class="sc-permission-switch"><input type="checkbox" name="puedeInteractuar" id="rol-interactua"> Puede interactuar con botones y ejecutar acciones</label>
        <div class="sc-panel-picker visible" id="paneles-rol">
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="dashboard"> Dashboard</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="productos"> Productos</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="proveedor"> Proveedores</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="movimientos"> Movimientos</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="alertas"> Alertas</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="usuarios"> Usuarios</label>
            <label class="sc-panel-option"><input type="checkbox" name="panelesRol" value="despachos"> Despachos</label>
        </div>
        <div class="sc-form-actions">
            <button type="submit" class="sc-btn-primary">Guardar rol</button>
            <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-rol').classList.add('sc-hidden')">Cancelar</button>
        </div>
        </form>
    </div>
</div>

<div id="modal-usuario" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal">
        <h3>Crear usuario</h3>
        <p class="sc-card-sub">Seleccione el rol y los paneles disponibles para el usuario.</p>
        <form action="usuarios" method="post" onsubmit="return validarClaves(this)">
            <input type="hidden" name="accion" value="crear">
            <div class="sc-form-group"><label>Nombre</label><input type="text" name="nombres" required></div>
            <div class="sc-form-group"><label>Apellido</label><input type="text" name="apellidos" required></div>
            <div class="sc-form-group"><label>Número de teléfono</label><input type="tel" name="telefono" maxlength="25" required></div>
            <div class="sc-form-group"><label>Correo</label><input type="email" name="correo" required></div>
            <div class="sc-form-group">
                <label>Contraseña temporal</label>
                <div class="sc-password-row">
                    <input type="password" name="clave" minlength="8" required>
                    <button type="button" class="sc-btn-sm" onclick="togglePassword(this)">Ver</button>
                </div>
            </div>
            <div class="sc-form-group">
                <label>Repetir contraseña</label>
                <div class="sc-password-row">
                    <input type="password" name="claveConfirmacion" minlength="8" required>
                    <button type="button" class="sc-btn-sm" onclick="togglePassword(this)">Ver</button>
                </div>
            </div>
            <div class="sc-form-group">
                <label>Área</label>
                <select name="area" required>
                    <option value="Administración">Administración</option>
                    <option value="Bodega">Bodega</option>
                </select>
            </div>
            <div class="sc-form-group">
                <label>Rol</label>
                <select name="rol" required onchange="aplicarPanelesRol(this)">
                    <% if (rolesDisponibles != null) {
                        for (String rolDisponible : rolesDisponibles) { %>
                    <option value="<%= rolDisponible %>"><%= rolDisponible %></option>
                    <%  }
                    } %>
                </select>
            </div>
            <div class="sc-form-group">
                <button type="button" class="sc-btn-secondary-action" onclick="togglePaneles('paneles-crear')">Paneles</button>
            </div>
            <div class="sc-panel-picker" id="paneles-crear">
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="dashboard"> Dashboard</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="productos"> Productos</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="proveedor"> Proveedores</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="movimientos"> Movimientos</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="alertas"> Alertas</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="usuarios"> Usuarios</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="despachos"> Despachos</label>
            </div>
            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Crear usuario</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-usuario').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>
<div id="modal-editar-usuario" class="sc-modal-bg sc-hidden" onclick="if(event.target===this)this.classList.add('sc-hidden')">
    <div class="sc-modal">
        <h3>Modificar usuario</h3>
        <p class="sc-card-sub">Actualice información, credenciales, área, estado, rol y paneles asignados.</p>
        <form action="usuarios" method="post" id="form-editar-usuario" onsubmit="return validarClaves(this)">
            <input type="hidden" name="accion" value="modificar">
            <input type="hidden" name="correoOriginal" value="">
            <div class="sc-form-group"><label>Nombre</label><input type="text" name="nombres" required></div>
            <div class="sc-form-group"><label>Apellido</label><input type="text" name="apellidos" required></div>
            <div class="sc-form-group"><label>Número de teléfono</label><input type="tel" name="telefono" maxlength="25" required></div>
            <div class="sc-form-group"><label>Correo</label><input type="email" name="correo" required></div>
            <div class="sc-form-group">
                <label>Contraseña</label>
                <div class="sc-password-row">
                    <input type="password" name="clave" minlength="8" required>
                    <button type="button" class="sc-btn-sm" onclick="togglePassword(this)">Ver</button>
                </div>
            </div>
            <div class="sc-form-group">
                <label>Repetir contraseña</label>
                <div class="sc-password-row">
                    <input type="password" name="claveConfirmacion" minlength="8" required>
                    <button type="button" class="sc-btn-sm" onclick="togglePassword(this)">Ver</button>
                </div>
            </div>
            <div class="sc-form-group">
                <label>Área</label>
                <select name="area" required>
                    <option value="Administración">Administración</option>
                    <option value="Bodega">Bodega</option>
                </select>
            </div>
            <div class="sc-form-group">
                <label>Rol</label>
                <select name="rol" required onchange="aplicarPanelesRol(this)">
                    <% if (rolesDisponibles != null) {
                        for (String rolDisponible : rolesDisponibles) { %>
                    <option value="<%= rolDisponible %>"><%= rolDisponible %></option>
                    <%  }
                    } %>
                </select>
            </div>
            <div class="sc-form-group">
                <button type="button" class="sc-btn-secondary-action" onclick="togglePaneles('paneles-editar')">Paneles</button>
            </div>
            <div class="sc-panel-picker" id="paneles-editar">
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="dashboard"> Dashboard</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="productos"> Productos</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="proveedor"> Proveedores</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="movimientos"> Movimientos</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="alertas"> Alertas</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="usuarios"> Usuarios</label>
                <label class="sc-panel-option"><input type="checkbox" name="paneles" value="despachos"> Despachos</label>
            </div>
            <div class="sc-form-group">
                <label>Estado</label>
                <select name="activo" required>
                    <option value="true">Activo</option>
                    <option value="false">Inactivo</option>
                </select>
            </div>
            <div class="sc-form-actions">
                <button type="submit" class="sc-btn-primary">Guardar cambios</button>
                <button type="button" class="sc-btn-sm" onclick="document.getElementById('modal-editar-usuario').classList.add('sc-hidden')">Cancelar</button>
            </div>
        </form>
    </div>
</div>
<script>
const panelesPorRol = {
    'Administrador': ['dashboard', 'productos', 'proveedor', 'movimientos', 'alertas', 'usuarios', 'despachos'],
    'Operativo': ['dashboard', 'productos', 'movimientos', 'alertas', 'despachos'],
    'Visualizador': ['dashboard']
    <% if (roles != null) for (Rol r : roles) { %>, '<%= js(r.getNombre()) %>': '<%= js(r.getPanelesPermitidos()) %>'.split(',').filter(Boolean)<% } %>
};

function abrirModificarUsuario(nombres, apellidos, telefono, correo, clave, rol, area, activo, paneles) {
    const modal = document.getElementById('modal-editar-usuario');
    const form = document.getElementById('form-editar-usuario');
    form.elements.correoOriginal.value = correo;
    form.elements.nombres.value = nombres;
    form.elements.apellidos.value = apellidos;
    form.elements.telefono.value = telefono;
    form.elements.correo.value = correo;
    form.elements.clave.value = clave;
    form.elements.claveConfirmacion.value = clave;
    const areaNormalizada = normalizarArea(area);
    form.elements.area.value = areaNormalizada;
    form.elements.rol.value = rol === 'Operador' ? 'Operativo' : ((rol === 'Vendedor' || rol === 'Ventas') ? 'Farmacia' : rol);
    form.elements.activo.value = String(activo === 'true' || activo === true);
    marcarPaneles('paneles-editar', paneles ? paneles.split(',') : panelesPorRol[form.elements.rol.value] || []);
    modal.classList.remove('sc-hidden');
}
function normalizarArea(area) {
    if (!area) return 'Bodega';
    const valor = area.toLowerCase();
    if (valor.includes('admin') || valor.includes('dirección') || valor.includes('direccion')) return 'Administración';
    if (valor.includes('venta') || valor.includes('devolucion') || valor.includes('devolución')) return 'Administración';
    return 'Bodega';
}
function abrirModalRol() {
    const form = document.getElementById('form-rol'); form.reset();
    form.elements.accion.value = 'crearRol'; form.elements.rolOriginal.value = '';
    document.getElementById('titulo-modal-rol').textContent = 'Crear rol';
    document.getElementById('nuevo-rol-nombre').value = '';
    document.querySelectorAll('#paneles-rol input[type="checkbox"]').forEach(c => c.checked = false);
    document.getElementById('modal-rol').classList.remove('sc-hidden');
}
function abrirModificarRol(nombre, paneles, interactua) {
    abrirModalRol();
    const form = document.getElementById('form-rol');
    document.getElementById('titulo-modal-rol').textContent = 'Modificar rol';
    form.elements.accion.value = 'modificarRol';
    form.elements.rolOriginal.value = nombre;
    form.elements.nombreRol.value = nombre;
    form.elements.puedeInteractuar.checked = interactua;
    marcarPaneles('paneles-rol', paneles ? paneles.split(',') : []);
}
function togglePaneles(id) {
    document.getElementById(id).classList.toggle('visible');
}
function aplicarPanelesRol(select) {
    const paneles = panelesPorRol[select.value];
    if (!paneles) return;
    const picker = select.closest('form').querySelector('.sc-panel-picker');
    if (!picker) return;
    marcarPaneles(picker.id, paneles);
}
function marcarPaneles(id, paneles) {
    const seleccionados = (paneles || []).map(p => p.trim().toLowerCase()).filter(Boolean);
    document.querySelectorAll('#' + id + ' input[type="checkbox"]').forEach(c => {
        c.checked = seleccionados.includes(c.value.toLowerCase());
    });
}
function togglePassword(btn) {
    const input = btn.parentElement.querySelector('input');
    input.type = input.type === 'password' ? 'text' : 'password';
    btn.textContent = input.type === 'password' ? 'Ver' : 'Ocultar';
}
function validarClaves(form) {
    if (form.elements.clave.value !== form.elements.claveConfirmacion.value) {
        alert('Las contraseñas no coinciden.');
        form.elements.claveConfirmacion.focus();
        return false;
    }
    return true;
}
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('select[name="rol"]').forEach(aplicarPanelesRol);
});
</script>
<script src="js/dashboard.js"></script>
</body>
</html>

