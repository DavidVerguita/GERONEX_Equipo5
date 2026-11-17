package com.geronex.backend.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla categoria_servicio: HOTEL, VUELO, TRANSPORTE, PAQUETE. */
@Entity
@Table(name = "categoria_servicio")
public class CategoriaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    public Integer getIdCategoria() { return idCategoria; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
}
