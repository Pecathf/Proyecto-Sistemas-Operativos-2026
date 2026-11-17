package procesos;
/**
 *
 * @author Jabri
 */
import disco.TipoOperacionIO;

public class Proceso {
    private int id;
    private String nombre;
    private int instruccionesTotales;
    private int pc;
    private int mar;
    private int prioridad;
    private int memoriaRequerida;
    private int deadlineInicial;
    private int deadlineRestante;

    private TipoProceso tipo;
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;

    // Atributos de E/S sobre Sistema de Archivos y Swap (Proyecto 2)
    private TipoOperacionIO operacionSolicitada;
    private String rutaArchivo;
    private String nuevoNombreArchivo;
    private int tamanoArchivoBloques;
    private int bloqueInicioSwap;
    private boolean operacionIOEjecutada;

    // Métricas y seguimiento
    private int tiempoLlegada;
    private int tiempoInicioEjecucion;
    private int tiempoFinalizacion;
    private int tiempoEsperaListo;
    private int tiempoBloqueado;
    private int tiempoEjecucionCPU;
    private int quantumRestante;
    private int idComputadorAsignado;

    public Proceso(int id, String nombre, int instruccionesTotales, int prioridad,
                   int memoriaRequerida, int deadline, TipoProceso tipo) {
        this.id = id;
        this.nombre = nombre;
        this.instruccionesTotales = Math.max(1, instruccionesTotales);
        this.pc = 0;
        this.mar = 0;
        this.prioridad = prioridad;
        this.memoriaRequerida = Math.max(1, memoriaRequerida);
        this.deadlineInicial = Math.max(1, deadline);
        this.deadlineRestante = this.deadlineInicial;

        this.tipo = (tipo != null) ? tipo : TipoProceso.CPU_BOUND;
        this.estado = EstadoProceso.NUEVO;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;

        this.operacionSolicitada = null;
        this.rutaArchivo = "";
        this.nuevoNombreArchivo = "";
        this.tamanoArchivoBloques = 0;
        this.bloqueInicioSwap = -1;
        this.operacionIOEjecutada = false;

        this.tiempoLlegada = 0;
        this.tiempoInicioEjecucion = -1;
        this.tiempoFinalizacion = -1;
        this.tiempoEsperaListo = 0;
        this.tiempoBloqueado = 0;
        this.tiempoEjecucionCPU = 0;
        this.quantumRestante = 0;
        this.idComputadorAsignado = 1;
    }

    /**
     * Configura una solicitud de operación sobre el sistema de archivos.
     */
    public void configurarOperacionArchivo(TipoOperacionIO operacion, String rutaArchivo, int tamanoBloques) {
        this.operacionSolicitada = operacion;
        this.rutaArchivo = (rutaArchivo != null) ? rutaArchivo : "";
        this.tamanoArchivoBloques = Math.max(1, tamanoBloques);
        this.tipo = TipoProceso.IO_BOUND;
        this.operacionIOEjecutada = false;
    }

    /**
     * Permite parsear el formato "OPERACION|RUTA|BLOQUES" usado por GestorJSON.
     */
    public void setBufferObjetivo(String cadenaCodificada) {
        if (cadenaCodificada == null || cadenaCodificada.trim().isEmpty()) return;
        String[] partes = cadenaCodificada.split("\\|");
        if (partes.length >= 2) {
            try {
                TipoOperacionIO op = TipoOperacionIO.valueOf(partes[0].trim().toUpperCase());
                String ruta = partes[1].trim();
                int blq = (partes.length >= 3) ? Integer.parseInt(partes[2].trim()) : 1;
                configurarOperacionArchivo(op, ruta, blq);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean ejecutarInstruccion(int cicloActual) {
        if (tiempoInicioEjecucion == -1) {
            tiempoInicioEjecucion = cicloActual;
        }
        pc++;
        mar++;
        tiempoEjecucionCPU++;
        if (deadlineRestante > 0) {
            deadlineRestante--;
        }
        if (quantumRestante > 0) {
            quantumRestante--;
        }
        return pc >= instruccionesTotales;
    }

    public void incrementarEsperaListo() {
        tiempoEsperaListo++;
        if (deadlineRestante > 0) {
            deadlineRestante--;
        }
    }

    public void incrementarTiempoBloqueado() {
        tiempoBloqueado++;
        if (deadlineRestante > 0) {
            deadlineRestante--;
        }
    }

    public boolean haTerminado() {
        return pc >= instruccionesTotales || estado == EstadoProceso.TERMINADO;
    }

    public boolean cumplioDeadline() {
        return deadlineRestante > 0 || (tiempoFinalizacion != -1 && (tiempoFinalizacion - tiempoLlegada) <= deadlineInicial);
    }

    // --- Getters y Setters ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public int getMemoriaRequerida() { return memoriaRequerida; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public int getDeadlineRestante() { return deadlineRestante; }

    public TipoProceso getTipo() { return tipo; }
    public void setTipo(TipoProceso tipo) { this.tipo = tipo; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(MotivoBloqueo motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }

    public TipoOperacionIO getOperacionSolicitada() { return operacionSolicitada; }
    public void setOperacionSolicitada(TipoOperacionIO operacionSolicitada) { this.operacionSolicitada = operacionSolicitada; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }
    public String getNuevoNombreArchivo() { return nuevoNombreArchivo; }
    public void setNuevoNombreArchivo(String nuevoNombreArchivo) { this.nuevoNombreArchivo = nuevoNombreArchivo; }
    public int getTamanoArchivoBloques() { return tamanoArchivoBloques; }
    public void setTamanoArchivoBloques(int tamanoArchivoBloques) { this.tamanoArchivoBloques = tamanoArchivoBloques; }
    public int getBloqueInicioSwap() { return bloqueInicioSwap; }
    public void setBloqueInicioSwap(int bloqueInicioSwap) { this.bloqueInicioSwap = bloqueInicioSwap; }
    public boolean isOperacionIOEjecutada() { return operacionIOEjecutada; }
    public void setOperacionIOEjecutada(boolean operacionIOEjecutada) { this.operacionIOEjecutada = operacionIOEjecutada; }

    public int getTiempoLlegada() { return tiempoLlegada; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }
    public int getTiempoInicioEjecucion() { return tiempoInicioEjecucion; }
    public int getTiempoFinalizacion() { return tiempoFinalizacion; }
    public void setTiempoFinalizacion(int tiempoFinalizacion) { this.tiempoFinalizacion = tiempoFinalizacion; }
    public int getTiempoEsperaListo() { return tiempoEsperaListo; }
    public int getTiempoBloqueado() { return tiempoBloqueado; }
    public int getTiempoEjecucionCPU() { return tiempoEjecucionCPU; }
    public int getQuantumRestante() { return quantumRestante; }
    public void setQuantumRestante(int quantumRestante) { this.quantumRestante = quantumRestante; }
    public int getIdComputadorAsignado() { return idComputadorAsignado; }
    public void setIdComputadorAsignado(int idComputadorAsignado) { this.idComputadorAsignado = idComputadorAsignado; }

    @Override
    public String toString() {
        return "P" + id + " (" + nombre + ") [" + estado + "]";
    }
}