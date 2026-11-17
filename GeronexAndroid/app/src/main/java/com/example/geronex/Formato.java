package com.example.geronex;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Formatos de dinero, fechas y estatus (los mismos textos que usa la web).
 * Se usa Calendar/SimpleDateFormat porque java.time necesita Android 8 y la app soporta desde Android 7.
 */
public final class Formato {

    public static final Locale MEXICO = Locale.forLanguageTag("es-MX");

    private Formato() {
    }

    /** 19500 -> "$19,500.00". null -> "—". */
    public static String dinero(double cantidad) {
        return NumberFormat.getCurrencyInstance(MEXICO).format(cantidad);
    }

    public static String dinero(Object valor) {
        if (valor == null || valor.toString().isEmpty() || "null".equals(valor.toString())) {
            return "—";
        }
        try {
            return dinero(Double.parseDouble(valor.toString()));
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    /** Convierte "2026-12-20" en Date (medianoche local), o null si no se puede. */
    public static Date leerFecha(String iso) {
        if (iso == null || iso.length() < 10) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.substring(0, 10));
        } catch (ParseException e) {
            return null;
        }
    }

    /** "2026-12-20" -> "20 dic 2026" */
    public static String fecha(String iso) {
        Date d = leerFecha(iso);
        return d == null ? "—" : new SimpleDateFormat("d MMM yyyy", MEXICO).format(d).replace(".", "");
    }

    /** "2026-11-16T22:19:52" -> "16 nov 2026, 22:19" */
    public static String fechaHora(String iso) {
        if (iso == null || iso.length() < 16 || "null".equals(iso)) {
            return "—";
        }
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(iso.substring(0, 16));
            return new SimpleDateFormat("d MMM yyyy, HH:mm", MEXICO).format(d).replace(".", "");
        } catch (ParseException e) {
            return "—";
        }
    }

    /** Fecha de hoy + dias en formato AAAA-MM-DD (lo que espera la API). */
    public static String diaISO(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, dias);
        return iso(c);
    }

    public static String iso(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar hoy() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** Dias que faltan para una fecha AAAA-MM-DD (negativo si ya paso). */
    public static int diasPara(String iso) {
        Date d = leerFecha(iso);
        if (d == null) {
            return 0;
        }
        return (int) Math.round((d.getTime() - hoy().getTimeInMillis()) / 86400000.0);
    }

    /** Noches entre dos fechas (minimo 1, como en la web). */
    public static int noches(String inicio, String fin) {
        Date a = leerFecha(inicio);
        Date b = leerFecha(fin);
        if (a == null || b == null) {
            return 1;
        }
        return Math.max(1, (int) Math.round((b.getTime() - a.getTime()) / 86400000.0));
    }

    /** Texto amable de cada estatus (igual que TEXTO_ESTATUS en la web). */
    public static String textoEstatus(String estatus) {
        if (estatus == null) {
            return "";
        }
        switch (estatus) {
            case "NUEVA": return "Esperando cotización";
            case "COTIZADA": return "Cotización enviada";
            case "ACEPTADA": return "Aceptada";
            case "ENVIADA": return "Por responder";
            case "RECHAZADA": return "Rechazada";
            case "POR_VALIDAR": return "Por validar";
            case "CONFIRMADA": return "Confirmada";
            case "CANCELADA": return "Cancelada";
            case "ACTIVO": return "Activo";
            case "INACTIVO": return "Inactivo";
            case "BLOQUEADO": return "Bloqueado";
            default: return estatus;
        }
    }

    /** Colores de la etiqueta de estatus: {fondo, texto} como recursos de color. */
    public static int[] coloresEstatus(String estatus) {
        if (estatus == null) {
            return new int[]{R.color.azul_claro, R.color.azul_grisaceo};
        }
        switch (estatus) {
            case "NUEVA":
            case "ENVIADA":
            case "POR_VALIDAR":
                return new int[]{R.color.ambar_claro, R.color.ambar};
            case "COTIZADA":
                return new int[]{R.color.azul_claro, R.color.azul_marino};
            case "ACEPTADA":
            case "CONFIRMADA":
            case "ACTIVO":
                return new int[]{R.color.verde_claro, R.color.verde};
            case "RECHAZADA":
            case "CANCELADA":
            case "INACTIVO":
            case "BLOQUEADO":
                return new int[]{R.color.rojo_claro, R.color.rojo_oscuro};
            default:
                return new int[]{R.color.azul_claro, R.color.azul_grisaceo};
        }
    }

    public static String iconoCategoria(String categoria) {
        if (categoria == null) {
            return "🌎";
        }
        switch (categoria) {
            case "HOTEL": return "🏨";
            case "VUELO": return "✈️";
            case "TRANSPORTE": return "🚌";
            case "PAQUETE": return "🧭";
            default: return "🌎";
        }
    }

    public static String nombreCategoria(String categoria) {
        if (categoria == null) {
            return "Todos";
        }
        switch (categoria) {
            case "HOTEL": return "Hoteles";
            case "VUELO": return "Vuelos";
            case "TRANSPORTE": return "Transporte";
            case "PAQUETE": return "Paquetes y tours";
            default: return categoria;
        }
    }

    public static String nombreRol(String rol) {
        if (rol == null) {
            return "";
        }
        switch (rol) {
            case "ADMINISTRADOR": return "Administrador";
            case "SUPERVISOR": return "Supervisor";
            case "AGENTE": return "Agente";
            case "RESPONSABLE_RESERVACIONES": return "Responsable de reservaciones";
            case "RESPONSABLE_PROVEEDORES": return "Responsable de proveedores";
            case "AUDITOR": return "Auditor";
            case "CLIENTE": return "Cliente";
            default: return rol;
        }
    }

    /** "Cancún" -> "cancun": minusculas y sin acentos, para buscar sin importar como se escriba. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim();
    }

    /** Valor de texto de un JSON que puede venir como null. */
    public static String texto(org.json.JSONObject o, String campo) {
        return o == null || o.isNull(campo) ? "" : o.optString(campo, "");
    }
}
