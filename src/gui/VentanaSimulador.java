package gui;

/**
 *
 * @author Jabri
 */

import archivos.EntradaFS;
import archivos.ModoUsuario;
import archivos.PermisoArchivo;
import archivos.SistemaArchivos;
import arquitectura.Cluster;
import arquitectura.Computador;
import disco.*;
import edd.ListaEnlazada;
import politicas.TipoPolitica;
import procesos.Proceso;
import utilidades.GestorJSON;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

public class VentanaSimulador extends JFrame {

    private static final Color COLOR_TITULO_PANEL = new Color(35, 40, 50);

    private Cluster sistema;

    // Barra superior
    private JLabel lblCiclo;
    private JButton btnIniciarPausar;
    private JButton btnPasoAPaso;
    private JComboBox<ModoUsuario> cmbModoUsuario;
    private JComboBox<TipoPolitica> cmbPoliticaCPU;
    private JComboBox<TipoPoliticaDisco> cmbPoliticaDisco;
    private JSlider sldVelocidad;
    private JButton btnNuevoProcCPU;

    // Panel Izquierdo: JTree y Detalles del Nodo
    private JTree arbolFS;
    private DefaultTreeModel modeloArbol;
    private JTextArea txtDetalleNodo;

    // Panel Central: Disco Visual, Cabezal, Cola E/S y Tabla Asignación
    private PanelMapaDisco panelMapaDisco;
    private JTextArea lblInfoCabezal;
    private DefaultListModel<String> modeloColaIO;
    private JTable tablaAsignacion;
    private DefaultTableModel modeloTablaAsignacion;

    // Panel Derecho: Computador Único (CPU, RAM y Colas 7 Estados)
    private JLabel lblEstadoCPU;
    private JProgressBar barRAM;
    private DefaultListModel<Proceso> modListos;
    private DefaultListModel<Proceso> modBloqueados;
    private DefaultListModel<Proceso> modListoSusp;
    private DefaultListModel<Proceso> modBloqSusp;
    private DefaultListModel<Proceso> modTerminados;
    private PanelGraficoDisco panelGraficoDisco;

    // Panel Inferior: Bitácora
    private JTextArea txtLog;

    public VentanaSimulador(Cluster sistema) {
        this.sistema = sistema;

        setTitle("ÁvilaOS 2.0 - Simulador de Sistema de Archivos, Disco y Memoria Virtual");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 840);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        construirInterfaz();
        conectarReloj();
        refrescarTodaLaInterfaz();
    }

    private void construirInterfaz() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(new Color(18, 21, 29));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // =====================================================================
        // BARRA SUPERIOR DE CONTROL
        // =====================================================================
        JPanel barraTop = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 6));
        barraTop.setBackground(new Color(28, 33, 45));

        lblCiclo = new JLabel("Ciclo: 0");
        lblCiclo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCiclo.setForeground(new Color(0, 229, 255));

        btnIniciarPausar = new JButton("Iniciar");
        btnIniciarPausar.setBackground(new Color(0, 200, 115));
        btnIniciarPausar.setForeground(Color.BLACK);

        btnPasoAPaso = new JButton("Paso +1");

        cmbModoUsuario = new JComboBox<>(ModoUsuario.values());
        cmbPoliticaCPU = new JComboBox<>(TipoPolitica.values());
        cmbPoliticaCPU.setSelectedItem(sistema.getComputadorUnico().getPlanificador().getPolitica().getTipo());

        cmbPoliticaDisco = new JComboBox<>(TipoPoliticaDisco.values());
        cmbPoliticaDisco.setSelectedItem(sistema.getDisco().getPolitica().getTipo());

        sldVelocidad = new JSlider(30, 1500, sistema.getReloj().getDuracionCicloMs());
        sldVelocidad.setPreferredSize(new Dimension(110, 24));
        sldVelocidad.setOpaque(false);

        btnNuevoProcCPU = new JButton("+ Proceso CPU");
        JButton btnGuardarJSON = new JButton("Guardar JSON FS");
        JButton btnMetricas = new JButton("Métricas");

        barraTop.add(lblCiclo);
        barraTop.add(btnIniciarPausar);
        barraTop.add(btnPasoAPaso);
        barraTop.add(new JLabel("Modo:") {{ setForeground(Color.WHITE); }});
        barraTop.add(cmbModoUsuario);
        barraTop.add(new JLabel("CPU:") {{ setForeground(Color.WHITE); }});
        barraTop.add(cmbPoliticaCPU);
        barraTop.add(new JLabel("Disco:") {{ setForeground(Color.WHITE); }});
        barraTop.add(cmbPoliticaDisco);
        barraTop.add(new JLabel("ms:") {{ setForeground(Color.WHITE); }});
        barraTop.add(sldVelocidad);
        barraTop.add(btnNuevoProcCPU);
        barraTop.add(btnGuardarJSON);
        barraTop.add(btnMetricas);

        barraTop.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (barraTop.getPreferredSize().height != barraTop.getHeight()) {
                    SwingUtilities.invokeLater(() -> {
                        barraTop.revalidate();
                        root.revalidate();
                    });
                }
            }
        });

        root.add(barraTop, BorderLayout.NORTH);

        // =====================================================================
        // PANEL CENTRAL DIVIDIDO EN 3 COLUMNAS
        // =====================================================================
        JSplitPane splitIzqCentro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitIzqCentro.setDividerLocation(295);
        splitIzqCentro.setResizeWeight(0.20);
        splitIzqCentro.setContinuousLayout(true);

        JSplitPane splitCentroDer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitCentroDer.setDividerLocation(620);
        splitCentroDer.setResizeWeight(0.56);
        splitCentroDer.setContinuousLayout(true);

        // --- COLUMNA 1: ÁRBOL DE ARCHIVOS Y OPERACIONES CRUD ---
        JPanel pnlIzq = new JPanel(new BorderLayout(6, 6));
        pnlIzq.setBackground(new Color(24, 28, 38));
        pnlIzq.setMinimumSize(new Dimension(200, 200));
        pnlIzq.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 216)),
                " Sistema de Archivos Jerárquico ", 0, 0, null, Color.WHITE));

        modeloArbol = new DefaultTreeModel(new DefaultMutableTreeNode("/"));
        arbolFS = new JTree(modeloArbol);
        arbolFS.setBackground(new Color(30, 35, 48));
        arbolFS.setForeground(Color.WHITE);
        DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer();
        treeRenderer.setBackgroundNonSelectionColor(new Color(30, 35, 48));
        treeRenderer.setTextNonSelectionColor(Color.WHITE);
        treeRenderer.setBackgroundSelectionColor(new Color(0, 120, 180));
        treeRenderer.setTextSelectionColor(Color.WHITE);
        arbolFS.setCellRenderer(treeRenderer);
        pnlIzq.add(new JScrollPane(arbolFS), BorderLayout.CENTER);

        JPanel pnlCRUD = new JPanel(new BorderLayout(4, 4));
        pnlCRUD.setOpaque(false);

        JPanel botonesCRUD = new JPanel(new GridLayout(2, 2, 4, 4));
        botonesCRUD.setOpaque(false);
        JButton btnCrear = new JButton("Crear (C)");
        JButton btnLeer = new JButton("Leer (R)");
        JButton btnRenombrar = new JButton("Renombrar (U)");
        JButton btnEliminar = new JButton("Eliminar (D)");
        botonesCRUD.add(btnCrear);
        botonesCRUD.add(btnLeer);
        botonesCRUD.add(btnRenombrar);
        botonesCRUD.add(btnEliminar);

        txtDetalleNodo = new JTextArea(5, 20);
        txtDetalleNodo.setEditable(false);
        txtDetalleNodo.setLineWrap(true);
        txtDetalleNodo.setWrapStyleWord(true);
        txtDetalleNodo.setBackground(new Color(18, 21, 29));
        txtDetalleNodo.setForeground(new Color(165, 215, 255));
        txtDetalleNodo.setFont(new Font("Consolas", Font.PLAIN, 11));

        pnlCRUD.add(botonesCRUD, BorderLayout.NORTH);
        pnlCRUD.add(new JScrollPane(txtDetalleNodo), BorderLayout.CENTER);
        pnlIzq.add(pnlCRUD, BorderLayout.SOUTH);

        // --- COLUMNA 2: DISCO SIMULADO Y TABLA DE ASIGNACIÓN ---
        JPanel pnlCentro = new JPanel(new BorderLayout(6, 6));
        pnlCentro.setBackground(new Color(24, 28, 38));
        pnlCentro.setMinimumSize(new Dimension(280, 200));

        JPanel pnlDiscoTop = new JPanel(new BorderLayout(4, 4));
        pnlDiscoTop.setBackground(new Color(24, 28, 38));
        pnlDiscoTop.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 171, 0)),
                " Mapa Visual de Bloques del Disco y Swap ", 0, 0, null, Color.WHITE));

        lblInfoCabezal = new JTextArea("Cabezal: 0 | Dirección: ↑ | Desplazamiento Total: 0") {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(100, d.height);
            }
        };
        lblInfoCabezal.setEditable(false);
        lblInfoCabezal.setFocusable(false);
        lblInfoCabezal.setOpaque(false);
        lblInfoCabezal.setBorder(null);
        lblInfoCabezal.setLineWrap(true);
        lblInfoCabezal.setWrapStyleWord(true);
        lblInfoCabezal.setForeground(new Color(255, 214, 0));
        lblInfoCabezal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlDiscoTop.add(lblInfoCabezal, BorderLayout.NORTH);

        pnlDiscoTop.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(pnlDiscoTop::revalidate);
            }
        });

        panelMapaDisco = new PanelMapaDisco();
        JScrollPane spMapaDisco = new JScrollPane(panelMapaDisco);
        spMapaDisco.setBorder(BorderFactory.createEmptyBorder());
        spMapaDisco.getViewport().setBackground(new Color(16, 19, 27));
        spMapaDisco.getVerticalScrollBar().setUnitIncrement(16);
        spMapaDisco.getHorizontalScrollBar().setUnitIncrement(16);
        pnlDiscoTop.add(spMapaDisco, BorderLayout.CENTER);

        // Cola de Solicitudes E/S
        modeloColaIO = new DefaultListModel<>();
        JList<String> lstColaIO = new JList<>(modeloColaIO);
        lstColaIO.setBackground(new Color(18, 21, 29));
        lstColaIO.setForeground(new Color(255, 200, 120));
        JScrollPane spColaIO = new JScrollPane(lstColaIO);
        spColaIO.setPreferredSize(new Dimension(200, 82));
        spColaIO.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 110, 130)), " Cola de Solicitudes E/S ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TITULO_PANEL));
        pnlDiscoTop.add(spColaIO, BorderLayout.SOUTH);

        pnlCentro.add(pnlDiscoTop, BorderLayout.CENTER);

        // Tabla de Asignación de Archivos
        String[] columnas = {"Color", "Archivo", "Bloques", "Primer Blq", "Dueño", "Permiso", "Creador"};
        modeloTablaAsignacion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaAsignacion = new JTable(modeloTablaAsignacion) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width < getParent().getWidth();
            }
        };
        tablaAsignacion.setRowHeight(22);
        tablaAsignacion.setBackground(new Color(18, 21, 29));
        tablaAsignacion.setForeground(Color.WHITE);
        tablaAsignacion.setGridColor(new Color(40, 46, 62));
        tablaAsignacion.getTableHeader().setBackground(new Color(28, 33, 45));
        tablaAsignacion.getTableHeader().setForeground(Color.WHITE);
        tablaAsignacion.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(new Color(28, 33, 45));
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(50, 58, 75)));
                return lbl;
            }
        });
        tablaAsignacion.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaAsignacion.getColumnModel().getColumn(0).setPreferredWidth(44);
        tablaAsignacion.getColumnModel().getColumn(0).setMinWidth(40);
        tablaAsignacion.getColumnModel().getColumn(1).setPreferredWidth(165);
        tablaAsignacion.getColumnModel().getColumn(1).setMinWidth(110);
        tablaAsignacion.getColumnModel().getColumn(2).setPreferredWidth(60);
        tablaAsignacion.getColumnModel().getColumn(2).setMinWidth(50);
        tablaAsignacion.getColumnModel().getColumn(3).setPreferredWidth(70);
        tablaAsignacion.getColumnModel().getColumn(3).setMinWidth(58);
        tablaAsignacion.getColumnModel().getColumn(4).setPreferredWidth(64);
        tablaAsignacion.getColumnModel().getColumn(4).setMinWidth(50);
        tablaAsignacion.getColumnModel().getColumn(5).setPreferredWidth(64);
        tablaAsignacion.getColumnModel().getColumn(5).setMinWidth(50);
        tablaAsignacion.getColumnModel().getColumn(6).setPreferredWidth(95);
        tablaAsignacion.getColumnModel().getColumn(6).setMinWidth(70);
        tablaAsignacion.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel();
                lbl.setOpaque(true);
                if (value instanceof Color) {
                    lbl.setBackground((Color) value);
                }
                return lbl;
            }
        });

        JScrollPane spTabla = new JScrollPane(tablaAsignacion);
        spTabla.getViewport().setBackground(new Color(18, 21, 29));
        spTabla.setPreferredSize(new Dimension(400, 175));
        spTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 170, 85)),
                " Tabla de Asignación de Archivos (Encadenada) ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TITULO_PANEL));
        pnlCentro.add(spTabla, BorderLayout.SOUTH);

        // --- COLUMNA 3: COMPUTADOR ÚNICO (7 ESTADOS) Y GRÁFICO ---
        JPanel pnlDer = new JPanel(new BorderLayout(6, 6));
        pnlDer.setBackground(new Color(24, 28, 38));
        pnlDer.setMinimumSize(new Dimension(240, 200));
        pnlDer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 90, 220)),
                " Computador Único (Modelo de 7 Estados) ", 0, 0, null, Color.WHITE));

        JPanel pnlCPURAM = new JPanel(new GridLayout(2, 1, 4, 4));
        pnlCPURAM.setOpaque(false);
        lblEstadoCPU = new JLabel("CPU: [LIBRE - Sistema Operativo]");
        lblEstadoCPU.setForeground(new Color(105, 240, 174));
        lblEstadoCPU.setFont(new Font("Segoe UI", Font.BOLD, 13));

        barRAM = new JProgressBar(0, sistema.getComputadorUnico().getMemoriaTotal());
        barRAM.setStringPainted(true);
        pnlCPURAM.add(lblEstadoCPU);
        pnlCPURAM.add(barRAM);
        pnlDer.add(pnlCPURAM, BorderLayout.NORTH);

        JPanel pnlColas = new JPanel(new GridLayout(5, 1, 4, 4));
        pnlColas.setOpaque(false);
        modListos = new DefaultListModel<>();
        modBloqueados = new DefaultListModel<>();
        modListoSusp = new DefaultListModel<>();
        modBloqSusp = new DefaultListModel<>();
        modTerminados = new DefaultListModel<>();

        pnlColas.add(crearPanelCola("Cola LISTOS (RAM)", modListos, new Color(0, 130, 65)));
        pnlColas.add(crearPanelCola("Cola BLOQUEADOS (E/S - RAM)", modBloqueados, new Color(185, 95, 0)));
        pnlColas.add(crearPanelCola("Cola LISTO / SUSPENDIDO (Swap)", modListoSusp, new Color(0, 125, 165)));
        pnlColas.add(crearPanelCola("Cola BLOQUEADO / SUSPENDIDO (Swap)", modBloqSusp, new Color(120, 35, 185)));
        pnlColas.add(crearPanelCola("Procesos TERMINADOS", modTerminados, new Color(180, 180, 180)));

        pnlDer.add(pnlColas, BorderLayout.CENTER);

        panelGraficoDisco = new PanelGraficoDisco();
        pnlDer.add(panelGraficoDisco, BorderLayout.SOUTH);

        splitCentroDer.setLeftComponent(pnlCentro);
        splitCentroDer.setRightComponent(pnlDer);
        splitIzqCentro.setLeftComponent(pnlIzq);
        splitIzqCentro.setRightComponent(splitCentroDer);

        root.add(splitIzqCentro, BorderLayout.CENTER);

        // =====================================================================
        // BITÁCORA INFERIOR
        // =====================================================================
        txtLog = new JTextArea(5, 50);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setBackground(new Color(14, 16, 22));
        txtLog.setForeground(new Color(180, 255, 180));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane spLog = new JScrollPane(txtLog);
        spLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 110, 130)), " Bitácora de Eventos del Sistema ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 11), COLOR_TITULO_PANEL));
        root.add(spLog, BorderLayout.SOUTH);

        setContentPane(root);

        // =====================================================================
        // LISTENERS Y ACCIONES
        // =====================================================================
        btnIniciarPausar.addActionListener(e -> {
            if (sistema.getReloj().isPausado()) {
                sistema.getReloj().iniciarSimulacion();
                btnIniciarPausar.setText("Pausar");
                btnIniciarPausar.setBackground(new Color(255, 171, 0));
            } else {
                sistema.getReloj().pausarSimulacion();
                btnIniciarPausar.setText("Reanudar");
                btnIniciarPausar.setBackground(new Color(0, 200, 115));
            }
        });

        btnPasoAPaso.addActionListener(e -> {
            if (!sistema.getReloj().isPausado()) {
                sistema.getReloj().pausarSimulacion();
                btnIniciarPausar.setText("Reanudar");
            }
            sistema.getReloj().avanzarUnCiclo();
        });

        cmbModoUsuario.addActionListener(e -> {
            ModoUsuario m = (ModoUsuario) cmbModoUsuario.getSelectedItem();
            sistema.getSistemaArchivos().setModoActual(m);
            refrescarBitacora();
        });

        cmbPoliticaCPU.addActionListener(e -> {
            TipoPolitica tp = (TipoPolitica) cmbPoliticaCPU.getSelectedItem();
            sistema.cambiarPoliticaCPU(VentanaConfiguracionInicial.crearPoliticaCPU(tp));
            refrescarBitacora();
        });

        cmbPoliticaDisco.addActionListener(e -> {
            TipoPoliticaDisco td = (TipoPoliticaDisco) cmbPoliticaDisco.getSelectedItem();
            sistema.getDisco().setPolitica(VentanaConfiguracionInicial.crearPoliticaDisco(td));
            refrescarBitacora();
        });

        sldVelocidad.addChangeListener(e -> sistema.getReloj().setDuracionCicloMs(sldVelocidad.getValue()));

        btnNuevoProcCPU.addActionListener(e -> dialogoCrearProcesoCPU());
        btnGuardarJSON.addActionListener(e -> exportarSistemaArchivosJSON());
        btnMetricas.addActionListener(e -> new DialogoMetricas(this, sistema).setVisible(true));

        arbolFS.addTreeSelectionListener(e -> mostrarDetallesNodoSeleccionado());
        btnCrear.addActionListener(e -> accionCRUDCrear());
        btnLeer.addActionListener(e -> accionCRUDLeer());
        btnRenombrar.addActionListener(e -> accionCRUDRenombrar());
        btnEliminar.addActionListener(e -> accionCRUDEliminar());
    }

    private JScrollPane crearPanelCola(String titulo, DefaultListModel<Proceso> modelo, Color bordeColor) {
        JList<Proceso> lista = new JList<>(modelo);
        lista.setBackground(new Color(18, 21, 29));
        lista.setForeground(Color.WHITE);
        lista.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lista.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Proceso sel = lista.getSelectedValue();
                    if (sel != null) new DialogoPCB(VentanaSimulador.this, sel).setVisible(true);
                }
            }
        });
        JScrollPane sp = new JScrollPane(lista);
        sp.setMinimumSize(new Dimension(150, 48));
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(bordeColor), " " + titulo + " ", 0, 0,
                new Font("Segoe UI", Font.BOLD, 11),
                "Procesos TERMINADOS".equals(titulo) ? COLOR_TITULO_PANEL : bordeColor));
        return sp;
    }

    private void conectarReloj() {
        sistema.getReloj().setListenerActualizacionGUI(() -> SwingUtilities.invokeLater(this::refrescarTodaLaInterfaz));
    }

    public void refrescarTodaLaInterfaz() {
        lblCiclo.setText("Ciclo: " + sistema.getReloj().getCicloActual());

        // CPU y RAM
        Computador comp = sistema.getComputadorUnico();
        Proceso enCPU = comp.getCpu().getProcesoActual();
        if (enCPU != null) {
            lblEstadoCPU.setText("CPU: " + enCPU.getNombre() + " (PC=" + enCPU.getPc() + "/" + enCPU.getInstruccionesTotales() + ")");
            lblEstadoCPU.setForeground(new Color(255, 214, 0));
        } else {
            lblEstadoCPU.setText("CPU: [LIBRE - Sistema Operativo]");
            lblEstadoCPU.setForeground(new Color(105, 240, 174));
        }

        barRAM.setValue(comp.getMemoriaUsada());
        barRAM.setString("RAM: " + comp.getMemoriaUsada() + " / " + comp.getMemoriaTotal() + " MB");

        // Colas 7 Estados
        actualizarModeloCola(modListos, comp.getColaListos().obtenerListaElementos());
        actualizarModeloCola(modBloqueados, comp.getColaBloqueados().obtenerListaElementos());
        actualizarModeloCola(modListoSusp, comp.getColaListoSuspendidos().obtenerListaElementos());
        actualizarModeloCola(modBloqSusp, comp.getColaBloqueadoSuspendidos().obtenerListaElementos());
        actualizarModeloCola(modTerminados, comp.getColaTerminados().obtenerListaElementos());

        // Disco y Cabezal
        DiscoSimulado disco = sistema.getDisco();
        String dir = disco.isDireccionSubiendo() ? "↑ (Subiendo)" : "↓ (Bajando)";
        lblInfoCabezal.setText("Cabezal: Bloque " + disco.getCabezalActual() +
                " | Dirección: " + dir +
                " | Desplazamiento Total: " + disco.getDesplazamientoTotal() +
                " | Swap Libre: " + disco.contarBloquesLibresSwap() + "/" + disco.getTamanoSwapBloques());
        lblInfoCabezal.revalidate();
        panelMapaDisco.revalidate();
        panelMapaDisco.repaint();
        panelGraficoDisco.registrarPunto(disco.getDesplazamientoTotal());

        // Cola de Solicitudes E/S
        modeloColaIO.clear();
        if (disco.getSolicitudEnCurso() != null) {
            modeloColaIO.addElement("[EN CURSO] " + disco.getSolicitudEnCurso().toString());
        }
        ListaEnlazada<SolicitudIO> pend = disco.getColaSolicitudesSnapshot();
        for (int i = 0; i < pend.getTamano(); i++) {
            modeloColaIO.addElement(pend.obtener(i).toString());
        }

        // Árbol y Tabla de Asignación
        reconstruirArbolFS();
        actualizarTablaAsignacion();
        refrescarBitacora();
    }

    private void actualizarModeloCola(DefaultListModel<Proceso> mod, ListaEnlazada<Proceso> lista) {
        mod.clear();
        for (int i = 0; i < lista.getTamano(); i++) {
            mod.addElement(lista.obtener(i));
        }
    }

    private void reconstruirArbolFS() {
        SistemaArchivos fs = sistema.getSistemaArchivos();
        if (fs == null) return;
        DefaultMutableTreeNode rootNode = construirNodoVisual(fs.getRaiz());
        modeloArbol.setRoot(rootNode);
        for (int i = 0; i < arbolFS.getRowCount(); i++) {
            arbolFS.expandRow(i);
        }
    }

    private DefaultMutableTreeNode construirNodoVisual(EntradaFS entrada) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(entrada);
        if (entrada.isEsDirectorio() && entrada.getHijos() != null) {
            for (int i = 0; i < entrada.getHijos().getTamano(); i++) {
                nodo.add(construirNodoVisual(entrada.getHijos().obtener(i)));
            }
        }
        return nodo;
    }

    private void actualizarTablaAsignacion() {
        modeloTablaAsignacion.setRowCount(0);
        ListaEnlazada<EntradaFS> archivos = sistema.getSistemaArchivos().obtenerTodosLosArchivos();
        for (int i = 0; i < archivos.getTamano(); i++) {
            EntradaFS f = archivos.obtener(i);
            modeloTablaAsignacion.addRow(new Object[]{
                f.getColor(),
                f.getRutaCompleta(),
                f.getTamanoBloques(),
                f.getPrimerBloque(),
                f.getDueno(),
                f.getPermiso().getValorJson(),
                f.getProcesoCreador()
            });
        }
    }

    private void refrescarBitacora() {
        txtLog.setText(sistema.getLogger().obtenerHistorialCompleto());
    }

    private EntradaFS obtenerEntradaSeleccionadaArbol() {
        TreePath path = arbolFS.getSelectionPath();
        if (path == null) return sistema.getSistemaArchivos().getRaiz();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node.getUserObject() instanceof EntradaFS) {
            return (EntradaFS) node.getUserObject();
        }
        return sistema.getSistemaArchivos().getRaiz();
    }

    private void mostrarDetallesNodoSeleccionado() {
        EntradaFS sel = obtenerEntradaSeleccionadaArbol();
        if (sel == null) return;
        txtDetalleNodo.setText(
                "Ruta: " + sel.getRutaCompleta() + "\n" +
                "Tipo: " + (sel.isEsDirectorio() ? "DIRECTORIO" : "ARCHIVO") + "\n" +
                "Dueño: " + sel.getDueno() + " | Permiso: " + sel.getPermiso().getValorJson() + "\n" +
                "Tamaño: " + sel.getTamanoBloques() + " bloques | Bloque Inicial: " + sel.getPrimerBloque() + "\n" +
                "Proceso Creador: " + sel.getProcesoCreador()
        );
    }

    // =========================================================================
    // ACCIONES CRUD DESDE LA INTERFAZ
    // =========================================================================

    private void accionCRUDCrear() {
        EntradaFS sel = obtenerEntradaSeleccionadaArbol();
        String rutaDir = sel.isEsDirectorio() ? sel.getRutaCompleta() : "/";

        String[] opciones = {"Archivo", "Directorio"};
        int tipo = JOptionPane.showOptionDialog(this, "¿Qué desea crear en " + rutaDir + "?",
                "Crear Elemento", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (tipo < 0) return;

        boolean esDir = (tipo == 1);
        String nombre = JOptionPane.showInputDialog(this, "Nombre del " + (esDir ? "directorio:" : "archivo:"));
        if (nombre == null || nombre.trim().isEmpty()) return;

        int bloques = 0;
        if (!esDir) {
            String blqStr = JOptionPane.showInputDialog(this, "Tamaño en bloques:", "4");
            if (blqStr == null) return;
            try {
                bloques = Math.max(1, Integer.parseInt(blqStr.trim()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cantidad de bloques inválida.");
                return;
            }
        }

        String error = sistema.solicitarOperacionCRUDDesdeGUI(
                TipoOperacionIO.CREATE, rutaDir, nombre.trim(), esDir, bloques, PermisoArchivo.PUBLICO);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        }
        refrescarTodaLaInterfaz();
    }

    private void accionCRUDLeer() {
        EntradaFS sel = obtenerEntradaSeleccionadaArbol();
        if (sel == null || sel.isEsDirectorio()) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo en el árbol para leer.");
            return;
        }
        String error = sistema.solicitarOperacionCRUDDesdeGUI(
                TipoOperacionIO.READ, sel.getRutaCompleta(), sel.getNombre(), false, sel.getTamanoBloques(), sel.getPermiso());
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        }
        refrescarTodaLaInterfaz();
    }

    private void accionCRUDRenombrar() {
        EntradaFS sel = obtenerEntradaSeleccionadaArbol();
        if (sel == null || "/".equals(sel.getRutaCompleta())) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o directorio válido para renombrar.");
            return;
        }
        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre para " + sel.getNombre() + ":", sel.getNombre());
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return;

        String error = sistema.solicitarOperacionCRUDDesdeGUI(
                TipoOperacionIO.UPDATE, sel.getRutaCompleta(), nuevoNombre.trim(), sel.isEsDirectorio(), sel.getTamanoBloques(), sel.getPermiso());
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        }
        refrescarTodaLaInterfaz();
    }

    private void accionCRUDEliminar() {
        EntradaFS sel = obtenerEntradaSeleccionadaArbol();
        if (sel == null || "/".equals(sel.getRutaCompleta())) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo o directorio válido para eliminar.");
            return;
        }
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar " + sel.getRutaCompleta() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        String error = sistema.solicitarOperacionCRUDDesdeGUI(
                TipoOperacionIO.DELETE, sel.getRutaCompleta(), sel.getNombre(), sel.isEsDirectorio(), sel.getTamanoBloques(), sel.getPermiso());
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        }
        refrescarTodaLaInterfaz();
    }

    private void dialogoCrearProcesoCPU() {
        JTextField txtNombre = new JTextField("Tarea_CPU");
        JSpinner spnInst = new JSpinner(new SpinnerNumberModel(8, 1, 100, 1));
        JSpinner spnPrio = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        JSpinner spnMem = new JSpinner(new SpinnerNumberModel(128, 16, 2048, 16));
        JSpinner spnDead = new JSpinner(new SpinnerNumberModel(200, 10, 2000, 10));

        JPanel p = new JPanel(new GridLayout(5, 2, 6, 6));
        p.add(new JLabel("Nombre:")); p.add(txtNombre);
        p.add(new JLabel("Instrucciones:")); p.add(spnInst);
        p.add(new JLabel("Prioridad (1=Alta):")); p.add(spnPrio);
        p.add(new JLabel("Memoria RAM (MB):")); p.add(spnMem);
        p.add(new JLabel("Deadline (ciclos):")); p.add(spnDead);

        if (JOptionPane.showConfirmDialog(this, p, "Nuevo Proceso CPU-Bound", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            sistema.crearProcesoCPU(txtNombre.getText().trim(), (int) spnInst.getValue(),
                    (int) spnPrio.getValue(), (int) spnMem.getValue(), (int) spnDead.getValue());
            refrescarTodaLaInterfaz();
        }
    }

    private void exportarSistemaArchivosJSON() {
        JFileChooser fc = new JFileChooser(".");
        fc.setSelectedFile(new File("filesystem_exportado.json"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                GestorJSON.guardarSistemaArchivosJSON(sistema.getSistemaArchivos(), fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Estado guardado exitosamente en JSON.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar JSON: " + ex.getMessage());
            }
        }
    }

    // =========================================================================
    // PANEL INTERNO: MAPA VISUAL DE BLOQUES DEL DISCO
    // =========================================================================
    private class PanelMapaDisco extends JPanel {
        private static final int COLS = 20;
        private static final int PAD = 8;
        private static final int MIN_CELL_W = 22;
        private static final int MIN_CELL_H = 18;

        public PanelMapaDisco() {
            setBackground(new Color(16, 19, 27));
        }

        @Override
        public Dimension getPreferredSize() {
            DiscoSimulado disco = sistema.getDisco();
            int total = (disco != null) ? disco.getTotalBloques() : 200;
            int rows = Math.max(1, (int) Math.ceil((double) total / COLS));
            int minW = COLS * MIN_CELL_W + PAD * 2;
            int minH = rows * MIN_CELL_H + PAD * 2;
            Container parent = getParent();
            if (parent instanceof JViewport) {
                Dimension vp = parent.getSize();
                return new Dimension(Math.max(minW, vp.width), Math.max(minH, vp.height));
            }
            return new Dimension(Math.max(560, minW), Math.max(220, minH));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            DiscoSimulado disco = sistema.getDisco();
            if (disco == null) return;

            int total = disco.getTotalBloques();
            int rows = Math.max(1, (int) Math.ceil((double) total / COLS));

            int cellW = Math.max(MIN_CELL_W, (getWidth() - PAD * 2) / COLS);
            int cellH = Math.max(MIN_CELL_H, (getHeight() - PAD * 2) / rows);

            g.setFont(new Font("Consolas", Font.PLAIN, 9));
            FontMetrics fm = g.getFontMetrics();

            for (int i = 0; i < total; i++) {
                Bloque b = disco.getBloque(i);
                int r = i / COLS;
                int c = i % COLS;
                int x = PAD + c * cellW;
                int y = PAD + r * cellH;

                if (b.isOcupado()) {
                    g.setColor(b.getColor() != null ? b.getColor() : Color.CYAN);
                } else if (b.isEsAreaSwap()) {
                    g.setColor(new Color(55, 38, 72)); // Área Swap libre
                } else {
                    g.setColor(new Color(38, 44, 58)); // Bloque archivo libre
                }
                g.fillRect(x + 1, y + 1, cellW - 2, cellH - 2);

                // Resaltar posición actual del cabezal
                if (i == disco.getCabezalActual()) {
                    g.setColor(Color.YELLOW);
                    ((Graphics2D) g).setStroke(new BasicStroke(2f));
                    g.drawRect(x + 1, y + 1, cellW - 2, cellH - 2);
                }

                g.setColor(b.isOcupado() ? Color.BLACK : Color.LIGHT_GRAY);
                String txt = String.valueOf(i);
                int tx = x + Math.max(2, (cellW - fm.stringWidth(txt)) / 2);
                int ty = y + (cellH + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(txt, tx, ty);
            }
        }
    }

    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth <= 0 && target.getParent() != null) {
                    targetWidth = target.getParent().getWidth();
                }
                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = (targetWidth > 0) ? (targetWidth - horizontalInsetsAndGap) : Integer.MAX_VALUE;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) {
                dim.height += getVgap();
            }
            dim.height += rowHeight;
        }
    }
}