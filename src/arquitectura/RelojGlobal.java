/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author PECA
 */
import edd.ListaEnlazada;
public class RelojGlobal {
    private ListaEnlazada<ReaccionableReloj> componentes;
    private int cicloActual;
    private int duracionCicloMs;
    private boolean enEjecucion;

    public RelojGlobal(int duracionCicloMs) {
        this.componentes = new ListaEnlazada<>();
        this.cicloActual = 0;
        this.duracionCicloMs = duracionCicloMs;
        this.enEjecucion = true;
    }

    public void agregarComponente(ReaccionableReloj componente) {
        componentes.agregar(componente);
    }

    public void run() {
        while (enEjecucion) {
            try {
                Thread.sleep(duracionCicloMs);
                cicloActual++;
                for (int i = 0; i < componentes.getTamano(); i++) {
                    componentes.obtener(i).tick(cicloActual);
                }
            } catch (InterruptedException e) {
                System.out.println("Reloj Global interrumpido.");
            }
        }
    }

    public void detener() { enEjecucion = false; }
    public int getCicloActual() { return cicloActual; }
    
}
