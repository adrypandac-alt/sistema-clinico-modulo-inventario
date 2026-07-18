package com.nexodist.model;

import java.io.Serializable;

public class Movimiento implements Serializable {
    private int id;
    private int productoId;
    private String productoNombre;
    private String tipo;
    private int cantidad;
    private int stockAntes;
    private int stockDespues;
    private String motivo;
    private String usuario;
    private String fecha;

    public Movimiento() {
    }

    public Movimiento(int id, int productoId, String productoNombre, String tipo, int cantidad,
                      int stockAntes, int stockDespues, String motivo, String usuario, String fecha) {
        this.id = id;
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAntes = stockAntes;
        this.stockDespues = stockDespues;
        this.motivo = motivo;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getStockAntes() {
        return stockAntes;
    }

    public void setStockAntes(int stockAntes) {
        this.stockAntes = stockAntes;
    }

    public int getStockDespues() {
        return stockDespues;
    }

    public void setStockDespues(int stockDespues) {
        this.stockDespues = stockDespues;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
