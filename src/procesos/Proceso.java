package procesos;

public class Proceso {

    // 1. Identificación del Proceso (PCB)
    private int id;                     
    private String nombre;
    private int idUsuario;              
    private int idPadre;                
    private int indiceTablaProcesos;    

    // 2. Información de Control y Planificación
    private int tiempoEjecucion;        
    private int tiempoRestante;         
    private int prioridad;
    private EstadoProceso estado;
    private ModoEjecucion modo;

    // 3. Estado del Procesador (PCB - Contexto de CPU)
    private int pc;                     
    private String psw;                 
    private int apuntadorPila;          
    private int[] registrosCPU;         

    // Bandera para evitar bucle infinito de E/S
    private boolean realizoIO;
    private int memoriaRequerida;
    

    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int idUsuario, int idPadre, int indiceTablaProcesos, int memoriaRequerida) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoEjecucion = tiempoEjecucion;
        this.tiempoRestante = tiempoEjecucion;
        this.prioridad = prioridad;
        this.idUsuario = idUsuario;
        this.idPadre = idPadre;
        this.indiceTablaProcesos = indiceTablaProcesos;
        this.estado = EstadoProceso.NUEVO;
        this.modo = ModoEjecucion.USUARIO;
        this.realizoIO = false;
        this.memoriaRequerida = memoriaRequerida;

        this.pc = 0;
        this.psw = "OK";
        this.apuntadorPila = 0xFFFF;
        this.registrosCPU = new int[4];
    }

    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int idUsuario, int idPadre,int memoriaRequerida) {
        this(id, nombre, tiempoEjecucion, prioridad, idUsuario, idPadre, id, memoriaRequerida);
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

    public ModoEjecucion getModo() { return modo; }
    public void setModo(ModoEjecucion modo) { this.modo = modo; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    public String getPsw() { return psw; }
    public void setPsw(String psw) { this.psw = psw; }

    public int getApuntadorPila() { return apuntadorPila; }
    public void setApuntadorPila(int apuntadorPila) { this.apuntadorPila = apuntadorPila; }

    public int[] getRegistrosCPU() { return registrosCPU; }
    public void setRegistrosCPU(int[] registrosCPU) { this.registrosCPU = registrosCPU; }
    
    public int getMemoriaRequerida() { return memoriaRequerida; }
    public void setMemoriaRequerida(int memoriaRequerida) { this.memoriaRequerida = memoriaRequerida; }

    public boolean isRealizoIO() { return realizoIO; }
    public void setRealizoIO(boolean realizoIO) { this.realizoIO = realizoIO; }
}