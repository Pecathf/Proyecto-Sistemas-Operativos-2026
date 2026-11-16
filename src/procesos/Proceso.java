package procesos;
/**
 *
 * @author Jabri
 */
public class Proceso {
    // --- Identificación y Contexto ---
    private int id;
    private String nombre;
    private String computadorAsignado;
    private int idUsuario;
    private int idPadre;

    // --- Estado y Planificación ---
    private EstadoProceso estado;
    private MotivoBloqueo motivoBloqueo;
    private TipoProceso tipo;
    private ModoEjecucion modo;
    private int prioridad;
    private int memoriaRequerida;

    // --- Tiempos y Ciclos ---
    private int tiempoEjecucion; // Tiempo total de CPU requerido
    private int tiempoRestante;  // Tiempo restante de CPU
    private int deadline;        // Deadline restante (cuenta regresiva)
    private int deadlineOriginal;

    // --- Hardware / Registros CPU ---
    private int pc;                 // Program Counter
    private int mar;                // Memory Address Register
    private String psw;             // Program Status Word
    private int apuntadorPila;      // Stack Pointer
    private int[] registrosCPU;     // Registros de propósito general

    // --- Parámetros Específicos de E/S (I/O Bound) ---
    private int ciclosEntreIO;
    private int contadorParaIO;
    private int duracionIO;
    private int tiempoRestanteIO;

    // --- Parámetros Específicos de Productor / Consumidor ---
    private String bufferObjetivo;      // ID del buffer con el que interactúa
    private int ciclosPorOperacion;     // Cada cuántos ciclos produce/consume
    private int contadorCiclosOperacion;
    private int elementosMeta;          // Cuántos elementos requiere para terminar
    private int elementosProcesados;    // Cuántos lleva producidos o consumidos
    private int faseSincronizacion;     // Estado en el algoritmo de Stallings

    // --- Red y Latencia ---
    private int ciclosLatenciaRestantes;

    // --- Métricas de Rendimiento ---
    private int cicloLlegada;
    private int cicloPrimeraEjecucion;
    private int cicloFinalizacion;
    private int tiempoBloqueadoSem;
    private int tiempoEsperaListo;
    private boolean cumplioDeadline;

    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int memoriaRequerida, int deadline, TipoProceso tipo) {
        this.id = id;
        this.nombre = nombre;
        this.computadorAsignado = "No asignado";
        this.idUsuario = 1000;
        this.idPadre = 0;

        this.tiempoEjecucion = tiempoEjecucion;
        this.tiempoRestante = tiempoEjecucion;
        this.prioridad = prioridad;
        this.memoriaRequerida = memoriaRequerida;
        this.deadline = (deadline > 0) ? deadline : tiempoEjecucion * 2;
        this.deadlineOriginal = this.deadline;
        this.tipo = (tipo != null) ? tipo : TipoProceso.CPU_BOUND;

        this.estado = EstadoProceso.NUEVO;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        this.modo = ModoEjecucion.USUARIO;

        this.pc = 0;
        this.mar = 0;
        this.psw = "OK";
        this.apuntadorPila = 0xFFFF;
        this.registrosCPU = new int[4];

        // I/O defaults
        this.ciclosEntreIO = 3;
        this.contadorParaIO = 3;
        this.duracionIO = 2;
        this.tiempoRestanteIO = 0;

        // Productor/Consumidor defaults
        this.bufferObjetivo = "";
        this.ciclosPorOperacion = 2;
        this.contadorCiclosOperacion = 2;
        this.elementosMeta = 5;
        this.elementosProcesados = 0;
        this.faseSincronizacion = 0;

        // Red
        this.ciclosLatenciaRestantes = 0;

        // Métricas
        this.cicloLlegada = 0;
        this.cicloPrimeraEjecucion = -1;
        this.cicloFinalizacion = 0;
        this.tiempoBloqueadoSem = 0;
        this.tiempoEsperaListo = 0;
        this.cumplioDeadline = false;
    }

    // Constructor sobrecargado compatible
    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int memoriaRequerida) {
        this(id, nombre, tiempoEjecucion, prioridad, memoriaRequerida, tiempoEjecucion * 2, TipoProceso.CPU_BOUND);
    }

    // --- GETTERS Y SETTERS ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getComputadorAsignado() { return computadorAsignado; }
    public void setComputadorAsignado(String computadorAsignado) { this.computadorAsignado = computadorAsignado; }

    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { 
        this.estado = estado;
        if (estado != EstadoProceso.BLOQUEADO) {
            this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        }
    }

    public MotivoBloqueo getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(MotivoBloqueo motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }

    public TipoProceso getTipo() { return tipo; }
    public void setTipo(TipoProceso tipo) { this.tipo = tipo; }

    public ModoEjecucion getModo() { return modo; }
    public void setModo(ModoEjecucion modo) { this.modo = modo; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public int getMemoriaRequerida() { return memoriaRequerida; }
    public void setMemoriaRequerida(int memoriaRequerida) { this.memoriaRequerida = memoriaRequerida; }

    public int getTiempoEjecucion() { return tiempoEjecucion; }
    public void setTiempoEjecucion(int tiempoEjecucion) { this.tiempoEjecucion = tiempoEjecucion; }

    public int getTiempoRestante() { return tiempoRestante; }
    public void setTiempoRestante(int tiempoRestante) { this.tiempoRestante = tiempoRestante; }

    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }

    public int getDeadlineOriginal() { return deadlineOriginal; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    public int getMar() { return mar; }
    public void setMar(int mar) { this.mar = mar; }

    public String getPsw() { return psw; }
    public int getApuntadorPila() { return apuntadorPila; }
    public int[] getRegistrosCPU() { return registrosCPU; }

    // E/S
    public int getCiclosEntreIO() { return ciclosEntreIO; }
    public void setCiclosEntreIO(int ciclosEntreIO) { 
        this.ciclosEntreIO = ciclosEntreIO; 
        this.contadorParaIO = ciclosEntreIO;
    }
    public int getContadorParaIO() { return contadorParaIO; }
    public void setContadorParaIO(int contadorParaIO) { this.contadorParaIO = contadorParaIO; }
    public int getDuracionIO() { return duracionIO; }
    public void setDuracionIO(int duracionIO) { this.duracionIO = duracionIO; }
    public int getTiempoRestanteIO() { return tiempoRestanteIO; }
    public void setTiempoRestanteIO(int tiempoRestanteIO) { this.tiempoRestanteIO = tiempoRestanteIO; }

    // Productor / Consumidor
    public String getBufferObjetivo() { return bufferObjetivo; }
    public void setBufferObjetivo(String bufferObjetivo) { this.bufferObjetivo = bufferObjetivo; }

    public int getCiclosPorOperacion() { return ciclosPorOperacion; }
    public void setCiclosPorOperacion(int ciclosPorOperacion) { 
        this.ciclosPorOperacion = ciclosPorOperacion;
        this.contadorCiclosOperacion = ciclosPorOperacion;
    }
    public int getContadorCiclosOperacion() { return contadorCiclosOperacion; }
    public void setContadorCiclosOperacion(int contadorCiclosOperacion) { this.contadorCiclosOperacion = contadorCiclosOperacion; }

    public int getElementosMeta() { return elementosMeta; }
    public void setElementosMeta(int elementosMeta) { this.elementosMeta = elementosMeta; }

    public int getElementosProcesados() { return elementosProcesados; }
    public void setElementosProcesados(int elementosProcesados) { this.elementosProcesados = elementosProcesados; }
    public void incrementarElementosProcesados() { this.elementosProcesados++; }

    public int getFaseSincronizacion() { return faseSincronizacion; }
    public void setFaseSincronizacion(int fase) { this.faseSincronizacion = fase; }

    // Red
    public int getCiclosLatenciaRestantes() { return ciclosLatenciaRestantes; }
    public void setCiclosLatenciaRestantes(int ciclos) { this.ciclosLatenciaRestantes = ciclos; }

    // Métricas
    public int getCicloLlegada() { return cicloLlegada; }
    public void setCicloLlegada(int cicloLlegada) { this.cicloLlegada = cicloLlegada; }

    public int getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public void setCicloPrimeraEjecucion(int cicloPrimeraEjecucion) { this.cicloPrimeraEjecucion = cicloPrimeraEjecucion; }

    public int getCicloFinalizacion() { return cicloFinalizacion; }
    public void setCicloFinalizacion(int cicloFinalizacion) { this.cicloFinalizacion = cicloFinalizacion; }

    public int getTiempoBloqueadoSem() { return tiempoBloqueadoSem; }
    public void incrementarTiempoBloqueadoSem() { this.tiempoBloqueadoSem++; }

    public int getTiempoEsperaListo() { return tiempoEsperaListo; }
    public void incrementarTiempoEsperaListo() { this.tiempoEsperaListo++; }

    public boolean isCumplioDeadline() { return cumplioDeadline; }
    public void setCumplioDeadline(boolean cumplioDeadline) { this.cumplioDeadline = cumplioDeadline; }

    @Override
    public String toString() {
        return nombre + " (ID:" + id + ", Tipo:" + tipo + ", T:" + tiempoRestante + ")";
    }
}
