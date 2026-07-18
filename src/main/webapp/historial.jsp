<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Historial Cookie</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/accessibility.css">
    <script src="${pageContext.request.contextPath}/js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="layout" id="contenido-principal" tabindex="-1">
    <aside class="sidebar">
        <div class="app-brand"><span class="logo">SC</span><span>Sistema Clínico</span></div>
        <nav class="nav">
            <a href="${pageContext.request.contextPath}/panel">Panel</a>
            <a href="${pageContext.request.contextPath}/productos">Productos</a>
            <a href="${pageContext.request.contextPath}/proveedor">Proveedores</a>
            <a href="${pageContext.request.contextPath}/carrito">Carrito</a>
            <a class="activo" href="${pageContext.request.contextPath}/historial">Cookies</a>
            <a href="${pageContext.request.contextPath}/estado">Estatus y cabeceras</a>
            <a href="${pageContext.request.contextPath}/logout">Cerrar sesion</a>
        </nav>
    </aside>
    <section class="contenido">
        <div class="topbar">
            <div><h1>Cookie de historial</h1><p class="texto">Cookie HttpOnly: <strong>historialClinico</strong></p></div>
            <span class="pill">Cookie HTTP</span>
        </div>
        <article class="tarjeta">
            <h2>Movimientos guardados en el navegador</h2>
            <c:choose>
                <c:when test="${empty transacciones}"><p class="texto">Aun no existen movimientos guardados en la cookie.</p></c:when>
                <c:otherwise>
                    <ul class="eventos">
                        <c:forEach var="transaccion" items="${transacciones}"><li><c:out value="${transaccion}"/></li></c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
            <nav class="acciones">
                <a class="btn" href="${pageContext.request.contextPath}/carrito">Registrar movimiento</a>
                <a class="btn secundario" href="${pageContext.request.contextPath}/panel">Panel</a>
            </nav>
        </article>
    </section>
</main>
</body>
</html>

