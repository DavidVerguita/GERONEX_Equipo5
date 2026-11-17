package com.example.geronex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Comprobante de una reservacion (GET /reservaciones/{id}).
 * El cliente puede cancelar solo si la API dice puedeCancelar = true:
 * faltan al menos 3 dias para el viaje; si ya estaba CONFIRMADA hay un cargo del 5% (sin cobro real).
 */
public class ComprobanteActivity extends BaseActivity {

    public static final String EXTRA_ID = "idReservacion";

    private int idReservacion;
    private JSONObject reservacion;
    private TextView tvMensaje, btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!exigirCliente()) return;
        setContentView(R.layout.activity_comprobante);
        configurarEncabezado("Comprobante");
        idReservacion = getIntent().getIntExtra(EXTRA_ID, -1);
        tvMensaje = findViewById(R.id.tvMensaje);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v -> pedirCancelacion());
        findViewById(R.id.btnCompartir).setOnClickListener(v -> compartir());
        findViewById(R.id.btnCopiar).setOnClickListener(v -> copiarFolio());
        if (idReservacion < 0) {
            mostrarMensaje(tvMensaje, "Falta el número de reservación.", false);
        }
    }

    @Override
    protected void cargar() {
        if (tvMensaje == null || idReservacion < 0) return;
        cargando(true);
        ApiClient.llamar(this, "GET", "/reservaciones/" + idReservacion, null, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                cargando(false);
                if (datos == null) {
                    mostrarMensaje(tvMensaje, "Respuesta inesperada del servidor.", false);
                    return;
                }
                tvMensaje.setVisibility(View.GONE);
                pintar(datos);
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    private void pintar(JSONObject r) {
        reservacion = r;
        String estatus = r.optString("estatus");
        String inicio = r.optString("fechaInicio"), fin = r.optString("fechaFin");
        configurarEncabezado("Comprobante " + r.optString("folio"));
        findViewById(R.id.contComprobante).setVisibility(View.VISIBLE);
        findViewById(R.id.contAcciones).setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.tvFolio)).setText(r.optString("folio"));
        ((TextView) findViewById(R.id.tvEmitido)).setText("Emitido " + Formato.fechaHora(r.optString("fechaCreacion")));
        badge(findViewById(R.id.tvEstatus), estatus);
        imagenDestino(findViewById(R.id.ivDestino), r.optString("destino"));
        findViewById(R.id.ivDestino).setBackground(null);

        // Aviso segun el estatus
        TextView aviso = findViewById(R.id.tvAviso);
        switch (estatus) {
            case "POR_VALIDAR":
                pintarAviso(aviso, "⏳ Tu reservación está registrada. El equipo de reservaciones está validando la disponibilidad con los proveedores.",
                        R.color.ambar_claro, R.color.ambar);
                break;
            case "CONFIRMADA":
                String quien = Formato.texto(r, "responsable");
                pintarAviso(aviso, "✅ ¡Reservación confirmada!" + (quien.isEmpty() ? "" : " La validó " + quien)
                        + " el " + Formato.fechaHora(r.optString("fechaValidacion")) + ".", R.color.verde_claro, R.color.verde);
                break;
            case "RECHAZADA":
                String motivo = Formato.texto(r, "motivoRechazo");
                pintarAviso(aviso, "No fue posible confirmar: " + (motivo.isEmpty() ? "sin disponibilidad" : motivo)
                        + ". Tu agente te enviará otra propuesta en \"Mis viajes\".", R.color.rojo_claro, R.color.rojo_oscuro);
                break;
            case "CANCELADA":
                pintarAviso(aviso, "Reservación cancelada el " + Formato.fechaHora(r.optString("fechaCancelacion"))
                        + ". Cargo por cancelación: " + Formato.dinero(r.opt("cargoCancelacion")) + ".", R.color.rojo_claro, R.color.rojo_oscuro);
                break;
            default:
                aviso.setVisibility(View.GONE);
        }

        LinearLayout cliente = findViewById(R.id.contCliente);
        cliente.removeAllViews();
        cliente.addView(filaDato("Nombre", r.optString("clienteNombre")));
        cliente.addView(filaDato("Usuario", r.optString("clienteUsuario")));
        cliente.addView(filaDato("Correo", r.optString("clienteCorreo")));

        LinearLayout viaje = findViewById(R.id.contViaje);
        viaje.removeAllViews();
        viaje.addView(filaDato("Destino", r.optString("destino")));
        viaje.addView(filaDato("Fechas", Formato.fecha(inicio) + " al " + Formato.fecha(fin) + " (" + Formato.noches(inicio, fin) + " noches)"));
        viaje.addView(filaDato("Personas", String.valueOf(r.optInt("numPersonas"))));
        viaje.addView(filaDato("Agente", r.optString("agente")));
        viaje.addView(filaDato("Solicitud / cotización", "#" + r.optInt("idSolicitud") + " / #" + r.optInt("idCotizacion")));
        int faltan = Formato.diasPara(inicio);
        if (("POR_VALIDAR".equals(estatus) || "CONFIRMADA".equals(estatus)) && faltan >= 0) {
            viaje.addView(filaDato("Cuenta regresiva", faltan == 0 ? "¡Hoy sales!" : "Faltan " + faltan + " día(s)"));
        }

        LinearLayout servicios = findViewById(R.id.contServicios);
        servicios.removeAllViews();
        JSONArray detalles = r.optJSONArray("detalles");
        for (int i = 0; detalles != null && i < detalles.length(); i++) {
            JSONObject d = detalles.optJSONObject(i);
            LinearLayout renglon = new LinearLayout(this);
            renglon.setPadding(0, dp(6), 0, dp(6));
            TextView izq = new TextView(this);
            izq.setText(Formato.iconoCategoria(d.optString("categoria")) + " " + d.optString("servicio") + "\n"
                    + d.optInt("cantidad") + " × " + Formato.dinero(d.opt("precioUnitario")) + " " + d.optString("unidad")
                    + " · " + d.optString("proveedor"));
            izq.setTextColor(getColor(R.color.azul_grisaceo));
            izq.setTextSize(13);
            izq.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView der = new TextView(this);
            der.setText(Formato.dinero(d.opt("subtotal")));
            der.setTextColor(getColor(R.color.azul_marino));
            der.setTypeface(Typeface.DEFAULT_BOLD);
            renglon.addView(izq);
            renglon.addView(der);
            servicios.addView(renglon);
        }
        ((TextView) findViewById(R.id.tvTotal)).setText(Formato.dinero(r.opt("total")));

        TextView tvCargo = findViewById(R.id.tvCargo);
        double cargo = r.optDouble("cargoCancelacion", 0);
        if (cargo > 0) {
            tvCargo.setText("Cargo por cancelación (5%): " + Formato.dinero(cargo));
            tvCargo.setVisibility(View.VISIBLE);
        } else {
            tvCargo.setVisibility(View.GONE);
        }

        // Regla de cancelacion y boton
        TextView regla = findViewById(R.id.tvRegla);
        if ("POR_VALIDAR".equals(estatus) || "CONFIRMADA".equals(estatus)) {
            String texto = "Puedes cancelar hasta el " + Formato.fecha(r.optString("fechaLimiteCancelacion")) + " (3 días antes del viaje). ";
            texto += "CONFIRMADA".equals(estatus)
                    ? "Como ya está confirmada, se aplica un cargo del 5% (" + Formato.dinero(r.optDouble("total", 0) * 0.05) + ")."
                    : "Mientras siga por validar, la cancelación no tiene cargo.";
            if (!r.optBoolean("puedeCancelar")) texto = "Ya pasó la fecha límite para cancelar (" + Formato.fecha(r.optString("fechaLimiteCancelacion")) + ").";
            regla.setText(texto);
            regla.setVisibility(View.VISIBLE);
        } else {
            regla.setVisibility(View.GONE);
        }
        btnCancelar.setVisibility(r.optBoolean("puedeCancelar") ? View.VISIBLE : View.GONE);
        ocupado(btnCancelar, false);
    }

    private void pintarAviso(TextView aviso, String texto, int fondoRes, int colorRes) {
        aviso.setText(texto);
        aviso.setTextColor(getColor(colorRes));
        aviso.setBackground(fondo(fondoRes, 8));
        aviso.setVisibility(View.VISIBLE);
    }

    private void pedirCancelacion() {
        if (reservacion == null) return;
        boolean confirmada = "CONFIRMADA".equals(reservacion.optString("estatus"));
        String cargo = confirmada
                ? "Se registrará un cargo de " + Formato.dinero(reservacion.optDouble("total", 0) * 0.05) + " (5% del total)."
                : "No tiene cargo porque todavía no estaba confirmada.";
        confirmar("¿Cancelar la reservación " + reservacion.optString("folio") + "?",
                cargo + " Esta acción no se puede deshacer.", "Sí, cancelar", this::cancelar);
    }

    private void cancelar() {
        ocupado(btnCancelar, true);
        cargando(true);
        ApiClient.llamar(this, "POST", "/reservaciones/" + idReservacion + "/cancelar", null, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                cargando(false);
                Toast.makeText(ComprobanteActivity.this, "Tu reservación se canceló.", Toast.LENGTH_LONG).show();
                if (datos != null) pintar(datos);
                else cargar();
            }

            @Override
            public void onError(int estado, String mensaje) {
                cargando(false);
                ocupado(btnCancelar, false);
                manejarError(estado, mensaje, tvMensaje);
            }
        });
    }

    /** Comparte un resumen en texto (WhatsApp, correo, etc.). */
    private void compartir() {
        if (reservacion == null) return;
        JSONObject r = reservacion;
        String texto = "GERONEX · Reservación " + r.optString("folio") + "\n"
                + "Estatus: " + Formato.textoEstatus(r.optString("estatus")) + "\n"
                + "Destino: " + r.optString("destino") + "\n"
                + "Fechas: " + Formato.fecha(r.optString("fechaInicio")) + " al " + Formato.fecha(r.optString("fechaFin")) + "\n"
                + "Personas: " + r.optInt("numPersonas") + "\n"
                + "Total: " + Formato.dinero(r.opt("total"));
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Mi reservación GERONEX " + r.optString("folio"));
        i.putExtra(Intent.EXTRA_TEXT, texto);
        startActivity(Intent.createChooser(i, "Compartir comprobante"));
    }

    private void copiarFolio() {
        if (reservacion == null) return;
        ClipboardManager portapapeles = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (portapapeles != null) {
            portapapeles.setPrimaryClip(ClipData.newPlainText("Folio GERONEX", reservacion.optString("folio")));
            Toast.makeText(this, "Folio " + reservacion.optString("folio") + " copiado.", Toast.LENGTH_SHORT).show();
        }
    }
}
