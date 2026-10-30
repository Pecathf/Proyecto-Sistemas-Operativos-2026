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

public class PoliticaSJF implements PoliticaPlanificacion {

    @Override
    public Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos) {
        if (colaListos.estaVacia())
            return null;

        Cola<Proceso> auxiliar = new Cola<>();
        Proceso masCorto = colaListos.desencolar();
        auxiliar.encolar(masCorto);

        while (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            if (p.getTiempoRestante() < masCorto.getTiempoRestante()) {
                masCorto = p;
            }
            auxiliar.encolar(p);
        }

        boolean removido = false;
        while (!auxiliar.estaVacia()) {
            Proceso p = auxiliar.desencolar();
            if (!removido && p.getId() == masCorto.getId()) {
                removido = true;
            } else {
                colaListos.encolar(p);
            }
        }
        return masCorto;
    }

    @Override
    public boolean usaQuantum() {
        return false;
    }

    @Override
    public boolean debeApropiar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        return false; // SJF clásico no apropiativo
    }

    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.SJF;
    }
}