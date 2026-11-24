package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Catalogo publico (GET /servicios): busqueda sin acentos, filtro por tipo y destino,
 * "solo con lugares", orden por precio, nombre o cupo, y detalle de cada servicio.
 * Todo el filtrado se hace aqui con la lista completa, igual que la web.
 */
public class CatalogoActivity extends BaseActivity {

    public static final String EXTRA_TEXTO = "texto";
    public static final String EXTRA_CATEGORIA = "categoria";
    public static final String EXTRA_DESTINO = "destino";

    private static final String[] CATEGORIAS = {null, "HOTEL", "VUELO", "TRANSPORTE", "PAQUETE"};
    private static final String[] ORDENES = {"Ordenar: destino", "Precio: menor a mayor", "Precio: mayor a menor",
            "Nombre (A-Z)", "Más lugares"};

    private final List<JSONObject> catalogo = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();
    private final List<String> destinos = new ArrayList<>();
    private String categoria = null;
    private String destinoInicial = null;
    private boolean esCliente;

    private EditText etBuscar;
    private Spinner spDestino, spOrden;
    private CheckBox cbDisponibles;
    private TextView tvContador, tvMensaje;
    private LinearLayout contServicios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SesionStore.haySesion(this)) {
            irALogin();
            return;
        }
        setContentView(R.layout.activity_catalogo);
        configurarEncabezado("Catálogo");
        JSONObject u = SesionStore.obtenerUsuario(this);
        esCliente = u != null && "CLIENTE".equals(u.optString("rol"));

        etBuscar = findViewById(R.id.etBuscar);
        spDestino = findViewById(R.id.spDestino);
        spOrden = findViewById(R.id.spOrden);
        cbDisponibles = findViewById(R.id.cbDisponibles);
        tvContador = findViewById(R.id.tvContador);
        tvMensaje = findViewById(R.id.tvMensaje);
        contServicios = findViewById(R.id.contServicios);

        etBuscar.setText(getIntent().getStringExtra(EXTRA_TEXTO));
        categoria = getIntent().getStringExtra(EXTRA_CATEGORIA);
        destinoInicial = getIntent().getStringExtra(EXTRA_DESTINO);

        // Chips de tipo de servicio
        LinearLayout contChips = findViewById(R.id.contChips);
        for (String c : CATEGORIAS) {
            TextView chip = chip(Formato.nombreCategoria(c), equals(c, categoria));
            chip.setOnClickListener(v -> {
                categoria = c;
                for (int i = 0; i < chips.size(); i++) marcarChip(chips.get(i), equals(CATEGORIAS[i], categoria));
                pintar();
            });
            chips.add(chip);
            contChips.addView(chip);
        }

        ArrayAdapter<String> ordenes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ORDENES);
        spOrden.setAdapter(ordenes);
        AdapterView.OnItemSelectedListener alCambiar = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                pintar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        };
        spOrden.setOnItemSelectedListener(alCambiar);
        spDestino.setOnItemSelectedListener(alCambiar);
        cbDisponibles.setOnCheckedChangeListener((b, marcado) -> pintar());
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
        descargar();
    }

    private static boolean equals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private void descargar() {
        cargando(true);
        ApiClient.llamarLista(this, "GET", "/servicios", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                cargando(false);
                catalogo.clear();
                Set<String> unicos = new LinkedHashSet<>();
                for (int i = 0; i < lista.length(); i++) {
                    JSONObject s = lista.optJSONObject(i);
                    catalogo.add(s);
                    unicos.add(s.optString("destino"));
                }
                destinos.clear();
                destinos.add("Todos los destinos");
                destinos.addAll(unicos);
                spDestino.setAdapter(new ArrayAdapter<>(CatalogoActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, destinos));
                if (destinoInicial != null && destinos.contains(destinoInicial)) {
                    spDestino.setSelection(destinos.indexOf(destinoInicial));
                }
                pintar();
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    private List<JSONObject> filtrados() {
        String texto = Formato.normalizar(etBuscar.getText().toString());
        int posDestino = spDestino.getSelectedItemPosition();
        String destino = posDestino > 0 ? destinos.get(posDestino) : null;
        boolean soloDisponibles = cbDisponibles.isChecked();

        List<JSONObject> lista = new ArrayList<>();
        for (JSONObject s : catalogo) {
            if (categoria != null && !categoria.equals(s.optString("categoria"))) continue;
            if (destino != null && !destino.equals(s.optString("destino"))) continue;
            if (soloDisponibles && s.optInt("cupoDisponible") <= 0) continue;
            String todo = Formato.normalizar(s.optString("nombre") + " " + s.optString("destino") + " "
                    + s.optString("proveedor") + " " + Formato.texto(s, "descripcion"));
            if (!texto.isEmpty() && !todo.contains(texto)) continue;
            lista.add(s);
        }
        switch (spOrden.getSelectedItemPosition()) {
            case 1: lista.sort((a, b) -> Double.compare(a.optDouble("precio"), b.optDouble("precio"))); break;
            case 2: lista.sort((a, b) -> Double.compare(b.optDouble("precio"), a.optDouble("precio"))); break;
            case 3: lista.sort((a, b) -> a.optString("nombre").compareToIgnoreCase(b.optString("nombre"))); break;
            case 4: lista.sort((a, b) -> Integer.compare(b.optInt("cupoDisponible"), a.optInt("cupoDisponible"))); break;
            default: break; // la API ya los manda ordenados por destino
        }
        return lista;
    }

    private void pintar() {
        if (contServicios == null) return;
        List<JSONObject> lista = filtrados();
        tvContador.setText(lista.size() + " de " + catalogo.size());
        contServicios.removeAllViews();
        if (catalogo.isEmpty()) return;
        if (lista.isEmpty()) {
            tvMensaje.setText("No encontramos servicios con esos filtros. Prueba con otros o pide un viaje y un agente te ayuda.");
            tvMensaje.setTextColor(getColor(R.color.gris));
            tvMensaje.setBackground(fondo(R.color.white, 10));
            tvMensaje.setVisibility(View.VISIBLE);
            return;
        }
        tvMensaje.setVisibility(View.GONE);
        LayoutInflater inflador = LayoutInflater.from(this);
        for (JSONObject s : lista) {
            View t = inflador.inflate(R.layout.item_servicio, contServicios, false);
            String destino = s.optString("destino");
            int cupo = s.optInt("cupoDisponible");
            imagenDestino(t.findViewById(R.id.ivImagen), destino);
            TextView tvCategoria = t.findViewById(R.id.tvCategoria);
            tvCategoria.setText(s.optString("categoria"));
            tvCategoria.setBackground(fondo(R.color.azul_marino, 20));
            TextView tvCupo = t.findViewById(R.id.tvCupo);
            if (cupo <= 0) {
                tvCupo.setText("Agotado");
                tvCupo.setTextColor(getColor(R.color.white));
                tvCupo.setBackground(fondo(R.color.rojo, 20));
            } else if (cupo <= 5) {
                tvCupo.setText("¡Quedan " + cupo + "!");
                tvCupo.setTextColor(getColor(R.color.white));
                tvCupo.setBackground(fondo(R.color.naranja, 20));
            } else {
                tvCupo.setText(cupo + " disponibles");
                tvCupo.setTextColor(getColor(R.color.azul_marino));
                tvCupo.setBackground(fondo(R.color.white, 20));
            }
            ((TextView) t.findViewById(R.id.tvNombre)).setText(s.optString("nombre"));
            ((TextView) t.findViewById(R.id.tvLugar)).setText(destino + " · " + s.optString("proveedor"));
            ((TextView) t.findViewById(R.id.tvDescripcion)).setText(Formato.texto(s, "descripcion"));
            ((TextView) t.findViewById(R.id.tvPrecio)).setText(Formato.dinero(s.opt("precio")) + "  " + s.optString("unidad"));
            View btn = t.findViewById(R.id.btnSolicitar);
            btn.setVisibility(esCliente ? View.VISIBLE : View.GONE);
            btn.setOnClickListener(v -> solicitar(destino));
            t.setOnClickListener(v -> detalle(s));
            contServicios.addView(t);
        }
    }

    private void detalle(JSONObject s) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialogo_servicio, null);
        String destino = s.optString("destino");
        imagenDestino(vista.findViewById(R.id.ivImagen), destino);
        ((TextView) vista.findViewById(R.id.tvNombre)).setText(s.optString("nombre"));
        String descripcion = Formato.texto(s, "descripcion");
        ((TextView) vista.findViewById(R.id.tvDescripcion)).setText(descripcion.isEmpty() ? "Sin descripción." : descripcion);
        LinearLayout datos = vista.findViewById(R.id.contDatos);
        datos.addView(filaDato("Tipo", Formato.nombreCategoria(s.optString("categoria"))));
        datos.addView(filaDato("Destino", destino));
        datos.addView(filaDato("Proveedor", s.optString("proveedor")));
        datos.addView(filaDato("Precio", Formato.dinero(s.opt("precio")) + " " + s.optString("unidad")));
        int cupo = s.optInt("cupoDisponible");
        datos.addView(filaDato("Disponibilidad", cupo > 0 ? cupo + " lugares" : "Agotado"));

        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(this)
                .setView(vista)
                .setNegativeButton("Cerrar", null);
        if (esCliente) {
            dialogo.setPositiveButton("Solicitar viaje a " + destino, (d, w) -> solicitar(destino));
        }
        dialogo.show();
    }

    private void solicitar(String destino) {
        Intent i = new Intent(this, NuevaSolicitudActivity.class);
        i.putExtra(NuevaSolicitudActivity.EXTRA_DESTINO, destino);
        startActivity(i);
    }
}
