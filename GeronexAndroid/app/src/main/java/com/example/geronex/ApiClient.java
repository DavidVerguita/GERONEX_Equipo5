package com.example.geronex;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Cliente de la API REST de GERONEX (la misma que usa la web).
 * Las peticiones se hacen en un hilo aparte y la respuesta regresa al hilo principal,
 * asi que se puede tocar la pantalla directamente dentro de onExito y onError.
 */
public final class ApiClient {

    public interface Respuesta {
        /** datos es null cuando la API responde 204 (sin contenido). */
        void onExito(JSONObject datos);

        /** estado 0 = no hubo conexion; si no, es el codigo HTTP (400, 401, 409...). */
        void onError(int estado, String mensaje);
    }

    private static final String MENSAJE_RED =
            "No se pudo conectar con el servidor. Revisa que el backend esté encendido y el teléfono conectado.";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient CLIENTE = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private ApiClient() {
    }

    /** metodo: GET, POST, PUT o PATCH. ruta: por ejemplo "/auth/login". cuerpo puede ser null. */
    public static void llamar(Context contexto, String metodo, String ruta,
                              JSONObject cuerpo, Respuesta respuesta) {
        Context app = contexto.getApplicationContext();
        // Las rutas /auth/* son publicas: mandar un token viejo ahi haria que el servidor las rechace
        String token = ruta.startsWith("/auth/") ? null : SesionStore.obtenerToken(app);

        Request.Builder pedido = new Request.Builder().url(Config.BASE_URL + ruta);
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        RequestBody cuerpoHttp = null;
        if (!metodo.equals("GET")) {
            cuerpoHttp = RequestBody.create(cuerpo != null ? cuerpo.toString() : "", JSON);
        }
        pedido.method(metodo, cuerpoHttp);

        CLIENTE.newCall(pedido.build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                PRINCIPAL.post(() -> respuesta.onError(0, MENSAJE_RED));
            }

            @Override
            public void onResponse(Call call, Response r) {
                try (Response resp = r) {
                    String texto = resp.body() != null ? resp.body().string() : "";
                    int estado = resp.code();
                    JSONObject json = null;
                    try {
                        if (!texto.isEmpty()) {
                            json = new JSONObject(texto);
                        }
                    } catch (Exception ignorada) {
                        // respuesta que no es JSON: se trata como sin datos
                    }
                    if (resp.isSuccessful()) {
                        JSONObject datos = json;
                        PRINCIPAL.post(() -> respuesta.onExito(datos));
                    } else {
                        if (estado == 401 && token != null) {
                            SesionStore.cerrar(app); // token vencido o cuenta ya no activa
                        }
                        String mensaje = textoError(json);
                        PRINCIPAL.post(() -> respuesta.onError(estado, mensaje));
                    }
                } catch (IOException e) {
                    PRINCIPAL.post(() -> respuesta.onError(0, MENSAJE_RED));
                }
            }
        });
    }

    /** Mensaje para mostrar: el detalle por campo si existe; si no, el mensaje general. */
    private static String textoError(JSONObject json) {
        if (json == null) {
            return "Ocurrió un error inesperado.";
        }
        JSONObject errores = json.optJSONObject("errores");
        if (errores != null && errores.length() > 0) {
            List<String> detalles = new ArrayList<>();
            Iterator<String> claves = errores.keys();
            while (claves.hasNext()) {
                detalles.add(errores.optString(claves.next()));
            }
            return String.join(" ", detalles);
        }
        return json.optString("mensaje", "Ocurrió un error inesperado.");
    }
}
