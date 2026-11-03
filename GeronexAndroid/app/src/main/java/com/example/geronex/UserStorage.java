package com.example.geronex;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Guarda los usuarios registrados en el telefono (SharedPreferences).
 * Es TEMPORAL: cuando exista el backend Spring Boot, el registro y el login
 * se haran contra la API y esta clase ya no se usara.
 * La contrasena nunca se guarda en texto plano, solo su hash SHA-256.
 */
public class UserStorage {

    private static final String PREFS = "geronex_usuarios";
    private final SharedPreferences prefs;

    public UserStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String clave(String usuario) {
        return "user_" + usuario.toLowerCase();
    }

    public boolean existeUsuario(String usuario) {
        return prefs.contains(clave(usuario));
    }

    public void guardarUsuario(String usuario, String password) {
        prefs.edit().putString(clave(usuario), hash(password)).apply();
    }

    public boolean credencialesCorrectas(String usuario, String password) {
        String guardado = prefs.getString(clave(usuario), null);
        return guardado != null && guardado.equals(hash(password));
    }

    private String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
