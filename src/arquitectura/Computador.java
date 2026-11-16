/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author Jabri
 */


import edd.Cola;
import edd.ListaEnlazada;
import politicas.PoliticaPlanificacion;
import procesos.Planificador;
import procesos.Proceso;

public class Computador extends Thread implements ComponenteReloj {
    private String nombre;
    private int ramTotal;
    private int ramUsada;
    private int ramOcupadaBuffers;

    private CPU cpu;
    private Planificador planificador;
    private Cola<Proceso> colaNuevos;
    private RelojGlobal reloj;
    private Cluster cluster;
    private boolean activo;

    public Computador(String nombre, int ramTotal, int quantum, PoliticaPlanificacion politicaInicial) {
        this.nombre = nombre;
        this.ramTotal = ramTotal;
        this.ramUsada = 0;
        this.ramOcupadaBuffers = 0;

        this.cpu = new CPU();
        this.planificador = new Planificador(quantum, politicaInicial);
        this.planificador.setComputador(this);
        this.colaNuevos = new Cola<>();
        this.activo = true;
    }

    public void setReloj(RelojGlobal reloj) {
        this.reloj = reloj;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
        this.planificador.setCluster(cluster);
    }

    public synchronized void recibirProceso(Proceso p, int cicloActual) {
        p.setCicloLlegada(cicloActual);
        p.setComputadorAsignado(this.nombre);
        colaNuevos.encolar(p);
    }

    public synchronized void admitirProcesosSiHayMemoria() {
        Cola<Proceso> aux = new Cola<>();
        while (!colaNuevos.estaVacia()) {
            Proceso p = colaNuevos.desencolar();
            if (ramUsada + p.getMemoriaRequerida() <= ramTotal) {
                ramUsada += p.getMemoriaRequerida();
                planificador.admitirProcesoListo(p);
                if (cluster != null) {
                    cluster.log("Memoria suficiente en " + nombre + ". Proceso " + p.getNombre() + " admitido a Listo (RAM usada: " + ramUsada + "/" + ramTotal + " MB)");
                }
            } else {
                aux.encolar(p);
            }
        }
        while (!aux.estaVacia()) {
            colaNuevos.encolar(aux.desencolar());
        }
    }

    public synchronized void ocuparMemoriaBuffer(int cantidadMb) {
        this.ramOcupadaBuffers += cantidadMb;
        this.ramUsada += cantidadMb;
    }

    public synchronized void liberarMemoria(int cantidad) {
        this.ramUsada -= cantidad;
        if (this.ramUsada < ramOcupadaBuffers) {
            this.ramUsada = ramOcupadaBuffers;
        }
    }

    @Override
    public void ejecutarCiclo(int cicloActual) {
        planificador.ejecutarCiclo(cicloActual);
    }

    @Override
    public void run() {
        while (activo) {
            try {
                // 1. Esperar la señal del Reloj Global
                reloj.esperarInicioCiclo();
                
                int cicloActual = reloj.getCicloActual();
                
                try {
                    // 2. Intentar admitir procesos de la cola de nuevos a la cola de listos si hay RAM
                    admitirProcesosSiHayMemoria();

                    // 3. Ejecutar el ciclo en los componentes (CPU y Planificador)
                    ejecutarCiclo(cicloActual);
                    
                    // 4. Liberar memoria de todos los procesos terminados en este ciclo
                    ListaEnlazada<Proceso> terminados = planificador.extraerProcesosTerminadosEnCiclo();
                    for (int i = 0; i < terminados.getTamano(); i++) {
                        liberarMemoria(terminados.obtener(i).getMemoriaRequerida());
                    }

                    // 5. Tras liberar memoria, revisar de inmediato si se puede admitir otro proceso
                    if (!terminados.estaVacia()) {
                        admitirProcesosSiHayMemoria();
                    }
                } catch (Throwable ex) {
                    System.err.println("Error en ciclo de " + nombre + ": " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    // 6. Avisar al Reloj SIEMPRE para evitar interbloqueo del clúster
                    reloj.notificarFinCiclo();
                }
                
            } catch (InterruptedException e) {
                System.out.println(nombre + " fue interrumpido.");
            }
        }
    }

    public void apagar() { this.activo = false; }

    public String getNombre() { return nombre; }
    public CPU getCpu() { return cpu; }
    public Planificador getPlanificador() { return planificador; }
    public Cola<Proceso> getColaNuevos() { return colaNuevos; }
    public int getRamLibre() { return ramTotal - ramUsada; }
    public int getRamUsada() { return ramUsada; }
    public int getRamTotal() { return ramTotal; }
    public int getRamOcupadaBuffers() { return ramOcupadaBuffers; }
}