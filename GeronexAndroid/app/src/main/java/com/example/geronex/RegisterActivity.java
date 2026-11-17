package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsuario, etCorreo, etPassword, etConfirmar;
    private TextView tvMensaje, btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etUsuario = findViewById(R.id.etUsuario);
        etCorreo = findViewById(R.id.etCorreo);
        etPassword = findViewById(R.id.etPassword);
        etConfirmar = findViewById(R.id.etConfirmar);
        tvMensaje = findViewById(R.id.tvMensaje);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> intentarRegistro());
        findViewById(R.id.tvIrLogin).setOnClickListener(v -> finish()); // regresa al login
    }

    private void intentarRegistro() {
        String usuario = etUsuario.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String password = etPassword.getText().toString();

        String error = Validador.validarUsuario(usuario);
        if (error == null) error = Validador.validarCorreo(correo);
        if (error == null) error = Validador.validarPassword(password);
        if (error == null && !password.equals(etConfirmar.getText().toString())) {
            error = "Las contraseñas no coinciden.";
        }
        if (error != null) {
            mostrarError(error);
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombreUsuario", usuario);
            cuerpo.put("correo", correo);
            cuerpo.put("contrasena", password);
        } catch (JSONException e) {
            mostrarError("No se pudo preparar la solicitud.");
            return;
        }

        ocupado(true);
        ApiClient.llamar(this, "POST", "/auth/registro", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                iniciarSesion(usuario, password); // cuenta creada: entra directo
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupado(false);
                mostrarError(mensaje);
            }
        });
    }

    private void iniciarSesion(String usuario, String password) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombreUsuario", usuario);
            cuerpo.put("contrasena", password);
        } catch (JSONException e) {
            avisarCuentaCreada();
            return;
        }
        ApiClient.llamar(this, "POST", "/auth/login", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                try {
                    SesionStore.guardar(RegisterActivity.this,
                            datos.getString("token"), datos.getJSONObject("usuario"));
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    avisarCuentaCreada();
                }
            }

            @Override
            public void onError(int estado, String mensaje) {
                avisarCuentaCreada();
            }
        });
    }

    private void avisarCuentaCreada() {
        Toast.makeText(this, "Cuenta creada. Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show();
        finish(); // vuelve al login
    }

    private void ocupado(boolean si) {
        btnRegistrar.setEnabled(!si);
        btnRegistrar.setAlpha(si ? 0.6f : 1f);
    }

    private void mostrarError(String mensaje) {
        tvMensaje.setText(mensaje);
        tvMensaje.setVisibility(View.VISIBLE);
    }
}
