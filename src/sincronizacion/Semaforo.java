/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sincronizacion;

/**
 *
 * @author Jabri
 */



import edd.Cola;
import edd.ListaEnlazada;
import procesos.EstadoProceso;
import procesos.Proceso;

public class Semaforo {
    private String nombre;
    private int valor;
    private Cola<Proceso> colaBloqueados;

    public Semaforo(String nombre, int valorInicial) {
        this.nombre = nombre;
        this.valor = valorInicial;
        this.colaBloqueados = new Cola<>();
    }

    /**
     * Operación semWait clásica de Dijkstra/Stallings.
     * @param p Proceso que invoca el wait.
     * @return true si el proceso puede continuar, false si debe bloquearse.
     */
    public synchronized boolean semWait(Proceso p) {
        valor--;
        if (valor < 0) {
            p.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueados.encolar(p);
            return false;
        }
        return true;
    }

    /**
     * Operación semSignal clásica.
     * @return El proceso desbloqueado (si existía alguno esperando), o null.
     */
    public synchronized Proceso semSignal() {
        valor++;
        if (valor <= 0) {
            if (!colaBloqueados.estaVacia()) {
                Proceso despierto = colaBloqueados.desencolar();
                despierto.setEstado(EstadoProceso.LISTO);
                return despierto;
            }
        }
        return null;
    }

    public synchronized int getValor() {
        return valor;
    }

    public String getNombre() {
        return nombre;
    }

    public synchronized Cola<Proceso> getColaBloqueados() {
        return colaBloqueados;
    }

    public synchronized ListaEnlazada<Proceso> obtenerProcesosBloqueados() {
        return colaBloqueados.obtenerListaElementos();
    }
}