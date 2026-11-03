package com.example.geronex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private String usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        JSONObject cuenta = SesionStore.obtenerUsuario(this);
        if (cuenta == null) { // sin sesion: volver al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        usuario = cuenta.optString("nombreUsuario", "");

        TextView tvBienvenida = findViewById(R.id.tvBienvenida);
        tvBienvenida.setText("Hola, " + usuario);

        // Botones principales: por ahora solo avisan que vienen despues
        asignarProximamente(R.id.btnReservaciones, "Reservaciones");
        asignarProximamente(R.id.btnCotizaciones, "Cotizaciones");
        asignarProximamente(R.id.btnServicios, "Servicios");

        findViewById(R.id.btnCuenta).setOnClickListener(this::mostrarMenuCuenta);
        findViewById(R.id.btnFiltros).setOnClickListener(this::mostrarMenuFiltros);
    }

    private void asignarProximamente(int idBoton, String nombre) {
        findViewById(idBoton).setOnClickListener(v ->
                Toast.makeText(this, nombre + ": próximamente", Toast.LENGTH_SHORT).show());
    }

    private void mostrarMenuCuenta(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "Usuario: " + usuario).setEnabled(false);
        menu.getMenu().add(0, 3, 1, "Mi cuenta");
        menu.getMenu().add(0, 2, 2, "Cerrar sesión");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 3) {
                startActivity(new Intent(MainActivity.this, CuentaActivity.class));
                return true;
            }
            if (item.getItemId() == 2) {
                SesionStore.cerrar(MainActivity.this);
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void mostrarMenuFiltros(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Destino");
        menu.getMenu().add("Fecha");
        menu.getMenu().add("Integrantes");
        menu.getMenu().add("Pet-friendly");
        menu.setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "Filtro " + item.getTitle() + ": próximamente",
                    Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }
}
