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

public class PoliticaRoundRobin implements PoliticaPlanificacion {
    @Override
    public Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos) {
        if (!colaListos.estaVacia()) {
            return colaListos.desencolar();
        }
        return null;
    }

    @Override
    public boolean usaQuantum() {
        return true;
    }
}