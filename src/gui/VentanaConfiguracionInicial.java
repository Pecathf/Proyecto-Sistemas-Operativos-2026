package gui;

/**
 *
 * @author Jabri
 */

import arquitectura.Cluster;
import arquitectura.Computador;
import disco.*;
import politicas.*;
import procesos.Proceso;
import utilidades.GestorJSON;
import utilidades.RegistroEventos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class VentanaConfiguracionInicial extends JFrame {

    private File archivoFSSeleccionado;
    private File archivoProcesosSeleccionado;

    private JLabel lblArchivoFS;
    private JLabel lblArchivoProcesos;

    private JSpinner spnDuracionCiclo;
    private JSpinner spnMemoriaRAM;
    private JSpinner spnQuantum;
    private JSpinner spnSwapBloques;
    private JComboBox<TipoPolitica> cmbPoliticaCPU;
    private JComboBox<TipoPoliticaDisco> cmbPoliticaDisco;

    private GestorJSON.ResultadoCargaProcesos datosProcesosCargados;
    private JButton btnIniciar;

    public VentanaConfiguracionInicial() {
        setTitle("ÁvilaOS 2.0 - Configuración Inicial del Sistema");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        construirUI();
    }

    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(new Color(22, 26, 35));
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel titulo = new JLabel("Inicialización del Sistema Operativo (ÁvilaOS 2)", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 19));
        titulo.setForeground(new Color(0, 229, 255));
        root.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        // --- Panel Carga Obligatoria de JSON ---
        JPanel pnlJSON = new JPanel(new GridLayout(2, 3, 10, 10));
        pnlJSON.setBackground(new Color(32, 38, 50));
        pnlJSON.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 216)),
                " 1. Carga Obligatoria de Archivos JSON ",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        JButton btnCargarFS = new JButton("Cargar JSON Disco / FS...");
        lblArchivoFS = new JLabel("No seleccionado (Obligatorio)");
        lblArchivoFS.setForeground(new Color(255, 110, 110));

        JButton btnCargarProc = new JButton("Cargar JSON Procesos...");
        lblArchivoProcesos = new JLabel("No seleccionado (Obligatorio)");
        lblArchivoProcesos.setForeground(new Color(255, 110, 110));

        pnlJSON.add(new JLabel("  Estado Disco y Archivos:") {{ setForeground(Color.LIGHT_GRAY); }});
        pnlJSON.add(btnCargarFS);
        pnlJSON.add(lblArchivoFS);

        pnlJSON.add(new JLabel("  Configuración y Procesos:") {{ setForeground(Color.LIGHT_GRAY); }});
        pnlJSON.add(btnCargarProc);
        pnlJSON.add(lblArchivoProcesos);

        centro.add(pnlJSON);
        centro.add(Box.createVerticalStrut(14));

        // --- Panel Parámetros del Computador Único y Disco ---
        JPanel pnlParams = new JPanel(new GridLayout(6, 2, 10, 10));
        pnlParams.setBackground(new Color(32, 38, 50));
        pnlParams.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 115, 140)),
                " 2. Parámetros de Hardware y Planificación ",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        spnDuracionCiclo = new JSpinner(new SpinnerNumberModel(300, 20, 5000, 50));
        spnMemoriaRAM = new JSpinner(new SpinnerNumberModel(1024, 64, 16384, 64));
        spnQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        spnSwapBloques = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        cmbPoliticaCPU = new JComboBox<>(TipoPolitica.values());
        cmbPoliticaDisco = new JComboBox<>(TipoPoliticaDisco.values());

        agregarFilaParam(pnlParams, "Duración de Ciclo (ms):", spnDuracionCiclo);
        agregarFilaParam(pnlParams, "Memoria RAM Total (MB):", spnMemoriaRAM);
        agregarFilaParam(pnlParams, "Bloques Reservados Swap:", spnSwapBloques);
        agregarFilaParam(pnlParams, "Política Planificación CPU:", cmbPoliticaCPU);
        agregarFilaParam(pnlParams, "Quantum (Round Robin):", spnQuantum);
        agregarFilaParam(pnlParams, "Política Planificación Disco:", cmbPoliticaDisco);

        centro.add(pnlParams);
        root.add(centro, BorderLayout.CENTER);

        // --- Botón Arrancar ---
        btnIniciar = new JButton("INICIAR SIMULACIÓN");
        btnIniciar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnIniciar.setBackground(new Color(0, 200, 115));
        btnIniciar.setForeground(Color.BLACK);
        btnIniciar.setEnabled(false);
        btnIniciar.setPreferredSize(new Dimension(220, 40));

        JPanel pnlSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlSur.setOpaque(false);
        pnlSur.add(btnIniciar);
        root.add(pnlSur, BorderLayout.SOUTH);

        setContentPane(root);

        // Eventos
        btnCargarFS.addActionListener(e -> seleccionarJSONFS());
        btnCargarProc.addActionListener(e -> seleccionarJSONProcesos());
        btnIniciar.addActionListener(e -> arrancarSimulador());
    }

    private void agregarFilaParam(JPanel panel, String texto, JComponent comp) {
        JLabel lbl = new JLabel("  " + texto);
        lbl.setForeground(Color.LIGHT_GRAY);
        panel.add(lbl);
        panel.add(comp);
    }

    private void seleccionarJSONFS() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Seleccionar JSON del Sistema de Archivos y Disco");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoFSSeleccionado = fc.getSelectedFile();
            lblArchivoFS.setText(archivoFSSeleccionado.getName());
            lblArchivoFS.setForeground(new Color(105, 240, 174));
            verificarListoParaIniciar();
        }
    }

    private void seleccionarJSONProcesos() {
        JFileChooser fc = new JFileChooser(".");
        fc.setDialogTitle("Seleccionar JSON de Configuración y Procesos");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                datosProcesosCargados = GestorJSON.cargarProcesosJSON(f);
                archivoProcesosSeleccionado = f;
                lblArchivoProcesos.setText(f.getName() + " (" + datosProcesosCargados.procesos.getTamano() + " proc)");
                lblArchivoProcesos.setForeground(new Color(105, 240, 174));

                // Pre-cargar valores leídos desde el JSON en los controles
                spnDuracionCiclo.setValue(datosProcesosCargados.cycleDurationMs);
                spnMemoriaRAM.setValue(datosProcesosCargados.ramCapacity);
                spnSwapBloques.setValue(datosProcesosCargados.swapSizeBlocks);
                spnQuantum.setValue(datosProcesosCargados.quantum);
                cmbPoliticaCPU.setSelectedItem(datosProcesosCargados.cpuPolicy);
                cmbPoliticaDisco.setSelectedItem(datosProcesosCargados.initialDiskPolicy);

                verificarListoParaIniciar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error leyendo JSON de procesos: " + ex.getMessage(),
                        "Error de formato", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void verificarListoParaIniciar() {
        btnIniciar.setEnabled(archivoFSSeleccionado != null && archivoProcesosSeleccionado != null);
    }

    private void arrancarSimulador() {
        try {
            int duracionCiclo = (int) spnDuracionCiclo.getValue();
            int ramTotal = (int) spnMemoriaRAM.getValue();
            int swapBloques = (int) spnSwapBloques.getValue();
            int quantum = (int) spnQuantum.getValue();
            TipoPolitica tipoCPU = (TipoPolitica) cmbPoliticaCPU.getSelectedItem();
            TipoPoliticaDisco tipoDisco = (TipoPoliticaDisco) cmbPoliticaDisco.getSelectedItem();

            GestorJSON.ResultadoCargaFS resFS = GestorJSON.cargarSistemaArchivosJSON(archivoFSSeleccionado, swapBloques);
            resFS.disco.setPolitica(crearPoliticaDisco(tipoDisco));

            RegistroEventos logger = new RegistroEventos();
            Computador comp = new Computador(1, "Computador-Principal", ramTotal, crearPoliticaCPU(tipoCPU), quantum);
            Cluster sistema = new Cluster(comp, resFS.disco, resFS.sistemaArchivos, duracionCiclo, logger);

            if (datosProcesosCargados != null) {
                for (int i = 0; i < datosProcesosCargados.procesos.getTamano(); i++) {
                    Proceso p = datosProcesosCargados.procesos.obtener(i);
                    sistema.registrarYAdmitirProceso(p);
                }
            }

            VentanaSimulador ventana = new VentanaSimulador(sistema);
            ventana.setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al inicializar simulación: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static PoliticaPlanificacion crearPoliticaCPU(TipoPolitica tipo) {
        if (tipo == null) return new PoliticaFCFS();
        switch (tipo) {
            case SJF: return new PoliticaSJF();
            case ROUND_ROBIN: return new PoliticaRoundRobin();
            case PRIORIDAD_APROPIATIVA: return new PoliticaPrioridadApropiativa();
            case EDF: return new PoliticaEDF();
            default: return new PoliticaFCFS();
        }
    }

    public static PoliticaPlanificacionDisco crearPoliticaDisco(TipoPoliticaDisco tipo) {
        if (tipo == null) return new PoliticaDiscoFIFO();
        switch (tipo) {
            case SSTF: return new PoliticaDiscoSSTF();
            case SCAN: return new PoliticaDiscoSCAN();
            case C_SCAN: return new PoliticaDiscoCSCAN();
            default: return new PoliticaDiscoFIFO();
        }
    }
}
