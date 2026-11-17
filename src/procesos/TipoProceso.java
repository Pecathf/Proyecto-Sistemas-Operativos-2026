/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package procesos;

/**
 *
 * @author Jabri
 */

public enum TipoProceso {
    CPU_BOUND("CPU-Bound (Cómputo)"),
    IO_BOUND("I/O-Bound (Archivos / Disco)");

    private final String descripcion;

    TipoProceso(String descripcion) {
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
