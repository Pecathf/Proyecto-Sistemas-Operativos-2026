/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */
import edd.Cola;

public class PoliticaDiscoCSCAN implements PoliticaPlanificacionDisco {

    @Override
    public SolicitudIO seleccionarSiguiente(Cola<SolicitudIO> colaSolicitudes, int cabezalActual, boolean[] subiendo, int totalBloques) {
        if (colaSolicitudes.estaVacia()) return null;

        // C-SCAN atiende siempre en una sola dirección (subiendo hacia bloques mayores)
        subiendo[0] = true;
        SolicitudIO elegida = buscarMayorOIgual(colaSolicitudes, cabezalActual);

        // Si ya no hay bloques >= cabezal, salta al bloque 0 y toma el menor disponible
        if (elegida == null) {
            elegida = buscarMayorOIgual(colaSolicitudes, 0);
        }

        if (elegida != null) {
            removerDeCola(colaSolicitudes, elegida);
        }
        return elegida;
    }

    private SolicitudIO buscarMayorOIgual(Cola<SolicitudIO> cola, int referencia) {
        Cola<SolicitudIO> aux = new Cola<>();
        SolicitudIO candidata = null;
        int menorBloque = Integer.MAX_VALUE;

        while (!cola.estaVacia()) {
            SolicitudIO s = cola.desencolar();
            int pos = s.getBloqueObjetivo();
            if (pos >= referencia && pos < menorBloque) {
                menorBloque = pos;
                candidata = s;
            }
            aux.encolar(s);
        }

        while (!aux.estaVacia()) {
            cola.encolar(aux.desencolar());
        }
        return candidata;
    }

    private void removerDeCola(Cola<SolicitudIO> cola, SolicitudIO objetivo) {
        Cola<SolicitudIO> aux = new Cola<>();
        boolean removida = false;
        while (!cola.estaVacia()) {
            SolicitudIO s = cola.desencolar();
            if (!removida && s == objetivo) {
                removida = true;
            } else {
                aux.encolar(s);
            }
        }
        while (!aux.estaVacia()) {
            cola.encolar(aux.desencolar());
        }
    }

    @Override
    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.C_SCAN;
    }
}