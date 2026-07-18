package com.nexodist.model;

import java.io.Serializable;

public class VentaItem implements Serializable {
    private int productoId;
    private String sku;
    private String nombre;
    private int cantidad;
    private double precioUnitario;

    public VentaItem() {
    }

    public VentaItem(int productoId, String sku, String nombre, int cantidad, double precioUnitario) {
        this.productoId = productoId;
        this.sku = sku;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getSubtotal() {
        return precioUnitario * cantidad;
    }
}
