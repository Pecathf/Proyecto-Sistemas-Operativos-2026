package arquitectura;

/**
 *
 * @author Jabri
 */
import archivos.PermisoArchivo;
import archivos.SistemaArchivos;
import disco.DiscoSimulado;
import disco.TipoOperacionIO;
import edd.ListaEnlazada;
import politicas.PoliticaFCFS;
import politicas.PoliticaPlanificacion;
import procesos.Proceso;
import procesos.TipoProceso;
import sincronizacion.buffer;
import utilidades.RegistroEventos;

public class Cluster {

    private Computador computadorUnico;
    private DiscoSimulado disco;
    private SistemaArchivos sistemaArchivos;
    private RelojGlobal reloj;
    private RegistroEventos logger;
    private GeneradorID generadorID;
    private ListaEnlazada<Proceso> historialProcesos;
    private ListaEnlazada<Computador> computadores;
    private ListaEnlazada<buffer> buffers;
    private int latenciaRedCiclos;

    public Cluster() {
        this.computadores = new ListaEnlazada<>();
        this.buffers = new ListaEnlazada<>();
        this.latenciaRedCiclos = 2;
        this.logger = null;
        this.generadorID = new GeneradorID();
        this.historialProcesos = new ListaEnlazada<>();
        this.reloj = new RelojGlobal(1000);

        // Computador por defecto para compatibilidad
        this.computadorUnico = new Computador(1, "Computador 1", 1024, new PoliticaFCFS(), 3);
        this.computadores.agregar(this.computadorUnico);
        this.reloj.registrarComponente(this.computadorUnico);
    }

    public Cluster(Computador computador, DiscoSimulado disco, SistemaArchivos sistemaArchivos,
                   int duracionCicloMs, RegistroEventos logger) {
        this.computadores = new ListaEnlazada<>();
        this.buffers = new ListaEnlazada<>();
        this.latenciaRedCiclos = 2;

        this.computadorUnico = computador;
        if (this.computadorUnico != null) {
            this.computadores.agregar(this.computadorUnico);
        }
        this.disco = disco;
        this.sistemaArchivos = sistemaArchivos;
        this.logger = logger;
        this.generadorID = new GeneradorID();
        this.historialProcesos = new ListaEnlazada<>();

        if (this.computadorUnico != null) {
            this.computadorUnico.vincularAlmacenamiento(this.disco, this.sistemaArchivos, this.logger);
        }

        this.reloj = new RelojGlobal(duracionCicloMs);
        if (this.computadorUnico != null) {
            this.reloj.registrarComponente(this.computadorUnico);
        }
        if (this.disco != null) {
            this.reloj.registrarComponente(this.disco);
        }
    }

    public void setLogger(RegistroEventos logger) {
        this.logger = logger;
    }

    public void log(String mensaje) {
        if (logger != null) {
            logger.registrar(mensaje);
        }
    }

    /**
     * Agrega un proceso previamente construido (por ejemplo, desde el JSON inicial).
     */
    public void registrarYAdmitirProceso(Proceso p) {
        if (p == null) return;
        historialProcesos.agregar(p);
        if (computadorUnico != null && computadorUnico.getPlanificador() != null) {
            int ciclo = (reloj != null) ? reloj.getCicloActual() : 0;
            computadorUnico.getPlanificador().admitirProceso(p, ciclo);
        }
    }

    /**
     * Crea y admite un nuevo proceso CPU-Bound.
     */
    public Proceso crearProcesoCPU(String nombre, int instrucciones, int prioridad, int memoriaMB, int deadline) {
        int id = GeneradorID.generar();
        Proceso p = new Proceso(id, nombre, instrucciones, prioridad, memoriaMB, deadline, TipoProceso.CPU_BOUND);
        registrarYAdmitirProceso(p);
        return p;
    }

    /**
     * Crea y admite un proceso de E/S que ejecutará una operación CRUD sobre el sistema de archivos.
     */
    public Proceso crearProcesoArchivo(String nombre, TipoOperacionIO operacion, String rutaArchivo,
                                       int tamanoBloques, int prioridad, int memoriaMB, int deadline) {
        int id = GeneradorID.generar();
        Proceso p = new Proceso(id, nombre, 2, prioridad, memoriaMB, deadline, TipoProceso.IO_BOUND);
        p.configurarOperacionArchivo(operacion, rutaArchivo, tamanoBloques);
        registrarYAdmitirProceso(p);
        return p;
    }

    /**
     * Ejecuta una operación CRUD desde la GUI respetando permisos del modo de usuario activo.
     * @return Mensaje de error si los permisos son insuficientes, o null si el proceso fue creado correctamente.
     */
    public String solicitarOperacionCRUDDesdeGUI(TipoOperacionIO operacion, String rutaPadreODestino,
                                                 String nombreElemento, boolean esDirectorio,
                                                 int tamanoBloques, PermisoArchivo permiso) {
        if (sistemaArchivos == null) return "Sistema de archivos no inicializado.";

        String rutaNormalizada = rutaPadreODestino.endsWith("/") && rutaPadreODestino.length() > 1
                ? rutaPadreODestino.substring(0, rutaPadreODestino.length() - 1)
                : rutaPadreODestino;

        if (operacion == TipoOperacionIO.CREATE) {
            String errorPermiso = sistemaArchivos.validarPermisoCrear(rutaNormalizada, esDirectorio);
            if (errorPermiso != null) return errorPermiso;

            String rutaNueva = "/".equals(rutaNormalizada)
                    ? "/" + nombreElemento
                    : rutaNormalizada + "/" + nombreElemento;
            if (esDirectorio) rutaNueva += "/";

            crearProcesoArchivo("IO_Create_" + nombreElemento, TipoOperacionIO.CREATE,
                    rutaNueva, esDirectorio ? 0 : tamanoBloques, 2, 32, 300);
            return null;

        } else if (operacion == TipoOperacionIO.READ) {
            String errorPermiso = sistemaArchivos.validarPermisoLeer(rutaNormalizada);
            if (errorPermiso != null) return errorPermiso;

            crearProcesoArchivo("IO_Read", TipoOperacionIO.READ, rutaNormalizada, 1, 2, 24, 300);
            return null;

        } else if (operacion == TipoOperacionIO.UPDATE) {
            String errorPermiso = sistemaArchivos.validarPermisoActualizar(rutaNormalizada);
            if (errorPermiso != null) return errorPermiso;

            Proceso p = crearProcesoArchivo("IO_Update", TipoOperacionIO.UPDATE, rutaNormalizada, 1, 2, 24, 300);
            p.setNuevoNombreArchivo(nombreElemento);
            return null;

        } else if (operacion == TipoOperacionIO.DELETE) {
            String errorPermiso = sistemaArchivos.validarPermisoEliminar(rutaNormalizada);
            if (errorPermiso != null) return errorPermiso;

            crearProcesoArchivo("IO_Delete", TipoOperacionIO.DELETE, rutaNormalizada, 1, 2, 24, 300);
            return null;
        }

        return "Operación no soportada.";
    }

    public void cambiarPoliticaCPU(PoliticaPlanificacion nuevaPolitica) {
        if (computadorUnico != null && computadorUnico.getPlanificador() != null) {
            computadorUnico.getPlanificador().setPolitica(nuevaPolitica);
        }
    }

    // --- Métodos de compatibilidad de Clúster ---
    public synchronized void agregarComputador(Computador c) {
        if (c == null) return;
        c.setCluster(this);
        computadores.agregar(c);
        if (computadorUnico == null) {
            computadorUnico = c;
        }
    }

    public synchronized void agregarBuffer(buffer b) {
        buffers.agregar(b);
        Computador anfitrion = buscarComputadorPorNombre(b.getComputadorAnfitrion());
        if (anfitrion != null) {
            anfitrion.ocuparMemoriaBuffer(b.getMemoriaOcupadaMb());
        }
        log("Buffer '" + b.getId() + "' creado en " + b.getComputadorAnfitrion());
    }

    public synchronized buffer buscarBuffer(String id) {
        for (int i = 0; i < buffers.getTamano(); i++) {
            buffer b = buffers.obtener(i);
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
        if (computadores.estaVacia()) return;
        Computador c = computadores.obtener(0);
        p.setComputadorAsignado(c.getNombre());
        c.recibirProceso(p, cicloActual);
        log("Proceso " + p.getNombre() + " asignado automáticamente a " + c.getNombre());
    }

    public synchronized Computador getComputador(int indice) {
        if (indice >= 0 && indice < computadores.getTamano()) {
            return computadores.obtener(indice);
        }
        return computadorUnico;
    }

    public synchronized int getCantidadComputadores() {
        return Math.max(1, computadores.getTamano());
    }

    public synchronized ListaEnlazada<Computador> getComputadores() {
        return computadores.clonar();
    }

    public synchronized ListaEnlazada<buffer> getBuffers() {
        return buffers.clonar();
    }

    public int getLatenciaRedCiclos() {
        return latenciaRedCiclos;
    }

    public void setLatenciaRedCiclos(int latenciaRedCiclos) {
        this.latenciaRedCiclos = latenciaRedCiclos;
    }

    public synchronized double getUsoCPUGlobal() {
        if (computadorUnico != null && computadorUnico.getCpu() != null) {
            return computadorUnico.getCpu().getPorcentajeUso();
        }
        return 0.0;
    }

    public synchronized double getThroughputGlobal(int cicloActual) {
        if (cicloActual <= 0 || computadorUnico == null || computadorUnico.getPlanificador() == null) {
            return 0.0;
        }
        int totalTerminados = computadorUnico.getColaTerminados().getTamano();
        return ((double) totalTerminados / cicloActual) * 100.0;
    }

    public synchronized double getTiempoRespuestaPromedioGlobal() {
        if (computadorUnico == null) return 0.0;
        double sumaRespuestas = 0;
        int total = 0;
        ListaEnlazada<Proceso> term = computadorUnico.getColaTerminados().obtenerListaElementos();
        for (int i = 0; i < term.getTamano(); i++) {
            Proceso p = term.obtener(i);
            if (p.getCicloPrimeraEjecucion() != -1) {
                sumaRespuestas += (p.getCicloPrimeraEjecucion() - p.getCicloLlegada());
                total++;
            }
        }
        return (total == 0) ? 0.0 : (sumaRespuestas / total);
    }

    public synchronized double getTasaCumplimientoDeadlinesGlobal() {
        if (computadorUnico == null) return 100.0;
        int total = 0;
        int exitosos = 0;
        ListaEnlazada<Proceso> term = computadorUnico.getColaTerminados().obtenerListaElementos();
        for (int i = 0; i < term.getTamano(); i++) {
            total++;
            if (term.obtener(i).isCumplioDeadline()) {
                exitosos++;
            }
        }
        return (total == 0) ? 100.0 : ((double) exitosos / total) * 100.0;
    }

    public synchronized double getIndiceEquidadGlobal() {
        if (computadorUnico == null) return 1.0;
        double suma = 0;
        double sumaCuadrados = 0;
        int n = 0;
        ListaEnlazada<Proceso> term = computadorUnico.getColaTerminados().obtenerListaElementos();
        for (int i = 0; i < term.getTamano(); i++) {
            int espera = term.obtener(i).getTiempoEsperaListo();
            suma += espera;
            sumaCuadrados += (espera * espera);
            n++;
        }
        if (n <= 1 || sumaCuadrados == 0) return 1.0;
        return (suma * suma) / (n * sumaCuadrados);
    }

    public synchronized double getTiempoPromedioBloqueadoSem() {
        return 0.0;
    }

    // --- Getters ---
    public Computador getComputadorUnico() { return computadorUnico; }
    public DiscoSimulado getDisco() { return disco; }
    public SistemaArchivos getSistemaArchivos() { return sistemaArchivos; }
    public RelojGlobal getReloj() { return reloj; }
    public RegistroEventos getLogger() { return logger; }
    public ListaEnlazada<Proceso> getHistorialProcesos() { return historialProcesos; }
}