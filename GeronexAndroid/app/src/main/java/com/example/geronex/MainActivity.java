package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inicio. Para el CLIENTE: resumen de sus viajes, accesos a cada seccion, buscador y destinos populares.
 * Para cualquier otro rol: aviso de que la app es para clientes (los empleados usan la web).
 */
public class MainActivity extends BaseActivity {

    private String usuario;
    private boolean esCliente;
    private TextView tvKpiCurso, tvKpiResponder, tvKpiConfirmadas, tvAvisoCotizacion, tvMsgDestinos;
    private LinearLayout contDestinos;
    private boolean destinosCargados = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JSONObject cuenta = SesionStore.obtenerUsuario(this);
        if (cuenta == null || !SesionStore.haySesion(this)) { // sin sesion: volver al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        usuario = cuenta.optString("nombreUsuario", "");
        String nombre = Formato.texto(cuenta, "nombre");
        String rol = cuenta.optString("rol", "");
        esCliente = "CLIENTE".equals(rol);

        ((TextView) findViewById(R.id.tvBienvenida)).setText("Hola, " + (nombre.isEmpty() ? usuario : nombre));
        tvKpiCurso = findViewById(R.id.tvKpiCurso);
        tvKpiResponder = findViewById(R.id.tvKpiResponder);
        tvKpiConfirmadas = findViewById(R.id.tvKpiConfirmadas);
        tvAvisoCotizacion = findViewById(R.id.tvAvisoCotizacion);
        tvMsgDestinos = findViewById(R.id.tvMsgDestinos);
        contDestinos = findViewById(R.id.contDestinos);

        if (!esCliente) {
            findViewById(R.id.panelCliente).setVisibility(View.GONE);
            findViewById(R.id.cardEmpleado).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvSubtitulo)).setText("Cuenta de " + Formato.nombreRol(rol));
            ((TextView) findViewById(R.id.tvEmpleado)).setText("Tu cuenta es de " + Formato.nombreRol(rol)
                    + ". Las bandejas de trabajo (solicitudes, reservaciones y administración) están en la página web de GERONEX."
                    + " Aquí puedes consultar el catálogo y tu cuenta.");
        }

        findViewById(R.id.btnNuevaSolicitud).setOnClickListener(v -> abrir(NuevaSolicitudActivity.class));
        findViewById(R.id.btnCotizaciones).setOnClickListener(v -> abrir(MisViajesActivity.class));
        findViewById(R.id.btnReservaciones).setOnClickListener(v -> abrir(ReservacionesActivity.class));
        findViewById(R.id.btnServicios).setOnClickListener(v -> abrirCatalogo(null, null, null));
        findViewById(R.id.kpiCurso).setOnClickListener(v -> abrir(MisViajesActivity.class));
        findViewById(R.id.kpiResponder).setOnClickListener(v -> abrirMisViajes(MisViajesActivity.FILTRO_RESPONDER));
        findViewById(R.id.kpiConfirmadas).setOnClickListener(v -> abrir(ReservacionesActivity.class));
        tvAvisoCotizacion.setOnClickListener(v -> abrirMisViajes(MisViajesActivity.FILTRO_RESPONDER));
        findViewById(R.id.btnCuenta).setOnClickListener(this::mostrarMenuCuenta);
        findViewById(R.id.btnFiltros).setOnClickListener(this::mostrarMenuFiltros);

        EditText etBuscar = findViewById(R.id.etBuscar);
        etBuscar.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH || accion == EditorInfo.IME_ACTION_DONE) {
                abrirCatalogo(etBuscar.getText().toString().trim(), null, null);
                return true;
            }
            return false;
        });
    }

    @Override
    protected int segundosRefresco() {
        return 20;
    }

    @Override
    protected void cargar() {
        if (!destinosCargados) {
            cargarDestinos();
        }
        if (esCliente) {
            cargarResumen();
        }
    }

    // ------------------------------------------------------------------ resumen del cliente

    private void cargarResumen() {
        ApiClient.llamarLista(this, "GET", "/solicitudes/mias", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                int enCurso = 0, porResponder = 0;
                for (int i = 0; i < lista.length(); i++) {
                    String estatus = lista.optJSONObject(i).optString("estatus");
                    if (!"ACEPTADA".equals(estatus)) enCurso++;
                    if ("COTIZADA".equals(estatus)) porResponder++;
                }
                tvKpiCurso.setText(String.valueOf(enCurso));
                tvKpiResponder.setText(String.valueOf(porResponder));
                if (porResponder > 0) {
                    tvAvisoCotizacion.setText("🔔  Tienes " + porResponder + (porResponder == 1
                            ? " cotización por responder. Tócala para verla." : " cotizaciones por responder. Tócalas para verlas."));
                    tvAvisoCotizacion.setVisibility(View.VISIBLE);
                } else {
                    tvAvisoCotizacion.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(int estado, String mensaje) {
                if (estado == 401) manejarError(estado, mensaje, null);
            }
        });
        ApiClient.llamarLista(this, "GET", "/reservaciones/mias", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                int confirmadas = 0;
                for (int i = 0; i < lista.length(); i++) {
                    if ("CONFIRMADA".equals(lista.optJSONObject(i).optString("estatus"))) confirmadas++;
                }
                tvKpiConfirmadas.setText(String.valueOf(confirmadas));
            }

            @Override
            public void onError(int estado, String mensaje) {
                if (estado == 401) manejarError(estado, mensaje, null);
            }
        });
    }

    // ------------------------------------------------------------------ destinos populares

    private void cargarDestinos() {
        cargando(true);
        ApiClient.llamarLista(this, "GET", "/servicios", null, new ApiClient.RespuestaLista() {
            @Override
            public void onExito(JSONArray lista) {
                cargando(false);
                destinosCargados = true;
                pintarDestinos(lista);
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                tvMsgDestinos.setText(mensaje);
                tvMsgDestinos.setVisibility(View.VISIBLE);
            }
        });
    }

    private void pintarDestinos(JSONArray servicios) {
        // destino -> {numero de servicios, precio mas bajo}
        Map<String, double[]> porDestino = new LinkedHashMap<>();
        for (int i = 0; i < servicios.length(); i++) {
            JSONObject s = servicios.optJSONObject(i);
            String destino = s.optString("destino");
            double precio = s.optDouble("precio", 0);
            double[] d = porDestino.get(destino);
            if (d == null) {
                porDestino.put(destino, new double[]{1, precio});
            } else {
                d[0]++;
                d[1] = Math.min(d[1], precio);
            }
        }
        List<Map.Entry<String, double[]>> orden = new ArrayList<>(porDestino.entrySet());
        orden.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));

        contDestinos.removeAllViews();
        if (orden.isEmpty()) {
            tvMsgDestinos.setText("El catálogo todavía no tiene servicios.");
            return;
        }
        tvMsgDestinos.setVisibility(View.GONE);
        LayoutInflater inflador = LayoutInflater.from(this);
        for (Map.Entry<String, double[]> e : orden) {
            View tarjeta = inflador.inflate(R.layout.item_destino, contDestinos, false);
            imagenDestino(tarjeta.findViewById(R.id.ivDestino), e.getKey());
            ((TextView) tarjeta.findViewById(R.id.tvDestino)).setText(e.getKey());
            ((TextView) tarjeta.findViewById(R.id.tvDesde)).setText((int) e.getValue()[0] + " servicio(s) · desde "
                    + Formato.dinero(e.getValue()[1]));
            tarjeta.setOnClickListener(v -> abrirCatalogo(null, null, e.getKey()));
            contDestinos.addView(tarjeta);
        }
    }

    // ------------------------------------------------------------------ navegacion

    private void abrir(Class<?> pantalla) {
        if (!esCliente && pantalla != CatalogoActivity.class) {
            confirmar("Solo para clientes", "Esta sección es para clientes. Los empleados trabajan desde la página web de GERONEX.", "Entendido", () -> { });
            return;
        }
        startActivity(new Intent(this, pantalla));
    }

    private void abrirMisViajes(String filtro) {
        Intent i = new Intent(this, MisViajesActivity.class);
        i.putExtra(MisViajesActivity.EXTRA_FILTRO, filtro);
        startActivity(i);
    }

    private void abrirCatalogo(String texto, String categoria, String destino) {
        Intent i = new Intent(this, CatalogoActivity.class);
        if (texto != null && !texto.isEmpty()) i.putExtra(CatalogoActivity.EXTRA_TEXTO, texto);
        if (categoria != null) i.putExtra(CatalogoActivity.EXTRA_CATEGORIA, categoria);
        if (destino != null) i.putExtra(CatalogoActivity.EXTRA_DESTINO, destino);
        startActivity(i);
    }

    private void mostrarMenuCuenta(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Usuario: " + usuario).setEnabled(false);
        menu.getMenu().add(0, 3, 1, "Mi cuenta");
        if (esCliente) {
            menu.getMenu().add(0, 4, 2, "Mis viajes");
            menu.getMenu().add(0, 5, 3, "Mis reservaciones");
        }
        menu.getMenu().add(0, 2, 4, "Cerrar sesión");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 3:
                    startActivity(new Intent(MainActivity.this, CuentaActivity.class));
                    return true;
                case 4:
                    abrir(MisViajesActivity.class);
                    return true;
                case 5:
                    abrir(ReservacionesActivity.class);
                    return true;
                case 2:
                    confirmar("Cerrar sesión", "¿Quieres salir de tu cuenta?", "Salir", () -> {
                        SesionStore.cerrar(MainActivity.this);
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    /** Filtros del buscador: abren el catalogo con ese tipo de servicio. */
    private void mostrarMenuFiltros(View anchor) {
        String[] categorias = {"HOTEL", "VUELO", "TRANSPORTE", "PAQUETE"};
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, -1, 0, "🌎 Todo el catálogo");
        for (int i = 0; i < categorias.length; i++) {
            menu.getMenu().add(0, i, i + 1, Formato.iconoCategoria(categorias[i]) + " " + Formato.nombreCategoria(categorias[i]));
        }
        menu.setOnMenuItemClickListener(item -> {
            String texto = ((EditText) findViewById(R.id.etBuscar)).getText().toString().trim();
            abrirCatalogo(texto, item.getItemId() >= 0 ? categorias[item.getItemId()] : null, null);
            return true;
        });
        menu.show();
    }
}
