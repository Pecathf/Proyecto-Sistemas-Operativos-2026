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

public class Main {

    public static void main(String[] args) {
        // 2. LA INSTANCIACIÓN VA DENTRO DE UN MÉTODO O DEL MAIN
        Cola<Proceso> colaListos = new Cola<>();

        // Creamos un par de procesos de prueba
        Proceso p1 = new Proceso(1, "Navegador", 5, 1);
        Proceso p2 = new Proceso(2, "EditorTexto", 3, 2);

        // Cambiamos su estado a LISTO y los encolamos
        p1.setEstado(EstadoProceso.LISTO);
        p2.setEstado(EstadoProceso.LISTO);

        colaListos.encolar(p1);
        colaListos.encolar(p2);

        System.out.println("Procesos en cola de listos: " + colaListos.getTamano());
    }
}