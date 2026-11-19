package gui;

/**
 *
 * @author Jabri
 */

import procesos.Proceso;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DialogoPCB extends JDialog {

    public DialogoPCB(Frame owner, Proceso p) {
        super(owner, "Inspector de PCB - " + p.getNombre(), true);
        setSize(440, 440);
        setLocationRelativeTo(owner);

        JPanel panel = new JPanel(new GridLayout(14, 2, 8, 6));
        panel.setBackground(new Color(26, 30, 40));
        panel.setBorder(new EmptyBorder(15, 18, 15, 18));

        agregarCampo(panel, "ID Proceso:", String.valueOf(p.getId()));
        agregarCampo(panel, "Nombre:", p.getNombre());
        agregarCampo(panel, "Estado Actual (7-Estados):", p.getEstado().name());
        agregarCampo(panel, "Tipo de Proceso:", p.getTipo().getDescripcion());
        agregarCampo(panel, "Motivo de Bloqueo:", p.getMotivoBloqueo().getDescripcion());
        agregarCampo(panel, "PC / MAR:", p.getPc() + " / " + p.getMar() + " (de " + p.getInstruccionesTotales() + ")");
        agregarCampo(panel, "Prioridad:", String.valueOf(p.getPrioridad()));
        agregarCampo(panel, "Memoria RAM Requerida:", p.getMemoriaRequerida() + " MB");
        agregarCampo(panel, "Deadline Restante:", p.getDeadlineRestante() + " / " + p.getDeadlineInicial());
        agregarCampo(panel, "Operación Archivo (E/S):", p.getOperacionSolicitada() != null ? p.getOperacionSolicitada().name() : "N/A");
        agregarCampo(panel, "Ruta Archivo Objetivo:", (p.getRutaArchivo() != null && !p.getRutaArchivo().isEmpty()) ? p.getRutaArchivo() : "N/A");
        agregarCampo(panel, "Bloque Inicio Swap:", p.getBloqueInicioSwap() >= 0 ? String.valueOf(p.getBloqueInicioSwap()) : "En RAM");
        agregarCampo(panel, "Tiempo Espera Listo:", p.getTiempoEsperaListo() + " ciclos");
        agregarCampo(panel, "Tiempo Bloqueado:", p.getTiempoBloqueado() + " ciclos");

        setContentPane(panel);
    }

    private void agregarCampo(JPanel panel, String etiqueta, String valor) {
        JLabel lbl1 = new JLabel(etiqueta);
        lbl1.setForeground(new Color(160, 175, 195));
        lbl1.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel lbl2 = new JLabel(valor);
        lbl2.setForeground(Color.WHITE);
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panel.add(lbl1);
        panel.add(lbl2);
    }
}
