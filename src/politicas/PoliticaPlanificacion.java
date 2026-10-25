/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package politicas;

/**
 *
 * @author Jabri
 */
import procesos.Proceso;
import edd.Cola;

public interface PoliticaPlanificacion {
    // Devuelve el siguiente proceso que le toca entrar a la CPU según la regla de la política
    Proceso seleccionarSiguienteProceso(Cola<Proceso> colaListos);
    
    // Indica si la política requiere desalojo por Quantum (como Round Robin)
    boolean usaQuantum();
}