<%@ page import="jakarta.servlet.http.Cookie" %>
<%@ page import="com.nexodist.model.Usuario" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Usuario sesionActiva = (Usuario) session.getAttribute("usuario");
    String ultimoUsuario = "";
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("ultimoUsuarioSistemaClinico".equals(cookie.getName())) {
                ultimoUsuario = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema Clínico</title>
    <link rel="stylesheet" href="css/estilos.css">
    <link rel="stylesheet" href="css/accessibility.css">
    <script src="js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="login-page" id="contenido-principal" tabindex="-1">
    <section class="brand-panel">
        <div class="brand-top">
            <span class="logo">SC</span>
            <span>Sistema Clínico</span>
        </div>

        <div>
            <h1 class="hero-title">Sistema Clínico <span>Inventario.</span></h1>
            <p class="hero-copy">Controle medicamentos, lotes, fechas de caducidad, despachos y movimientos por usuario desde una sola plataforma.</p>
        </div>

        <div class="brand-stats">
            <div class="stat">
                <strong>FIFO</strong>
                <span>Primero en entrar, primero en salir</span>
            </div>
            <div class="stat">
                <strong>90 días</strong>
                <span>Alerta anticipada</span>
            </div>
            <div class="stat">
                <strong>4 roles</strong>
                <span>Acceso controlado</span>
            </div>
        </div>
    </section>

    <section class="login-panel">
        <div class="login-card">
            <h1>Sistema Clínico</h1>
            <p class="texto">Acceda con sus credenciales para iniciar sesion</p>

            <% if (sesionActiva != null) { %>
                <p class="aviso">
                    Sesión activa: <strong><%= sesionActiva.getNombre() %></strong> (<%= sesionActiva.getRol() %>).
                    <a href="inicio" style="color:#7eb8ff;">Ir al inventario</a> ·
                    <a href="logout" style="color:#ffb4bf;">Cerrar sesión</a>
                </p>
            <% } %>

            <% if (request.getAttribute("error") != null) { %>
                <p class="error" role="alert" aria-live="assertive"><%= request.getAttribute("error") %></p>
            <% } %>

            <form action="login" method="post" class="formulario">
                <label>
                    Usuario o correo
                    <input type="email" name="usuario" value="<%= ultimoUsuario %>" placeholder="admin@sistema-clinico.local" maxlength="120" autocomplete="username" aria-required="true" required>
                </label>
                <label>
                    Contrasena
                    <input type="password" name="clave" placeholder="Contraseña" minlength="8" maxlength="128" autocomplete="current-password" aria-required="true" required>
                </label>
                <button type="submit">Iniciar sesión</button>
            </form>

            <div class="demo-list">
                <p class="texto">Cuentas de demostracion:</p>
                <div class="demo-item"><span>admin@sistema-clinico.local / admin123</span><strong>Administrador</strong></div>
                <div class="demo-item"><span>operativo@sistema-clinico.local / operativo123</span><strong>Operativo</strong></div>
            </div>
        </div>
    </section>
</main>
</body>
</html>

