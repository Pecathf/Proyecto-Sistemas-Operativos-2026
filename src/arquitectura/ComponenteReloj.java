/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author samue
 */
public interface ComponenteReloj {
    /**
     * Se ejecuta de forma sincronizada en cada ciclo marcado por el Reloj Global.
     * 
     * @param cicloActual Número de ciclo global actual
     */
    void ejecutarCiclo(int cicloActual);
}