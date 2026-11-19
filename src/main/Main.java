package main;

/**
 *
 * @author Jabri
 */

import gui.VentanaConfiguracionInicial;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            VentanaConfiguracionInicial ventana = new VentanaConfiguracionInicial();
            ventana.setVisible(true);
        });
    }
}