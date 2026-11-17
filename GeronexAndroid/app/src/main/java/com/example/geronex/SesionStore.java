package com.example.geronex;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Guarda la sesion. El token JWT se cifra con una llave AES que vive en el Android Keystore
 * (la llave no sale del telefono); los datos de la cuenta no son secretos y van en claro.
 */
public final class SesionStore {

    private static final String PREFS = "geronex_sesion";
    private static final String ALIAS = "geronex_token_key";
    private static final String KEYSTORE = "AndroidKeyStore";

    private SesionStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey llave() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return (SecretKey) ks.getKey(ALIAS, null);
        }
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return gen.generateKey();
    }

    public static void guardar(Context c, String token, JSONObject usuario) throws Exception {
        Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
        cifrador.init(Cipher.ENCRYPT_MODE, llave());
        byte[] cifrado = cifrador.doFinal(token.getBytes(StandardCharsets.UTF_8));
        String paquete = Base64.encodeToString(cifrador.getIV(), Base64.NO_WRAP)
                + ":" + Base64.encodeToString(cifrado, Base64.NO_WRAP);
        prefs(c).edit().putString("token", paquete).putString("usuario", usuario.toString()).apply();
    }

    /** Regresa el token, o null si no hay sesion o no se pudo descifrar. */
    public static String obtenerToken(Context c) {
        String paquete = prefs(c).getString("token", null);
        if (paquete == null) {
            return null;
        }
        try {
            String[] partes = paquete.split(":");
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, llave(),
                    new GCMParameterSpec(128, Base64.decode(partes[0], Base64.NO_WRAP)));
            byte[] claro = cifrador.doFinal(Base64.decode(partes[1], Base64.NO_WRAP));
            return new String(claro, StandardCharsets.UTF_8);
        } catch (Exception e) {
            cerrar(c); // token ilegible: se descarta la sesion
            return null;
        }
    }

    public static JSONObject obtenerUsuario(Context c) {
        String texto = prefs(c).getString("usuario", null);
        if (texto == null) {
            return null;
        }
        try {
            return new JSONObject(texto);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean haySesion(Context c) {
        return obtenerToken(c) != null;
    }

    public static void cerrar(Context c) {
        prefs(c).edit().clear().apply();
    }
}
