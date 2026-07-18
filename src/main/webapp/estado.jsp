<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Estatus y cabeceras</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body>
<main class="layout">
    <aside class="sidebar">
        <div class="app-brand"><span class="logo">SC</span><span>Sistema Clínico</span></div>
        <nav class="nav">
            <a href="${pageContext.request.contextPath}/panel">Panel</a>
            <a href="${pageContext.request.contextPath}/productos">Productos</a>
            <a href="${pageContext.request.contextPath}/proveedor">Proveedores</a>
            <a href="${pageContext.request.contextPath}/carrito">Carrito</a>
            <a href="${pageContext.request.contextPath}/historial">Cookies</a>
            <a class="activo" href="${pageContext.request.contextPath}/estado">Estatus y cabeceras</a>
            <a href="${pageContext.request.contextPath}/logout">Cerrar sesion</a>
        </nav>
    </aside>
    <section class="contenido">
        <div class="topbar">
            <div><h1>Estatus y cabeceras HTTP</h1><p class="texto">Modulo de prueba con respuestas 200, 400 y 404.</p></div>
            <span class="pill">HTTP ${codigoEstado}</span>
        </div>
        <section class="grid cols-2">
            <article class="tarjeta">
                <h2>Respuesta actual</h2>
                <table>
                    <tr><th>Status HTTP</th><td>${codigoEstado}</td></tr>
                    <tr><th>Mensaje</th><td><c:out value="${mensaje}"/></td></tr>
                    <tr><th>X-Modulo</th><td>Estatus-y-Cabeceras</td></tr>
                    <tr><th>X-Empresa</th><td>Sistema-Clinico</td></tr>
                    <tr><th>Compresion</th><td>${aceptaGzip ? 'gzip aceptado' : 'gzip no detectado'}</td></tr>
                </table>
            </article>
            <article class="tarjeta">
                <h2>Probar codigo</h2>
                <p class="texto">El servlet envia diferentes codigos y cabeceras personalizadas.</p>
                <nav class="acciones">
                    <a class="btn" href="${pageContext.request.contextPath}/estado">HTTP 200 OK</a>
                    <a class="btn secundario" href="${pageContext.request.contextPath}/estado?tipo=error">HTTP 400</a>
                    <a class="btn peligro" href="${pageContext.request.contextPath}/estado?tipo=no-encontrado">HTTP 404</a>
                </nav>
            </article>
        </section>
        <article class="tarjeta" style="margin-top:18px;">
            <h2>Cabeceras de entrada</h2>
            <table>
                <thead><tr><th>Cabecera</th><th>Valor recibido</th><th>Descripcion</th></tr></thead>
                <tbody>
                <c:forEach var="entrada" items="${entradas}">
                    <tr><td>${entrada.nombre}</td><td><c:out value="${entrada.valor}"/></td><td>${entrada.descripcion}</td></tr>
                </c:forEach>
                </tbody>
            </table>
        </article>
    </section>
</main>
</body>
</html>

