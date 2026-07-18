package com.nexodist.model;

public class CabeceraInfo {
    private final String nombre;
    private final String valor;
    private final String descripcion;

    public CabeceraInfo(String nombre, String valor, String descripcion) {
        this.nombre = nombre;
        this.valor = valor;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getValor() {
        return valor;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
