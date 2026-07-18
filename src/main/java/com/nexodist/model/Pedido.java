package com.nexodist.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Pedido implements Serializable {
    private int id;
    private String usuario;
    private String rol;
    private String fecha;
    private String estado;
    private String clinica;
    private String solicitante;
    private String despachadoPor;
    private String entregadoA;
    private List<PedidoItem> items = new ArrayList<>();

    public Pedido() {
    }

    public Pedido(int id, String usuario, String rol, String fecha, String estado, List<PedidoItem> items) {
        this.id = id;
        this.usuario = usuario;
        this.rol = rol;
        this.fecha = fecha;
        this.estado = estado;
        this.items = items;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClinica() { return clinica; }
    public void setClinica(String clinica) { this.clinica = clinica; }
    public String getSolicitante() { return solicitante == null || solicitante.isBlank() ? usuario : solicitante; }
    public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
    public String getDespachadoPor() { return despachadoPor; }
    public void setDespachadoPor(String despachadoPor) { this.despachadoPor = despachadoPor; }
    public String getEntregadoA() { return entregadoA; }
    public void setEntregadoA(String entregadoA) { this.entregadoA = entregadoA; }

    public List<PedidoItem> getItems() {
        return items;
    }

    public void setItems(List<PedidoItem> items) {
        this.items = items;
    }
}
