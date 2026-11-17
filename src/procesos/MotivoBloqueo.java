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
    NINGUNO("Ninguno"),
    ENTRADA_SALIDA("Operación de E/S en Disco"),
    SUSPENSION_SWAP("Suspendido en Área de Swap"),
    REACTIVACION_SWAP("Reactivando desde Swap a RAM");

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