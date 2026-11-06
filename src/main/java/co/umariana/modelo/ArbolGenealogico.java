package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre;
        Nodo madre;
        List<Nodo> hijos;

        Nodo(Persona persona) {
            this.persona = persona;
            this.hijos = new ArrayList<>();
        }
    }

    private final Nodo raiz;
    private int cantidadPersonas;

    public ArbolGenealogico(Persona sujeto) {
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("La posición del sujeto debe ser 1");
        }
        this.raiz = new Nodo(sujeto);
        this.cantidadPersonas = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidadPersonas;
    }

    private Nodo buscarNodo(String posicion) {
        if (posicion == null || !Posicion.esValida(posicion)) {
            return null;
        }
        if (Posicion.esSujeto(posicion)) {
            return raiz;
        }

        if (Posicion.esAscendiente(posicion)) {
            int n = Integer.parseInt(posicion);
            String bin = Integer.toBinaryString(n);
            Nodo actual = raiz;
            for (int i = 1; i < bin.length(); i++) {
                if (actual == null) return null;
                if (bin.charAt(i) == '0') {
                    actual = actual.padre;
                } else {
                    actual = actual.madre;
                }
            }
            return actual;
        }

        if (Posicion.esDescendiente(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length; i++) {
                if (actual == null) return null;
                int ordenBuscado = Integer.parseInt(partes[i]);
                Nodo encontrado = null;
                for (Nodo hijo : actual.hijos) {
                    if (Posicion.orden(hijo.persona.getPosicion()) == ordenBuscado) {
                        encontrado = hijo;
                        break;
                    }
                }
                actual = encontrado;
            }
            return actual;
        }

        return null;
    }

    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo != null ? nodo.persona : null;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    public Persona agregar(Persona persona) {
        persona.validar();
        String pos = persona.getPosicion();

        if (Posicion.esSujeto(pos)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) ya existe");
        }
        if (contiene(pos)) {
            throw new ArbolInvalidoException("La posición ya existe en el árbol");
        }

        if (Posicion.esAscendiente(pos)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo == null) {
                throw new ArbolInvalidoException("Falta el hijo en línea de este ascendiente");
            }
            if (persona.getNacimiento() >= nodoHijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo");
            }

            int n = Integer.parseInt(pos);
            if (n % 2 == 0) {
                nodoHijo.padre = new Nodo(persona);
            } else {
                nodoHijo.madre = new Nodo(persona);
            }

        } else if (Posicion.esDescendiente(pos)) {
            String posProg = Posicion.posicionDelProgenitor(pos);
            Nodo nodoProg = buscarNodo(posProg);
            if (nodoProg == null) {
                throw new ArbolInvalidoException("Falta el progenitor de este descendiente");
            }
            if (persona.getNacimiento() <= nodoProg.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después que su progenitor");
            }

            nodoProg.hijos.add(new Nodo(persona));
            nodoProg.hijos.sort((n1, n2) -> Integer.compare(Posicion.orden(n1.persona.getPosicion()), Posicion.orden(n2.persona.getPosicion())));
        }
        
        cantidadPersonas++;
        return persona;
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esValida(posicion) || Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("La posición no corresponde al sujeto ni a un ascendiente");
        }
        String posPadre = Posicion.posicionDelPadre(posicion);
        Persona padre = new Persona(posPadre, nombre, nacimiento, fallecimiento);
        return agregar(padre);
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esValida(posicion) || Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("La posición no corresponde al sujeto ni a un ascendiente");
        }
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        Persona madre = new Persona(posMadre, nombre, nacimiento, fallecimiento);
        return agregar(madre);
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esValida(posicion) || Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("La posición no corresponde al sujeto ni a un descendiente");
        }
        Nodo nodoProg = buscarNodo(posicion);
        if (nodoProg == null) {
            throw new ArbolInvalidoException("La posición del progenitor no existe en el árbol");
        }

        int maxOrden = 0;
        for (Nodo hijo : nodoProg.hijos) {
            int ordenActual = Posicion.orden(hijo.persona.getPosicion());
            if (ordenActual > maxOrden) {
                maxOrden = ordenActual;
            }
        }
        
        String posHijo = posicion + "." + (maxOrden + 1);
        Persona hijo = new Persona(posHijo, nombre, nacimiento, fallecimiento);
        return agregar(hijo);
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición no existe en el árbol");
        }

        int eliminados = 0;
        if (Posicion.esAscendiente(posicion)) {
            eliminados = contarAscendientes(nodo);
            Nodo nodoHijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (Integer.parseInt(posicion) % 2 == 0) {
                nodoHijo.padre = null;
            } else {
                nodoHijo.madre = null;
            }
        } else {
            eliminados = contarDescendientes(nodo);
            Nodo nodoProg = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            nodoProg.hijos.remove(nodo);
        }
        
        cantidadPersonas -= eliminados;
        return eliminados;
    }

    private int contarAscendientes(Nodo n) {
        if (n == null) return 0;
        return 1 + contarAscendientes(n.padre) + contarAscendientes(n.madre);
    }

    private int contarDescendientes(Nodo n) {
        if (n == null) return 0;
        int count = 1;
        for (Nodo h : n.hijos) {
            count += contarDescendientes(h);
        }
        return count;
    }

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(sujeto());
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        List<Nodo> ascNodos = new ArrayList<>();
        recolectarAscendientes(raiz, ascNodos);
        ascNodos.remove(raiz);
        
        List<Persona> ascPersonas = new ArrayList<>();
        for (Nodo n : ascNodos) {
            ascPersonas.add(n.persona);
        }
        
        ascPersonas.sort((a, b) -> Integer.compare(Integer.parseInt(a.getPosicion()), Integer.parseInt(b.getPosicion())));
        return ascPersonas;
    }

    private void recolectarAscendientes(Nodo n, List<Nodo> lista) {
        if (n == null) return;
        lista.add(n);
        recolectarAscendientes(n.padre, lista);
        recolectarAscendientes(n.madre, lista);
    }

    public List<Persona> descendientes() {
        List<Persona> descPersonas = new ArrayList<>();
        for (Nodo hijo : raiz.hijos) {
            recolectarDescendientes(hijo, descPersonas);
        }
        return descPersonas;
    }

    private void recolectarDescendientes(Nodo n, List<Persona> lista) {
        if (n == null) return;
        lista.add(n.persona);
        for (Nodo hijo : n.hijos) {
            recolectarDescendientes(hijo, lista);
        }
    }
}