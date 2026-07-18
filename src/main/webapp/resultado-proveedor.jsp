<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Proveedor registrado</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/accessibility.css">
    <script src="${pageContext.request.contextPath}/js/accessibility.js"></script>
</head>
<body>
<a class="sc-skip-link" href="#contenido-principal">Saltar al contenido principal</a>
<main class="contenido" id="contenido-principal" tabindex="-1">
    <article class="tarjeta">
        <p class="aviso">Proveedor registrado correctamente con HTTP 201.</p>
        <h1><c:out value="${proveedor.nombre}"/></h1>
        <table>
            <tr><th>RUC</th><td>${proveedor.ruc}</td></tr>
            <tr><th>Contacto</th><td><c:out value="${proveedor.contacto}"/></td></tr>
            <tr><th>Telefono</th><td>${proveedor.telefono}</td></tr>
            <tr><th>Correo</th><td><c:out value="${proveedor.correo}"/></td></tr>
            <tr><th>Categoria</th><td>${proveedor.categoria}</td></tr>
            <tr><th>Documento</th><td>${proveedor.documento}</td></tr>
        </table>
        <nav class="acciones">
            <a class="btn" href="${pageContext.request.contextPath}/proveedor">Registrar otro</a>
            <a class="btn secundario" href="${pageContext.request.contextPath}/panel">Volver al panel</a>
        </nav>
    </article>
</main>
</body>
</html>

