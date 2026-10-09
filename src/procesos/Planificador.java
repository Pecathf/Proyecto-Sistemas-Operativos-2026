package procesos;

import edd.Cola;
import politicas.PoliticaPlanificacion;
import politicas.PoliticaRoundRobin;
import hardware.Computador;

public class Planificador {

    private Cola<Proceso> colaTrabajos;   
    private Cola<Proceso> colaListos;     
    private Cola<Proceso> colaBloqueados; 
    private Cola<Proceso> colaTerminados; 
    
    private Proceso procesoEnEjecucion;
    private int quantum;
    
    // PATRÓN STRATEGY
    private PoliticaPlanificacion politica;

    public Planificador(int quantum, PoliticaPlanificacion politica) {
        this.colaTrabajos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesoEnEjecucion = null;
        this.quantum = quantum;
        this.politica = politica;
    }

    // Constructor por defecto usando Round Robin
    public Planificador(int quantum) {
        this(quantum, new PoliticaRoundRobin());
    }
    private Computador computador; // Referencia al computador que gestiona la RAM

    public void setComputador(Computador computador) {
    this.computador = computador;
}
    

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
        System.out.println("\n[CONFIG] Política de planificación cambiada a: " + politica.getClass().getSimpleName());
    }

    public void crearProceso(Proceso p) {
        p.setEstado(EstadoProceso.NUEVO);
        colaTrabajos.encolar(p);
        System.out.println("[TRABAJOS] Proceso encolado en Trabajos: " + p.getNombre() + " (PID: " + p.getId() + ")");
    }

    public void admitirProcesos() {
       int tamañoInicial = colaTrabajos.getTamano();

    for (int i = 0; i < tamañoInicial; i++) {
        Proceso p = (Proceso) colaTrabajos.desencolar();

        if (computador != null) {
            // 1. Si supera la RAM MÁXIMA (2000 MB > 1024 MB), no se vuelve a encolar (se descarta)
            if (p.getMemoriaRequerida() > computador.getMemoriaTotal()) {
                System.out.println("[DESCARTO] Proceso " + p.getNombre() + " excede la RAM total de la máquina.");
            } 
            // 2. Si cabe y hay RAM libre, entra a Cola Listos
            else if (computador.admitirProceso(p)) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                System.out.println("[LISTO] Proceso admitido en cola de listos: " + p.getNombre());
            } 
            // 3. Si cabe en el PC pero la RAM está llena por ahora, se reingresa a Trabajos a esperar
            else {
                colaTrabajos.encolar(p);
                System.out.println("[RECHAZADO] Memoria insuficiente por el momento para: " + p.getNombre());
            }
        } else {
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
        }
    } 
    }

    public void simularLlamadaAlSistema(Proceso p, String operacion) {
        System.out.println("   [SYSCALL / TRAP] " + p.getNombre() + " solicita: " + operacion);
        p.setModo(ModoEjecucion.NUCLEO);
        System.out.println("   --> [MODO NUCLEO ACTIVADO] Ejecutando rutina del Kernel...");
        p.setModo(ModoEjecucion.USUARIO);
        System.out.println("   --> Operación finalizada. Retornando a MODO USUARIO.");
    }

    public void bloquearProcesoActual(String razon) {
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueados.encolar(procesoEnEjecucion);
            System.out.println("   [BLOQUEO] Proceso " + procesoEnEjecucion.getNombre() 
                               + " pasa a BLOQUEADO por: " + razon);
            procesoEnEjecucion = null; 
        }
    }

    public void desbloquearProceso() {
        if (!colaBloqueados.estaVacia()) {
            Proceso p = colaBloqueados.desencolar();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
            System.out.println("   [INTERRUPCIÓN E/S] Proceso " + p.getNombre() 
                               + " completó E/S y regresa a LISTOS.");
        }
    }

    public void ejecutarCiclo() {
        if (colaListos.estaVacia() && procesoEnEjecucion == null) {
            if (!colaBloqueados.estaVacia()) {
                System.out.println("\n[CPU INACTIVA] Esperando dispositivos de E/S...");
                desbloquearProceso();
                return;
            } else {
                System.out.println("No hay procesos pendientes.");
                return;
            }
        }

        // Selección del siguiente proceso usando la política (STRATEGY)
        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = politica.seleccionarSiguienteProceso(colaListos);
            if (procesoEnEjecucion != null) {
                procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                
    }
            }

        if (procesoEnEjecucion != null) {
            System.out.println("\n[CPU] Ejecutando: " + procesoEnEjecucion.getNombre() 
                               + " | Modo actual: " + procesoEnEjecucion.getModo()
                               + " | Tiempo restante previo: " + procesoEnEjecucion.getTiempoRestante());

            if (procesoEnEjecucion.getTiempoRestante() == 4 && !procesoEnEjecucion.isRealizoIO()) {
                procesoEnEjecucion.setRealizoIO(true);
                simularLlamadaAlSistema(procesoEnEjecucion, "Solicitud Lectura de Disco");
                bloquearProcesoActual("Esperando datos del disco");
                
                if (!colaBloqueados.estaVacia()) {
                    desbloquearProceso();
                }
                return; 
            }

            // Si la política usa Quantum (RR), se limita por el quantum, de lo contrario ejecuta todo su tiempo
            int tiempoEjecutado = politica.usaQuantum() 
                    ? Math.min(procesoEnEjecucion.getTiempoRestante(), quantum) 
                    : procesoEnEjecucion.getTiempoRestante();

            procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - tiempoEjecutado);
            procesoEnEjecucion.setPc(procesoEnEjecucion.getPc() + tiempoEjecutado);

            System.out.println("[CPU] Se usaron " + tiempoEjecutado + " unidades de tiempo.");

            if (procesoEnEjecucion.getTiempoRestante() <= 0) {
                procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Proceso " + procesoEnEjecucion.getNombre() + " TERMINÓ su ejecución.");
                procesoEnEjecucion = null;
            } else {
                procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Expiró ráfaga. Proceso " + procesoEnEjecucion.getNombre() 
                                   + " reingresa a LISTOS. Tiempo restante: " + procesoEnEjecucion.getTiempoRestante());
                procesoEnEjecucion = null;
            }

            if (!colaBloqueados.estaVacia()) {
                desbloquearProceso();
            }
        }
    }

    public void ejecutarSimulacionCompleta() {
        admitirProcesos();
        System.out.println("\n================ INICIANDO PLANIFICACIÓN (" + politica.getClass().getSimpleName() + ") ================");
        while (!colaListos.estaVacia() || !colaBloqueados.estaVacia() || procesoEnEjecucion != null) {
            ejecutarCiclo();
        }
        System.out.println("\n================ SIMULACIÓN COMPLETADA ================");
    }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public PoliticaPlanificacion getPolitica() { return politica; }
    public Cola<Proceso> getColaTrabajos() { return colaTrabajos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
}