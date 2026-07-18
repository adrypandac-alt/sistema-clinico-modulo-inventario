package com.nexodist.model;

import java.io.Serializable;

public class Rol implements Serializable {
    private String nombre;
    private String panelesPermitidos;
    private boolean puedeInteractuar;

    public Rol(String nombre, String panelesPermitidos, boolean puedeInteractuar) {
        this.nombre = nombre;
        this.panelesPermitidos = panelesPermitidos == null ? "" : panelesPermitidos;
        this.puedeInteractuar = puedeInteractuar;
    }

    public String getNombre() { return nombre; }
    public String getPanelesPermitidos() { return panelesPermitidos; }
    public boolean isPuedeInteractuar() { return puedeInteractuar; }
}
