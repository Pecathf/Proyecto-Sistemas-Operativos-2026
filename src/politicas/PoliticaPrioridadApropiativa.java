/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package politicas;

/**
 *
 * @author samue
 */
import procesos.Proceso;
import edd.Cola;
import edd.ListaEnlazada;

public class PoliticaPrioridadApropiativa implements PoliticaPlanificacion {

    @Override
    public Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos) {
        if (colaListos.estaVacia()) return null;

        Cola<Proceso> auxiliar = new Cola<>();
        Proceso mayorPrioridad = colaListos.desencolar();
        auxiliar.encolar(mayorPrioridad);

        while (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            // Menor valor numérico representa mayor prioridad (1 es más prioritario que 5)
            if (p.getPrioridad() < mayorPrioridad.getPrioridad()) {
                mayorPrioridad = p;
            }
            auxiliar.encolar(p);
        }

        boolean removido = false;
        while (!auxiliar.estaVacia()) {
            Proceso p = auxiliar.desencolar();
            if (!removido && p.getId() == mayorPrioridad.getId()) {
                removido = true;
            } else {
                colaListos.encolar(p);
            }
        }
        return mayorPrioridad;
    }

    @Override
    public boolean usaQuantum() {
        return false;
    }

    @Override
    public boolean debeApropiar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        if (enEjecucion == null || colaListos.estaVacia()) return false;
        
        ListaEnlazada<Proceso> lista = colaListos.obtenerListaElementos();
        for (int i = 0; i < lista.getTamano(); i++) {
            Proceso p = lista.obtener(i);
            if (p != null && p.getPrioridad() < enEjecucion.getPrioridad()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDAD_APROPIATIVA;
    }
}