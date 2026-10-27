/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package politicas;

/**
 *
 * @author samue
 */
public enum TipoPolitica {
    FCFS("FCFS"),
    ROUND_ROBIN("Round Robin"),
    SJF("SJF"),
    EDF("EDF (Earliest Deadline First)"),
    PRIORIDAD_APROPIATIVA("Prioridad Apropiativa");

    private final String nombreLegible;

    TipoPolitica(String nombreLegible) {
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