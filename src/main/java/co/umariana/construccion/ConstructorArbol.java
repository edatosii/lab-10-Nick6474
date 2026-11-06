package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.List;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class LineaParseada {
        Persona persona;
        int numeroLinea;

        LineaParseada(Persona persona, int numeroLinea) {
            this.persona = persona;
            this.numeroLinea = numeroLinea;
        }
    }

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío o no contiene información válida.");
        }

        String[] lineas = texto.split("\r?\n", -1);
        List<LineaParseada> personasParseadas = new ArrayList<>();
        List<String> posicionesVistas = new ArrayList<>();
        boolean tieneContenido = false;

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            int numeroLinea = i + 1;

            if (linea.trim().isEmpty()) {
                continue;
            }
            tieneContenido = true;

            String[] campos = linea.split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw new ArbolInvalidoException("Línea " + numeroLinea + ": cantidad de campos incorrecta.");
            }

            String posicion = campos[0].trim();
            String nombre = campos[1].trim();
            String strNacimiento = campos[2].trim();
            String strFallecimiento = campos.length == 4 ? campos[3].trim() : "";

            if (posicionesVistas.contains(posicion)) {
                throw new ArbolInvalidoException("Línea " + numeroLinea + ": la posición " + posicion + " está repetida.");
            }
            posicionesVistas.add(posicion);

            int nacimiento;
            Integer fallecimiento = null;

            try {
                nacimiento = Integer.parseInt(strNacimiento);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + numeroLinea + ": el año de nacimiento debe ser un número entero.");
            }

            if (!strFallecimiento.isEmpty()) {
                try {
                    fallecimiento = Integer.parseInt(strFallecimiento);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException("Línea " + numeroLinea + ": el año de fallecimiento debe ser un número entero.");
                }
            }

            Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
            
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw new ArbolInvalidoException("Línea " + numeroLinea + ": " + e.getMessage());
            }

            personasParseadas.add(new LineaParseada(persona, numeroLinea));
        }

        if (!tieneContenido) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        LineaParseada sujetoParseado = null;
        for (LineaParseada lp : personasParseadas) {
            if (Posicion.esSujeto(lp.persona.getPosicion())) {
                sujetoParseado = lp;
                break;
            }
        }

        if (sujetoParseado == null) {
            throw new ArbolInvalidoException("Falta el sujeto (posición 1) en el texto.");
        }

        personasParseadas.sort((lp1, lp2) -> {
            String p1 = lp1.persona.getPosicion();
            String p2 = lp2.persona.getPosicion();

            if (Posicion.esSujeto(p1)) return -1;
            if (Posicion.esSujeto(p2)) return 1;

            boolean asc1 = Posicion.esAscendiente(p1);
            boolean asc2 = Posicion.esAscendiente(p2);

            if (asc1 && !asc2) return -1;
            if (!asc1 && asc2) return 1;

            if (asc1 && asc2) {
                return Integer.compare(Integer.parseInt(p1), Integer.parseInt(p2));
            }

            int g1 = Posicion.generacion(p1);
            int g2 = Posicion.generacion(p2);
            if (g1 != g2) {
                return Integer.compare(g1, g2);
            }
            return p1.compareTo(p2);
        });

        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(sujetoParseado.persona);
        } catch (ArbolInvalidoException e) {
            throw new ArbolInvalidoException("Línea " + sujetoParseado.numeroLinea + ": " + e.getMessage());
        }

        for (LineaParseada lp : personasParseadas) {
            if (Posicion.esSujeto(lp.persona.getPosicion())) {
                continue;
            }
            try {
                arbol.agregar(lp.persona);
            } catch (ArbolInvalidoException e) {
                throw new ArbolInvalidoException("Línea " + lp.numeroLinea + ": " + e.getMessage());
            }
        }

        return arbol;
    }
}