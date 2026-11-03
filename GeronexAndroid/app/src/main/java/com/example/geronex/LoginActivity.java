package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private TextView tvMensaje, btnIngresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SesionStore.haySesion(this)) { // ya habia iniciado sesion
            irAPrincipal();
            return;
        }
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        tvMensaje = findViewById(R.id.tvMensaje);
        btnIngresar = findViewById(R.id.btnIngresar);
        TextView tvIrRegistro = findViewById(R.id.tvIrRegistro);

        btnIngresar.setOnClickListener(v -> intentarLogin());
        tvIrRegistro.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void intentarLogin() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString();

        String error = Validador.validarUsuario(usuario);
        if (error == null && password.isEmpty()) {
            error = "Escribe tu contraseña.";
        }
        if (error != null) {
            mostrarError(error);
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombreUsuario", usuario);
            cuerpo.put("contrasena", password);
        } catch (JSONException e) {
            mostrarError("No se pudo preparar la solicitud.");
            return;
        }

        ocupado(true);
        ApiClient.llamar(this, "POST", "/auth/login", cuerpo, new ApiClient.Respuesta() {
            @Override
            public void onExito(JSONObject datos) {
                try {
                    SesionStore.guardar(LoginActivity.this,
                            datos.getString("token"), datos.getJSONObject("usuario"));
                    irAPrincipal();
                } catch (Exception e) {
                    ocupado(false);
                    mostrarError("Respuesta inesperada del servidor.");
                }
            }

            @Override
            public void onError(int estado, String mensaje) {
                ocupado(false);
                mostrarError(mensaje);
            }
        });
    }

    private void irAPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // para que "atras" no regrese al login
    }

    private void ocupado(boolean si) {
        btnIngresar.setEnabled(!si);
        btnIngresar.setAlpha(si ? 0.6f : 1f);
    }

    private void mostrarError(String mensaje) {
        tvMensaje.setText(mensaje);
        tvMensaje.setVisibility(View.VISIBLE);
    }
}
