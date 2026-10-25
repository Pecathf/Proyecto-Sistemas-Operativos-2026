/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author PECA
 */
public class ConfiguracionInicial {
    public int duracionCicloMs;
    public int numComputadores;
    public int ramPorComputador;
    public int quantumPorDefecto;
    public int latenciaRedCiclos;

    public ConfiguracionInicial() {
        // Valores por defecto
        this.duracionCicloMs = 1000;
        this.numComputadores = 2;
        this.ramPorComputador = 4096;
        this.quantumPorDefecto = 3;
        this.latenciaRedCiclos = 2;
    }
}
