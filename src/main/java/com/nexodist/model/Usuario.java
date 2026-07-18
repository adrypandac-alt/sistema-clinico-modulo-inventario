package com.nexodist.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Usuario implements Serializable {
    private String nombre;
    private String correo;
    private String clave;
    private String rol;
    private String area;
    private boolean activo;
    private String ultimoAcceso;
    private String panelesPermitidos;
    private String telefono;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(String nombre, String correo, String clave, String rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.clave = clave;
        this.rol = rol;
        this.activo = true;
    }

    public Usuario(String nombre, String correo, String clave, String rol, String area, boolean activo, String ultimoAcceso) {
        this.nombre = nombre;
        this.correo = correo;
        this.clave = clave;
        this.rol = rol;
        this.area = area;
        this.activo = activo;
        this.ultimoAcceso = ultimoAcceso;
    }

    public Usuario(String nombre, String correo, String clave, String rol, String area, boolean activo,
                   String ultimoAcceso, String panelesPermitidos) {
        this(nombre, correo, clave, rol, area, activo, ultimoAcceso);
        this.panelesPermitidos = panelesPermitidos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(String ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public String getPanelesPermitidos() {
        return panelesPermitidos;
    }

    public void setPanelesPermitidos(String panelesPermitidos) {
        this.panelesPermitidos = panelesPermitidos;
    }

    public String getTelefono() { return telefono == null ? "" : telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getNombres() {
        if (nombre == null) return "";
        int pos = nombre.trim().indexOf(' ');
        return pos < 0 ? nombre.trim() : nombre.trim().substring(0, pos);
    }
    public String getApellidos() {
        if (nombre == null) return "";
        int pos = nombre.trim().indexOf(' ');
        return pos < 0 ? "" : nombre.trim().substring(pos + 1).trim();
    }

    public List<String> getPanelesPermitidosLista() {
        List<String> paneles = new ArrayList<>();
        if (panelesPermitidos == null || panelesPermitidos.isBlank()) {
            return paneles;
        }
        Arrays.stream(panelesPermitidos.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(paneles::add);
        return paneles;
    }

    public boolean tienePanelAsignado(String panel) {
        if (panel == null || panel.isBlank()) return false;
        for (String p : getPanelesPermitidosLista()) {
            if (p.equalsIgnoreCase(panel.trim())) return true;
        }
        return false;
    }

    public String getIniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "??";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length >= 2) {
            return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
        }
        return nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
    }

    public String getPanelPrincipal() {
        if (panelesPermitidos != null && !panelesPermitidos.isBlank()) return "Paneles personalizados";
        if ("Vendedor".equalsIgnoreCase(rol) || "Ventas".equalsIgnoreCase(rol) || "Farmacia".equalsIgnoreCase(rol)) {
            return "Farmacia - ventas y facturación";
        }
        if ("Operador".equalsIgnoreCase(rol) || "Operativo".equalsIgnoreCase(rol)) return "Inventario y movimientos";
        return "Panel administrativo";
    }

    public String getAccionesPermitidas() {
        if (panelesPermitidos != null && !panelesPermitidos.isBlank()) {
            return panelesPermitidos.replace(",", ", ");
        }
        if ("Administrador".equalsIgnoreCase(rol)) {
            return "Todos los paneles de inventario, usuarios, roles y permisos";
        }
        if ("Operador".equalsIgnoreCase(rol) || "Operativo".equalsIgnoreCase(rol)) {
            return "Dashboard, productos, entradas, salidas, movimientos y alertas";
        }
        if ("Vendedor".equalsIgnoreCase(rol) || "Ventas".equalsIgnoreCase(rol) || "Farmacia".equalsIgnoreCase(rol)) {
            return "Panel de venta, mis ventas, alertas de farmacia y facturación";
        }
        return "Sin permisos de inventario asignados";
    }
}
