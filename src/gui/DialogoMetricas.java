package gui;

/**
 *
 * @author Jabri
 */

import arquitectura.Cluster;
import arquitectura.Computador;
import disco.DiscoSimulado;
import edd.ListaEnlazada;
import procesos.Planificador;
import procesos.Proceso;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DialogoMetricas extends JDialog {

    public DialogoMetricas(Frame owner, Cluster sistema) {
        super(owner, "Métricas Globales de Rendimiento (CPU, Memoria, Disco y Swap)", true);
        setSize(580, 520);
        setLocationRelativeTo(owner);

        Computador comp = sistema.getComputadorUnico();
        DiscoSimulado disco = sistema.getDisco();
        Planificador plan = comp.getPlanificador();

        ListaEnlazada<Proceso> historial = sistema.getHistorialProcesos();
        int terminados = 0;
        int cumplidos = 0;
        long sumaEspera = 0;
        long sumaRetorno = 0;

        for (int i = 0; i < historial.getTamano(); i++) {
            Proceso p = historial.obtener(i);
            if (p.haTerminado()) {
                terminados++;
                if (p.cumplioDeadline()) cumplidos++;
                sumaEspera += p.getTiempoEsperaListo();
                sumaRetorno += Math.max(1, p.getTiempoFinalizacion() - p.getTiempoLlegada());
            }
        }

        double promEsperaCPU = (terminados == 0) ? 0.0 : ((double) sumaEspera / terminados);
        double promRetorno = (terminados == 0) ? 0.0 : ((double) sumaRetorno / terminados);
        double tasaCumplimiento = (terminados == 0) ? 100.0 : ((cumplidos * 100.0) / terminados);
        int ciclosTotales = Math.max(1, sistema.getReloj().getCicloActual());
        double throughput = (double) terminados / ciclosTotales;

        JPanel root = new JPanel(new GridLayout(13, 2, 10, 7));
        root.setBackground(new Color(24, 28, 38));
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        agregarFila(root, "Ciclos Totales Simulados:", String.valueOf(sistema.getReloj().getCicloActual()));
        
        // --- CÁLCULO DIRECTO DE UTILIZACIÓN DE CPU (Solución al error) ---
        long ciclosCPU = 0;
        for (int i = 0; i < historial.getTamano(); i++) {
            ciclosCPU += historial.obtener(i).getTiempoEjecucionCPU();
        }
        double utilizacionCPU = Math.min(100.0, (ciclosCPU * 100.0) / ciclosTotales);
        agregarFila(root, "Utilización de CPU:", String.format("%.2f %%", utilizacionCPU));
        // -----------------------------------------------------------------

        agregarFila(root, "Procesos Terminados / Totales:", terminados + " / " + historial.getTamano());
        agregarFila(root, "Throughput (Proc / Ciclo):", String.format("%.4f", throughput));
        agregarFila(root, "Tiempo Promedio Espera CPU:", String.format("%.2f ciclos", promEsperaCPU));
        agregarFila(root, "Tiempo Promedio Retorno:", String.format("%.2f ciclos", promRetorno));
        agregarFila(root, "Cumplimiento de Deadlines:", String.format("%.1f %%", tasaCumplimiento));
        agregarFila(root, "Desplazamiento Total Cabezal Disco:", disco.getDesplazamientoTotal() + " bloques");
        agregarFila(root, "Tiempo Promedio Espera Cola E/S:", String.format("%.2f ciclos", disco.getTiempoPromedioEsperaIO()));
        agregarFila(root, "Solicitudes Atendidas por Política:",
                "FIFO=" + disco.getAtendidasFIFO() + " | SSTF=" + disco.getAtendidasSSTF() +
                " | SCAN=" + disco.getAtendidasSCAN() + " | C-SCAN=" + disco.getAtendidasCSCAN());
        agregarFila(root, "Suspensiones a Swap (Out):", String.valueOf(plan.getTotalSuspensiones()));
        agregarFila(root, "Reactivaciones desde Swap (In):", String.valueOf(plan.getTotalReactivaciones()));
        agregarFila(root, "Uso Actual RAM / Swap:",
                comp.getMemoriaUsada() + "/" + comp.getMemoriaTotal() + " MB | Swap libres: " +
                disco.contarBloquesLibresSwap() + "/" + disco.getTamanoSwapBloques());

        setContentPane(root);
    }

    private void agregarFila(JPanel panel, String clave, String valor) {
        JLabel l1 = new JLabel(clave);
        l1.setForeground(new Color(165, 180, 200));
        l1.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel l2 = new JLabel(valor);
        l2.setForeground(new Color(0, 229, 255));
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panel.add(l1);
        panel.add(l2);
    }
}