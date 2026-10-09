/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hardware;

/**
 *
 * @author PECA
 */
import edd.ListaEnlazada;
import procesos.Proceso;

public class Cluster {
    private ListaEnlazada nodos;
    private int relojGlobal;

    public Cluster() {
        this.nodos = new ListaEnlazada();
        this.relojGlobal = 0;
    }

    // Registra una nueva computadora físicamente aislada en la red
    public void agregarNodo(Computador computador) {
        if (computador != null) {
            this.nodos.agregar(computador);
        }
    }

    // Intenta despachar el proceso buscando el primer nodo con RAM suficiente (First-Fit)
    public boolean despacharProceso(Proceso p) {
        if (p == null) return false;

        for (int i = 0; i < nodos.getTamano(); i++) {
            Computador nodo = (Computador) nodos.obtener(i);
            
            // Intenta cargar el proceso en el nodo (asigna RAM y lo encola en su Kernel local)
            if (nodo.admitirProceso(p)) {
                System.out.println("Proceso " + p.getId() + " [RAM: " + p.getMemoriaRequerida() + 
                                   "MB] asignado al Nodo " + nodo.getIdComputador());
                return true;
            }
        }

        System.out.println("Admitir proceso " + p.getId() + " falló: Ningún nodo dispone de " + 
                           p.getMemoriaRequerida() + "MB de RAM libres.");
        return false;
    }

    // Avanza un tick en el reloj del cluster y en la CPU de todos los nodos
    public void ejecutarCicloGlobal() {
        this.relojGlobal++;
        
        for (int i = 0; i < nodos.getTamano(); i++) {
            Computador nodo = (Computador) nodos.obtener(i);
            nodo.getKernelLocal().ejecutarCiclo();
        }
    }

    public ListaEnlazada getNodos() {
        return nodos;
    }

    public int getRelojGlobal() {
        return relojGlobal;
    }
    
    
}
