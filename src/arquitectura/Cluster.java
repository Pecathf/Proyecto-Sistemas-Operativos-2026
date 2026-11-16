/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author Jabri
 */
import edd.ListaEnlazada;
import procesos.Proceso;
import sincronizacion.Buffer;
import utilidades.RegistroEventos;
public class Cluster {
    private ListaEnlazada<Computador> computadores;
    private ListaEnlazada<Buffer> buffers;
    private int latenciaRedCiclos;
    private RegistroEventos logger;

    public Cluster() {
        this.computadores = new ListaEnlazada<>();
        this.buffers = new ListaEnlazada<>();
        this.latenciaRedCiclos = 2; // Default 2 ciclos
        this.logger = null;
    }

    public void setLogger(RegistroEventos logger) {
        this.logger = logger;
    }

    public void log(String mensaje) {
        if (logger != null) {
            logger.registrar(mensaje);
        }
    }

    public synchronized void agregarComputador(Computador c) {
        c.setCluster(this);
        computadores.agregar(c);
    }

    public synchronized void agregarBuffer(Buffer b) {
        buffers.agregar(b);
        // Descontar RAM en el computador anfitrión si existe
        Computador anfitrion = buscarComputadorPorNombre(b.getComputadorAnfitrion());
        if (anfitrion != null) {
            anfitrion.ocuparMemoriaBuffer(b.getMemoriaOcupadaMb());
        }
        log("Buffer '" + b.getId() + "' creado en " + b.getComputadorAnfitrion() + " (Capacidad: " + b.getCapacidad()
                + ", RAM: " + b.getMemoriaOcupadaMb() + " MB)");
    }

    public synchronized Buffer buscarBuffer(String id) {
        for (int i = 0; i < buffers.getTamano(); i++) {
            Buffer b = buffers.obtener(i);
            if (b.getId().equalsIgnoreCase(id)) {
                return b;
            }
        }
        return null;
    }

    public synchronized Computador buscarComputadorPorNombre(String nombre) {
        for (int i = 0; i < computadores.getTamano(); i++) {
            Computador c = computadores.obtener(i);
            if (c.getNombre().equalsIgnoreCase(nombre)) {
                return c;
            }
        }
        return null;
    }

    public synchronized void asignarProcesoManual(Proceso p, int indiceComputador, int cicloActual) {
        if (indiceComputador >= 0 && indiceComputador < computadores.getTamano()) {
            Computador c = computadores.obtener(indiceComputador);
            p.setComputadorAsignado(c.getNombre());
            c.recibirProceso(p, cicloActual);
            log("Proceso " + p.getNombre() + " asignado manualmente a " + c.getNombre());
        }
    }

    public synchronized void asignarProcesoAutomatico(Proceso p, int cicloActual) {
        if (computadores.estaVacia())
            return;

        Computador mejorOpcion = computadores.obtener(0);
        for (int i = 1; i < computadores.getTamano(); i++) {
            Computador actual = computadores.obtener(i);
            // Criterio de balanceo de carga: mayor RAM libre disponible
            if (actual.getRamLibre() > mejorOpcion.getRamLibre()) {
                mejorOpcion = actual;
            }
        }
        p.setComputadorAsignado(mejorOpcion.getNombre());
        mejorOpcion.recibirProceso(p, cicloActual);
        log("Proceso " + p.getNombre() + " asignado automáticamente a " + mejorOpcion.getNombre() + " (RAM Libre: "
                + mejorOpcion.getRamLibre() + " MB)");
    }

    // --- Getters ---
    public synchronized Computador getComputador(int indice) {
        return computadores.obtener(indice);
    }

    public synchronized int getCantidadComputadores() {
        return computadores.getTamano();
    }

    public synchronized ListaEnlazada<Computador> getComputadores() {
        return computadores.clonar();
    }

    public synchronized ListaEnlazada<Buffer> getBuffers() {
        return buffers.clonar();
    }

    public int getLatenciaRedCiclos() {
        return latenciaRedCiclos;
    }

    public void setLatenciaRedCiclos(int latenciaRedCiclos) {
        this.latenciaRedCiclos = latenciaRedCiclos;
    }

    // --- Métricas Globales del Clúster ---
    public synchronized double getUsoCPUGlobal() {
        if (computadores.estaVacia())
            return 0.0;
        double sumaUso = 0;
        for (int i = 0; i < computadores.getTamano(); i++) {
            sumaUso += computadores.obtener(i).getCpu().getPorcentajeUso();
        }
        return sumaUso / computadores.getTamano();
    }

    public synchronized double getThroughputGlobal(int cicloActual) {
        if (cicloActual <= 0)
            return 0.0;
        int totalTerminados = 0;
        for (int i = 0; i < computadores.getTamano(); i++) {
            totalTerminados += computadores.obtener(i).getPlanificador().getColaTerminados().getTamano();
        }
        return ((double) totalTerminados / cicloActual) * 100.0; // Procesos por cada 100 ciclos
    }

    public synchronized double getTiempoRespuestaPromedioGlobal() {
        double sumaRespuestas = 0;
        int totalProcesosConRespuesta = 0;

        for (int i = 0; i < computadores.getTamano(); i++) {
            Computador c = computadores.obtener(i);
            ListaEnlazada<Proceso> listos = c.getPlanificador().getColaListos().obtenerListaElementos();
            ListaEnlazada<Proceso> term = c.getPlanificador().getColaTerminados().obtenerListaElementos();

            Proceso ejec = c.getPlanificador().getProcesoEnEjecucion();
            if (ejec != null && ejec.getCicloPrimeraEjecucion() != -1) {
                sumaRespuestas += (ejec.getCicloPrimeraEjecucion() - ejec.getCicloLlegada());
                totalProcesosConRespuesta++;
            }

            for (int j = 0; j < listos.getTamano(); j++) {
                Proceso p = listos.obtener(j);
                if (p.getCicloPrimeraEjecucion() != -1) {
                    sumaRespuestas += (p.getCicloPrimeraEjecucion() - p.getCicloLlegada());
                    totalProcesosConRespuesta++;
                }
            }

            for (int j = 0; j < term.getTamano(); j++) {
                Proceso p = term.obtener(j);
                if (p.getCicloPrimeraEjecucion() != -1) {
                    sumaRespuestas += (p.getCicloPrimeraEjecucion() - p.getCicloLlegada());
                    totalProcesosConRespuesta++;
                }
            }
        }

        return (totalProcesosConRespuesta == 0) ? 0.0 : (sumaRespuestas / totalProcesosConRespuesta);
    }

    public synchronized double getTasaCumplimientoDeadlinesGlobal() {
        int totalProcesos = 0;
        int exitosos = 0;

        for (int i = 0; i < computadores.getTamano(); i++) {
            ListaEnlazada<Proceso> term = computadores.obtener(i).getPlanificador().getColaTerminados()
                    .obtenerListaElementos();
            for (int j = 0; j < term.getTamano(); j++) {
                totalProcesos++;
                if (term.obtener(j).isCumplioDeadline()) {
                    exitosos++;
                }
            }
        }

        return (totalProcesos == 0) ? 100.0 : ((double) exitosos / totalProcesos) * 100.0;
    }

    public synchronized double getIndiceEquidadGlobal() {
        // Índice de equidad de Jain sobre tiempos de espera
        double suma = 0;
        double sumaCuadrados = 0;
        int n = 0;

        for (int i = 0; i < computadores.getTamano(); i++) {
            ListaEnlazada<Proceso> term = computadores.obtener(i).getPlanificador().getColaTerminados()
                    .obtenerListaElementos();
            for (int j = 0; j < term.getTamano(); j++) {
                int espera = term.obtener(j).getTiempoEsperaListo();
                suma += espera;
                sumaCuadrados += (espera * espera);
                n++;
            }
        }

        if (n <= 1 || sumaCuadrados == 0)
            return 1.0; // Perfectamente equitativo
        return (suma * suma) / (n * sumaCuadrados);
    }

    public synchronized double getTiempoPromedioBloqueadoSem() {
        int totalProcesos = 0;
        double sumaTiempoBloq = 0;

        for (int i = 0; i < computadores.getTamano(); i++) {
            ListaEnlazada<Proceso> term = computadores.obtener(i).getPlanificador().getColaTerminados()
                    .obtenerListaElementos();
            for (int j = 0; j < term.getTamano(); j++) {
                Proceso p = term.obtener(j);
                if (p.getTipo() == procesos.TipoProceso.PRODUCTOR || p.getTipo() == procesos.TipoProceso.CONSUMIDOR) {
                    sumaTiempoBloq += p.getTiempoBloqueadoSem();
                    totalProcesos++;
                }
            }
        }
        return (totalProcesos == 0) ? 0.0 : (sumaTiempoBloq / totalProcesos);
    }
    
}