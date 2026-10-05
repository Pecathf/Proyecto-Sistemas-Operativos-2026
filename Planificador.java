package procesos;

import edd.Cola;

public class Planificador {

    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaTerminados;
    private Proceso procesoEnEjecucion;
    private int quantum;

    public Planificador(int quantum) {
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesoEnEjecucion = null;
        this.quantum = quantum;
    }

    // Registrar e ingresar un proceso a la cola de listos
    public void agregarProceso(Proceso p) {
        p.setEstado(EstadoProceso.LISTO);
        colaListos.encolar(p);
        System.out.println("Proceso encolado en LISTO: " + p.getNombre() + " (PID: " + p.getId() + ")");
    }

    // Ejecuta un ciclo de CPU procesando según el Quantum
    public void ejecutarCiclo() {
        if (colaListos.estaVacia() && procesoEnEjecucion == null) {
            System.out.println("No hay procesos pendientes en la cola de listos.");
            return;
        }

        // Si la CPU está libre, se despacha el siguiente proceso
        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = (Proceso) colaListos.desencolar();
            procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
        }

        if (procesoEnEjecucion != null) {
            System.out.println("\n[CPU] Ejecutando: " + procesoEnEjecucion.getNombre() 
                               + " | Tiempo restante previo: " + procesoEnEjecucion.getTiempoRestante());

            // Determinamos cuánto tiempo ejecutará en esta ráfaga
            int tiempoEjecutado = Math.min(procesoEnEjecucion.getTiempoRestante(), quantum);
            procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - tiempoEjecutado);

            // Simulación básica de avance de PC
            procesoEnEjecucion.setPc(procesoEnEjecucion.getPc() + tiempoEjecutado);

            System.out.println("[CPU] Se usaron " + tiempoEjecutado + " unidades de Quantum.");

            // Evaluación de salida
            if (procesoEnEjecucion.getTiempoRestante() <= 0) {
                procesoEnEjecucion.setEstado(EstadoProceso.TERMINADO);
                colaTerminados.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Proceso " + procesoEnEjecucion.getNombre() + " TERMINÓ su ejecución.");
                procesoEnEjecucion = null; // Liberar CPU
            } else {
                // Preempción: Expiró el quantum y el proceso reingresa a la cola de listos
                procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(procesoEnEjecucion);
                System.out.println("[ESTADO] Expiró Quantum. Proceso " + procesoEnEjecucion.getNombre() 
                                   + " reingresa a LISTOS. Tiempo restante: " + procesoEnEjecucion.getTiempoRestante());
                procesoEnEjecucion = null; // Liberar CPU para alternar
            }
        }
    }

    // Ejecuta todos los procesos de la cola de listos hasta completar todos los trabajos
    public void ejecutarSimulacionCompleta() {
        System.out.println("\n================ INICIANDO PLANIFICACIÓN ROUND ROBIN (Quantum = " + quantum + ") ================");
        while (!colaListos.estaVacia() || procesoEnEjecucion != null) {
            ejecutarCiclo();
        }
        System.out.println("\n================ SIMULACIÓN COMPLETADA ================");
    }

    // Getters y Setters
    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public Cola<Proceso> getColaListos() {
        return colaListos;
    }

    public Cola<Proceso> getColaBloqueados() {
        return colaBloqueados;
    }

    public Cola<Proceso> getColaTerminados() {
        return colaTerminados;
    }

    public Proceso getProcesoEnEjecucion() {
        return procesoEnEjecucion;
    }
}