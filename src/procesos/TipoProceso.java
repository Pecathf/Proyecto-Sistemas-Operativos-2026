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
    CPU_BOUND("CPU Bound"),
    IO_BOUND("I/O Bound"),
    PRODUCTOR("Productor"),
    CONSUMIDOR("Consumidor");

    private final String nombreLegible;

    TipoProceso(String nombreLegible) {
        this.nombreLegible = nombreLegible;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }

    @Override
    public String toString() {
        return nombreLegible;
    }
}
