package com.petvet.mascotas.domain;

import java.util.Locale;
import java.util.Set;

/** Foto subida de una mascota: los bytes y su tipo de contenido. */
public record FotoMascota(String tipoContenido, byte[] datos) {

    /** Mismo tope que el front y que spring.servlet.multipart.max-file-size. */
    public static final int TAMANO_MAXIMO = 5 * 1024 * 1024;

    private static final Set<String> TIPOS_ADMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    /**
     * Valida la foto recibida. El tipo declarado debe ser JPG, PNG o WebP y los primeros bytes deben
     * corresponder a uno de esos formatos, porque el tipo declarado lo elige el cliente. Se guarda el
     * tipo que indican los bytes.
     */
    public static FotoMascota desde(byte[] contenido, String tipoDeclarado) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("La foto está vacía.");
        }
        if (contenido.length > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La foto pesa más de 5 MB.");
        }
        String tipo = tipoSegunBytes(contenido);
        if (tipo == null || !TIPOS_ADMITIDOS.contains(sinParametros(tipoDeclarado))) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPG, PNG o WebP.");
        }
        return new FotoMascota(tipo, contenido);
    }

    private static String tipoSegunBytes(byte[] datos) {
        if (empiezaCon(datos, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (empiezaCon(datos, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (empiezaCon(datos, 0, 'R', 'I', 'F', 'F') && empiezaCon(datos, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        return null;
    }

    private static boolean empiezaCon(byte[] datos, int desde, int... firma) {
        if (datos.length < desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((datos[desde + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sinParametros(String tipo) {
        if (tipo == null) {
            return "";
        }
        int parametros = tipo.indexOf(';');
        return (parametros < 0 ? tipo : tipo.substring(0, parametros)).strip().toLowerCase(Locale.ROOT);
    }
}
