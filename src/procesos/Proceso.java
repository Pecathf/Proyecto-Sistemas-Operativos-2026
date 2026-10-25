package procesos;

public class Proceso {
    private int id;                     
    private String nombre;
    private int idUsuario;              
    private int idPadre;                
    private int indiceTablaProcesos;    

    private int tiempoEjecucion;        
    private int tiempoRestante;         
    private int prioridad;
    private EstadoProceso estado;
    private ModoEjecucion modo;
    
    private int memoriaRequerida;
    private int deadline;
    private String tipo; 

    private int pc;                     
    private String psw;                 
    private int apuntadorPila;          
    private int[] registrosCPU;         

    // MÉTRICAS (Módulo 4)
    private int cicloLlegada;
    private int cicloPrimeraEjecucion;
    private int cicloFinalizacion;
    
    // RED (Módulo 2)
    private int ciclosLatenciaRestantes;

    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad, int memoriaRequerida) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoEjecucion = tiempoEjecucion;
        this.tiempoRestante = tiempoEjecucion;
        this.prioridad = prioridad;
        this.idUsuario = 1000;
        this.idPadre = 0;
        this.indiceTablaProcesos = id;
        this.memoriaRequerida = memoriaRequerida;
        this.deadline = tiempoEjecucion * 2;
        this.tipo = "CPU_BOUND";
        this.estado = EstadoProceso.NUEVO;
        this.modo = ModoEjecucion.USUARIO;
        
        this.pc = 0;
        this.psw = "OK";
        this.apuntadorPila = 0xFFFF;
        this.registrosCPU = new int[4];
        
        this.cicloLlegada = 0;
        this.cicloPrimeraEjecucion = -1; // -1 significa que no ha entrado a CPU
        this.cicloFinalizacion = 0;
        this.ciclosLatenciaRestantes = 0;
    }

    // --- GETTERS Y SETTERS ---
    public int getId() { return id; }
    public String getNombre() { return nombre; }
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
    public int getMemoriaRequerida() { return memoriaRequerida; }
    public void setMemoriaRequerida(int memoriaRequerida) { this.memoriaRequerida = memoriaRequerida; }
    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    // Getters y Setters de Métricas y Red
    public int getCicloLlegada() { return cicloLlegada; }
    public void setCicloLlegada(int cicloLlegada) { this.cicloLlegada = cicloLlegada; }
    public int getCicloPrimeraEjecucion() { return cicloPrimeraEjecucion; }
    public void setCicloPrimeraEjecucion(int cicloPrimeraEjecucion) { this.cicloPrimeraEjecucion = cicloPrimeraEjecucion; }
    public int getCicloFinalizacion() { return cicloFinalizacion; }
    public void setCicloFinalizacion(int cicloFinalizacion) { this.cicloFinalizacion = cicloFinalizacion; }
    public int getCiclosLatenciaRestantes() { return ciclosLatenciaRestantes; }
    public void setCiclosLatenciaRestantes(int ciclos) { this.ciclosLatenciaRestantes = ciclos; }
}