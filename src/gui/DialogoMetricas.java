/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gui;

/**
 *
 * @author Jabri
 */
import arquitectura.Cluster;
import arquitectura.Computador;
import edd.ListaEnlazada;
import procesos.Proceso;
import sincronizacion.Buffer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class DialogoMetricas extends JDialog {

    private Cluster cluster;
    private int cicloActual;

    public DialogoMetricas(JFrame parent, Cluster cluster, int cicloActual) {
        super(parent, "Métricas de Rendimiento - ÁvilaOS", false);
        this.cluster = cluster;
        this.cicloActual = cicloActual;

        setSize(750, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(24, 28, 38));

        JPanel contenedor = new JPanel();
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setBackground(new Color(24, 28, 38));
        contenedor.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. Métricas Globales
        JPanel panelGlobal = new JPanel(new GridLayout(0, 2, 8, 8));
        panelGlobal.setBackground(new Color(34, 40, 52));
        panelGlobal.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 85)), "Métricas Globales del Sistema Distribuido",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(0, 200, 115)));

        agregarDato(panelGlobal, "Ciclos Transcurridos:", String.valueOf(cicloActual));
        agregarDato(panelGlobal, "Throughput Global:", String.format("%.2f proc / 100 ciclos", cluster.getThroughputGlobal(cicloActual)));
        agregarDato(panelGlobal, "Utilización Promedio CPU:", String.format("%.1f %%", cluster.getUsoCPUGlobal()));
        agregarDato(panelGlobal, "Tiempo de Respuesta Promedio:", String.format("%.2f ciclos", cluster.getTiempoRespuestaPromedioGlobal()));
        agregarDato(panelGlobal, "Tasa de Cumplimiento de Deadlines:", String.format("%.1f %%", cluster.getTasaCumplimientoDeadlinesGlobal()));
        agregarDato(panelGlobal, "Índice de Equidad (Jain's Fairness):", String.format("%.3f / 1.000", cluster.getIndiceEquidadGlobal()));
        agregarDato(panelGlobal, "Tiempo Promedio Bloqueado en Semáforos:", String.format("%.2f ciclos", cluster.getTiempoPromedioBloqueadoSem()));
        contenedor.add(panelGlobal);
        contenedor.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. Métricas por Computador
        JPanel panelComputadores = new JPanel(new GridLayout(0, 2, 8, 8));
        panelComputadores.setBackground(new Color(34, 40, 52));
        panelComputadores.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 85)), "Métricas por Computador",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(41, 121, 255)));

        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            Computador c = cluster.getComputador(i);
            int compTerminados = c.getPlanificador().getColaTerminados().getTamano();
            double uso = c.getCpu().getPorcentajeUso();

            agregarDato(panelComputadores, c.getNombre() + " - Política:", c.getPlanificador().getPolitica().getTipo().name());
            agregarDato(panelComputadores, c.getNombre() + " - Utilización CPU:", String.format("%.1f %%", uso));
            agregarDato(panelComputadores, c.getNombre() + " - Procesos Terminados:", String.valueOf(compTerminados));
            agregarDato(panelComputadores, c.getNombre() + " - RAM Usada:", c.getRamUsada() + " / " + c.getRamTotal() + " MB");
        }
        contenedor.add(panelComputadores);
        contenedor.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. Métricas Productor-Consumidor por Buffer
        JPanel panelBuffers = new JPanel(new GridLayout(0, 2, 8, 8));
        panelBuffers.setBackground(new Color(34, 40, 52));
        panelBuffers.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 85)), "Métricas de Buffers (Sincronización)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12), new Color(255, 145, 0)));

        ListaEnlazada<Buffer> bufs = cluster.getBuffers();
        if (bufs.estaVacia()) {
            agregarDato(panelBuffers, "Buffers:", "No hay buffers creados en la simulación");
            agregarDato(panelBuffers, "", "");
        } else {
            for (int i = 0; i < bufs.getTamano(); i++) {
                Buffer b = bufs.obtener(i);
                agregarDato(panelBuffers, b.getId() + " (Anfitrión: " + b.getComputadorAnfitrion() + "):", "Capacidad: " + b.getCapacidad() + " | Ocupación: " + b.getElementosActuales());
                agregarDato(panelBuffers, "  -> Producidos / Consumidos:", b.getTotalProducidos() + " prod / " + b.getTotalConsumidos() + " cons");
            }
        }
        contenedor.add(panelBuffers);

        JScrollPane scroll = new JScrollPane(contenedor);
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

    private void agregarDato(JPanel p, String etiqueta, String valor) {
        JLabel lblE = new JLabel(etiqueta);
        lblE.setForeground(new Color(170, 180, 200));
        lblE.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel lblV = new JLabel(valor);
        lblV.setForeground(new Color(230, 235, 245));
        lblV.setFont(new Font("Monospaced", Font.PLAIN, 11));

        p.add(lblE);
        p.add(lblV);
    }
}