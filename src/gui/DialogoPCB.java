/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

    public DialogoPCB(JFrame parent, Proceso p) {
        super(parent, "Bloque de Control del Proceso (PCB) - " + p.getNombre(), true);
        setSize(550, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(24, 28, 38));

        JPanel panelContenido = new JPanel(new GridLayout(0, 2, 8, 8));
        panelContenido.setBackground(new Color(34, 40, 52));
        panelContenido.setBorder(new EmptyBorder(16, 16, 16, 16));

        agregarFila(panelContenido, "Identificador Único (PID):", String.valueOf(p.getId()));
        agregarFila(panelContenido, "Nombre del Proceso:", p.getNombre());
        agregarFila(panelContenido, "Computador Asignado:", p.getComputadorAsignado());
        agregarFila(panelContenido, "Tipo de Proceso:", p.getTipo().getNombreLegible());
        agregarFila(panelContenido, "Estado Actual:", p.getEstado().name());
        agregarFila(panelContenido, "Motivo de Bloqueo:", p.getMotivoBloqueo().getDescripcion());
        agregarFila(panelContenido, "Modo de Procesador:", p.getModo().name());
        agregarFila(panelContenido, "Nivel de Prioridad:", String.valueOf(p.getPrioridad()));
        agregarFila(panelContenido, "Memoria RAM Asignada:", p.getMemoriaRequerida() + " MB");
        agregarFila(panelContenido, "Program Counter (PC):", String.valueOf(p.getPc()));
        agregarFila(panelContenido, "Memory Address Reg (MAR):", String.valueOf(p.getMar()));
        agregarFila(panelContenido, "Tiempo CPU Restante / Total:", p.getTiempoRestante() + " / " + p.getTiempoEjecucion() + " ciclos");
        agregarFila(panelContenido, "Deadline Restante:", p.getDeadline() + " ciclos");
        agregarFila(panelContenido, "Program Status Word (PSW):", p.getPsw());
        agregarFila(panelContenido, "Stack Pointer (SP):", "0x" + Integer.toHexString(p.getApuntadorPila()).toUpperCase());

        if (p.getTipo() == procesos.TipoProceso.PRODUCTOR || p.getTipo() == procesos.TipoProceso.CONSUMIDOR) {
            agregarFila(panelContenido, "Buffer Asociado:", p.getBufferObjetivo());
            agregarFila(panelContenido, "Progreso Elementos:", p.getElementosProcesados() + " de " + p.getElementosMeta());
            agregarFila(panelContenido, "Ciclos por Operación:", String.valueOf(p.getCiclosPorOperacion()));
            agregarFila(panelContenido, "Tiempo Bloqueado en Semáforos:", p.getTiempoBloqueadoSem() + " ciclos");
        } else if (p.getTipo() == procesos.TipoProceso.IO_BOUND) {
            agregarFila(panelContenido, "Frecuencia E/S:", "Cada " + p.getCiclosEntreIO() + " ciclos");
            agregarFila(panelContenido, "Duración de E/S:", p.getDuracionIO() + " ciclos");
        }

        agregarFila(panelContenido, "Ciclo de Llegada:", String.valueOf(p.getCicloLlegada()));
        agregarFila(panelContenido, "Ciclo de 1ra Ejecución:", (p.getCicloPrimeraEjecucion() == -1) ? "Pendiente" : String.valueOf(p.getCicloPrimeraEjecucion()));
        agregarFila(panelContenido, "Tiempo de Espera en Listos:", p.getTiempoEsperaListo() + " ciclos");
        agregarFila(panelContenido, "¿Cumplió Deadline?:", p.isCumplioDeadline() ? "SÍ" : (p.getEstado() == procesos.EstadoProceso.TERMINADO ? "NO (Expiró)" : "En curso"));

        JScrollPane scroll = new JScrollPane(panelContenido);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setBackground(new Color(41, 121, 255));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> dispose());

        JPanel panelBoton = new JPanel();
        panelBoton.setBackground(new Color(24, 28, 38));
        panelBoton.add(btnCerrar);
        add(panelBoton, BorderLayout.SOUTH);
    }

    private void agregarFila(JPanel p, String etiqueta, String valor) {
        JLabel lblEtiq = new JLabel(etiqueta);
        lblEtiq.setForeground(new Color(170, 180, 200));
        lblEtiq.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel lblVal = new JLabel(valor);
        lblVal.setForeground(new Color(230, 235, 245));
        lblVal.setFont(new Font("Monospaced", Font.PLAIN, 12));

        p.add(lblEtiq);
        p.add(lblVal);
    }
}