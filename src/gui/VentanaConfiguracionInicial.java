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
import arquitectura.RelojGlobal;
import edd.ListaEnlazada;
import politicas.PoliticaEDF;
import politicas.PoliticaFCFS;
import politicas.PoliticaPlanificacion;
import politicas.PoliticaPrioridadApropiativa;
import politicas.PoliticaRoundRobin;
import politicas.PoliticaSJF;
import politicas.TipoPolitica;
import procesos.Proceso;
import sincronizacion.Buffer;
import utilidades.ConfigComputador;
import utilidades.ConfiguracionInicial;
import utilidades.GestorArchivos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

public class VentanaConfiguracionInicial extends JFrame {

    private static final long serialVersionUID = 1L;

    private JSpinner spinNumComputadores;
    private JTextField txtDuracionCiclo;
    private JTextField txtLatenciaRed;
    private JPanel panelFilasComputadores;
    private ConfiguracionInicial configActual;

    private static final Color COLOR_FONDO = new Color(24, 28, 38);
    private static final Color COLOR_PANEL = new Color(34, 40, 52);
    private static final Color COLOR_TEXTO = new Color(230, 235, 245);
    private static final Color COLOR_BORDE = new Color(55, 65, 85);
    private static final Color COLOR_VERDE = new Color(0, 200, 115);
    private static final Color COLOR_AZUL = new Color(41, 121, 255);

    public VentanaConfiguracionInicial() {
        this.configActual = ConfiguracionInicial.crearConfiguracionPorDefecto();

        setTitle("ÁvilaOS - Configuración Inicial del Sistema Distribuido");
        setSize(780, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout(12, 12));

        // 1. TÍTULO Y PARÁMETROS GLOBALES
        JPanel panelNorte = new JPanel(new GridLayout(2, 1, 8, 8));
        panelNorte.setBackground(COLOR_FONDO);
        panelNorte.setBorder(new EmptyBorder(12, 12, 6, 12));

        JLabel lblTitulo = new JLabel("ÁvilaOS: Sistema Operativo Distribuido - Configuración", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitulo.setForeground(COLOR_VERDE);
        panelNorte.add(lblTitulo);

        JPanel panelGlobal = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 6));
        panelGlobal.setBackground(COLOR_PANEL);
        panelGlobal.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDE), "Parámetros Globales de Simulación",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11), COLOR_TEXTO));

        txtDuracionCiclo = new JTextField(String.valueOf(configActual.duracionCicloMs), 5);
        txtLatenciaRed = new JTextField(String.valueOf(configActual.latenciaRedCiclos), 4);
        spinNumComputadores = new JSpinner(new SpinnerNumberModel(2, 2, 6, 1));
        spinNumComputadores.addChangeListener(e -> actualizarFilasComputadores((int) spinNumComputadores.getValue()));

        panelGlobal.add(crearLabel("Duración Ciclo (ms):"));
        panelGlobal.add(txtDuracionCiclo);
        panelGlobal.add(crearLabel("Latencia Red (ciclos):"));
        panelGlobal.add(txtLatenciaRed);
        panelGlobal.add(crearLabel("Nº de Computadores (mín. 2):"));
        panelGlobal.add(spinNumComputadores);

        panelNorte.add(panelGlobal);
        add(panelNorte, BorderLayout.NORTH);

        // 2. FILAS DE CONFIGURACIÓN DE CADA COMPUTADOR
        panelFilasComputadores = new JPanel();
        panelFilasComputadores.setLayout(new BoxLayout(panelFilasComputadores, BoxLayout.Y_AXIS));
        panelFilasComputadores.setBackground(COLOR_FONDO);

        JScrollPane scrollFilas = new JScrollPane(panelFilasComputadores);
        scrollFilas.setBackground(COLOR_FONDO);
        scrollFilas.getViewport().setBackground(COLOR_FONDO);
        scrollFilas.setBorder(new EmptyBorder(6, 12, 6, 12));
        add(scrollFilas, BorderLayout.CENTER);

        actualizarFilasComputadores(2);

        // 3. BOTONES DE ACCIÓN (CARGAR CSV, GUARDAR CSV, INICIAR)
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        panelSur.setBackground(COLOR_FONDO);

        JButton btnCargar = new JButton("Importar Configuración CSV");
        estilarBoton(btnCargar, new Color(55, 65, 85));
        btnCargar.addActionListener(e -> accionCargarCSV());

        JButton btnGuardar = new JButton("Exportar Configuración CSV");
        estilarBoton(btnGuardar, new Color(55, 65, 85));
        btnGuardar.addActionListener(e -> accionGuardarCSV());

        JButton btnIniciar = new JButton("Iniciar Simulación ÁvilaOS");
        estilarBoton(btnIniciar, COLOR_VERDE);
        btnIniciar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnIniciar.addActionListener(e -> accionIniciarSimulacion());

        panelSur.add(btnCargar);
        panelSur.add(btnGuardar);
        panelSur.add(btnIniciar);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void actualizarFilasComputadores(int cantidad) {
        panelFilasComputadores.removeAll();
        for (int i = 1; i <= cantidad; i++) {
            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            fila.setBackground(COLOR_PANEL);
            fila.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(COLOR_BORDE), "Computador " + i,
                    TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11), COLOR_AZUL));

            JTextField txtRam = new JTextField(i == 1 ? "8192" : "16384", 6);
            txtRam.setName("ram_comp_" + i);

            JComboBox<TipoPolitica> comboPol = new JComboBox<>(TipoPolitica.values());
            comboPol.setName("pol_comp_" + i);

            JTextField txtQuantum = new JTextField("3", 3);
            txtQuantum.setName("quantum_comp_" + i);

            fila.add(crearLabel("Memoria RAM (MB):"));
            fila.add(txtRam);
            fila.add(crearLabel("Política Inicial:"));
            fila.add(comboPol);
            fila.add(crearLabel("Quantum RR:"));
            fila.add(txtQuantum);

            panelFilasComputadores.add(fila);
            panelFilasComputadores.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        panelFilasComputadores.revalidate();
        panelFilasComputadores.repaint();
    }

    private void accionCargarCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ConfiguracionInicial cargada = GestorArchivos.cargarConfiguracionCSV(fc.getSelectedFile());
                this.configActual = cargada;
                txtDuracionCiclo.setText(String.valueOf(cargada.duracionCicloMs));
                txtLatenciaRed.setText(String.valueOf(cargada.latenciaRedCiclos));
                if (cargada.computadores != null && cargada.computadores.getTamano() >= 2) {
                    spinNumComputadores.setValue(cargada.computadores.getTamano());
                }
                JOptionPane.showMessageDialog(this, "Configuración cargada correctamente desde CSV.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al leer archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void accionGuardarCSV() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                recopilarConfiguracionDesdeUI();
                File destino = fc.getSelectedFile();
                if (!destino.getName().toLowerCase().endsWith(".csv")) {
                    destino = new File(destino.getAbsolutePath() + ".csv");
                }
                GestorArchivos.guardarConfiguracionCSV(configActual, destino);
                JOptionPane.showMessageDialog(this, "Configuración guardada exitosamente en " + destino.getName(), "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void recopilarConfiguracionDesdeUI() {
        try {
            configActual.duracionCicloMs = Math.max(20, Integer.parseInt(txtDuracionCiclo.getText().trim()));
            configActual.latenciaRedCiclos = Math.max(0, Integer.parseInt(txtLatenciaRed.getText().trim()));
        } catch (NumberFormatException e) {
            configActual.duracionCicloMs = 1000;
            configActual.latenciaRedCiclos = 2;
        }

        int cantidad = (int) spinNumComputadores.getValue();
        configActual.computadores = new ListaEnlazada<>();

        for (int i = 1; i <= cantidad; i++) {
            JTextField txtRam = findComponentByName(panelFilasComputadores, "ram_comp_" + i, JTextField.class);
            JComboBox<?> comboPol = findComponentByName(panelFilasComputadores, "pol_comp_" + i, JComboBox.class);
            JTextField txtQuantum = findComponentByName(panelFilasComputadores, "quantum_comp_" + i, JTextField.class);

            int ram = 8192;
            int q = 3;
            TipoPolitica pol = TipoPolitica.ROUND_ROBIN;

            if (txtRam != null) {
                try { ram = Integer.parseInt(txtRam.getText().trim()); } catch (NumberFormatException ignored) {}
            }
            if (comboPol != null && comboPol.getSelectedItem() instanceof TipoPolitica) {
                pol = (TipoPolitica) comboPol.getSelectedItem();
            }
            if (txtQuantum != null) {
                try { q = Integer.parseInt(txtQuantum.getText().trim()); } catch (NumberFormatException ignored) {}
            }

            configActual.computadores.agregar(new ConfigComputador("Computador " + i, ram, pol, q));
        }
    }

    private void accionIniciarSimulacion() {
        recopilarConfiguracionDesdeUI();

        // 1. Crear el Reloj Global
        RelojGlobal reloj = new RelojGlobal(configActual.duracionCicloMs);

        // 2. Crear el Clúster
        Cluster cluster = new Cluster();
        cluster.setLatenciaRedCiclos(configActual.latenciaRedCiclos);

        // 3. Crear e instanciar computadores
        if (configActual.computadores != null) {
            for (int i = 0; i < configActual.computadores.getTamano(); i++) {
                ConfigComputador cfg = configActual.computadores.obtener(i);
                PoliticaPlanificacion polObj;
                TipoPolitica tipoPol = (cfg != null && cfg.politica != null) ? cfg.politica : TipoPolitica.ROUND_ROBIN;
                switch (tipoPol) {
                    case FCFS: polObj = new PoliticaFCFS(); break;
                    case SJF: polObj = new PoliticaSJF(); break;
                    case EDF: polObj = new PoliticaEDF(); break;
                    case PRIORIDAD_APROPIATIVA: polObj = new PoliticaPrioridadApropiativa(); break;
                    default: polObj = new PoliticaRoundRobin(); break;
                }
                String nombreComp = (cfg != null && cfg.nombre != null) ? cfg.nombre : "Computador " + (i + 1);
                int ramComp = (cfg != null) ? cfg.ramMb : 8192;
                int qComp = (cfg != null) ? cfg.quantum : 3;

                Computador comp = new Computador(nombreComp, ramComp, qComp, polObj);
                cluster.agregarComputador(comp);
                reloj.agregarComputador(comp);
            }
        }

        // 4. Agregar buffers iniciales
        if (configActual.buffersIniciales != null) {
            for (int i = 0; i < configActual.buffersIniciales.getTamano(); i++) {
                Buffer b = configActual.buffersIniciales.obtener(i);
                if (b != null) {
                    cluster.agregarBuffer(b);
                }
            }
        }

        // 5. Cargar procesos iniciales
        if (configActual.procesosIniciales != null) {
            for (int i = 0; i < configActual.procesosIniciales.getTamano(); i++) {
                Proceso p = configActual.procesosIniciales.obtener(i);
                if (p != null) {
                    Computador destino = cluster.buscarComputadorPorNombre(p.getComputadorAsignado());
                    if (destino != null) {
                        destino.recibirProceso(p, 0);
                    } else {
                        cluster.asignarProcesoAutomatico(p, 0);
                    }
                }
            }
        }

        // 6. Iniciar los hilos de simulación
        reloj.iniciarSimulacion();

        // 7. Abrir la ventana principal del simulador
        SwingUtilities.invokeLater(() -> {
            VentanaSimulador ventana = new VentanaSimulador(cluster, reloj);
            cluster.setLogger(ventana::registrarLog);
            ventana.setVisible(true);
        });

        this.dispose();
    }

    private JLabel crearLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(COLOR_TEXTO);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return l;
    }

    private void estilarBoton(JButton b, Color fondo) {
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentByName(Container container, String name, Class<T> type) {
        for (Component c : container.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return (T) c;
            }
            if (c instanceof Container) {
                T found = findComponentByName((Container) c, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}