/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author Jabri
 */


import procesos.Planificador;
import procesos.Proceso;
import politicas.PoliticaPlanificacion;
import edd.Cola;

public class Computador extends Thread {
    private String nombre;
    private int ramTotal;
    private int ramUsada;
    private Planificador planificador;
    private Cola<Proceso> colaNuevos;
    private RelojGlobal reloj;
    private boolean activo;

    public Computador(String nombre, int ramTotal, int quantum, PoliticaPlanificacion politicaInicial) {
        this.nombre = nombre;
        this.ramTotal = ramTotal;
        this.ramUsada = 0;
        this.planificador = new Planificador(quantum, politicaInicial);
        this.colaNuevos = new Cola<>();
        this.activo = true;
    }

    public void setReloj(RelojGlobal reloj) {
        this.reloj = reloj;
    }

    public synchronized void recibirProceso(Proceso p, int cicloActual) {
        p.setCicloLlegada(cicloActual);
        colaNuevos.encolar(p);
    }

    private synchronized void admitirProcesosSiHayMemoria() {
        Cola<Proceso> aux = new Cola<>();
        while (!colaNuevos.estaVacia()) {
            Proceso p = colaNuevos.desencolar();
            if (ramUsada + p.getMemoriaRequerida() <= ramTotal) {
                ramUsada += p.getMemoriaRequerida();
                planificador.crearProceso(p);
            } else {
                aux.encolar(p);
            }
        }
        while (!aux.estaVacia()) {
            colaNuevos.encolar(aux.desencolar());
        }
    }

    public synchronized void liberarMemoria(int cantidad) {
        this.ramUsada -= cantidad;
        if (this.ramUsada < 0) this.ramUsada = 0;
    }

    @Override
    public void run() {
        while (activo) {
            try {
                // 1. Esperar la señal del Reloj Global
                reloj.esperarInicioCiclo();
                
                int cicloActual = reloj.getCicloActual();
                
                // 2. Ejecutar la lógica del computador (AHORA SÍ EN PARALELO)
                admitirProcesosSiHayMemoria();
                planificador.ejecutarCiclo(cicloActual);
                
                if (planificador.getUltimoTerminado() != null) {
                    liberarMemoria(planificador.getUltimoTerminado().getMemoriaRequerida());
                    planificador.limpiarUltimoTerminado();
                }
                
                // 3. Avisar al Reloj que este computador ya terminó su trabajo por este ciclo
                reloj.notificarFinCiclo();
                
            } catch (InterruptedException e) {
                System.out.println(nombre + " fue interrumpido.");
            }
        }
    }
    
    public void apagar() { this.activo = false; }

    public String getNombre() { return nombre; }
    public Planificador getPlanificador() { return planificador; }
    public int getRamLibre() { return ramTotal - ramUsada; }
    public int getRamUsada() { return ramUsada; }
    public int getRamTotal() { return ramTotal; }
}