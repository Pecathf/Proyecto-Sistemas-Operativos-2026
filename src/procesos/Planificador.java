package procesos;
/**
 *
 * @author Jabri
 */

import edd.Cola;
import politicas.PoliticaPlanificacion;
import politicas.PoliticaRoundRobin;
import politicas.PoliticaPrioridadApropiativa;
import sincronizacion.Semaforo;

public class Planificador {

    private Cola<Proceso> colaTrabajos;   
    private Cola<Proceso> colaListos;     
    private Cola<Proceso> colaBloqueados; 
    private Cola<Proceso> colaLatenciaRed; // Para el retardo de buffers remotos
    private Cola<Proceso> colaTerminados; 
    
    private Proceso procesoEnEjecucion;
    private Proceso ultimoTerminado;
    private int quantum;
    private int quantumActual;
    private PoliticaPlanificacion politica;
    
    // Contadores para Métricas
    private int ciclosCPUActiva = 0;
    private int ciclosTotal = 0;

    public Planificador(int quantum, PoliticaPlanificacion politica) {
        this.colaTrabajos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaLatenciaRed = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesoEnEjecucion = null;
        this.ultimoTerminado = null;
        this.quantum = quantum;
        this.quantumActual = 0;
        this.politica = politica;
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
        if(procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null;
        }
    }

    public void crearProceso(Proceso p) {
        p.setEstado(EstadoProceso.LISTO);
        colaListos.encolar(p);
    }

    public void bloquearProcesoActual(String razon) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueados.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null; 
        }
    }
    
    public void aplicarLatenciaRedProcesoActual(int ciclos) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
            procesoEnEjecucion.setCiclosLatenciaRestantes(ciclos);
            colaLatenciaRed.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null;
        }
    }

    public void desbloquearProcesoEspecifico(Proceso p) {
        Cola<Proceso> aux = new Cola<>();
        boolean encontrado = false;
        while (!colaBloqueados.estaVacia()) {
            Proceso actual = colaBloqueados.desencolar();
            if (actual.getId() == p.getId() && !encontrado) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                encontrado = true;
            } else {
                aux.encolar(actual);
            }
        }
        while (!aux.estaVacia()) {
            colaBloqueados.encolar(aux.desencolar());
        }
    }

    private void procesarLatenciaRed() {
        Cola<Proceso> aux = new Cola<>();
        while(!colaLatenciaRed.estaVacia()) {
            Proceso p = colaLatenciaRed.desencolar();
            p.setCiclosLatenciaRestantes(p.getCiclosLatenciaRestantes() - 1);
            if (p.getCiclosLatenciaRestantes() <= 0) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
            } else {
                aux.encolar(p);
            }
        }
        while(!aux.estaVacia()) colaLatenciaRed.encolar(aux.desencolar());
    }

    public void ejecutarCiclo(int cicloActual) {
        ciclosTotal++;
        procesarLatenciaRed();

        if (politica instanceof politicas.PoliticaEDF) {
            Cola<Proceso> aux = new Cola<>();
            while (!colaListos.estaVacia()) {
                Proceso p = colaListos.desencolar();
                p.setDeadline(p.getDeadline() - 1);
                if (p.getDeadline() <= 0) {
                    p.setEstado(EstadoProceso.TERMINADO);
                    p.setCicloFinalizacion(cicloActual);
                    colaTerminados.encolar(p);
                    ultimoTerminado = p;
                } else {
                    aux.encolar(p);
                }
            }
            while (!aux.estaVacia()) colaListos.encolar(aux.desencolar());
        }

        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = politica.seleccionarSiguienteProceso(colaListos);
            if (procesoEnEjecucion != null) {
                procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                quantumActual = 0;
                // Métrica: Tiempo de respuesta (solo la primera vez)
                if (procesoEnEjecucion.getCicloPrimeraEjecucion() == -1) {
                    procesoEnEjecucion.setCicloPrimeraEjecucion(cicloActual);
                }
            }
        }

        if (procesoEnEjecucion != null) {
            ciclosCPUActiva++; // Métrica: % Uso de CPU
            
            if(politica instanceof politicas.PoliticaEDF) {
                procesoEnEjecucion.setDeadline(procesoEnEjecucion.getDeadline() - 1);
            }

            procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);
            procesoEnEjecucion.setPc(procesoEnEjecucion.getPc() + 1);
            quantumActual++;

            if (procesoEnEjecucion.getTiempoRestante() <= 0 || procesoEnEjecucion.getDeadline() <= 0) {
                procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
                procesoEnEjecucion.setCicloFinalizacion(cicloActual); // Métrica: Turnaround
                colaTerminados.encolar(procesoEnEjecucion);
                ultimoTerminado = procesoEnEjecucion; 
                procesoEnEjecucion = null;
            } else if (politica.usaQuantum() && quantumActual >= quantum) {
                procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(procesoEnEjecucion);
                procesoEnEjecucion = null;
            } else if (politica instanceof PoliticaPrioridadApropiativa) {
                Proceso masPrioritario = politica.seleccionarSiguienteProceso(colaListos);
                if (masPrioritario != null) {
                    if (masPrioritario.getPrioridad() < procesoEnEjecucion.getPrioridad()) {
                        procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                        colaListos.encolar(procesoEnEjecucion);
                        procesoEnEjecucion = masPrioritario;
                        procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                        if (procesoEnEjecucion.getCicloPrimeraEjecucion() == -1) {
                            procesoEnEjecucion.setCicloPrimeraEjecucion(cicloActual);
                        }
                    } else {
                        colaListos.encolar(masPrioritario);
                    }
                }
            }
        }
    }

    public Proceso getUltimoTerminado() { return ultimoTerminado; }
    public void limpiarUltimoTerminado() { this.ultimoTerminado = null; }
    public double getUsoCPU() { return ciclosTotal == 0 ? 0 : ((double) ciclosCPUActiva / ciclosTotal) * 100; }
    public int getProcesosCompletados() { return colaTerminados.getTamano(); }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public PoliticaPlanificacion getPolitica() { return politica; }
    public Cola<Proceso> getColaTrabajos() { return colaTrabajos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaLatenciaRed() { return colaLatenciaRed; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
}