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

public class PoliticaDiscoFIFO implements PoliticaPlanificacionDisco {

    @Override
    public SolicitudIO seleccionarSiguiente(Cola<SolicitudIO> colaSolicitudes, int cabezalActual, boolean[] subiendo, int totalBloques) {
        if (colaSolicitudes.estaVacia()) return null;
        return colaSolicitudes.desencolar();
    }

    @Override
    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.FIFO;
    }
}