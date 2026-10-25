package main;

import arquitectura.Cluster;
import arquitectura.Computador;
import arquitectura.RelojGlobal;
import gui.VentanaSimulador;
import politicas.PoliticaRoundRobin;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // 1. Crear el Reloj Global (duración de ciclo p. ej. 1000 ms)
        RelojGlobal reloj = new RelojGlobal(1000);

        // 2. Crear el Cluster
        Cluster cluster = new Cluster();

        // 3. Crear los Nodos/Computadores iniciales del sistema distribuido
        Computador comp1 = new Computador("Computador 1", 8192, 3, new PoliticaRoundRobin());
        Computador comp2 = new Computador("Computador 2", 16384, 3, new PoliticaRoundRobin());

        // 4. Agregar los computadores al Cluster y al Reloj
        cluster.agregarComputador(comp1);
        cluster.agregarComputador(comp2);

        reloj.agregarComputador(comp1);
        reloj.agregarComputador(comp2);

        // 5. Iniciar los hilos de la simulación
        reloj.iniciarSimulacion();

        // 6. Lanzar la Interfaz Gráfica enviándole el Cluster y el Reloj
        SwingUtilities.invokeLater(() -> {
            VentanaSimulador ventana = new VentanaSimulador(cluster, reloj);
            ventana.setVisible(true);
        });
    }
}