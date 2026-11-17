package com.geronex.backend.operacion;

import com.geronex.backend.agente.Agente;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tabla cotizacion: propuesta del agente para una solicitud. */
@Entity
@Table(name = "cotizacion")
public class Cotizacion {

    public static final String ENVIADA = "ENVIADA";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion")
    private Integer idCotizacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private SolicitudViaje solicitud;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agente", nullable = false)
    private Agente agente;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "notas", length = 500)
    private String notas;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    /** El cascade guarda los detalles junto con la cotizacion. */
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idDetalle")
    private List<DetalleCotizacion> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleCotizacion detalle) {
        detalle.setCotizacion(this);
        detalles.add(detalle);
    }

    public Integer getIdCotizacion() { return idCotizacion; }

    public SolicitudViaje getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudViaje solicitud) { this.solicitud = solicitud; }

    public Agente getAgente() { return agente; }
    public void setAgente(Agente agente) { this.agente = agente; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }

    public List<DetalleCotizacion> getDetalles() { return detalles; }
}
