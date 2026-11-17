package com.geronex.backend.catalogo;

import com.geronex.backend.dto.CatalogoDto.CategoriaResponse;
import com.geronex.backend.dto.CatalogoDto.ServicioResponse;
import com.geronex.backend.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catalogo publico: se puede consultar sin iniciar sesion. */
@Service
public class CatalogoService {

    private final ServicioRepository servicioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final CategoriaServicioRepository categoriaRepository;

    public CatalogoService(ServicioRepository servicioRepository,
                           DisponibilidadRepository disponibilidadRepository,
                           CategoriaServicioRepository categoriaRepository) {
        this.servicioRepository = servicioRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> new CategoriaResponse(c.getIdCategoria(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    /**
     * Lista los servicios activos. Filtros opcionales:
     * categoria (HOTEL, VUELO...) y texto (busca en destino y nombre, sin importar acentos).
     */
    @Transactional(readOnly = true)
    public List<ServicioResponse> servicios(String categoria, String texto) {
        Map<Integer, Integer> cupos = new HashMap<>();
        for (Disponibilidad d : disponibilidadRepository.findAll()) {
            cupos.put(d.getServicio().getIdServicio(), d.getCupoDisponible());
        }
        String cat = vacio(categoria) ? null : categoria.trim().toUpperCase(Locale.ROOT);
        String buscar = vacio(texto) ? null : normalizar(texto);

        return servicioRepository.findActivosConDetalle().stream()
                .filter(s -> cat == null || s.getCategoria().getNombre().equals(cat))
                .filter(s -> buscar == null
                        || normalizar(s.getDestino()).contains(buscar)
                        || normalizar(s.getNombre()).contains(buscar))
                .map(s -> respuesta(s, cupos.getOrDefault(s.getIdServicio(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse servicio(Integer idServicio) {
        Servicio s = servicioRepository.findById(idServicio)
                .filter(x -> Servicio.ESTATUS_ACTIVO.equals(x.getEstatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El servicio no existe"));
        int cupo = disponibilidadRepository.findByServicioIdServicio(idServicio)
                .map(Disponibilidad::getCupoDisponible).orElse(0);
        return respuesta(s, cupo);
    }

    private ServicioResponse respuesta(Servicio s, int cupo) {
        return new ServicioResponse(s.getIdServicio(), s.getNombre(), s.getDescripcion(), s.getDestino(),
                s.getCategoria().getNombre(), s.getProveedor().getNombre(), s.getPrecio(), s.getUnidad(), cupo);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    /** "Cancún" -> "cancun": minusculas y sin acentos, para buscar sin importar como se escriba. */
    private static String normalizar(String s) {
        String sinAcentos = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }
}
