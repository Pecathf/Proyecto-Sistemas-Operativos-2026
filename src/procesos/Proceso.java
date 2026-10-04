package procesos;

public class Proceso {

    private int id;
    private String nombre;
    private int tiempoEjecucion; // Tiempo de ráfaga (burst time)
    private int prioridad;
    private EstadoProceso estado;

    public Proceso(int id, String nombre, int tiempoEjecucion, int prioridad) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoEjecucion = tiempoEjecucion;
        this.prioridad = prioridad;
        this.estado = EstadoProceso.NUEVO;
    }

    // --- GETTERS Y SETTERS QUE PIDE PLANIFICADOR.JAVA ---

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getTiempoEjecucion() {
        return tiempoEjecucion;
    }

    public void setTiempoEjecucion(int tiempoEjecucion) {
        this.tiempoEjecucion = tiempoEjecucion;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }
}