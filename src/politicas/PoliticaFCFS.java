/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package politicas;

/**
 *
 * @author Jabri
 */
import procesos.Proceso;
import edd.Cola;

public class PoliticaFCFS implements PoliticaPlanificacion {

    @Override
    public Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos) {
        if (!colaListos.estaVacia()) {
            return colaListos.desencolar();
        }
        return null;
    }

    @Override
    public boolean usaQuantum() {
        return false;
    }

    @Override
    public boolean debeApropiar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        return false;
    }

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.FCFS;
    }
}