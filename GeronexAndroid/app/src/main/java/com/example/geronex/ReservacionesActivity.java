package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Mis reservaciones (GET /reservaciones/mias): folio, destino, fechas, total y estatus,
 * con busqueda por folio o destino y filtro por estatus. Tocar una abre su comprobante.
 */
public class ReservacionesActivity extends BaseActivity {

    private static final String[][] FILTROS = {
            {"", "Todas"}, {"POR_VALIDAR", "Por validar"}, {"CONFIRMADA", "Confirmadas"},
            {"RECHAZADA", "Rechazadas"}, {"CANCELADA", "Canceladas"}};

    private final List<JSONObject> reservaciones = new ArrayList<>();
    private String filtro = "";
    private EditText etBuscar;
    private LinearLayout contFiltros, contReservaciones;
    private TextView tvMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirCliente()) return;
        setContentView(R.layout.activity_reservaciones);
        configurarEncabezado("Mis reservaciones");
        etBuscar = findViewById(R.id.etBuscar);
        contFiltros = findViewById(R.id.contFiltros);
        contReservaciones = findViewById(R.id.contReservaciones);
        tvMensaje = findViewById(R.id.tvMensaje);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                pintar();
            }
        });
    }

    @Override
    protected int segundosRefresco() {
        return 20;
    }

    @Override
    protected void cargar() {
        if (contReservaciones == null) return;
        cargando(true);
        ApiClient.llamarLista(this, "GET", "/reservaciones/mias", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                cargando(false);
                reservaciones.clear();
                for (int i = 0; i < lista.length(); i++) reservaciones.add(lista.optJSONObject(i));
                tvMensaje.setVisibility(View.GONE);
                pintar();
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    private void pintar() {
        // Filtros con contador
        contFiltros.removeAllViews();
        for (String[] f : FILTROS) {
            int n = 0;
            for (JSONObject r : reservaciones) if (f[0].isEmpty() || f[0].equals(r.optString("estatus"))) n++;
            TextView c = chip(f[1] + "  " + n, f[0].equals(filtro));
            c.setOnClickListener(v -> {
                filtro = f[0];
                pintar();
            });
            contFiltros.addView(c);
        }

        contReservaciones.removeAllViews();
        if (reservaciones.isEmpty()) {
            vacio("Todavía no tienes reservaciones. Acepta una cotización en \"Mis viajes\" para crear una.");
            return;
        }
        String texto = Formato.normalizar(etBuscar.getText().toString());
        LayoutInflater inflador = LayoutInflater.from(this);
        int mostradas = 0;
        for (JSONObject r : reservaciones) {
            String estatus = r.optString("estatus");
            if (!filtro.isEmpty() && !filtro.equals(estatus)) continue;
            if (!texto.isEmpty() && !Formato.normalizar(r.optString("folio") + " " + r.optString("destino")).contains(texto)) continue;
            contReservaciones.addView(renglon(inflador, r));
            mostradas++;
        }
        if (mostradas == 0) vacio("Ninguna reservación coincide con la búsqueda.");
    }

    private View renglon(LayoutInflater inflador, JSONObject r) {
        View v = inflador.inflate(R.layout.item_reservacion, contReservaciones, false);
        String estatus = r.optString("estatus");
        String inicio = r.optString("fechaInicio");
        imagenDestino(v.findViewById(R.id.ivDestino), r.optString("destino"));
        ((TextView) v.findViewById(R.id.tvFolio)).setText(r.optString("folio"));
        ((TextView) v.findViewById(R.id.tvDestino)).setText(r.optString("destino") + " · " + r.optInt("numPersonas") + " pers.");
        ((TextView) v.findViewById(R.id.tvFechas)).setText(Formato.fecha(inicio) + " – " + Formato.fecha(r.optString("fechaFin")));
        badge(v.findViewById(R.id.tvEstatus), estatus);
        ((TextView) v.findViewById(R.id.tvTotal)).setText(Formato.dinero(r.opt("total")));

        double cargo = r.optDouble("cargoCancelacion", 0);
        if (!Double.isNaN(cargo) && cargo > 0) {
            TextView tvCargo = v.findViewById(R.id.tvCargo);
            tvCargo.setText("Cargo " + Formato.dinero(cargo));
            tvCargo.setVisibility(View.VISIBLE);
        }
        int faltan = Formato.diasPara(inicio);
        boolean activa = "POR_VALIDAR".equals(estatus) || "CONFIRMADA".equals(estatus);
        if (activa && faltan >= 0) {
            TextView tvFaltan = v.findViewById(R.id.tvFaltan);
            tvFaltan.setText(faltan == 0 ? "¡Hoy sales!" : "Faltan " + faltan + " día(s)");
            tvFaltan.setTextColor(getColor(faltan <= 7 ? R.color.naranja : R.color.gris));
            tvFaltan.setVisibility(View.VISIBLE);
        }
        v.setOnClickListener(x -> {
            Intent i = new Intent(this, ComprobanteActivity.class);
            i.putExtra(ComprobanteActivity.EXTRA_ID, r.optInt("idReservacion"));
            startActivity(i);
        });
        return v;
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
        contReservaciones.addView(tv);
    }
}
