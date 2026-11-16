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

public class PoliticaDiscoSSTF implements PoliticaPlanificacionDisco {

    @Override
    public SolicitudIO seleccionarSiguiente(Cola<SolicitudIO> colaSolicitudes, int cabezalActual, boolean[] subiendo, int totalBloques) {
        if (colaSolicitudes.estaVacia()) return null;

        Cola<SolicitudIO> aux = new Cola<>();
        SolicitudIO masCercana = colaSolicitudes.desencolar();
        int menorDistancia = Math.abs(masCercana.getBloqueObjetivo() - cabezalActual);
        aux.encolar(masCercana);

        while (!colaSolicitudes.estaVacia()) {
            SolicitudIO actual = colaSolicitudes.desencolar();
            int dist = Math.abs(actual.getBloqueObjetivo() - cabezalActual);
            if (dist < menorDistancia) {
                menorDistancia = dist;
                masCercana = actual;
            }
            aux.encolar(actual);
        }

        boolean removida = false;
        while (!aux.estaVacia()) {
            SolicitudIO s = aux.desencolar();
            if (!removida && s == masCercana) {
                removida = true;
            } else {
                colaSolicitudes.encolar(s);
            }
        }

        if (masCercana.getBloqueObjetivo() > cabezalActual) {
            subiendo[0] = true;
        } else if (masCercana.getBloqueObjetivo() < cabezalActual) {
            subiendo[0] = false;
        }

        return masCercana;
    }

    @Override
    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.SSTF;
    }
}