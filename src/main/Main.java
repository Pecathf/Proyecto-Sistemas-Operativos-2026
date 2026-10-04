/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

/**
 *
 * @author Jabri
 */

// 1. LOS IMPORTS VAN AQUÍ ARRIBA
import edd.Cola;
import procesos.EstadoProceso;
import procesos.Proceso;
import procesos.Planificador;


public class Main {

    public static void main(String[] args) {
        // Instanciamos el planificador con un Quantum de 3 unidades
        Planificador planificador = new Planificador(3);

        // Creamos 3 procesos de prueba (id, nombre, tiempoEjecucion, prioridad)
        Proceso p1 = new Proceso(1, "Navegador Web", 7, 1);
        Proceso p2 = new Proceso(2, "Editor de Texto", 2, 2);
        Proceso p3 = new Proceso(3, "Reproductor Musica", 5, 3);

        // Agregamos los procesos al planificador
        planificador.agregarProceso(p1);
        planificador.agregarProceso(p2);
        planificador.agregarProceso(p3);

        // Ejecutamos la simulación completa Round Robin
        planificador.ejecutarSimulacionCompleta();
    }
}