package com.geronex.backend.operacion;

import com.geronex.backend.agente.Agente;
import com.geronex.backend.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tabla solicitud_viaje: lo que pide el cliente. */
@Entity
@Table(name = "solicitud_viaje")
public class SolicitudViaje {

    public static final String NUEVA = "NUEVA";
    public static final String COTIZADA = "COTIZADA";
    public static final String ACEPTADA = "ACEPTADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    /** Cliente que pide el viaje. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario cliente;

    /** Agente que la cotizo (null mientras nadie la cotiza). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agente")
    private Agente agente;

    @Column(name = "destino", nullable = false, length = 100)
    private String destino;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "num_personas", nullable = false)
    private Integer numPersonas;

    @Column(name = "presupuesto", precision = 10, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "necesidades", length = 500)
    private String necesidades;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public Integer getIdSolicitud() { return idSolicitud; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Agente getAgente() { return agente; }
    public void setAgente(Agente agente) { this.agente = agente; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Integer getNumPersonas() { return numPersonas; }
    public void setNumPersonas(Integer numPersonas) { this.numPersonas = numPersonas; }

    public BigDecimal getPresupuesto() { return presupuesto; }
    public void setPresupuesto(BigDecimal presupuesto) { this.presupuesto = presupuesto; }

    public String getNecesidades() { return necesidades; }
    public void setNecesidades(String necesidades) { this.necesidades = necesidades; }

    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
