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

public class PoliticaEDF implements PoliticaPlanificacion {
    @Override
    public Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos) {
        if (colaListos.estaVacia()) return null;

        Cola<Proceso> auxiliar = new Cola<>();
        Proceso menorDeadline = colaListos.desencolar();
        auxiliar.encolar(menorDeadline);

        while (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            if (p.getDeadline() < menorDeadline.getDeadline()) {
                menorDeadline = p;
            }
            auxiliar.encolar(p);
        }

        boolean removido = false;
        while (!auxiliar.estaVacia()) {
            Proceso p = auxiliar.desencolar();
            if (!removido && p.getId() == menorDeadline.getId()) {
                removido = true;
            } else {
                colaListos.encolar(p);
            }
        }
        return menorDeadline;
    }

    @Override
    public boolean usaQuantum() {
        return false;
    }
}