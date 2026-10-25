package gui;

import arquitectura.Cluster;
import arquitectura.Computador;
import arquitectura.RelojGlobal;
import arquitectura.GeneradorID;
import procesos.Proceso;
import politicas.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class VentanaSimulador extends JFrame {

    private Cluster cluster;
    private RelojGlobal reloj;

    // Componentes de Control Global
    private JLabel lblReloj;
    private JTextArea txtLogs;
    private JProgressBar barUsoCPUGlobal;
    private JProgressBar barUsoRAMGlobal;

    // Componentes para Crear Proceso
    private JTextField txtNombreProceso, txtTiempoProceso, txtPrioridadProceso, txtRamProceso;
    private JComboBox<String> comboComputadores;

    // Colors del Tema Dashboard Oscuro
    private static final Color COLOR_FONDO_VENTANA = new Color(24, 28, 38);
    private static final Color COLOR_PANEL = new Color(34, 40, 52);
    private static final Color COLOR_PANEL_INTERNO = new Color(20, 24, 32);
    private static final Color COLOR_TEXTO_PRINCIPAL = new Color(230, 235, 245);
    private static final Color COLOR_TEXTO_SECUNDARIO = new Color(170, 180, 200);
    private static final Color COLOR_BORDE = new Color(55, 65, 85);
    private static final Color COLOR_ACENTO_VERDE = new Color(0, 200, 115);
    private static final Color COLOR_ACENTO_AZUL = new Color(41, 121, 255);

    // Timer de Swing
    private javax.swing.Timer timerRefrescoGUI;

    public VentanaSimulador(Cluster cluster, RelojGlobal reloj) {
        this.cluster = cluster;
        this.reloj = reloj;

        setTitle("ÁvilaOS - Simulador de Sistema Operativo Distribuido");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // Fondo Principal
        getContentPane().setBackground(COLOR_FONDO_VENTANA);

        // 1. PANEL SUPERIOR: Formularios de Control
        add(crearPanelSuperiorBarraControl(), BorderLayout.NORTH);

        // 2. PANEL CENTRAL: Cluster de Computadores
        add(crearPanelClusterNodos(), BorderLayout.CENTER);

        // 3. PANEL DERECHO: Logs y Métricas
        add(crearPanelMétricasYLogs(), BorderLayout.EAST);

        // Iniciar temporizador visual
        iniciarTimerRefresco();
    }

    private JPanel crearPanelSuperiorBarraControl() {
        JPanel panelContenedor = new JPanel(new GridLayout(2, 1, 8, 8));
        panelContenedor.setOpaque(false);

        // --- FILA 1: Formulario para Crear Proceso ---
        JPanel panelCrear = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panelCrear.setBackground(COLOR_PANEL);
        panelCrear.setBorder(crearBordeTitutlado("Crear y Asignar Nuevo Proceso"));

        txtNombreProceso = crearTextField("Proceso_" + GeneradorID.generar(), 8);
        txtTiempoProceso = crearTextField("5", 4);
        txtPrioridadProceso = crearTextField("1", 4);
        txtRamProceso = crearTextField("1024", 5);

        comboComputadores = new JComboBox<>();
        comboComputadores.setBackground(COLOR_PANEL_INTERNO);
        comboComputadores.setForeground(COLOR_TEXTO_PRINCIPAL);
        comboComputadores.addItem("Automático (Mejor RAM)");
        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            comboComputadores.addItem(cluster.getComputador(i).getNombre());
        }

        JButton btnAgregar = new JButton("Crear Proceso");
        btnAgregar.setBackground(COLOR_ACENTO_VERDE);
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnAgregar.setFocusPainted(false);
        btnAgregar.addActionListener(e -> agregarNuevoProceso());

        panelCrear.add(crearLabel("Nombre:"));
        panelCrear.add(txtNombreProceso);
        panelCrear.add(crearLabel("Tiempo CPU:"));
        panelCrear.add(txtTiempoProceso);
        panelCrear.add(crearLabel("Prioridad:"));
        panelCrear.add(txtPrioridadProceso);
        panelCrear.add(crearLabel("RAM (MB):"));
        panelCrear.add(txtRamProceso);
        panelCrear.add(crearLabel("Asignar a:"));
        panelCrear.add(comboComputadores);
        panelCrear.add(btnAgregar);

        // --- FILA 2: Control del Reloj ---
        JPanel panelRelojControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        panelRelojControl.setBackground(COLOR_PANEL);
        panelRelojControl.setBorder(crearBordeTitutlado("Estado y Control del Reloj Global"));

        lblReloj = new JLabel("Reloj de Simulación: Ciclo 0", SwingConstants.CENTER);
        lblReloj.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblReloj.setForeground(COLOR_ACENTO_VERDE);

        panelRelojControl.add(lblReloj);

        panelContenedor.add(panelCrear);
        panelContenedor.add(panelRelojControl);

        return panelContenedor;
    }

    private JPanel crearPanelMétricasYLogs() {
        JPanel panelMétricas = new JPanel(new BorderLayout(8, 8));
        panelMétricas.setPreferredSize(new Dimension(360, 0));
        panelMétricas.setBackground(COLOR_PANEL);
        panelMétricas.setBorder(crearBordeTitutlado("Métricas y Eventos Globales"));

        // Barras Globales
        JPanel panelBarras = new JPanel(new GridLayout(2, 1, 6, 6));
        panelBarras.setOpaque(false);
        barUsoCPUGlobal = crearBarraProgresiva("Uso de CPU Global", COLOR_ACENTO_AZUL);
        barUsoRAMGlobal = crearBarraProgresiva("Ocupación RAM Cluster", new Color(255, 145, 0));
        panelBarras.add(barUsoCPUGlobal);
        panelBarras.add(barUsoRAMGlobal);

        // Caja de Logs
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setBackground(COLOR_PANEL_INTERNO);
        txtLogs.setForeground(COLOR_TEXTO_PRINCIPAL);
        txtLogs.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLogs.setCaretColor(Color.WHITE);
        JScrollPane scrollLogs = new JScrollPane(txtLogs);
        scrollLogs.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));

        panelMétricas.add(panelBarras, BorderLayout.NORTH);
        panelMétricas.add(scrollLogs, BorderLayout.CENTER);

        return panelMétricas;
    }

    private JPanel crearPanelClusterNodos() {
        JPanel panelNodosContainer = new JPanel(new GridLayout(1, cluster.getCantidadComputadores(), 12, 12));
        panelNodosContainer.setOpaque(false);

        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            Computador nodo = cluster.getComputador(i);
            panelNodosContainer.add(crearPanelTarjetaNodo(nodo));
        }

        JScrollPane scrollNodos = new JScrollPane(panelNodosContainer);
        scrollNodos.setOpaque(false);
        scrollNodos.getViewport().setOpaque(false);
        scrollNodos.setBorder(null);

        return panelNodosContainer;
    }

    private JPanel crearPanelTarjetaNodo(Computador nodo) {
        JPanel tarjeta = new JPanel(new BorderLayout(6, 6));
        tarjeta.setBackground(COLOR_PANEL);
        tarjeta.setBorder(crearBordeTitutlado(nodo.getNombre()));

        // Encabezado del Nodo (Política + RAM)
        JPanel headerNodo = new JPanel(new GridLayout(2, 1, 4, 4));
        headerNodo.setOpaque(false);

        JComboBox<String> comboPolitica = new JComboBox<>(new String[]{"Round Robin", "FCFS", "SJF", "EDF", "Prioridad"});
        comboPolitica.setBackground(COLOR_PANEL_INTERNO);
        comboPolitica.setForeground(COLOR_TEXTO_PRINCIPAL);
        comboPolitica.addActionListener(e -> {
            String sel = (String) comboPolitica.getSelectedItem();
            switch (sel) {
                case "FCFS": nodo.getPlanificador().setPolitica(new PoliticaFCFS()); break;
                case "SJF": nodo.getPlanificador().setPolitica(new PoliticaSJF()); break;
                case "EDF": nodo.getPlanificador().setPolitica(new PoliticaEDF()); break;
                case "Prioridad": nodo.getPlanificador().setPolitica(new PoliticaPrioridadApropiativa()); break;
                default: nodo.getPlanificador().setPolitica(new PoliticaRoundRobin()); break;
            }
        });

        JProgressBar barRAMNodo = new JProgressBar(0, nodo.getRamTotal());
        barRAMNodo.setName("barRAM_" + nodo.getNombre());
        barRAMNodo.setStringPainted(true);
        barRAMNodo.setForeground(COLOR_ACENTO_VERDE);
        barRAMNodo.setBackground(COLOR_PANEL_INTERNO);

        headerNodo.add(comboPolitica);
        headerNodo.add(barRAMNodo);
        tarjeta.add(headerNodo, BorderLayout.NORTH);

        // Colas de Trabajo
        JPanel panelColas = new JPanel(new GridLayout(1, 5, 4, 4));
        panelColas.setOpaque(false);

        panelColas.add(crearSubPanelCola("Nuevos", "lstNuevos_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("Listos", "lstListos_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("CPU", "lstCPU_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("Bloqueados", "lstBloq_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("Terminados", "lstTerm_" + nodo.getNombre()));

        tarjeta.add(panelColas, BorderLayout.CENTER);

        return tarjeta;
    }

    private JPanel crearSubPanelCola(String titulo, String nombreComponente) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(COLOR_PANEL_INTERNO);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_BORDE), titulo,
            TitledBorder.CENTER, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 10), COLOR_TEXTO_SECUNDARIO));

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> lista = new JList<>(model);
        lista.setName(nombreComponente);
        lista.setBackground(COLOR_PANEL_INTERNO);
        lista.setForeground(COLOR_TEXTO_PRINCIPAL);
        lista.setFont(new Font("SansSerif", Font.PLAIN, 10));

        JScrollPane sp = new JScrollPane(lista);
        sp.setBorder(null);

        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JLabel crearLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(COLOR_TEXTO_PRINCIPAL);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        return label;
    }

    private JTextField crearTextField(String textoInicial, int columnas) {
        JTextField tf = new JTextField(textoInicial, columnas);
        tf.setBackground(COLOR_PANEL_INTERNO);
        tf.setForeground(COLOR_TEXTO_PRINCIPAL);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDE),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        return tf;
    }

    private void agregarNuevoProceso() {
        try {
            String nombre = txtNombreProceso.getText().trim();
            int tiempo = Integer.parseInt(txtTiempoProceso.getText().trim());
            int prioridad = Integer.parseInt(txtPrioridadProceso.getText().trim());
            int ram = Integer.parseInt(txtRamProceso.getText().trim());

            int id = GeneradorID.generar();
            Proceso p = new Proceso(id, nombre, tiempo, prioridad, ram);

            int seleccionDestino = comboComputadores.getSelectedIndex();
            int cicloActual = reloj.getCicloActual();

            if (seleccionDestino == 0) {
                cluster.asignarProcesoAutomatico(p, cicloActual);
                registrarLog("Proceso " + nombre + " (RAM: " + ram + "MB) asignado AUTOMÁTICAMENTE.");
            } else {
                int idxComputador = seleccionDestino - 1;
                cluster.asignarProcesoManual(p, idxComputador, cicloActual);
                registrarLog("Proceso " + nombre + " asignado a " + cluster.getComputador(idxComputador).getNombre());
            }

            txtNombreProceso.setText("Proceso_" + GeneradorID.generar());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese valores numéricos válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciarTimerRefresco() {
        timerRefrescoGUI = new javax.swing.Timer(250, e -> refrescarInterfaz());
        timerRefrescoGUI.start();
    }

    private void refrescarInterfaz() {
        lblReloj.setText("Reloj de Simulación: Ciclo " + reloj.getCicloActual());

        int ramUsadaTotal = 0;
        int ramCapacidadTotal = 0;

        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            Computador nodo = cluster.getComputador(i);
            ramUsadaTotal += nodo.getRamUsada();
            ramCapacidadTotal += nodo.getRamTotal();

            JProgressBar barRAM = findComponentByName(this, "barRAM_" + nodo.getNombre(), JProgressBar.class);
            if (barRAM != null) {
                barRAM.setValue(nodo.getRamUsada());
                barRAM.setString("RAM: " + nodo.getRamUsada() + " / " + nodo.getRamTotal() + " MB");
            }

            actualizarListaGUI("lstListos_" + nodo.getNombre(), nodo.getPlanificador().getColaListos());
            actualizarListaGUI("lstBloq_" + nodo.getNombre(), nodo.getPlanificador().getColaBloqueados());
            actualizarListaGUI("lstTerm_" + nodo.getNombre(), nodo.getPlanificador().getColaTerminados());

            JList<String> lstCPU = findComponentByName(this, "lstCPU_" + nodo.getNombre(), JList.class);
            if (lstCPU != null) {
                DefaultListModel<String> model = (DefaultListModel<String>) lstCPU.getModel();
                model.clear();
                Proceso ejec = nodo.getPlanificador().getProcesoEnEjecucion();
                if (ejec != null) {
                    model.addElement(">> " + ejec.getNombre() + " (T:" + ejec.getTiempoRestante() + ")");
                }
            }
        }

        if (ramCapacidadTotal > 0) {
            int pctRAM = (int) (((double) ramUsadaTotal / ramCapacidadTotal) * 100);
            barUsoRAMGlobal.setValue(pctRAM);
            barUsoRAMGlobal.setString("RAM Cluster: " + pctRAM + "%");
        }
    }

    private void actualizarListaGUI(String nombreComponente, edd.Cola<Proceso> colaOrigen) {
        JList<String> lst = findComponentByName(this, nombreComponente, JList.class);
        if (lst == null) return;

        DefaultListModel<String> model = (DefaultListModel<String>) lst.getModel();
        model.clear();

        edd.Cola<Proceso> aux = new edd.Cola<>();
        while (!colaOrigen.estaVacia()) {
            Proceso p = colaOrigen.desencolar();
            model.addElement(p.getNombre() + " (T:" + p.getTiempoRestante() + ")");
            aux.encolar(p);
        }
        while (!aux.estaVacia()) {
            colaOrigen.encolar(aux.desencolar());
        }
    }

    private void registrarLog(String mensaje) {
        txtLogs.append("[Ciclo " + reloj.getCicloActual() + "] " + mensaje + "\n");
        txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
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

    private JProgressBar crearBarraProgresiva(String titulo, Color color) {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(0);
        pb.setStringPainted(true);
        pb.setString(titulo + ": 0%");
        pb.setForeground(color);
        pb.setBackground(COLOR_PANEL_INTERNO);
        return pb;
    }

    private TitledBorder crearBordeTitutlado(String titulo) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_BORDE, 1),
            titulo, TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), COLOR_TEXTO_PRINCIPAL);
    }
}