package com.geronex.backend.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla proveedor: empresas (ficticias) que dan los servicios. */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "telefono", length = 10)
    private String telefono;

    @Column(name = "correo", length = 254)
    private String correo;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    public Integer getIdProveedor() { return idProveedor; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getCorreo() { return correo; }
    public String getEstatus() { return estatus; }
}
