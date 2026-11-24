package com.example.geronex;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Nueva solicitud de viaje (POST /solicitudes). Mismas reglas que el backend:
 * la ida es a partir de manana, el regreso no antes de la ida, de 1 a 50 personas,
 * presupuesto opcional no negativo y necesidades de 500 caracteres como maximo.
 */
public class NuevaSolicitudActivity extends BaseActivity {

    public static final String EXTRA_DESTINO = "destino";

    private AutoCompleteTextView etDestino;
    private EditText etPresupuesto, etNecesidades;
    private TextView btnIda, btnRegreso, tvPersonas, tvResumen, tvMensaje, tvContador, btnEnviar;
    private ImageView ivDestino;
    private String fechaIda = null, fechaRegreso = null;
    private int personas = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirCliente()) return;
        setContentView(R.layout.activity_nueva_solicitud);
        configurarEncabezado("Pedir un viaje");

        etDestino = findViewById(R.id.etDestino);
        etPresupuesto = findViewById(R.id.etPresupuesto);
        etNecesidades = findViewById(R.id.etNecesidades);
        btnIda = findViewById(R.id.btnIda);
        btnRegreso = findViewById(R.id.btnRegreso);
        tvPersonas = findViewById(R.id.tvPersonas);
        tvResumen = findViewById(R.id.tvResumen);
        tvMensaje = findViewById(R.id.tvMensaje);
        tvContador = findViewById(R.id.tvContador);
        btnEnviar = findViewById(R.id.btnEnviar);
        ivDestino = findViewById(R.id.ivDestino);

        String destino = getIntent().getStringExtra(EXTRA_DESTINO);
        if (destino != null) etDestino.setText(destino);
        imagenDestino(ivDestino, destino);
        etDestino.addTextChangedListener(new Observador(() -> imagenDestino(ivDestino, etDestino.getText().toString())));
        etNecesidades.addTextChangedListener(new Observador(() -> tvContador.setText(etNecesidades.length() + " / 500")));

        btnIda.setOnClickListener(v -> elegirFecha(true));
        btnRegreso.setOnClickListener(v -> elegirFecha(false));
        findViewById(R.id.btnMenos).setOnClickListener(v -> cambiarPersonas(-1));
        findViewById(R.id.btnMas).setOnClickListener(v -> cambiarPersonas(1));
        btnEnviar.setOnClickListener(v -> enviar());
        cargarSugerencias();
    }

    /** Destinos del catalogo como sugerencias (autocompletar y chips). */
    private void cargarSugerencias() {
        ApiClient.llamarLista(this, "GET", "/servicios", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                Set<String> unicos = new LinkedHashSet<>();
                for (int i = 0; i < lista.length(); i++) unicos.add(lista.optJSONObject(i).optString("destino"));
                List<String> destinos = new ArrayList<>(unicos);
                etDestino.setAdapter(new ArrayAdapter<>(NuevaSolicitudActivity.this,
                        android.R.layout.simple_dropdown_item_1line, destinos));
                LinearLayout cont = findViewById(R.id.contSugerencias);
                cont.removeAllViews();
                for (String d : destinos) {
                    TextView c = chip(d, false);
                    c.setOnClickListener(v -> {
                        etDestino.setText(d);
                        etDestino.setSelection(d.length());
                        etDestino.dismissDropDown();
                    });
                    cont.addView(c);
                }
            }

            @Override
            public void onError(int estado, String mensaje) {
                // Las sugerencias son opcionales: si fallan, se puede escribir el destino a mano
            }
        });
    }

    private void cambiarPersonas(int cambio) {
        personas = Math.max(1, Math.min(50, personas + cambio));
        tvPersonas.setText(String.valueOf(personas));
        pintarResumen();
    }

    private void elegirFecha(boolean ida) {
        Calendar inicial = Calendar.getInstance();
        String actual = ida ? fechaIda : (fechaRegreso != null ? fechaRegreso : fechaIda);
        if (actual != null) inicial.setTime(Formato.leerFecha(actual));
        else inicial.add(Calendar.DAY_OF_MONTH, ida ? 1 : 2);

        DatePickerDialog dialogo = new DatePickerDialog(this, (picker, anio, mes, dia) -> {
            Calendar c = Calendar.getInstance();
            c.set(anio, mes, dia);
            String iso = Formato.iso(c);
            if (ida) {
                fechaIda = iso;
                btnIda.setText(Formato.fecha(iso));
                // Si el regreso quedo antes de la nueva ida, se mueve a la misma fecha
                if (fechaRegreso != null && fechaRegreso.compareTo(fechaIda) < 0) {
                    fechaRegreso = fechaIda;
                    btnRegreso.setText(Formato.fecha(fechaRegreso));
                }
            } else {
                fechaRegreso = iso;
                btnRegreso.setText(Formato.fecha(iso));
            }
            pintarResumen();
        }, inicial.get(Calendar.YEAR), inicial.get(Calendar.MONTH), inicial.get(Calendar.DAY_OF_MONTH));

        // La ida es a partir de manana; el regreso, a partir de la ida
        String minimo = ida || fechaIda == null ? Formato.diaISO(1) : fechaIda;
        dialogo.getDatePicker().setMinDate(Formato.leerFecha(minimo).getTime());
        dialogo.show();
    }

    private void pintarResumen() {
        if (fechaIda == null || fechaRegreso == null) {
            tvResumen.setText("Elige tus fechas para ver la duración del viaje.");
            return;
        }
        int noches = Formato.noches(fechaIda, fechaRegreso);
        int faltan = Formato.diasPara(fechaIda);
        String texto = noches + " noche(s) · " + personas + " persona(s) · sales en " + faltan + " día(s)";
        if (faltan < 3) {
            texto += "\nAviso: con menos de 3 días de anticipación ya no podrás cancelar la reservación.";
        }
        tvResumen.setText(texto);
    }

    private void enviar() {
        String destino = etDestino.getText().toString().trim();
        String presupuestoTexto = etPresupuesto.getText().toString().trim();
        String error = null;
        if (destino.isEmpty()) error = "Escribe el destino.";
        else if (fechaIda == null) error = "Elige la fecha de ida.";
        else if (fechaRegreso == null) error = "Elige la fecha de regreso.";
        else if (fechaRegreso.compareTo(fechaIda) < 0) error = "El regreso no puede ser antes de la ida.";
        else if (Formato.diasPara(fechaIda) < 1) error = "La fecha de ida debe ser a partir de mañana.";
        Double presupuesto = null;
        if (error == null && !presupuestoTexto.isEmpty()) {
            try {
                presupuesto = Double.parseDouble(presupuestoTexto);
                if (presupuesto < 0) error = "El presupuesto no puede ser negativo.";
            } catch (NumberFormatException e) {
                error = "El presupuesto no es un número válido.";
            }
        }
        if (error != null) {
            mostrarMensaje(tvMensaje, error, false);
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("destino", destino);
            cuerpo.put("fechaInicio", fechaIda);
            cuerpo.put("fechaFin", fechaRegreso);
            cuerpo.put("numPersonas", personas);
            cuerpo.put("presupuesto", presupuesto == null ? JSONObject.NULL : presupuesto);
            cuerpo.put("necesidades", etNecesidades.getText().toString().trim());
        } catch (JSONException e) {
            mostrarMensaje(tvMensaje, "No se pudo preparar la solicitud.", false);
            return;
        }

        ocupado(btnEnviar, true);
        tvMensaje.setVisibility(View.GONE);
        ApiClient.llamar(this, "POST", "/solicitudes", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                ocupado(btnEnviar, false);
                new MaterialAlertDialogBuilder(NuevaSolicitudActivity.this)
                        .setTitle("¡Solicitud enviada!")
                        .setMessage("Tu solicitud #" + (datos != null ? datos.optInt("idSolicitud") : "") + " a " + destino
                                + " ya está con nuestros agentes. Te avisaremos en \"Mis viajes\" cuando llegue tu cotización.")
                        .setCancelable(false)
                        .setPositiveButton("Ver mis viajes", (d, w) -> {
                            startActivity(new Intent(NuevaSolicitudActivity.this, MisViajesActivity.class));
                            finish();
                        })
                        .show();
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupado(btnEnviar, false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    /** TextWatcher corto: solo avisa despues de cada cambio. */
    private static final class Observador implements TextWatcher {
        private final Runnable accion;

        Observador(Runnable accion) {
            this.accion = accion;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            accion.run();
        }
    }
}
