package main;

import gui.VentanaSimulador;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Ejecutar la interfaz gráfica en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            VentanaSimulador ventana = new VentanaSimulador();
            ventana.setVisible(true);
        });
    }
}