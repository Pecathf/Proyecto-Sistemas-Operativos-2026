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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;

public class VentanaSimulador extends JFrame {

    private Cluster sistema;

    // Barra superior
    private JLabel lblCiclo;
    private JButton btnIniciarPausar;
    private JButton btnPasoAPaso;
    private JComboBox<ModoUsuario> cmbModoUsuario;
    private JComboBox<TipoPolitica> cmbPoliticaCPU;
    private JComboBox<TipoPoliticaDisco> cmbPoliticaDisco;
    private JSlider sldVelocidad;

    // Panel Izquierdo: JTree y Detalles del Nodo
    private JTree arbolFS;
    private DefaultTreeModel modeloArbol;
    private JTextArea txtDetalleNodo;

    // Panel Central: Disco Visual, Cabezal, Cola E/S y Tabla Asignación
    private PanelMapaDisco panelMapaDisco;
    private JLabel lblInfoCabezal;
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
        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
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

        JButton btnNuevoProcCPU = new JButton("+ Proceso CPU");
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

        root.add(barraTop, BorderLayout.NORTH);

        // =====================================================================
        // PANEL CENTRAL DIVIDIDO EN 3 COLUMNAS
        // =====================================================================
        JSplitPane splitIzqCentro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitIzqCentro.setDividerLocation(310);

        JSplitPane splitCentroDer = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitCentroDer.setDividerLocation(640);

        // --- COLUMNA 1: ÁRBOL DE ARCHIVOS Y OPERACIONES CRUD ---
        JPanel pnlIzq = new JPanel(new BorderLayout(6, 6));
        pnlIzq.setBackground(new Color(24, 28, 38));
        pnlIzq.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 216)),
                " Sistema de Archivos Jerárquico ", 0, 0, null, Color.WHITE));

        modeloArbol = new DefaultTreeModel(new DefaultMutableTreeNode("/"));
        arbolFS = new JTree(modeloArbol);
        arbolFS.setBackground(new Color(30, 35, 48));
        arbolFS.setForeground(Color.WHITE);
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
        txtDetalleNodo.setBackground(new Color(18, 21, 29));
        txtDetalleNodo.setForeground(new Color(165, 215, 255));
        txtDetalleNodo.setFont(new Font("Consolas", Font.PLAIN, 11));

        pnlCRUD.add(botonesCRUD, BorderLayout.NORTH);
        pnlCRUD.add(new JScrollPane(txtDetalleNodo), BorderLayout.CENTER);
        pnlIzq.add(pnlCRUD, BorderLayout.SOUTH);

        // --- COLUMNA 2: DISCO SIMULADO Y TABLA DE ASIGNACIÓN ---
        JPanel pnlCentro = new JPanel(new BorderLayout(6, 6));
        pnlCentro.setBackground(new Color(24, 28, 38));

        JPanel pnlDiscoTop = new JPanel(new BorderLayout(4, 4));
        pnlDiscoTop.setBackground(new Color(24, 28, 38));
        pnlDiscoTop.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 171, 0)),
                " Mapa Visual de Bloques del Disco y Swap ", 0, 0, null, Color.WHITE));

        lblInfoCabezal = new JLabel("Cabezal: 0 | Dirección: ↑ | Desplazamiento Total: 0");
        lblInfoCabezal.setForeground(new Color(255, 214, 0));
        lblInfoCabezal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlDiscoTop.add(lblInfoCabezal, BorderLayout.NORTH);

        panelMapaDisco = new PanelMapaDisco();
        pnlDiscoTop.add(panelMapaDisco, BorderLayout.CENTER);

        // Cola de Solicitudes E/S
        modeloColaIO = new DefaultListModel<>();
        JList<String> lstColaIO = new JList<>(modeloColaIO);
        lstColaIO.setBackground(new Color(18, 21, 29));
        lstColaIO.setForeground(new Color(255, 200, 120));
        JScrollPane spColaIO = new JScrollPane(lstColaIO);
        spColaIO.setPreferredSize(new Dimension(200, 95));
        spColaIO.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), " Cola de Solicitudes E/S ", 0, 0, null, Color.LIGHT_GRAY));
        pnlDiscoTop.add(spColaIO, BorderLayout.SOUTH);

        pnlCentro.add(pnlDiscoTop, BorderLayout.CENTER);

        // Tabla de Asignación de Archivos
        String[] columnas = {"Color", "Archivo", "Bloques", "Primer Blq", "Dueño", "Permiso", "Creador"};
        modeloTablaAsignacion = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaAsignacion = new JTable(modeloTablaAsignacion);
        tablaAsignacion.setRowHeight(22);
        tablaAsignacion.getColumnModel().getColumn(0).setMaxWidth(50);
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
        spTabla.setPreferredSize(new Dimension(400, 210));
        spTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 200, 115)),
                " Tabla de Asignación de Archivos (Encadenada) ", 0, 0, null, Color.WHITE));
        pnlCentro.add(spTabla, BorderLayout.SOUTH);

        // --- COLUMNA 3: COMPUTADOR ÚNICO (7 ESTADOS) Y GRÁFICO ---
        JPanel pnlDer = new JPanel(new BorderLayout(6, 6));
        pnlDer.setBackground(new Color(24, 28, 38));
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

        pnlColas.add(crearPanelCola("Cola LISTOS (RAM)", modListos, new Color(0, 200, 115)));
        pnlColas.add(crearPanelCola("Cola BLOQUEADOS (E/S - RAM)", modBloqueados, new Color(255, 171, 0)));
        pnlColas.add(crearPanelCola("Cola LISTO / SUSPENDIDO (Swap)", modListoSusp, new Color(0, 229, 255)));
        pnlColas.add(crearPanelCola("Cola BLOQUEADO / SUSPENDIDO (Swap)", modBloqSusp, new Color(200, 120, 255)));
        pnlColas.add(crearPanelCola("Procesos TERMINADOS", modTerminados, Color.LIGHT_GRAY));

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
        txtLog.setBackground(new Color(14, 16, 22));
        txtLog.setForeground(new Color(180, 255, 180));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane spLog = new JScrollPane(txtLog);
        spLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY), " Bitácora de Eventos del Sistema ", 0, 0, null, Color.LIGHT_GRAY));
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
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(bordeColor), " " + titulo + " ", 0, 0, null, bordeColor));
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
        public PanelMapaDisco() {
            setBackground(new Color(16, 19, 27));
            setPreferredSize(new Dimension(560, 240));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            DiscoSimulado disco = sistema.getDisco();
            if (disco == null) return;

            int total = disco.getTotalBloques();
            int cols = 20;
            int rows = (int) Math.ceil((double) total / cols);

            int pad = 8;
            int cellW = Math.max(16, (getWidth() - pad * 2) / cols);
            int cellH = Math.max(16, (getHeight() - pad * 2) / Math.max(1, rows));

            g.setFont(new Font("Consolas", Font.PLAIN, 9));

            for (int i = 0; i < total; i++) {
                Bloque b = disco.getBloque(i);
                int r = i / cols;
                int c = i % cols;
                int x = pad + c * cellW;
                int y = pad + r * cellH;

                if (b.isOcupado()) {
                    g.setColor(b.getColorArchivo() != null ? b.getColorArchivo() : Color.CYAN);
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
                g.drawString(String.valueOf(i), x + 3, y + cellH - 4);
            }
        }
    }
}
