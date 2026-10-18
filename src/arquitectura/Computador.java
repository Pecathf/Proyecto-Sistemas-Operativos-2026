/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;
import procesos.Planificador;
import procesos.Proceso;
import politicas.PoliticaPlanificacion;
import edd.Cola;
/**
 *
 * @author Jabri
 */

public class Computador implements ReaccionableReloj {
    private String nombre;
    private int ramTotal;
    private int ramUsada;
    private Planificador planificador;
    private Cola<Proceso> colaNuevos;

    public Computador(String nombre, int ramTotal, int quantum, PoliticaPlanificacion politicaInicial) {
        this.nombre = nombre;
        this.ramTotal = ramTotal;
        this.ramUsada = 0;
        this.planificador = new Planificador(quantum, politicaInicial);
        this.colaNuevos = new Cola<>();
    }

    public void recibirProceso(Proceso p, int cicloActual) {
        p.setCicloLlegada(cicloActual); // Guardamos métrica de llegada
        colaNuevos.encolar(p);
    }

    private void admitirProcesosSiHayMemoria() {
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

    public void liberarMemoria(int cantidad) {
        this.ramUsada -= cantidad;
        if (this.ramUsada < 0) this.ramUsada = 0;
    }

    @Override
    public void tick(int cicloActual) {
        admitirProcesosSiHayMemoria();
        planificador.ejecutarCiclo(cicloActual);
        
        if (planificador.getUltimoTerminado() != null) {
            liberarMemoria(planificador.getUltimoTerminado().getMemoriaRequerida());
            planificador.limpiarUltimoTerminado();
        }
    }

    public String getNombre() { return nombre; }
    public Planificador getPlanificador() { return planificador; }
    public int getRamLibre() { return ramTotal - ramUsada; }
    public int getRamUsada() { return ramUsada; }
    public int getRamTotal() { return ramTotal; }
}