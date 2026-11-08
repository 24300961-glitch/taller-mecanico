package mx.tallermecanico.app.common;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {}

    public static String limpiar(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }

    public static String normalizar(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) return null;
        String sinAcentos = Normalizer.normalize(limpio, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    public static String clave(String valor) {
        String normalizado = normalizar(valor);
        if (normalizado == null) return null;
        return normalizado.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String soloDigitos(String valor) {
        if (valor == null) return null;
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    public static String correo(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toLowerCase(Locale.ROOT);
    }

    public static String alfanumericoMayusculas(String valor) {
        if (valor == null) return null;
        String limpio = valor.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return limpio.isEmpty() ? null : limpio;
    }

    public static String placa(String valor) {
        String limpio = limpiar(valor);
        if (limpio == null) return null;
        return limpio.toUpperCase(Locale.ROOT).replace(' ', '-');
    }
}
