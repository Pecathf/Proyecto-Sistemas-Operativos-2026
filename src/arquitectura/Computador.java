package arquitectura;

/**
 *
 * @author Jabri
 */
import archivos.SistemaArchivos;
import disco.DiscoSimulado;
import edd.Cola;
import politicas.PoliticaPlanificacion;
import procesos.Planificador;
import procesos.Proceso;
import utilidades.RegistroEventos;

public class Computador implements ComponenteReloj {

    private int id;
    private String nombre;
    private int memoriaTotal;
    private int memoriaUsada;

    private CPU cpu;
    private Planificador planificador;
    private DiscoSimulado disco;
    private SistemaArchivos sistemaArchivos;
    private Cluster cluster;
    private RelojGlobal reloj;

    // Colas del modelo de 7 estados
    private Cola<Proceso> colaNuevos;
    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaListoSuspendidos;
    private Cola<Proceso> colaBloqueadoSuspendidos;
    private Cola<Proceso> colaTerminados;

    public Computador(int id, String nombre, int memoriaTotal, PoliticaPlanificacion politicaInicial, int quantum) {
        this.id = id;
        this.nombre = nombre;
        this.memoriaTotal = Math.max(64, memoriaTotal);
        this.memoriaUsada = 0;

        this.cpu = new CPU();
        this.colaNuevos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaListoSuspendidos = new Cola<>();
        this.colaBloqueadoSuspendidos = new Cola<>();
        this.colaTerminados = new Cola<>();

        this.planificador = new Planificador(this, politicaInicial, quantum);
    }

    public Computador(String nombre, int ramTotal, int quantum, PoliticaPlanificacion politicaInicial) {
        this(1, nombre, ramTotal, politicaInicial, quantum);
    }

    public void vincularAlmacenamiento(DiscoSimulado disco, SistemaArchivos sistemaArchivos, RegistroEventos logger) {
        this.disco = disco;
        this.sistemaArchivos = sistemaArchivos;
        if (this.disco != null) {
            this.disco.setComputador(this);
            this.disco.setLogger(logger);
        }
        if (this.sistemaArchivos != null) {
            this.sistemaArchivos.setLogger(logger);
        }
        if (this.planificador != null) {
            this.planificador.setLogger(logger);
        }
    }

    @Override
    public void ejecutarCiclo(int cicloActual) {
        if (planificador != null) {
            planificador.ejecutarCicloPlanificacion(cicloActual);
        }
        if (cpu != null) {
            cpu.ejecutarCiclo(cicloActual);
        }
    }

    public synchronized boolean asignarMemoria(int cantidadMB) {
        if (memoriaUsada + cantidadMB <= memoriaTotal) {
            memoriaUsada += cantidadMB;
            return true;
        }
        return false;
    }

    public synchronized void liberarMemoria(int cantidadMB) {
        memoriaUsada = Math.max(0, memoriaUsada - cantidadMB);
    }

    public int getMemoriaDisponible() {
        return memoriaTotal - memoriaUsada;
    }

    // --- Métodos de compatibilidad ---
    public int getRamTotal() { return getMemoriaTotal(); }
    public int getRamUsada() { return getMemoriaUsada(); }
    public int getRamLibre() { return getMemoriaDisponible(); }

    public synchronized void ocuparMemoriaBuffer(int cantidadMb) {
        this.memoriaUsada = Math.min(memoriaTotal, memoriaUsada + cantidadMb);
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
        if (this.planificador != null) {
            this.planificador.setCluster(cluster);
        }
    }

    public void setReloj(RelojGlobal reloj) {
        this.reloj = reloj;
    }

    public synchronized void recibirProceso(Proceso p, int cicloActual) {
        if (p != null && planificador != null) {
            planificador.admitirProceso(p, cicloActual);
        }
    }

    public void admitirProcesosSiHayMemoria() {
        // Compatibilidad con invocaciones antiguas
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getMemoriaTotal() { return memoriaTotal; }
    public int getMemoriaUsada() { return memoriaUsada; }
    public CPU getCpu() { return cpu; }
    public Planificador getPlanificador() { return planificador; }
    public DiscoSimulado getDisco() { return disco; }
    public SistemaArchivos getSistemaArchivos() { return sistemaArchivos; }

    public Cola<Proceso> getColaNuevos() { return colaNuevos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaListoSuspendidos() { return colaListoSuspendidos; }
    public Cola<Proceso> getColaBloqueadoSuspendidos() { return colaBloqueadoSuspendidos; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
}
