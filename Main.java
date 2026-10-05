/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main;

/**
 *
 * @author Jabri
 */

import edd.Cola;
import procesos.EstadoProceso;
import procesos.Proceso;
import procesos.Planificador;

public class Main {

    public static void main(String[] args) {
        Planificador planificador = new Planificador(3);

        Proceso p1 = new Proceso(1, "Navegador Web", 7, 1, 1000, 0);
        Proceso p2 = new Proceso(2, "Editor de Texto", 2, 2, 1000, 0);
        Proceso p3 = new Proceso(3, "Reproductor Musica", 5, 3, 1000, 0);

        planificador.agregarProceso(p1);
        planificador.agregarProceso(p2);
        planificador.agregarProceso(p3);

        planificador.ejecutarSimulacionCompleta();
    }
}