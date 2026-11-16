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
import java.util.concurrent.Semaphore;

public class RelojGlobal extends Thread {
    private ListaEnlazada<Computador> computadores;
    private int cicloActual;
    private volatile int duracionCicloMs;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    
    // Semáforos para coordinar los hilos de los computadores en barrera paralela
    private Semaphore semaforoInicioCiclo;
    private Semaphore semaforoFinCiclo;

    public RelojGlobal(int duracionCicloMs) {
        this.computadores = new ListaEnlazada<>();
        this.cicloActual = 0;
        this.duracionCicloMs = Math.max(20, duracionCicloMs);
        this.enEjecucion = true;
        this.pausado = false;
    }

    public synchronized void agregarComputador(Computador c) {
        computadores.agregar(c);
    }

    public void iniciarSimulacion() {
        int numComputadores = computadores.getTamano();
        semaforoInicioCiclo = new Semaphore(0);
        semaforoFinCiclo = new Semaphore(0);
        
        // Arrancar los hilos de cada computador
        for (int i = 0; i < numComputadores; i++) {
            Computador c = computadores.obtener(i);
            c.setReloj(this);
            c.start();
        }
        
        this.start();
    }

    @Override
    public void run() {
        while (enEjecucion) {
            try {
                if (pausado) {
                    Thread.sleep(100);
                    continue;
                }

                Thread.sleep(duracionCicloMs);
                
                if (pausado) {
                    continue;
                }

                int numComputadores = computadores.getTamano();
                if (numComputadores == 0) continue;

                cicloActual++;
                
                // 1. Dar luz verde a todos los computadores para que ejecuten su ciclo en paralelo
                semaforoInicioCiclo.release(numComputadores);
                
                // 2. Esperar a que TODOS los computadores terminen su ciclo
                semaforoFinCiclo.acquire(numComputadores);
                
            } catch (InterruptedException e) {
                System.out.println("Reloj Global interrumpido.");
            }
        }
    }

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    public void detener() { 
        this.enEjecucion = false; 
    }
    
    public int getCicloActual() { 
        return cicloActual; 
    }

    public int getDuracionCicloMs() { 
        return duracionCicloMs; 
    }

    public void setDuracionCicloMs(int duracionCicloMs) { 
        this.duracionCicloMs = (duracionCicloMs > 20) ? duracionCicloMs : 20; 
    }
    
    // Métodos usados por los Computadores para sincronizarse con el reloj
    public void esperarInicioCiclo() throws InterruptedException {
        semaforoInicioCiclo.acquire();
    }
    
    public void notificarFinCiclo() {
        semaforoFinCiclo.release();
    }
}