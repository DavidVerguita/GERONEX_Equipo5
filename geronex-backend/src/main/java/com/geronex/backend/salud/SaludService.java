package com.geronex.backend.salud;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servicio TEMPORAL para comprobar la conexion con MySQL.
 * Se elimina (o se reemplaza) cuando exista la API real.
 */
@Service
public class SaludService {

    private final EntityManager entityManager;

    public SaludService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<String, Object> estadoBaseDeDatos() {
        Object baseDatos = entityManager.createNativeQuery("SELECT DATABASE()").getSingleResult();
        Object version = entityManager.createNativeQuery("SELECT VERSION()").getSingleResult();
        Number roles = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM rol").getSingleResult();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("estado", "OK");
        resultado.put("baseDeDatos", baseDatos);
        resultado.put("versionMysql", version);
        resultado.put("rolesRegistrados", roles.longValue());
        return resultado;
    }
}
