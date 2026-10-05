package procesos;

public class Proceso {

    // 1. Identificación del Proceso (PCB)
    private int id;                     // Identificador único (PID)
    private String nombre;
    private int idUsuario;              // Identificador del usuario responsable (UID)
    private int idPadre;                // Identificador del proceso que lo creó (PPID)
    private int indiceTablaProcesos;    // Índice en la tabla de procesos primaria

    // 2. Información de Control y Planificación
    private int tiempoEjecucion;        // Tiempo total de ráfaga (burst time)
    private int tiempoRestante;         // Tiempo que le falta por ejecutar
    private int prioridad;
    private EstadoProceso estado;

    // 3. Estado del Procesador (PCB - Contexto de CPU)
    private int pc;                     // Program Counter y código de condición
    private String psw;                 // Registro de control y estado (PSW)
    private int apuntadorPila;          // Stack Pointer (SP)
    private int[] registrosCPU;         // Registros visibles al usuario (AX, BX, CX, DX)

    // Constructor completo
    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int idUsuario, int idPadre, int indiceTablaProcesos) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoEjecucion = tiempoEjecucion;
        this.tiempoRestante = tiempoEjecucion;
        this.prioridad = prioridad;
        this.idUsuario = idUsuario;
        this.idPadre = idPadre;
        this.indiceTablaProcesos = indiceTablaProcesos;
        this.estado = EstadoProceso.NUEVO;

        // Inicialización del contexto de CPU
        this.pc = 0;
        this.psw = "OK";
        this.apuntadorPila = 0xFFFF;
        this.registrosCPU = new int[4];
    }

    // Constructor sobrecargado por conveniencia (para retrocompatibilidad)
    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int idUsuario, int idPadre) {
        this(id, nombre, tiempoEjecucion, prioridad, idUsuario, idPadre, id);
    }

    // --- GETTERS Y SETTERS ---

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getIdUsuario() { return idUsuario; }
    public int getIdPadre() { return idPadre; }
    public int getIndiceTablaProcesos() { return indiceTablaProcesos; }
    public void setIndiceTablaProcesos(int indiceTablaProcesos) { this.indiceTablaProcesos = indiceTablaProcesos; }

    public int getTiempoEjecucion() { return tiempoEjecucion; }
    public void setTiempoEjecucion(int tiempoEjecucion) { this.tiempoEjecucion = tiempoEjecucion; }

    public int getTiempoRestante() { return tiempoRestante; }
    public void setTiempoRestante(int tiempoRestante) { this.tiempoRestante = tiempoRestante; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    public String getPsw() { return psw; }
    public void setPsw(String psw) { this.psw = psw; }

    public int getApuntadorPila() { return apuntadorPila; }
    public void setApuntadorPila(int apuntadorPila) { this.apuntadorPila = apuntadorPila; }

    public int[] getRegistrosCPU() { return registrosCPU; }
    public void setRegistrosCPU(int[] registrosCPU) { this.registrosCPU = registrosCPU; }
}