package gui;

import edd.ListaEnlazada;

import javax.swing.*;
import java.awt.*;

public class PanelGraficoDisco extends JPanel {

    private ListaEnlazada<Integer> historialDesplazamiento;
    private static final int MAX_PUNTOS = 60;

    public PanelGraficoDisco() {
        this.historialDesplazamiento = new ListaEnlazada<>();
        setBackground(new Color(20, 24, 33));
        setPreferredSize(new Dimension(340, 145));
        setMinimumSize(new Dimension(200, 110));
    }

    public void registrarPunto(int desplazamientoAcumulado) {
        if (historialDesplazamiento.getTamano() >= MAX_PUNTOS) {
            historialDesplazamiento.eliminar(0);
        }
        historialDesplazamiento.agregar(desplazamientoAcumulado);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        if (w < 80 || h < 60) return;
        int pad = 28;

        g2.setColor(new Color(0, 229, 255));
        Font tituloFont = new Font("Segoe UI", Font.BOLD, 12);
        g2.setFont(tituloFont);
        String titulo = "Desplazamiento Acumulado del Cabezal vs Tiempo";
        if (g2.getFontMetrics().stringWidth(titulo) > w - pad - 8) {
            tituloFont = new Font("Segoe UI", Font.BOLD, 10);
            g2.setFont(tituloFont);
            if (g2.getFontMetrics().stringWidth(titulo) > w - pad - 8) {
                titulo = "Desplazamiento Cabezal vs Tiempo";
            }
        }
        g2.drawString(titulo, pad, 18);

        g2.setColor(new Color(70, 80, 100));
        g2.drawLine(pad, h - pad, w - 12, h - pad);
        g2.drawLine(pad, 26, pad, h - pad);

        int n = historialDesplazamiento.getTamano();
        if (n < 2) return;

        int maxVal = 10;
        for (int i = 0; i < n; i++) {
            if (historialDesplazamiento.obtener(i) > maxVal) {
                maxVal = historialDesplazamiento.obtener(i);
            }
        }

        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.drawString(String.valueOf(maxVal), 4, 34);
        g2.drawString("0", 14, h - pad);

        g2.setColor(new Color(255, 171, 0));
        g2.setStroke(new BasicStroke(2f));

        double stepX = (double) (w - pad - 18) / (MAX_PUNTOS - 1);
        int prevX = -1, prevY = -1;

        for (int i = 0; i < n; i++) {
            int val = historialDesplazamiento.obtener(i);
            int x = pad + (int) (i * stepX);
            int y = (h - pad) - (int) (((double) val / maxVal) * (h - pad - 30));

            if (prevX != -1) {
                g2.drawLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
        }
    }
}
