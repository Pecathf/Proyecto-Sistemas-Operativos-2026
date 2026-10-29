package main;

/**
 *
 * @author Jabri
 */

import gui.VentanaConfiguracionInicial;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Lanzar la vista de configuración inicial obligatoria del sistema distribuido
        SwingUtilities.invokeLater(() -> {
            VentanaConfiguracionInicial ventanaConfig = new VentanaConfiguracionInicial();
            ventanaConfig.setVisible(true);
        });
    }
}
