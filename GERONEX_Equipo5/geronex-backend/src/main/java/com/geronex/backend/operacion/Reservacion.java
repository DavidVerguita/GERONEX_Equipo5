package com.geronex.backend.operacion;

import com.geronex.backend.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tabla reservacion: nace cuando el cliente acepta una cotizacion. */
@Entity
@Table(name = "reservacion")
public class Reservacion {

    public static final String POR_VALIDAR = "POR_VALIDAR";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservacion")
    private Integer idReservacion;

    @Column(name = "folio", nullable = false, length = 20)
    private String folio;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario cliente;

    /** Usuario (responsable de reservaciones o supervisor) que la valido o rechazo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private Usuario responsable;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "cargo_cancelacion", precision = 12, scale = 2)
    private BigDecimal cargoCancelacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    public Integer getIdReservacion() { return idReservacion; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public Cotizacion getCotizacion() { return cotizacion; }
    public void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public BigDecimal getCargoCancelacion() { return cargoCancelacion; }
    public void setCargoCancelacion(BigDecimal cargoCancelacion) { this.cargoCancelacion = cargoCancelacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime fechaValidacion) { this.fechaValidacion = fechaValidacion; }

    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDateTime fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
}
