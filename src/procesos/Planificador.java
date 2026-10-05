package procesos;

import edd.Cola;

public class Planificador {

    private Cola<Proceso> colaTrabajos;   
    private Cola<Proceso> colaListos;     
    private Cola<Proceso> colaBloqueados; 
    private Cola<Proceso> colaTerminados; 
    
    private Proceso procesoEnEjecucion;
    private int quantum;

    public Planificador(int quantum) {
        this.colaTrabajos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesoEnEjecucion = null;
        this.quantum = quantum;
    }

    public void crearProceso(Proceso p) {
        p.setEstado(EstadoProceso.NUEVO);
        colaTrabajos.encolar(p);
        System.out.println("[TRABAJOS] Proceso encolado en Trabajos: " + p.getNombre() + " (PID: " + p.getId() + ")");
    }

    public void admitirProcesos() {
        while (!colaTrabajos.estaVacia()) {
            Proceso p = colaTrabajos.desencolar();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
            System.out.println("[LISTO] Proceso admitido en cola de listos: " + p.getNombre());
        }
    }

    public void simularLlamadaAlSistema(Proceso p, String operacion) {
        System.out.println("   [SYSCALL / TRAP] " + p.getNombre() + " solicita: " + operacion);
        p.setModo(ModoEjecucion.NUCLEO);
        System.out.println("   --> [MODO NUCLEO ACTIVADO] Ejecutando rutina del Kernel de forma protegida...");
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

        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = colaListos.desencolar();
            procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
        }

        if (procesoEnEjecucion != null) {
            System.out.println("\n[CPU] Ejecutando: " + procesoEnEjecucion.getNombre() 
                               + " | Modo actual: " + procesoEnEjecucion.getModo()
                               + " | Tiempo restante previo: " + procesoEnEjecucion.getTiempoRestante());

            // Si le quedan 4 unidades y no ha hecho E/S, se bloquea 1 sola vez
            if (procesoEnEjecucion.getTiempoRestante() == 4 && !procesoEnEjecucion.isRealizoIO()) {
                procesoEnEjecucion.setRealizoIO(true);
                simularLlamadaAlSistema(procesoEnEjecucion, "Solicitud Lectura de Disco");
                bloquearProcesoActual("Esperando datos del disco");
                
                if (!colaBloqueados.estaVacia()) {
                    desbloquearProceso();
                }
                return; 
            }

            int tiempoEjecutado = Math.min(procesoEnEjecucion.getTiempoRestante(), quantum);
            procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - tiempoEjecutado);
            procesoEnEjecucion.setPc(procesoEnEjecucion.getPc() + tiempoEjecutado);

            System.out.println("[CPU] Se usaron " + tiempoEjecutado + " unidades de Quantum.");

            if (procesoEnEjecucion.getTiempoRestante() <= 0) {
                procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Proceso " + procesoEnEjecucion.getNombre() + " TERMINÓ su ejecución.");
                procesoEnEjecucion = null;
            } else {
                procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Expiró Quantum. Proceso " + procesoEnEjecucion.getNombre() 
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
        System.out.println("\n================ INICIANDO PLANIFICACIÓN ROUND ROBIN (Quantum = " + quantum + ") ================");
        while (!colaListos.estaVacia() || !colaBloqueados.estaVacia() || procesoEnEjecucion != null) {
            ejecutarCiclo();
        }
        System.out.println("\n================ SIMULACIÓN COMPLETADA ================");
    }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public Cola<Proceso> getColaTrabajos() { return colaTrabajos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
}