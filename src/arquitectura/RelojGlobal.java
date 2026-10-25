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
    private int duracionCicloMs;
    private boolean enEjecucion;
    
    // Semáforos para coordinar los hilos de los computadores
    private Semaphore semaforoInicioCiclo;
    private Semaphore semaforoFinCiclo;

    public RelojGlobal(int duracionCicloMs) {
        this.computadores = new ListaEnlazada<>();
        this.cicloActual = 0;
        this.duracionCicloMs = duracionCicloMs;
        this.enEjecucion = true;
    }

    public void agregarComputador(Computador c) {
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
            c.start(); // Inicia el Thread del Computador
        }
        
        this.start(); // Inicia el Thread del Reloj
    }

    @Override
    public void run() {
        int numComputadores = computadores.getTamano();
        while (enEjecucion) {
            try {
                Thread.sleep(duracionCicloMs);
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

    public void detener() { 
        enEjecucion = false; 
    }
    
    public int getCicloActual() { 
        return cicloActual; 
    }
    
    // Métodos usados por los Computadores para sincronizarse con el reloj
    public void esperarInicioCiclo() throws InterruptedException {
        semaforoInicioCiclo.acquire();
    }
    
    public void notificarFinCiclo() {
        semaforoFinCiclo.release();
    }
}