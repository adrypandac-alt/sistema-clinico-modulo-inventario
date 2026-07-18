package com.nexodist.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Venta implements Serializable {
    private int id;
    private String vendedorNombre;
    private String vendedorCorreo;
    private String clienteNombre;
    private String clienteRuc;
    private String clienteTelefono;
    private String clienteCorreo;
    private String clienteDireccion;
    private List<VentaItem> items;
    private double total;
    private String fecha;
    private boolean facturada;

    public Venta() {
        this.items = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVendedorNombre() {
        return vendedorNombre;
    }

    public void setVendedorNombre(String vendedorNombre) {
        this.vendedorNombre = vendedorNombre;
    }

    public String getVendedorCorreo() {
        return vendedorCorreo;
    }

    public void setVendedorCorreo(String vendedorCorreo) {
        this.vendedorCorreo = vendedorCorreo;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteRuc() {
        return clienteRuc;
    }

    public void setClienteRuc(String clienteRuc) {
        this.clienteRuc = clienteRuc;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
    }

    public String getClienteCorreo() {
        return clienteCorreo;
    }

    public void setClienteCorreo(String clienteCorreo) {
        this.clienteCorreo = clienteCorreo;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public List<VentaItem> getItems() {
        return items;
    }

    public void setItems(List<VentaItem> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public boolean isFacturada() {
        return facturada;
    }

    public void setFacturada(boolean facturada) {
        this.facturada = facturada;
    }

    public void recalcularTotal() {
        double sum = 0;
        if (items != null) {
            for (VentaItem item : items) {
                sum += item.getSubtotal();
            }
        }
        this.total = sum;
    }
}
