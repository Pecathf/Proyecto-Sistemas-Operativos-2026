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
    private ModoEjecucion modo;

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

    // Compatibilidad adicional / Hardware / Red / Sincronización
    private String computadorAsignado;
    private int idUsuario;
    private int idPadre;
    private String psw;
    private int apuntadorPila;
    private int[] registrosCPU;
    private String bufferObjetivo;
    private int ciclosPorOperacion;
    private int elementosMeta;
    private int elementosProcesados;
    private int faseSincronizacion;
    private int ciclosLatenciaRestantes;
    private int tiempoBloqueadoSem;
    private int ciclosEntreIO;
    private int tiempoRestanteIO;
    private int duracionIO;
    private Boolean cumplioDeadlineManual;

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
        this.modo = ModoEjecucion.USUARIO;

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

        this.computadorAsignado = "Computador 1";
        this.idUsuario = 1000;
        this.idPadre = 0;
        this.psw = "OK";
        this.apuntadorPila = 0xFFFF;
        this.registrosCPU = new int[4];
        this.bufferObjetivo = "";
        this.ciclosPorOperacion = 1;
        this.elementosMeta = 0;
        this.elementosProcesados = 0;
        this.faseSincronizacion = 0;
        this.ciclosLatenciaRestantes = 0;
        this.tiempoBloqueadoSem = 0;
        this.ciclosEntreIO = 3;
        this.tiempoRestanteIO = 0;
        this.cumplioDeadlineManual = null;
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
        this.bufferObjetivo = cadenaCodificada;
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

    public void incrementarTiempoEsperaListo() {
        incrementarEsperaListo();
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
        if (cumplioDeadlineManual != null) return cumplioDeadlineManual;
        return deadlineRestante > 0 || (tiempoFinalizacion != -1 && (tiempoFinalizacion - tiempoLlegada) <= deadlineInicial);
    }

    // --- Getters y Setters ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }
    public int getMar() { return mar; }
    public void setMar(int mar) { this.mar = mar; }
    public ModoEjecucion getModo() { return modo != null ? modo : ModoEjecucion.USUARIO; }
    public void setModo(ModoEjecucion modo) { this.modo = modo; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public int getMemoriaRequerida() { return memoriaRequerida; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public int getDeadlineRestante() { return deadlineRestante; }
    public int getDeadline() { return deadlineRestante; }
    public void setDeadline(int deadline) { this.deadlineRestante = deadline; }
    public int getDeadlineOriginal() { return deadlineInicial; }
    public void setDeadlineOriginal(int deadlineOriginal) { this.deadlineInicial = deadlineOriginal; }

    public int getTiempoEjecucion() { return instruccionesTotales; }
    public void setTiempoEjecucion(int tiempoEjecucion) { this.instruccionesTotales = tiempoEjecucion; }
    public int getTiempoRestante() { return Math.max(0, instruccionesTotales - pc); }
    public void setTiempoRestante(int tiempoRestante) { this.instruccionesTotales = this.pc + tiempoRestante; }

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
    public void setTiempoInicioEjecucion(int tiempoInicioEjecucion) { this.tiempoInicioEjecucion = tiempoInicioEjecucion; }
    public int getTiempoFinalizacion() { return tiempoFinalizacion; }
    public void setTiempoFinalizacion(int tiempoFinalizacion) { this.tiempoFinalizacion = tiempoFinalizacion; }
    public int getTiempoEsperaListo() { return tiempoEsperaListo; }
    public int getTiempoBloqueado() { return tiempoBloqueado; }
    public int getTiempoEjecucionCPU() { return tiempoEjecucionCPU; }
    public int getQuantumRestante() { return quantumRestante; }
    public void setQuantumRestante(int quantumRestante) { this.quantumRestante = quantumRestante; }
    public int getIdComputadorAsignado() { return idComputadorAsignado; }
    public void setIdComputadorAsignado(int idComputadorAsignado) { this.idComputadorAsignado = idComputadorAsignado; }

    // Métricas y compatibilidad
    public String getComputadorAsignado() { return computadorAsignado; }
    public void setComputadorAsignado(String computadorAsignado) { this.computadorAsignado = computadorAsignado; }
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public int getIdPadre() { return idPadre; }
    public void setIdPadre(int idPadre) { this.idPadre = idPadre; }
    public String getPsw() { return psw; }
    public void setPsw(String psw) { this.psw = psw; }
    public int getApuntadorPila() { return apuntadorPila; }
    public void setApuntadorPila(int apuntadorPila) { this.apuntadorPila = apuntadorPila; }
    public int[] getRegistrosCPU() { return registrosCPU; }
    public void setRegistrosCPU(int[] registrosCPU) { this.registrosCPU = registrosCPU; }
    public String getBufferObjetivo() { return (bufferObjetivo != null && !bufferObjetivo.isEmpty()) ? bufferObjetivo : rutaArchivo; }
    public int getElementosMeta() { return elementosMeta; }
    public void setElementosMeta(int elementosMeta) { this.elementosMeta = elementosMeta; }
    public int getElementosProcesados() { return elementosProcesados; }
    public void setElementosProcesados(int elementosProcesados) { this.elementosProcesados = elementosProcesados; }
    public void incrementarElementosProcesados() { this.elementosProcesados++; }
    public int getCiclosPorOperacion() { return ciclosPorOperacion; }
    public void setCiclosPorOperacion(int ciclosPorOperacion) { this.ciclosPorOperacion = ciclosPorOperacion; }
    public int getFaseSincronizacion() { return faseSincronizacion; }
    public void setFaseSincronizacion(int faseSincronizacion) { this.faseSincronizacion = faseSincronizacion; }
    public int getCiclosLatenciaRestantes() { return ciclosLatenciaRestantes; }
    public void setCiclosLatenciaRestantes(int ciclosLatenciaRestantes) { this.ciclosLatenciaRestantes = ciclosLatenciaRestantes; }
    public int getCicloLlegada() { return tiempoLlegada; }
    public void setCicloLlegada(int cicloLlegada) { this.tiempoLlegada = cicloLlegada; }
    public int getCicloPrimeraEjecucion() { return tiempoInicioEjecucion; }
    public void setCicloPrimeraEjecucion(int cicloPrimeraEjecucion) { this.tiempoInicioEjecucion = cicloPrimeraEjecucion; }
    public int getCicloFinalizacion() { return tiempoFinalizacion; }
    public void setCicloFinalizacion(int cicloFinalizacion) { this.tiempoFinalizacion = cicloFinalizacion; }
    public int getTiempoBloqueadoSem() { return tiempoBloqueadoSem; }
    public void setTiempoBloqueadoSem(int tiempoBloqueadoSem) { this.tiempoBloqueadoSem = tiempoBloqueadoSem; }
    public void incrementarTiempoBloqueadoSem() { this.tiempoBloqueadoSem++; }
    public boolean isCumplioDeadline() { return cumplioDeadline(); }
    public void setCumplioDeadline(boolean cumplioDeadline) { this.cumplioDeadlineManual = cumplioDeadline; }
    public int getTiempoRestanteIO() { return tiempoRestanteIO; }
    public void setTiempoRestanteIO(int tiempoRestanteIO) { this.tiempoRestanteIO = tiempoRestanteIO; }
    public int getDuracionIO() { return duracionIO; }
    public void setDuracionIO(int duracionIO) { this.duracionIO = duracionIO; }
    public int getCiclosEntreIO() { return ciclosEntreIO; }
    public void setCiclosEntreIO(int ciclosEntreIO) { this.ciclosEntreIO = ciclosEntreIO; }

    @Override
    public String toString() {
        return "P" + id + " (" + nombre + ") [" + estado + "]";
    }
}
