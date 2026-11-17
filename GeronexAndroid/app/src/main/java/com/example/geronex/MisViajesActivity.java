package com.example.geronex;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mis viajes: las solicitudes del cliente (GET /solicitudes/mias) con su progreso y sus cotizaciones.
 * Aqui el cliente acepta (nace la reservacion POR_VALIDAR con folio) o rechaza (la solicitud vuelve a NUEVA).
 * Se actualiza sola cada 15 segundos mientras esta abierta.
 */
public class MisViajesActivity extends BaseActivity {

    public static final String EXTRA_FILTRO = "filtro";
    public static final String FILTRO_TODAS = "todas";
    public static final String FILTRO_CURSO = "curso";
    public static final String FILTRO_RESPONDER = "responder";
    public static final String FILTRO_ACEPTADAS = "aceptadas";

    private static final String[][] FILTROS = {
            {FILTRO_TODAS, "Todas"}, {FILTRO_CURSO, "Esperando cotización"},
            {FILTRO_RESPONDER, "Por responder"}, {FILTRO_ACEPTADAS, "Aceptadas"}};

    private final List<JSONObject> solicitudes = new ArrayList<>();
    private final Set<Integer> desplegadas = new HashSet<>();   // solicitudes con "anteriores" abiertas
    private Set<Integer> cotizacionesVistas = null;             // para avisar cuando llega una nueva
    private String filtro = FILTRO_TODAS;
    private boolean ocupada = false;                             // no se repinta durante una accion

    private LinearLayout contFiltros, contSolicitudes;
    private TextView tvMensaje, tvActualizado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirCliente()) return;
        setContentView(R.layout.activity_mis_viajes);
        configurarEncabezado("Mis viajes");
        contFiltros = findViewById(R.id.contFiltros);
        contSolicitudes = findViewById(R.id.contSolicitudes);
        tvMensaje = findViewById(R.id.tvMensaje);
        tvActualizado = findViewById(R.id.tvActualizado);
        String pedido = getIntent().getStringExtra(EXTRA_FILTRO);
        if (pedido != null) filtro = pedido;
        findViewById(R.id.btnNueva).setOnClickListener(v -> startActivity(new Intent(this, NuevaSolicitudActivity.class)));
    }

    @Override
    protected int segundosRefresco() {
        return 15;
    }

    @Override
    protected void cargar() {
        if (ocupada || contSolicitudes == null) return;
        cargando(true);
        ApiClient.llamarLista(this, "GET", "/solicitudes/mias", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                cargando(false);
                solicitudes.clear();
                Set<Integer> pendientes = new HashSet<>();
                for (int i = 0; i < lista.length(); i++) {
                    JSONObject s = lista.optJSONObject(i);
                    solicitudes.add(s);
                    JSONArray cots = s.optJSONArray("cotizaciones");
                    for (int j = 0; cots != null && j < cots.length(); j++) {
                        if ("ENVIADA".equals(cots.optJSONObject(j).optString("estatus"))) {
                            pendientes.add(cots.optJSONObject(j).optInt("idCotizacion"));
                        }
                    }
                }
                if (cotizacionesVistas != null) {
                    for (Integer id : pendientes) {
                        if (!cotizacionesVistas.contains(id)) {
                            Toast.makeText(MisViajesActivity.this, "🔔 ¡Tu agente te envió una nueva cotización!", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }
                cotizacionesVistas = pendientes;
                tvMensaje.setVisibility(View.GONE);
                tvActualizado.setText("Actualizado a las " + new SimpleDateFormat("HH:mm:ss", Formato.MEXICO).format(new Date()));
                pintar();
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    // ------------------------------------------------------------------ filtros

    private boolean pasaFiltro(JSONObject s, String f) {
        String estatus = s.optString("estatus");
        switch (f) {
            case FILTRO_CURSO: return "NUEVA".equals(estatus);
            case FILTRO_RESPONDER: return "COTIZADA".equals(estatus);
            case FILTRO_ACEPTADAS: return "ACEPTADA".equals(estatus);
            default: return true;
        }
    }

    private void pintarFiltros() {
        contFiltros.removeAllViews();
        for (String[] f : FILTROS) {
            int n = 0;
            for (JSONObject s : solicitudes) if (pasaFiltro(s, f[0])) n++;
            TextView c = chip(f[1] + "  " + n, f[0].equals(filtro));
            c.setOnClickListener(v -> {
                filtro = f[0];
                pintar();
            });
            contFiltros.addView(c);
        }
    }

    // ------------------------------------------------------------------ dibujo

    private void pintar() {
        pintarFiltros();
        contSolicitudes.removeAllViews();
        if (solicitudes.isEmpty()) {
            vacio("Aún no has pedido ningún viaje. Toca \"Pedir otro viaje\" para crear tu primera solicitud.");
            return;
        }
        LayoutInflater inflador = LayoutInflater.from(this);
        int mostradas = 0;
        for (JSONObject s : solicitudes) {
            if (!pasaFiltro(s, filtro)) continue;
            contSolicitudes.addView(tarjeta(inflador, s));
            mostradas++;
        }
        if (mostradas == 0) vacio("No hay solicitudes en este filtro.");
    }

    private void vacio(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getColor(R.color.gris));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(28), dp(16), dp(28));
        tv.setBackground(fondo(R.color.white, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        tv.setLayoutParams(lp);
        contSolicitudes.addView(tv);
    }

    private View tarjeta(LayoutInflater inflador, JSONObject s) {
        View t = inflador.inflate(R.layout.item_solicitud, contSolicitudes, false);
        int id = s.optInt("idSolicitud");
        String destino = s.optString("destino");
        String estatus = s.optString("estatus");
        String inicio = s.optString("fechaInicio"), fin = s.optString("fechaFin");

        imagenDestino(t.findViewById(R.id.ivDestino), destino);
        ((TextView) t.findViewById(R.id.tvDestino)).setText(destino);
        badge(t.findViewById(R.id.tvEstatus), estatus);
        String datos = "Solicitud #" + id + " · " + Formato.fecha(inicio) + " al " + Formato.fecha(fin)
                + " (" + Formato.noches(inicio, fin) + " noches) · " + s.optInt("numPersonas") + " persona(s)";
        if (!s.isNull("presupuesto")) datos += " · Presupuesto " + Formato.dinero(s.opt("presupuesto"));
        ((TextView) t.findViewById(R.id.tvDatos)).setText(datos);

        String necesidades = Formato.texto(s, "necesidades");
        if (!necesidades.isEmpty()) {
            TextView tvN = t.findViewById(R.id.tvNecesidades);
            tvN.setText("📝 " + necesidades);
            tvN.setVisibility(View.VISIBLE);
        }

        JSONArray cots = s.optJSONArray("cotizaciones");
        List<JSONObject> lista = new ArrayList<>();
        for (int i = 0; cots != null && i < cots.length(); i++) lista.add(cots.optJSONObject(i));
        JSONObject pendiente = null, aceptada = null;
        for (JSONObject c : lista) {
            if ("ENVIADA".equals(c.optString("estatus"))) pendiente = c;
            if ("ACEPTADA".equals(c.optString("estatus")) && aceptada == null) aceptada = c;
        }
        JSONObject vigente = pendiente != null ? pendiente : aceptada;

        pintarPasos(t.findViewById(R.id.contPasos), estatus, lista.isEmpty() ? null : lista.get(0));

        if ("NUEVA".equals(estatus)) {
            TextView aviso = t.findViewById(R.id.tvAviso);
            aviso.setText(lista.isEmpty() ? "⏳ Un agente está preparando tu cotización."
                    : "⏳ Tu agente está preparando una nueva propuesta.");
            aviso.setTextColor(getColor(R.color.ambar));
            aviso.setBackground(fondo(R.color.ambar_claro, 8));
            aviso.setVisibility(View.VISIBLE);
        }

        LinearLayout contCot = t.findViewById(R.id.contCotizacion);
        if (vigente != null) {
            contCot.addView(vistaCotizacion(vigente, s, vigente == pendiente));
        }

        // Cotizaciones anteriores (rechazadas, o con reservacion rechazada/cancelada), plegadas
        List<JSONObject> anteriores = new ArrayList<>();
        for (JSONObject c : lista) if (c != vigente) anteriores.add(c);
        if (!anteriores.isEmpty()) {
            TextView tvAnt = t.findViewById(R.id.tvAnteriores);
            LinearLayout contAnt = t.findViewById(R.id.contAnteriores);
            for (JSONObject c : anteriores) contAnt.addView(vistaCotizacion(c, s, false));
            boolean abierta = desplegadas.contains(id);
            contAnt.setVisibility(abierta ? View.VISIBLE : View.GONE);
            tvAnt.setText((abierta ? "▲ Ocultar " : "▼ Ver ") + anteriores.size() + " cotización(es) anterior(es)");
            tvAnt.setVisibility(View.VISIBLE);
            tvAnt.setOnClickListener(v -> {
                if (!desplegadas.remove(id)) desplegadas.add(id);
                pintar();
            });
        }
        return t;
    }

    /** Cuatro pasos: Solicitud, Cotizacion, Reservacion, Confirmada (igual que la web). */
    private void pintarPasos(LinearLayout cont, String estatus, JSONObject ultima) {
        String[] nombres = {"Solicitud", "Cotización", "Reservación", "Confirmada"};
        String[] estados; // hecho, actual, fallo o vacio
        String reservacion = ultima == null ? null : Formato.texto(ultima, "estatusReservacion");
        if ("NUEVA".equals(estatus)) {
            estados = new String[]{"hecho", "actual", "", ""};
        } else if ("COTIZADA".equals(estatus)) {
            estados = new String[]{"hecho", "actual", "", ""};
            nombres[1] = "Por responder";
        } else if ("CONFIRMADA".equals(reservacion)) {
            estados = new String[]{"hecho", "hecho", "hecho", "hecho"};
        } else if ("CANCELADA".equals(reservacion)) {
            estados = new String[]{"hecho", "hecho", "hecho", "fallo"};
            nombres[3] = "Cancelada";
        } else {
            estados = new String[]{"hecho", "hecho", "hecho", "actual"};
            nombres[3] = "Validando";
        }
        cont.removeAllViews();
        for (int i = 0; i < 4; i++) {
            LinearLayout paso = new LinearLayout(this);
            paso.setOrientation(LinearLayout.VERTICAL);
            paso.setGravity(Gravity.CENTER_HORIZONTAL);
            paso.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView circulo = new TextView(this);
            circulo.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
            circulo.setGravity(Gravity.CENTER);
            circulo.setTextSize(12);
            circulo.setTypeface(Typeface.DEFAULT_BOLD);
            int colorFondo, colorTexto = R.color.white, colorNombre;
            switch (estados[i]) {
                case "hecho": colorFondo = R.color.verde; colorNombre = R.color.verde; circulo.setText("✓"); break;
                case "actual": colorFondo = R.color.naranja; colorNombre = R.color.azul_marino; circulo.setText(String.valueOf(i + 1)); break;
                case "fallo": colorFondo = R.color.rojo; colorNombre = R.color.rojo_oscuro; circulo.setText("✕"); break;
                default: colorFondo = R.color.borde; colorTexto = R.color.gris; colorNombre = R.color.gris; circulo.setText(String.valueOf(i + 1)); break;
            }
            circulo.setBackground(fondo(colorFondo, 13));
            circulo.setTextColor(getColor(colorTexto));

            TextView nombre = new TextView(this);
            nombre.setText(nombres[i]);
            nombre.setTextSize(11);
            nombre.setGravity(Gravity.CENTER);
            nombre.setTextColor(getColor(colorNombre));
            if ("actual".equals(estados[i])) nombre.setTypeface(Typeface.DEFAULT_BOLD);

            paso.addView(circulo);
            paso.addView(nombre);
            cont.addView(paso);
        }
    }

    private View vistaCotizacion(JSONObject c, JSONObject solicitud, boolean porResponder) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        if (porResponder) v.setBackgroundResource(R.drawable.bg_destacado);
        else v.setBackground(fondo(R.color.crema, 10));
        LinearLayout.LayoutParams lpV = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpV.topMargin = dp(8);
        v.setLayoutParams(lpV);

        // Titulo y estatus
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        TextView titulo = texto("Cotización #" + c.optInt("idCotizacion") + " · " + c.optString("agente"), 15, R.color.azul_marino, true);
        titulo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView est = new TextView(this);
        badge(est, c.optString("estatus"));
        fila.addView(titulo);
        fila.addView(est);
        v.addView(fila);
        v.addView(texto("Enviada " + Formato.fechaHora(c.optString("fechaCreacion")), 12, R.color.gris, false));
        String notas = Formato.texto(c, "notas");
        if (!notas.isEmpty()) {
            TextView tvNotas = texto("“" + notas + "”", 13, R.color.azul_grisaceo, false);
            tvNotas.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            tvNotas.setPadding(0, dp(6), 0, 0);
            v.addView(tvNotas);
        }

        // Servicios
        JSONArray detalles = c.optJSONArray("detalles");
        for (int i = 0; detalles != null && i < detalles.length(); i++) {
            JSONObject d = detalles.optJSONObject(i);
            LinearLayout renglon = new LinearLayout(this);
            renglon.setPadding(0, dp(8), 0, 0);
            TextView izq = texto(Formato.iconoCategoria(d.optString("categoria")) + " " + d.optString("servicio")
                    + "\n" + d.optInt("cantidad") + " × " + Formato.dinero(d.opt("precioUnitario")) + " " + d.optString("unidad")
                    + " · " + d.optString("proveedor"), 13, R.color.azul_grisaceo, false);
            izq.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView der = texto(Formato.dinero(d.opt("subtotal")), 14, R.color.azul_marino, true);
            renglon.addView(izq);
            renglon.addView(der);
            v.addView(renglon);
        }
        View linea = new View(this);
        LinearLayout.LayoutParams lpLinea = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lpLinea.topMargin = dp(10);
        linea.setLayoutParams(lpLinea);
        linea.setBackgroundColor(getColor(R.color.borde));
        v.addView(linea);
        LinearLayout filaTotal = new LinearLayout(this);
        filaTotal.setPadding(0, dp(8), 0, 0);
        TextView lt = texto("Total", 15, R.color.azul_marino, true);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaTotal.addView(lt);
        filaTotal.addView(texto(Formato.dinero(c.opt("total")), 18, R.color.azul_marino, true));
        v.addView(filaTotal);

        if (porResponder && Formato.diasPara(solicitud.optString("fechaInicio")) <= 0) {
            // El backend ya no deja aceptar un viaje cuya fecha de inicio llego
            TextView vencida = texto("La fecha de inicio de este viaje ya llegó, así que ya no se puede aceptar. Pide un viaje nuevo con otras fechas.",
                    13, R.color.rojo_oscuro, true);
            vencida.setPadding(0, dp(8), 0, 0);
            v.addView(vencida);
        } else if (porResponder) {
            double total = c.optDouble("total", 0);
            if (!solicitud.isNull("presupuesto")) {
                double presupuesto = solicitud.optDouble("presupuesto", 0);
                TextView cmp = total <= presupuesto
                        ? texto("✔ Dentro de tu presupuesto (" + Formato.dinero(presupuesto) + ")", 13, R.color.verde, true)
                        : texto("Supera tu presupuesto por " + Formato.dinero(total - presupuesto), 13, R.color.rojo_oscuro, true);
                cmp.setPadding(0, dp(6), 0, 0);
                v.addView(cmp);
            }
            LinearLayout botones = new LinearLayout(this);
            botones.setPadding(0, dp(12), 0, 0);
            TextView aceptar = boton("Aceptar y reservar", R.drawable.bg_boton_verde, R.color.white);
            TextView rechazar = boton("Rechazar", R.drawable.bg_boton_claro, R.color.rojo_oscuro);
            ((LinearLayout.LayoutParams) rechazar.getLayoutParams()).setMarginStart(dp(10));
            aceptar.setOnClickListener(x -> confirmar("¿Aceptar esta cotización?",
                    "Se creará tu reservación por " + Formato.dinero(total) + " y el equipo validará la disponibilidad con los proveedores.",
                    "Sí, reservar", () -> responder(c.optInt("idCotizacion"), true, aceptar)));
            rechazar.setOnClickListener(x -> confirmar("¿Rechazar esta cotización?",
                    "Tu agente podrá enviarte otra propuesta.", "Rechazar",
                    () -> responder(c.optInt("idCotizacion"), false, rechazar)));
            botones.addView(aceptar);
            botones.addView(rechazar);
            v.addView(botones);
        } else if (!c.isNull("idReservacion")) {
            LinearLayout filaRes = new LinearLayout(this);
            filaRes.setGravity(Gravity.CENTER_VERTICAL);
            filaRes.setPadding(0, dp(12), 0, 0);
            TextView ver = boton("Ver comprobante " + c.optString("folio"), R.drawable.bg_boton_azul, R.color.white);
            int idRes = c.optInt("idReservacion");
            ver.setOnClickListener(x -> {
                Intent i = new Intent(this, ComprobanteActivity.class);
                i.putExtra(ComprobanteActivity.EXTRA_ID, idRes);
                startActivity(i);
            });
            TextView est2 = new TextView(this);
            badge(est2, c.optString("estatusReservacion"));
            LinearLayout.LayoutParams lpE = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpE.setMarginStart(dp(10));
            est2.setLayoutParams(lpE);
            filaRes.addView(ver);
            filaRes.addView(est2);
            v.addView(filaRes);
        }
        return v;
    }

    private void responder(int idCotizacion, boolean aceptar, View boton) {
        ocupada = true;
        ocupado(boton, true);
        cargando(true);
        String ruta = "/cotizaciones/" + idCotizacion + (aceptar ? "/aceptar" : "/rechazar");
        ApiClient.llamar(this, "POST", ruta, null, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                ocupada = false;
                cargando(false);
                if (aceptar && datos != null) {
                    confirmar("¡Listo, reservación creada!",
                            "Tu folio es " + datos.optString("folio") + ". El equipo de reservaciones está validando la disponibilidad;"
                                    + " te avisaremos aquí cuando quede confirmada.",
                            "Ver comprobante", () -> {
                                Intent i = new Intent(MisViajesActivity.this, ComprobanteActivity.class);
                                i.putExtra(ComprobanteActivity.EXTRA_ID, datos.optInt("idReservacion"));
                                startActivity(i);
                            });
                } else {
                    Toast.makeText(MisViajesActivity.this, "Rechazaste la cotización. Tu agente preparará otra propuesta.", Toast.LENGTH_LONG).show();
                }
                cargar();
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupada = false;
                cargando(false);
                ocupado(boton, false);
                manejarError(estado, mensaje, tvMensaje);
                cargar();
            }
        });
    }

    // ------------------------------------------------------------------ piezas

    private TextView texto(String contenido, int tamano, int color, boolean negrita) {
        TextView tv = new TextView(this);
        tv.setText(contenido);
        tv.setTextSize(tamano);
        tv.setTextColor(getColor(color));
        if (negrita) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView boton(String texto, int fondoRes, int color) {
        TextView b = texto(texto, 14, color, true);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(fondoRes);
        b.setPadding(dp(14), dp(10), dp(18), dp(14));
        b.setClickable(true);
        b.setFocusable(true);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return b;
    }
}
