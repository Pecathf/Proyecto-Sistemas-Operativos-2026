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

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class PanelGraficoCPU extends JPanel {

    private Cluster cluster;
    private static final int MAX_PUNTOS = 60; // Mostrar los últimos 60 ciclos
    private double[][] historialUso; // [computadorIndex][puntos]
    private int puntosActuales;

    private static final Color[] COLORES_NODOS = new Color[]{
        new Color(41, 121, 255),  // Azul eléctrico
        new Color(0, 200, 115),   // Verde neón
        new Color(255, 145, 0),   // Naranja
        new Color(213, 0, 249),   // Magenta
        new Color(255, 214, 0),   // Amarillo
        new Color(0, 229, 255)    // Cyan
    };

    public PanelGraficoCPU(Cluster cluster) {
        this.cluster = cluster;
        int nComps = Math.max(1, cluster.getCantidadComputadores());
        this.historialUso = new double[nComps][MAX_PUNTOS];
        this.puntosActuales = 0;

        setBackground(new Color(20, 24, 32));
        setPreferredSize(new Dimension(360, 200));
        setDoubleBuffered(true);
    }

    public synchronized void registrarMuestra() {
        int nComps = cluster.getCantidadComputadores();
        if (historialUso.length < nComps) {
            historialUso = new double[nComps][MAX_PUNTOS];
        }

        if (puntosActuales < MAX_PUNTOS) {
            for (int i = 0; i < nComps; i++) {
                historialUso[i][puntosActuales] = cluster.getComputador(i).getCpu().getPorcentajeUso();
            }
            puntosActuales++;
        } else {
            // Desplazar muestras a la izquierda
            for (int i = 0; i < nComps; i++) {
                System.arraycopy(historialUso[i], 1, historialUso[i], 0, MAX_PUNTOS - 1);
                historialUso[i][MAX_PUNTOS - 1] = cluster.getComputador(i).getCpu().getPorcentajeUso();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();
        int margenIzq = 35;
        int margenDer = 15;
        int margenSup = 25;
        int margenInf = 25;

        int graphW = ancho - margenIzq - margenDer;
        int graphH = alto - margenSup - margenInf;

        // Fondo del gráfico
        g2.setColor(new Color(15, 18, 24));
        g2.fillRect(margenIzq, margenSup, graphW, graphH);

        // Líneas de cuadrícula (0%, 25%, 50%, 75%, 100%)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        for (int pct = 0; pct <= 100; pct += 25) {
            int y = margenSup + graphH - (int) ((pct / 100.0) * graphH);
            g2.setColor(new Color(45, 55, 75));
            g2.drawLine(margenIzq, y, margenIzq + graphW, y);

            g2.setColor(new Color(140, 155, 180));
            g2.drawString(pct + "%", 6, y + 4);
        }

        // Título y Leyenda
        g2.setColor(new Color(230, 235, 245));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("Utilización de CPU en el Tiempo (%)", margenIzq, margenSup - 8);

        if (puntosActuales < 2) {
            g2.setColor(new Color(120, 130, 150));
            g2.drawString("Recolectando métricas...", margenIzq + 20, margenSup + graphH / 2);
            g2.dispose();
            return;
        }

        // Trazar cada curva
        int nComps = cluster.getCantidadComputadores();
        for (int c = 0; c < nComps; c++) {
            Color colorCurva = COLORES_NODOS[c % COLORES_NODOS.length];
            g2.setColor(colorCurva);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D.Double path = new Path2D.Double();
            for (int p = 0; p < puntosActuales; p++) {
                double x = margenIzq + ((double) p / (MAX_PUNTOS - 1)) * graphW;
                double val = Math.min(100.0, Math.max(0.0, historialUso[c][p]));
                double y = margenSup + graphH - (val / 100.0) * graphH;

                if (p == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            g2.draw(path);

            // Leyenda en la parte inferior
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            int legX = margenIzq + (c * 110);
            if (legX + 100 < ancho) {
                g2.fillRect(legX, alto - margenInf + 8, 8, 8);
                g2.setColor(new Color(200, 210, 230));
                String tag = cluster.getComputador(c).getNombre() + ": " + String.format("%.1f%%", historialUso[c][puntosActuales - 1]);
                g2.drawString(tag, legX + 12, alto - margenInf + 16);
            }
        }

        // Borde exterior
        g2.setColor(new Color(55, 65, 85));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRect(margenIzq, margenSup, graphW, graphH);

        g2.dispose();
    }
}
