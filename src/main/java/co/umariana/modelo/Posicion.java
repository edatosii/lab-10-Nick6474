package co.umariana.modelo;

public final class Posicion {

    public static final String SUJETO = "1";
    public static final int MAX_AHNENTAFEL = 2047;
    public static final int MAX_ORDEN = 9999;
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    public static boolean esAhnentafel(String posicion) {
        if (posicion == null || !posicion.matches("^[1-9]\\d*$")) {
            return false;
        }
        try {
            int n = Integer.parseInt(posicion);
            return n >= 1 && n <= MAX_AHNENTAFEL;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean esDAboville(String posicion) {
        if (posicion == null || posicion.length() > MAX_LONGITUD) {
            return false;
        }
        return posicion.matches("^1(\\.[1-9]\\d{0,3})+$");
    }

    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    public static String posicionDelPadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("No es una posición Ahnentafel válida.");
        }
        int n = Integer.parseInt(ahnentafel);
        return String.valueOf(n * 2);
    }

    public static String posicionDeLaMadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("No es una posición Ahnentafel válida.");
        }
        int n = Integer.parseInt(ahnentafel);
        return String.valueOf(n * 2 + 1);
    }

    public static String posicionDelHijoEnLinea(String ahnentafel) {
        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException("No es un ascendiente válido.");
        }
        int n = Integer.parseInt(ahnentafel);
        return String.valueOf(n / 2);
    }

    public static String posicionDelProgenitor(String daboville) {
        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException("No es un descendiente válido.");
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return daboville.substring(0, ultimoPunto);
    }

    public static int orden(String daboville) {
        if (!esDescendiente(daboville)) {
            throw new IllegalArgumentException("No es un descendiente válido.");
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return Integer.parseInt(daboville.substring(ultimoPunto + 1));
    }

    public static int generacion(String posicion) {
        if (esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            int gen = 0;
            while (n > 1) {
                n /= 2;
                gen++;
            }
            return gen;
        } else if (esDescendiente(posicion)) {
            int puntos = 0;
            for (int i = 0; i < posicion.length(); i++) {
                if (posicion.charAt(i) == '.') {
                    puntos++;
                }
            }
            return puntos;
        }
        throw new IllegalArgumentException("La posición no es válida.");
    }
}