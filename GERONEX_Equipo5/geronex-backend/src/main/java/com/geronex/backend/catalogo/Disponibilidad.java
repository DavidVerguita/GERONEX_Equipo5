package com.geronex.backend.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Tabla disponibilidad: cupo de cada servicio (1 a 1 con servicio). */
@Entity
@Table(name = "disponibilidad")
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidad")
    private Integer idDisponibilidad;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @Column(name = "cupo_total", nullable = false)
    private Integer cupoTotal;

    @Column(name = "cupo_disponible", nullable = false)
    private Integer cupoDisponible;

    public Integer getIdDisponibilidad() { return idDisponibilidad; }
    public Servicio getServicio() { return servicio; }
    public Integer getCupoTotal() { return cupoTotal; }
    public Integer getCupoDisponible() { return cupoDisponible; }

    /** Aparta lugares. Quien llama ya comprobo que hay suficientes. */
    public void descontar(int cantidad) {
        this.cupoDisponible = cupoDisponible - cantidad;
    }

    /** Devuelve lugares sin pasar del cupo total. */
    public void liberar(int cantidad) {
        this.cupoDisponible = Math.min(cupoTotal, cupoDisponible + cantidad);
    }
}
