/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package procesos;

/**
 *
 * @author Jabri
 */

public class Proceso {
    private int id;
    private String nombre;
    private EstadoProceso estado;
    private int tiempoTotal;     // Ráfaga / tiempo total que requiere de CPU
    private int tiempoRestante;  // Tiempo que le falta para terminar
    private int prioridad;       // Para algoritmos de planificación por prioridad

    public Proceso(int id, String nombre, int tiempoTotal, int prioridad) {
        this.id = id;
        this.nombre = nombre;
        this.tiempoTotal = tiempoTotal;
        this.tiempoRestante = tiempoTotal;
        this.prioridad = prioridad;
        this.estado = EstadoProceso.NUEVO;
    }

    // Método para simular la ejecución en CPU por 1 unidad de tiempo
    public void ejecutarPaso() {
        if (tiempoRestante > 0) {
            tiempoRestante--;
        }
        if (tiempoRestante == 0) {
            estado = EstadoProceso.TERMINADO;
        }
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public int getTiempoTotal() { return tiempoTotal; }
    public int getTiempoRestante() { return tiempoRestante; }
    public int getPrioridad() { return prioridad; }
}
