package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

/** Mi cuenta: consultar y editar nombre y correo, y cambiar la contrasena. */
public class CuentaActivity extends AppCompatActivity {

    private TextView tvUsuario, tvRol, tvMsgCuenta, tvMsgPass, btnGuardar, btnCambiar;
    private EditText etNombre, etCorreo, etActual, etNueva, etConfirmar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SesionStore.haySesion(this)) {
            irALogin();
            return;
        }
        setContentView(R.layout.activity_cuenta);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tvUsuario = findViewById(R.id.tvUsuario);
        tvRol = findViewById(R.id.tvRol);
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etActual = findViewById(R.id.etActual);
        etNueva = findViewById(R.id.etNueva);
        etConfirmar = findViewById(R.id.etConfirmar);
        tvMsgCuenta = findViewById(R.id.tvMsgCuenta);
        tvMsgPass = findViewById(R.id.tvMsgPass);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCambiar = findViewById(R.id.btnCambiar);

        findViewById(R.id.tvVolver).setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> guardarCuenta());
        btnCambiar.setOnClickListener(v -> cambiarPassword());
        cargarCuenta();
    }

    private void cargarCuenta() {
        ApiClient.llamar(this, "GET", "/usuarios/me", null, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                pintar(datos);
            }

            @Override
            public void onError(int estado, String mensaje) {
                if (estado == 401) irALogin();
                else mostrar(tvMsgCuenta, mensaje, false);
            }
        });
    }

    private void pintar(JSONObject u) {
        tvUsuario.setText(u.optString("nombreUsuario"));
        tvRol.setText(u.optString("rol"));
        etNombre.setText(u.isNull("nombre") ? "" : u.optString("nombre"));
        etCorreo.setText(u.optString("correo"));
    }

    private void guardarCuenta() {
        String correo = etCorreo.getText().toString().trim();
        String error = Validador.validarCorreo(correo);
        if (error != null) {
            mostrar(tvMsgCuenta, error, false);
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombre", etNombre.getText().toString().trim());
            cuerpo.put("correo", correo);
        } catch (JSONException e) {
            mostrar(tvMsgCuenta, "No se pudo preparar la solicitud.", false);
            return;
        }
        ocupado(btnGuardar, true);
        ApiClient.llamar(this, "PUT", "/usuarios/me", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                ocupado(btnGuardar, false);
                try {
                    SesionStore.guardar(CuentaActivity.this, SesionStore.obtenerToken(CuentaActivity.this), datos);
                } catch (Exception ignorada) {
                    // la cuenta si se guardo en el servidor; solo falla la copia local
                }
                pintar(datos);
                mostrar(tvMsgCuenta, "Tus datos se guardaron.", true);
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupado(btnGuardar, false);
                if (estado == 401) irALogin();
                else mostrar(tvMsgCuenta, mensaje, false);
            }
        });
    }

    private void cambiarPassword() {
        String actual = etActual.getText().toString();
        String nueva = etNueva.getText().toString();
        String error = actual.isEmpty() ? "Escribe tu contraseña actual." : Validador.validarPassword(nueva);
        if (error == null && !nueva.equals(etConfirmar.getText().toString())) {
            error = "Las contraseñas nuevas no coinciden.";
        }
        if (error != null) {
            mostrar(tvMsgPass, error, false);
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("contrasenaActual", actual);
            cuerpo.put("contrasenaNueva", nueva);
        } catch (JSONException e) {
            mostrar(tvMsgPass, "No se pudo preparar la solicitud.", false);
            return;
        }
        ocupado(btnCambiar, true);
        ApiClient.llamar(this, "PATCH", "/usuarios/me/password", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) { // 204: datos es null
                ocupado(btnCambiar, false);
                etActual.setText("");
                etNueva.setText("");
                etConfirmar.setText("");
                mostrar(tvMsgPass, "Tu contraseña se cambió.", true);
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupado(btnCambiar, false);
                if (estado == 401) irALogin();
                else mostrar(tvMsgPass, mensaje, false);
            }
        });
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void ocupado(TextView boton, boolean si) {
        boton.setEnabled(!si);
        boton.setAlpha(si ? 0.6f : 1f);
    }

    private void mostrar(TextView destino, String texto, boolean exito) {
        destino.setText(texto);
        destino.setTextColor(getColor(exito ? R.color.azul_marino : R.color.rojo));
        destino.setVisibility(View.VISIBLE);
    }
}
