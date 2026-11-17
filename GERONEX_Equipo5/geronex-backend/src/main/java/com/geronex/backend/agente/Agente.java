package com.geronex.backend.agente;

import com.geronex.backend.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Tabla agente: datos del agente de viajes, ligado 1 a 1 con su cuenta (rol AGENTE). */
@Entity
@Table(name = "agente")
public class Agente {

    public static final String ESTATUS_ACTIVO = "ACTIVO";
    public static final String ESTATUS_INACTIVO = "INACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agente")
    private Integer idAgente;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 254)
    private String correo;

    @Column(name = "telefono", length = 10)
    private String telefono;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    public Integer getIdAgente() { return idAgente; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
}
