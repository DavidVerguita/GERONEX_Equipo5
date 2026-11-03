package com.example.geronex;

/**
 * Comprobaciones minimas antes de llamar a la API. La validacion real es la del backend:
 * aqui solo se evita enviar campos vacios o claramente fuera de rango.
 * Cada metodo regresa el mensaje de error, o null si el dato es valido.
 */
public class Validador {

    public static final int USUARIO_MAX = 15;
    public static final int CORREO_MAX = 254;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 50;

    public static String validarUsuario(String usuario) {
        if (usuario.isEmpty()) {
            return "Escribe tu nombre de usuario.";
        }
        if (usuario.length() > USUARIO_MAX) {
            return "El usuario no puede tener más de " + USUARIO_MAX + " caracteres.";
        }
        return null;
    }

    public static String validarCorreo(String correo) {
        if (correo.isEmpty()) {
            return "Escribe tu correo electrónico.";
        }
        if (correo.length() > CORREO_MAX) {
            return "El correo es demasiado largo.";
        }
        return null;
    }

    /** Para crear o cambiar una contrasena (en el login solo se pide que no este vacia). */
    public static String validarPassword(String password) {
        if (password.isEmpty()) {
            return "Escribe tu contraseña.";
        }
        if (password.length() < PASSWORD_MIN) {
            return "La contraseña debe tener mínimo " + PASSWORD_MIN + " caracteres.";
        }
        if (password.length() > PASSWORD_MAX) {
            return "La contraseña no puede tener más de " + PASSWORD_MAX + " caracteres.";
        }
        return null;
    }
}
