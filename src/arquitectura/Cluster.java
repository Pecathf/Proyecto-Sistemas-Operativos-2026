/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;
import edd.ListaEnlazada;
import procesos.Proceso;
/**
 *
 * @author Jabri
 */

public class Cluster {
    private ListaEnlazada<Computador> computadores;

    public Cluster() {
        this.computadores = new ListaEnlazada<>();
    }

    public void agregarComputador(Computador c) {
        computadores.agregar(c);
    }

    // Asignación Manual: El usuario elige en la GUI (ej: 0 para Computador 1, 1 para Computador 2)
    public void asignarProcesoManual(Proceso p, int indiceComputador, int cicloActual) {
        if (indiceComputador >= 0 && indiceComputador < computadores.getTamano()) {
            computadores.obtener(indiceComputador).recibirProceso(p, cicloActual);
        }
    }

    // Asignación Automática (Balanceo de carga por Memoria RAM disponible)
    public void asignarProcesoAutomatico(Proceso p, int cicloActual) {
        if (computadores.estaVacia()) return;

        Computador mejorOpcion = computadores.obtener(0);
        for (int i = 1; i < computadores.getTamano(); i++) {
            Computador actual = computadores.obtener(i);
            // Elige el computador con más RAM libre en ese momento
            if (actual.getRamLibre() > mejorOpcion.getRamLibre()) {
                mejorOpcion = actual;
            }
        }
        mejorOpcion.recibirProceso(p, cicloActual);
    }

    public Computador getComputador(int indice) {
        return computadores.obtener(indice);
    }
    
    public int getCantidadComputadores() {
        return computadores.getTamano();
    }
}