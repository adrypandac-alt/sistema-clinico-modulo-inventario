package com.nexodist.model;

public class Proveedor {
    private String ruc;
    private String nombre;
    private String contacto;
    private String telefono;
    private String correo;
    private String direccion;
    private boolean activo;
    private String observaciones;
    private String categoria;
    private String documento;

    public Proveedor(String ruc, String nombre, String contacto, String telefono,
                     String correo, String categoria, String documento) {
        this.ruc = ruc;
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.correo = correo;
        this.categoria = categoria;
        this.documento = documento;
        this.direccion = "";
        this.activo = true;
        this.observaciones = "";
    }

    public Proveedor(String ruc, String nombre, String contacto, String telefono,
                     String correo, String direccion, boolean activo, String observaciones) {
        this.ruc = ruc;
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.activo = activo;
        this.observaciones = observaciones;
        this.categoria = "Insumos médicos";
        this.documento = "RUC";
    }

    public String getRuc() {
        return ruc;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDocumento() {
        return documento;
    }
}
