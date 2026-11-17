package com.geronex.backend.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Tabla servicio: lo que un agente puede incluir en una cotizacion. */
@Entity
@Table(name = "servicio")
public class Servicio {

    public static final String ESTATUS_ACTIVO = "ACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaServicio categoria;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "destino", nullable = false, length = 100)
    private String destino;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Como se cobra: "por persona", "por noche", "por viaje". */
    @Column(name = "unidad", nullable = false, length = 30)
    private String unidad;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    public Integer getIdServicio() { return idServicio; }
    public CategoriaServicio getCategoria() { return categoria; }
    public Proveedor getProveedor() { return proveedor; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getDestino() { return destino; }
    public BigDecimal getPrecio() { return precio; }
    public String getUnidad() { return unidad; }
    public String getEstatus() { return estatus; }
}
