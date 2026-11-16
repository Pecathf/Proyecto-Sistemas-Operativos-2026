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

public class PoliticaDiscoSCAN implements PoliticaPlanificacionDisco {

    @Override
    public SolicitudIO seleccionarSiguiente(Cola<SolicitudIO> colaSolicitudes, int cabezalActual, boolean[] subiendo, int totalBloques) {
        if (colaSolicitudes.estaVacia()) return null;

        SolicitudIO elegida = buscarEnDireccion(colaSolicitudes, cabezalActual, subiendo[0]);

        // Si no hay solicitudes en la dirección actual, el cabezal llega al extremo y cambia de dirección
        if (elegida == null) {
            subiendo[0] = !subiendo[0];
            elegida = buscarEnDireccion(colaSolicitudes, cabezalActual, subiendo[0]);
        }

        if (elegida != null) {
            removerDeCola(colaSolicitudes, elegida);
        }
        return elegida;
    }

    private SolicitudIO buscarEnDireccion(Cola<SolicitudIO> cola, int cabezal, boolean haciaArriba) {
        Cola<SolicitudIO> aux = new Cola<>();
        SolicitudIO candidata = null;
        int mejorDistancia = Integer.MAX_VALUE;

        while (!cola.estaVacia()) {
            SolicitudIO s = cola.desencolar();
            int pos = s.getBloqueObjetivo();
            boolean cumpleDireccion = haciaArriba ? (pos >= cabezal) : (pos <= cabezal);
            if (cumpleDireccion) {
                int dist = Math.abs(pos - cabezal);
                if (dist < mejorDistancia) {
                    mejorDistancia = dist;
                    candidata = s;
                }
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
        return TipoPoliticaDisco.SCAN;
    }
}