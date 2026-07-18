package com.nexodist.model;

import java.io.Serializable;

/**
 * Registro de una acción de cuenta de usuario:
 * login, logout, acceso denegado, cambio de rol, etc.
 */
public class ActividadCuenta implements Serializable {

    public enum Tipo { LOGIN, LOGOUT, ACCION, ERROR }

    private int id;
    private String usuarioNombre;
    private String usuarioCorreo;
    private String usuarioRol;
    private Tipo tipo;
    private String descripcion;
    private String ip;
    private String fecha;

    public ActividadCuenta() {}

    public ActividadCuenta(int id, String usuarioNombre, String usuarioCorreo,
                           String usuarioRol, Tipo tipo, String descripcion,
                           String ip, String fecha) {
        this.id = id;
        this.usuarioNombre = usuarioNombre;
        this.usuarioCorreo = usuarioCorreo;
        this.usuarioRol = usuarioRol;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.ip = ip;
        this.fecha = fecha;
    }

    // ── getters / setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String v) { this.usuarioNombre = v; }

    public String getUsuarioCorreo() { return usuarioCorreo; }
    public void setUsuarioCorreo(String v) { this.usuarioCorreo = v; }

    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String v) { this.usuarioRol = v; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public String getTipoCss() {
        if (tipo == null) return "accion";
        return switch (tipo) {
            case LOGIN  -> "login";
            case LOGOUT -> "logout";
            case ERROR  -> "error";
            default     -> "accion";
        };
    }

    public String getTipoIcono() {
        if (tipo == null) return "⚙";
        return switch (tipo) {
            case LOGIN  -> "→";
            case LOGOUT -> "←";
            case ERROR  -> "✕";
            default     -> "⚙";
        };
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }

    public String getFecha() { return fecha; }
    public void setFecha(String v) { this.fecha = v; }
}
