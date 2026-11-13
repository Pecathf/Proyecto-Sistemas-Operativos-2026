/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */
public enum TipoPoliticaDisco {
    FIFO("FIFO"),
    SSTF("SSTF (Shortest Seek Time First)"),
    SCAN("SCAN (Elevador)"),
    C_SCAN("C-SCAN (SCAN Circular)");

    private final String nombreLegible;

    TipoPoliticaDisco(String nombreLegible) {
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