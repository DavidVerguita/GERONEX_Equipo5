package com.example.geronex;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

/**
 * Base de las pantallas del cliente: sesion, encabezado, etiquetas de estatus, filtros (chips),
 * dialogos de confirmacion y actualizacion automatica (consulta la API cada cierto tiempo, como la web).
 */
public abstract class BaseActivity extends AppCompatActivity {

    private final Handler temporizador = new Handler(Looper.getMainLooper());
    private final Runnable refrescar = new Runnable() {
        @Override
        public void run() {
            cargar();
            temporizador.postDelayed(this, segundosRefresco() * 1000L);
        }
    };

    /** Vuelve a pedir los datos a la API. Se llama al abrir y al regresar a la pantalla. */
    protected void cargar() {
    }

    /** Cada cuantos segundos se actualiza sola la pantalla (0 = nunca). */
    protected int segundosRefresco() {
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            cargar();
            if (segundosRefresco() > 0) {
                temporizador.postDelayed(refrescar, segundosRefresco() * 1000L);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        temporizador.removeCallbacks(refrescar);
    }

    // ------------------------------------------------------------------ sesion

    /**
     * Las pantallas del negocio son solo para clientes (los empleados usan la web).
     * Regresa false si no hay sesion o la cuenta no es de cliente; en ese caso ya se redirigio.
     */
    protected boolean exigirCliente() {
        if (!SesionStore.haySesion(this)) {
            irALogin();
            return false;
        }
        JSONObject u = SesionStore.obtenerUsuario(this);
        if (u == null || !"CLIENTE".equals(u.optString("rol"))) {
            Toast.makeText(this, "Esta sección es para clientes. Los empleados usan la página web.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    protected void irALogin() {
        SesionStore.cerrar(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** 401 = sesion vencida: al login. Cualquier otro error se muestra. */
    protected void manejarError(int estado, String mensaje, TextView destino) {
        if (estado == 401) {
            Toast.makeText(this, "Tu sesión venció. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            irALogin();
            return;
        }
        if (destino != null) {
            mostrarMensaje(destino, mensaje, false);
        } else {
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }
    }

    protected void mostrarMensaje(TextView destino, String texto, boolean exito) {
        destino.setText(texto);
        destino.setTextColor(getColor(exito ? R.color.verde : R.color.rojo_oscuro));
        destino.setBackground(fondo(exito ? R.color.verde_claro : R.color.rojo_claro, 10));
        destino.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------ encabezado y carga

    /** Usa el include encabezado.xml: flecha para volver y titulo. */
    protected void configurarEncabezado(String titulo) {
        TextView tv = findViewById(R.id.tvTitulo);
        if (tv != null) {
            tv.setText(titulo);
        }
        View volver = findViewById(R.id.btnVolver);
        if (volver != null) {
            volver.setOnClickListener(v -> finish());
        }
    }

    protected void cargando(boolean si) {
        View p = findViewById(R.id.progreso);
        if (p != null) {
            p.setVisibility(si ? View.VISIBLE : View.GONE);
        }
    }

    protected void ocupado(View boton, boolean si) {
        boton.setEnabled(!si);
        boton.setAlpha(si ? 0.6f : 1f);
    }

    // ------------------------------------------------------------------ piezas visuales

    protected int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    /** Rectangulo redondeado de un color (para etiquetas y avisos). */
    protected GradientDrawable fondo(int colorRes, int radioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(getColor(colorRes));
        g.setCornerRadius(dp(radioDp));
        return g;
    }

    /** Pinta un TextView como la etiqueta de color de un estatus. */
    protected void badge(TextView tv, String estatus) {
        int[] colores = Formato.coloresEstatus(estatus);
        tv.setText(Formato.textoEstatus(estatus));
        tv.setTextColor(getColor(colores[1]));
        tv.setBackground(fondo(colores[0], 20));
        tv.setPadding(dp(10), dp(3), dp(10), dp(3));
        tv.setTextSize(12);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
    }

    /** Crea un filtro (chip) para una fila horizontal. */
    protected TextView chip(String texto, boolean activo) {
        TextView c = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        c.setLayoutParams(lp);
        c.setText(texto);
        c.setTextSize(14);
        c.setPadding(dp(14), dp(8), dp(14), dp(8));
        c.setClickable(true);
        c.setFocusable(true);
        marcarChip(c, activo);
        return c;
    }

    protected void marcarChip(TextView c, boolean activo) {
        c.setBackgroundResource(activo ? R.drawable.bg_chip_activo : R.drawable.bg_chip);
        c.setTextColor(getColor(activo ? R.color.white : R.color.azul_grisaceo));
    }

    /** Imagen del destino con esquinas redondeadas. */
    protected void imagenDestino(ImageView iv, String destino) {
        iv.setImageResource(Destinos.imagen(destino));
        iv.setBackgroundResource(R.drawable.bg_imagen);
        iv.setClipToOutline(true);
    }

    /** Fila "etiqueta: valor" para fichas de datos. */
    protected View filaDato(String etiqueta, String valor) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(0, dp(4), 0, dp(4));
        TextView e = new TextView(this);
        e.setText(etiqueta);
        e.setTextColor(getColor(R.color.gris));
        e.setTextSize(13);
        e.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(this);
        v.setText(valor);
        v.setTextColor(getColor(R.color.azul_marino));
        v.setTextSize(14);
        v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
        v.setGravity(android.view.Gravity.END);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        fila.addView(e);
        fila.addView(v);
        return fila;
    }

    /** Dialogo "¿Seguro?" con el estilo de Material. */
    protected void confirmar(String titulo, String mensaje, String textoSi, Runnable alAceptar) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(textoSi, (d, w) -> alAceptar.run())
                .show();
    }
}
