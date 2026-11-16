/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package procesos;

/**
 *
 * @author Jabri
 */

public enum MotivoBloqueo {
    NINGUNO("No bloqueado"),
    SEMAFORO_MUTEX("Semáforo Mutex"),
    SEMAFORO_LLENOS("Semáforo Llenos"),
    SEMAFORO_VACIOS("Semáforo Vacíos"),
    LATENCIA_RED("Latencia de Red"),
    ENTRADA_SALIDA("Operación de E/S");

    private final String descripcion;

    MotivoBloqueo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}