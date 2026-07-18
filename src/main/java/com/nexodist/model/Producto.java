package com.nexodist.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Producto implements Serializable {
    private int id;
    private String sku;
    private String nombre;
    private String categoria;
    private String proveedor;
    private double precio;
    private int stock;
    private int stockMinimo;
    private String ubicacion;
    private String deposito;
    private String lote;
    private String fechaCaducidad;
    private String registroSanitario;

    public Producto() {
    }

    public Producto(int id, String sku, String nombre, String categoria, String proveedor,
                    double precio, int stock, int stockMinimo, String ubicacion) {
        this(id, sku, nombre, categoria, proveedor, precio, stock, stockMinimo,
                ubicacion, "SIN-LOTE", "", "No registrado");
    }

    public Producto(int id, String sku, String nombre, String categoria, String proveedor,
                    double precio, int stock, int stockMinimo, String ubicacion,
                    String lote, String fechaCaducidad, String registroSanitario) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.categoria = categoria;
        this.proveedor = proveedor;
        this.precio = precio;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.ubicacion = ubicacion;
        this.deposito = ubicacion;
        this.lote = lote;
        this.fechaCaducidad = fechaCaducidad;
        this.registroSanitario = registroSanitario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getUbicacion() {
        return ubicacion != null ? ubicacion : deposito;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
        this.deposito = ubicacion;
    }

    public String getDeposito() {
        return deposito != null ? deposito : ubicacion;
    }

    public void setDeposito(String deposito) {
        this.deposito = deposito;
        this.ubicacion = deposito;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public String getFechaCaducidad() {
        return fechaCaducidad;
    }

    public void setFechaCaducidad(String fechaCaducidad) {
        this.fechaCaducidad = fechaCaducidad;
    }

    public String getRegistroSanitario() {
        return registroSanitario;
    }

    public void setRegistroSanitario(String registroSanitario) {
        this.registroSanitario = registroSanitario;
    }

    public long getDiasParaCaducar() {
        if (fechaCaducidad == null || fechaCaducidad.isBlank()) {
            return Long.MAX_VALUE;
        }
        try {
            return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(fechaCaducidad));
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    public boolean isCaducado() {
        return getDiasParaCaducar() < 0;
    }

    public boolean isProximoCaducar() {
        long dias = getDiasParaCaducar();
        return dias >= 0 && dias <= 90;
    }

    public boolean isAlertaCaducidad() {
        return isCaducado() || isProximoCaducar();
    }

    public boolean esStockCritico() {
        return stockMinimo > 0 && stock <= stockMinimo * 0.5;
    }

    public boolean esStockBajo() {
        return stockMinimo > 0 && stock < stockMinimo && !esStockCritico();
    }
}
