package procesos;

import edd.Cola;
import politicas.PoliticaPlanificacion;
import politicas.PoliticaRoundRobin;
import politicas.PoliticaPrioridadApropiativa;
import politicas.PoliticaEDF;

public class Planificador {

    private Cola<Proceso> colaTrabajos;   
    private Cola<Proceso> colaListos;     
    private Cola<Proceso> colaBloqueados; 
    private Cola<Proceso> colaTerminados; 
    
    private Proceso procesoEnEjecucion;
    private Proceso ultimoTerminado;
    private int quantum;
    private int quantumActual;
    
    private PoliticaPlanificacion politica;

    public Planificador(int quantum, PoliticaPlanificacion politica) {
        this.colaTrabajos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesoEnEjecucion = null;
        this.ultimoTerminado = null;
        this.quantum = quantum;
        this.quantumActual = 0;
        this.politica = politica;
    }

    public Planificador(int quantum) {
        this(quantum, new PoliticaRoundRobin());
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
        // Si hay un proceso en ejecución, se devuelve a listos para reevaluar con la nueva política
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

    public void admitirProcesos() {
        while (!colaTrabajos.estaVacia()) {
            Proceso p = colaTrabajos.desencolar();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
        }
    }

    public void simularLlamadaAlSistema(Proceso p, String operacion) {
        p.setModo(ModoEjecucion.NUCLEO);
        p.setModo(ModoEjecucion.USUARIO);
    }

    public void bloquearProcesoActual(String razon) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueados.encolar(procesoEnEjecucion);
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

    public void ejecutarCiclo() {
        // 1. Lógica específica para EDF: Reducir deadline de todos los procesos en LISTO
        if (politica instanceof PoliticaEDF) {
            Cola<Proceso> aux = new Cola<>();
            while (!colaListos.estaVacia()) {
                Proceso p = colaListos.desencolar();
                p.setDeadline(p.getDeadline() - 1);
                if (p.getDeadline() <= 0) {
                    p.setEstado(EstadoProceso.TERMINADO);
                    colaTerminados.encolar(p);
                    ultimoTerminado = p;
                } else {
                    aux.encolar(p);
                }
            }
            while (!aux.estaVacia()) {
                colaListos.encolar(aux.desencolar());
            }
        }

        // 2. Selección del siguiente proceso a ejecutar
        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = politica.seleccionarSiguienteProceso(colaListos);
            if (procesoEnEjecucion != null) {
                procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                quantumActual = 0;
            }
        }

        // 3. Ejecución del proceso actual
        if (procesoEnEjecucion != null) {
            if(politica instanceof PoliticaEDF) {
                procesoEnEjecucion.setDeadline(procesoEnEjecucion.getDeadline() - 1);
            }

            procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);
            procesoEnEjecucion.setPc(procesoEnEjecucion.getPc() + 1);
            quantumActual++;

            // Verificar condiciones de salida o desalojo
            if (procesoEnEjecucion.getTiempoRestante() <= 0 || procesoEnEjecucion.getDeadline() <= 0) {
                procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoEnEjecucion);
                ultimoTerminado = procesoEnEjecucion; 
                procesoEnEjecucion = null;
            } 
            else if (politica.usaQuantum() && quantumActual >= quantum) {
                procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(procesoEnEjecucion);
                procesoEnEjecucion = null;
            } 
            else if (politica instanceof PoliticaPrioridadApropiativa) {
                Proceso masPrioritario = politica.seleccionarSiguienteProceso(colaListos);
                if (masPrioritario != null) {
                    // Se asume que menor número = mayor prioridad
                    if (masPrioritario.getPrioridad() < procesoEnEjecucion.getPrioridad()) {
                        procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                        colaListos.encolar(procesoEnEjecucion);
                        procesoEnEjecucion = masPrioritario;
                        procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                    } else {
                        colaListos.encolar(masPrioritario);
                    }
                }
            }
        }
    }

    public Proceso getUltimoTerminado() { return ultimoTerminado; }
    public void limpiarUltimoTerminado() { this.ultimoTerminado = null; }
    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public PoliticaPlanificacion getPolitica() { return politica; }
    public Cola<Proceso> getColaTrabajos() { return colaTrabajos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
}