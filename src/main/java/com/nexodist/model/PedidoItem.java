package com.nexodist.model;

import java.io.Serializable;

public class PedidoItem implements Serializable {
    private int productoId;
    private String sku;
    private String nombre;
    private String proveedor;
    private int cantidad;
    private String observacion;

    public PedidoItem() {
    }

    public PedidoItem(Producto producto, int cantidad, String observacion) {
        this.productoId = producto.getId();
        this.sku = producto.getSku();
        this.nombre = producto.getNombre();
        this.proveedor = producto.getProveedor();
        this.cantidad = cantidad;
        this.observacion = observacion;
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

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
