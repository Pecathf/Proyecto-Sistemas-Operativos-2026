/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;

/**
 *
 * @author PECA
 */
public class GeneradorID {
    private static int contador = 1;
    
    // Sincronizado para evitar IDs duplicados en hilos concurrentes
    public static synchronized int generar() {
        return contador++;
    }
    
}