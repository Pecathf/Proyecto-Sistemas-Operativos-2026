package gui;
/**
 *
 * @author Jabri
 */
import arquitectura.Cluster;
import arquitectura.Computador;
import arquitectura.GeneradorID;
import arquitectura.RelojGlobal;
import edd.ListaEnlazada;
import politicas.*;
import procesos.*;
import sincronizacion.Buffer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VentanaSimulador extends JFrame {

    private Cluster cluster;
    private RelojGlobal reloj;

    // Componentes de Control Global
    private JLabel lblReloj;
    private JButton btnPausar;
    private JSpinner spinVelocidadMs;
    private JSpinner spinQuantumRR;
    private JTextArea txtLogs;
    private JProgressBar barUsoCPUGlobal;
    private JProgressBar barUsoRAMGlobal;
    private PanelGraficoCPU panelGrafico;

    // Componentes para Crear Proceso ("Vista 1")
    private JTextField txtNombreProceso, txtTiempoProceso, txtPrioridadProceso, txtRamProceso, txtDeadlineProceso;
    private JComboBox<TipoProceso> comboTipoProceso;
    private JComboBox<String> comboComputadores;
    private JComboBox<String> comboBuffersObjetivo;
    private JTextField txtCiclosOp, txtElementosMeta, txtFrecuenciaIO, txtDuracionIO;
    private JPanel panelCamposEspeciales;

    // Componentes de Buffers
    private JPanel panelBuffersGlobal;

    // Colores del Dashboard
    private static final Color COLOR_FONDO_VENTANA = new Color(24, 28, 38);
    private static final Color COLOR_PANEL = new Color(34, 40, 52);
    private static final Color COLOR_PANEL_INTERNO = new Color(20, 24, 32);
    private static final Color COLOR_TEXTO_PRINCIPAL = new Color(230, 235, 245);
    private static final Color COLOR_TEXTO_SECUNDARIO = new Color(170, 180, 200);
    private static final Color COLOR_BORDE = new Color(55, 65, 85);
    private static final Color COLOR_VERDE = new Color(0, 200, 115);
    private static final Color COLOR_AZUL = new Color(41, 121, 255);
    private static final Color COLOR_NARANJA = new Color(255, 145, 0);

    // Timer de Swing para refrescar la interfaz (NO destructivo)
    private javax.swing.Timer timerRefrescoGUI;

    public VentanaSimulador(Cluster cluster, RelojGlobal reloj) {
        this.cluster = cluster;
        this.reloj = reloj;

        setTitle("ÁvilaOS - Simulador de Sistema Operativo Distribuido");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(COLOR_FONDO_VENTANA);

        // 1. PANEL SUPERIOR: Barra de Control y Creación de Procesos ("Vista 1")
        add(crearPanelSuperiorBarraControl(), BorderLayout.NORTH);

        // 2. PANEL CENTRAL: Nodos del Clúster con Colas y CPU
        add(crearPanelClusterNodos(), BorderLayout.CENTER);

        // 3. PANEL DERECHO: Métricas, Gráfico CPU en Tiempo Real y Logs
        add(crearPanelMétricasYLogs(), BorderLayout.EAST);

        // 4. PANEL INFERIOR: Visualización de Buffers Compartidos y Semáforos
        add(crearPanelBuffersGlobales(), BorderLayout.SOUTH);

        // Iniciar temporizador visual seguro
        iniciarTimerRefresco();
    }

    private JPanel crearPanelSuperiorBarraControl() {
        JPanel contenedor = new JPanel(new BorderLayout(6, 6));
        contenedor.setOpaque(false);
        contenedor.setBorder(new EmptyBorder(8, 10, 4, 10));

        // --- FILA 1: Control del Reloj, Velocidad, Pausa, Quantum y Métricas ---
        JPanel panelRelojControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        panelRelojControl.setBackground(COLOR_PANEL);
        panelRelojControl.setBorder(crearBordeTitulado("Control de la Simulación y Reloj Global"));

        lblReloj = new JLabel("Reloj de Simulación: Ciclo 0", SwingConstants.CENTER);
        lblReloj.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblReloj.setForeground(COLOR_VERDE);

        btnPausar = new JButton("Pausar");
        estilarBoton(btnPausar, new Color(220, 53, 69));
        btnPausar.addActionListener(e -> alternarPausaSimulacion());

        spinVelocidadMs = new JSpinner(new SpinnerNumberModel(reloj.getDuracionCicloMs(), 50, 3000, 100));
        spinVelocidadMs.setPreferredSize(new Dimension(75, 24));
        spinVelocidadMs.addChangeListener(e -> reloj.setDuracionCicloMs((int) spinVelocidadMs.getValue()));

        spinQuantumRR = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        spinQuantumRR.setPreferredSize(new Dimension(55, 24));
        spinQuantumRR.addChangeListener(e -> {
            int nuevoQ = (int) spinQuantumRR.getValue();
            for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
                cluster.getComputador(i).getPlanificador().setQuantum(nuevoQ);
            }
            registrarLog("Quantum global de Round Robin actualizado a " + nuevoQ);
        });

        JButton btnMetricas = new JButton("Ver Métricas Completas");
        estilarBoton(btnMetricas, COLOR_AZUL);
        btnMetricas.addActionListener(e -> new DialogoMetricas(this, cluster, reloj.getCicloActual()).setVisible(true));

        JButton btnCrearBuffer = new JButton("+ Crear Buffer");
        estilarBoton(btnCrearBuffer, COLOR_NARANJA);
        btnCrearBuffer.addActionListener(e -> mostrarDialogoCrearBuffer());

        panelRelojControl.add(lblReloj);
        panelRelojControl.add(btnPausar);
        panelRelojControl.add(crearLabel("Velocidad (ms/ciclo):"));
        panelRelojControl.add(spinVelocidadMs);
        panelRelojControl.add(crearLabel("Quantum RR:"));
        panelRelojControl.add(spinQuantumRR);
        panelRelojControl.add(btnMetricas);
        panelRelojControl.add(btnCrearBuffer);

        // --- FILA 2: Formulario Completo de Creación de Procesos ("Vista 1") ---
        // BoxLayout en Y: campos base arriba y campos de Productor/Consumidor/IO abajo,
        // para que al aparecer no desborden por el bug de altura preferida de FlowLayout.
        JPanel panelCrear = new JPanel();
        panelCrear.setLayout(new BoxLayout(panelCrear, BoxLayout.Y_AXIS));
        panelCrear.setBackground(COLOR_PANEL);
        panelCrear.setBorder(crearBordeTitulado("Creación y Asignación de Procesos (Vista 1)"));
        panelCrear.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtNombreProceso = crearTextField("P_" + GeneradorID.generar(), 7);
        txtTiempoProceso = crearTextField("6", 3);
        txtPrioridadProceso = crearTextField("1", 3);
        txtRamProceso = crearTextField("1024", 4);
        txtDeadlineProceso = crearTextField("15", 3);

        comboTipoProceso = new JComboBox<>(TipoProceso.values());
        comboTipoProceso.setBackground(COLOR_PANEL_INTERNO);
        comboTipoProceso.setForeground(COLOR_TEXTO_PRINCIPAL);
        comboTipoProceso.addActionListener(e -> actualizarCamposSegunTipoProceso());

        comboComputadores = new JComboBox<>();
        comboComputadores.setBackground(COLOR_PANEL_INTERNO);
        comboComputadores.setForeground(COLOR_TEXTO_PRINCIPAL);
        actualizarComboComputadores();

        comboBuffersObjetivo = new JComboBox<>();
        comboBuffersObjetivo.setBackground(COLOR_PANEL_INTERNO);
        comboBuffersObjetivo.setForeground(COLOR_TEXTO_PRINCIPAL);
        actualizarComboBuffers();

        txtCiclosOp = crearTextField("2", 3);
        txtElementosMeta = crearTextField("4", 3);
        txtFrecuenciaIO = crearTextField("3", 3);
        txtDuracionIO = crearTextField("2", 3);

        panelCamposEspeciales = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelCamposEspeciales.setOpaque(false);
        panelCamposEspeciales.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnAgregar = new JButton("Crear Proceso");
        estilarBoton(btnAgregar, COLOR_VERDE);
        btnAgregar.addActionListener(e -> accionCrearProceso());

        JPanel filaBase = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filaBase.setOpaque(false);
        filaBase.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaBase.add(crearLabel("Nombre:"));
        filaBase.add(txtNombreProceso);
        filaBase.add(crearLabel("Tipo:"));
        filaBase.add(comboTipoProceso);
        filaBase.add(crearLabel("Tiempo CPU:"));
        filaBase.add(txtTiempoProceso);
        filaBase.add(crearLabel("Prioridad:"));
        filaBase.add(txtPrioridadProceso);
        filaBase.add(crearLabel("RAM (MB):"));
        filaBase.add(txtRamProceso);
        filaBase.add(crearLabel("Deadline:"));
        filaBase.add(txtDeadlineProceso);
        filaBase.add(crearLabel("Destino:"));
        filaBase.add(comboComputadores);
        filaBase.add(btnAgregar);

        panelCrear.add(filaBase);
        panelCrear.add(panelCamposEspeciales);

        actualizarCamposSegunTipoProceso();

        contenedor.add(panelRelojControl, BorderLayout.NORTH);
        contenedor.add(panelCrear, BorderLayout.SOUTH);
        return contenedor;
    }

    private void actualizarCamposSegunTipoProceso() {
        panelCamposEspeciales.removeAll();
        TipoProceso tipo = (TipoProceso) comboTipoProceso.getSelectedItem();

        if (tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR) {
            actualizarComboBuffers();
            panelCamposEspeciales.add(crearLabel("Buffer:"));
            panelCamposEspeciales.add(comboBuffersObjetivo);
            panelCamposEspeciales.add(crearLabel("Ciclos/Op:"));
            panelCamposEspeciales.add(txtCiclosOp);
            panelCamposEspeciales.add(crearLabel("Meta elem:"));
            panelCamposEspeciales.add(txtElementosMeta);
        } else if (tipo == TipoProceso.IO_BOUND) {
            panelCamposEspeciales.add(crearLabel("Cada (ciclos):"));
            panelCamposEspeciales.add(txtFrecuenciaIO);
            panelCamposEspeciales.add(crearLabel("Duración E/S:"));
            panelCamposEspeciales.add(txtDuracionIO);
        }

        panelCamposEspeciales.setVisible(panelCamposEspeciales.getComponentCount() > 0);
        panelCamposEspeciales.revalidate();
        panelCamposEspeciales.repaint();

        // Recalcular altura del panel superior (BorderLayout.NORTH) al mostrar/ocultar campos.
        Container ancestro = panelCamposEspeciales.getParent();
        while (ancestro != null) {
            ancestro.revalidate();
            ancestro.repaint();
            if (ancestro == getContentPane()) break;
            ancestro = ancestro.getParent();
        }
    }

    private void actualizarComboComputadores() {
        comboComputadores.removeAllItems();
        comboComputadores.addItem("Automático (Mayor RAM libre)");
        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            comboComputadores.addItem(cluster.getComputador(i).getNombre());
        }
    }

    private void actualizarComboBuffers() {
        comboBuffersObjetivo.removeAllItems();
        ListaEnlazada<Buffer> bufs = cluster.getBuffers();
        for (int i = 0; i < bufs.getTamano(); i++) {
            comboBuffersObjetivo.addItem(bufs.obtener(i).getId());
        }
    }

    private void alternarPausaSimulacion() {
        if (reloj.isPausado()) {
            reloj.reanudar();
            btnPausar.setText("Pausar");
            btnPausar.setBackground(new Color(220, 53, 69));
            registrarLog("Simulación reanudada.");
        } else {
            reloj.pausar();
            btnPausar.setText("Reanudar");
            btnPausar.setBackground(COLOR_VERDE);
            registrarLog("Simulación pausada.");
        }
    }

    private void accionCrearProceso() {
        try {
            String nombre = txtNombreProceso.getText().trim();
            int tiempo = Integer.parseInt(txtTiempoProceso.getText().trim());
            int prioridad = Integer.parseInt(txtPrioridadProceso.getText().trim());
            int ram = Integer.parseInt(txtRamProceso.getText().trim());
            int deadline = Integer.parseInt(txtDeadlineProceso.getText().trim());
            TipoProceso tipo = (TipoProceso) comboTipoProceso.getSelectedItem();

            if (nombre.isEmpty() || tiempo <= 0 || ram <= 0 || deadline <= 0) {
                JOptionPane.showMessageDialog(this, "Todos los valores numéricos deben ser mayores a 0.", "Error de Rango", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int id = GeneradorID.generar();
            Proceso p = new Proceso(id, nombre, tiempo, prioridad, ram, deadline, tipo);

            if (tipo == TipoProceso.PRODUCTOR || tipo == TipoProceso.CONSUMIDOR) {
                if (comboBuffersObjetivo.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Debe crear primero al menos un buffer para procesos productores o consumidores.", "Buffer Requerido", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                p.setBufferObjetivo((String) comboBuffersObjetivo.getSelectedItem());
                p.setCiclosPorOperacion(Math.max(1, Integer.parseInt(txtCiclosOp.getText().trim())));
                p.setElementosMeta(Math.max(1, Integer.parseInt(txtElementosMeta.getText().trim())));
            } else if (tipo == TipoProceso.IO_BOUND) {
                p.setCiclosEntreIO(Math.max(1, Integer.parseInt(txtFrecuenciaIO.getText().trim())));
                p.setDuracionIO(Math.max(1, Integer.parseInt(txtDuracionIO.getText().trim())));
            }

            int seleccion = comboComputadores.getSelectedIndex();
            int cicloActual = reloj.getCicloActual();

            if (seleccion == 0) {
                cluster.asignarProcesoAutomatico(p, cicloActual);
            } else {
                int idx = seleccion - 1;
                cluster.asignarProcesoManual(p, idx, cicloActual);
            }

            txtNombreProceso.setText("P_" + GeneradorID.generar());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese números enteros válidos en los campos numéricos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarDialogoCrearBuffer() {
        JTextField txtId = new JTextField("Buffer_" + (cluster.getBuffers().getTamano() + 1), 10);
        JTextField txtCap = new JTextField("5", 5);
        JComboBox<String> comboAnfitrion = new JComboBox<>();
        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            comboAnfitrion.addItem(cluster.getComputador(i).getNombre());
        }

        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.add(new JLabel("ID del Buffer:"));
        p.add(txtId);
        p.add(new JLabel("Capacidad:"));
        p.add(txtCap);
        p.add(new JLabel("Computador Anfitrión:"));
        p.add(comboAnfitrion);

        int res = JOptionPane.showConfirmDialog(this, p, "Crear Nuevo Buffer de Capacidad Limitada", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String id = txtId.getText().trim();
                int cap = Integer.parseInt(txtCap.getText().trim());
                String anf = (String) comboAnfitrion.getSelectedItem();
                if (cap <= 0) throw new NumberFormatException();

                Buffer b = new Buffer(id, cap, anf);
                cluster.agregarBuffer(b);
                actualizarComboBuffers();
                refrescarPanelBuffers();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capacidad inválida.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel crearPanelClusterNodos() {
        JPanel contenedorNodos = new JPanel(new GridLayout(1, cluster.getCantidadComputadores(), 10, 10));
        contenedorNodos.setOpaque(false);
        contenedorNodos.setBorder(new EmptyBorder(0, 10, 0, 10));

        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            Computador nodo = cluster.getComputador(i);
            contenedorNodos.add(crearPanelTarjetaNodo(nodo));
        }

        JScrollPane scroll = new JScrollPane(contenedorNodos);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        return contenedorNodos;
    }

    private JPanel crearPanelTarjetaNodo(Computador nodo) {
        JPanel tarjeta = new JPanel(new BorderLayout(6, 6));
        tarjeta.setBackground(COLOR_PANEL);
        tarjeta.setBorder(crearBordeTitulado(nodo.getNombre()));

        // --- ENCABEZADO: POLÍTICA Y RAM ---
        JPanel headerNodo = new JPanel(new GridLayout(3, 1, 4, 4));
        headerNodo.setOpaque(false);

        JComboBox<TipoPolitica> comboPolitica = new JComboBox<>(TipoPolitica.values());
        comboPolitica.setSelectedItem(nodo.getPlanificador().getPolitica().getTipo());
        comboPolitica.setBackground(COLOR_PANEL_INTERNO);
        comboPolitica.setForeground(COLOR_TEXTO_PRINCIPAL);
        comboPolitica.addActionListener(e -> {
            TipoPolitica sel = (TipoPolitica) comboPolitica.getSelectedItem();
            switch (sel) {
                case FCFS: nodo.getPlanificador().setPolitica(new PoliticaFCFS()); break;
                case SJF: nodo.getPlanificador().setPolitica(new PoliticaSJF()); break;
                case EDF: nodo.getPlanificador().setPolitica(new PoliticaEDF()); break;
                case PRIORIDAD_APROPIATIVA: nodo.getPlanificador().setPolitica(new PoliticaPrioridadApropiativa()); break;
                default: nodo.getPlanificador().setPolitica(new PoliticaRoundRobin()); break;
            }
        });

        JProgressBar barRAM = new JProgressBar(0, nodo.getRamTotal());
        barRAM.setName("barRAM_" + nodo.getNombre());
        barRAM.setStringPainted(true);
        barRAM.setForeground(COLOR_VERDE);
        barRAM.setBackground(COLOR_PANEL_INTERNO);

        // Panel de Estado de la CPU
        JLabel lblCPUInfo = new JLabel("CPU: Idle | PC: 0 | MAR: 0 | Modo: SISTEMA_OPERATIVO", SwingConstants.CENTER);
        lblCPUInfo.setName("lblCPU_" + nodo.getNombre());
        lblCPUInfo.setFont(new Font("Monospaced", Font.BOLD, 10));
        lblCPUInfo.setForeground(COLOR_AZUL);

        headerNodo.add(comboPolitica);
        headerNodo.add(barRAM);
        headerNodo.add(lblCPUInfo);
        tarjeta.add(headerNodo, BorderLayout.NORTH);

        // --- COLAS DE PROCESOS (NUEVOS, LISTOS, CPU, BLOQUEADOS, TERMINADOS) ---
        JPanel panelColas = new JPanel(new GridLayout(1, 5, 4, 4));
        panelColas.setOpaque(false);

        panelColas.add(crearSubPanelCola("Nuevos (RAM)", "lstNuevos_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("Listos", "lstListos_" + nodo.getNombre()));
        panelColas.add(crearSubPanelCola("En CPU", "lstCPU_" + nodo.getNombre()));
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

        DefaultListModel<ProcesoWrapper> model = new DefaultListModel<>();
        JList<ProcesoWrapper> lista = new JList<>(model);
        lista.setName(nombreComponente);
        lista.setBackground(COLOR_PANEL_INTERNO);
        lista.setForeground(COLOR_TEXTO_PRINCIPAL);
        lista.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Doble clic para abrir el PCB completo
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ProcesoWrapper seleccionado = lista.getSelectedValue();
                    if (seleccionado != null && seleccionado.proceso != null) {
                        new DialogoPCB(VentanaSimulador.this, seleccionado.proceso).setVisible(true);
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(lista);
        sp.setBorder(null);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearPanelMétricasYLogs() {
        JPanel panelDerecho = new JPanel(new BorderLayout(8, 8));
        panelDerecho.setPreferredSize(new Dimension(380, 0));
        panelDerecho.setBackground(COLOR_PANEL);
        panelDerecho.setBorder(crearBordeTitulado("Rendimiento en Tiempo Real y Registro"));

        // Barras Globales de Uso
        JPanel panelBarras = new JPanel(new GridLayout(2, 1, 4, 4));
        panelBarras.setOpaque(false);
        barUsoCPUGlobal = crearBarraProgresiva("Uso CPU Promedio", COLOR_AZUL);
        barUsoRAMGlobal = crearBarraProgresiva("Ocupación RAM Clúster", COLOR_NARANJA);
        panelBarras.add(barUsoCPUGlobal);
        panelBarras.add(barUsoRAMGlobal);

        // Gráfico de Utilización de CPU vs Tiempo
        panelGrafico = new PanelGraficoCPU(cluster);
        panelGrafico.setBorder(BorderFactory.createLineBorder(COLOR_BORDE));

        JPanel panelSuperiorDerecho = new JPanel(new BorderLayout(4, 4));
        panelSuperiorDerecho.setOpaque(false);
        panelSuperiorDerecho.add(panelBarras, BorderLayout.NORTH);
        panelSuperiorDerecho.add(panelGrafico, BorderLayout.CENTER);

        // Caja de Logs de Eventos
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setBackground(COLOR_PANEL_INTERNO);
        txtLogs.setForeground(COLOR_TEXTO_PRINCIPAL);
        txtLogs.setFont(new Font("Monospaced", Font.PLAIN, 10));
        txtLogs.setCaretColor(Color.WHITE);
        JScrollPane scrollLogs = new JScrollPane(txtLogs);
        scrollLogs.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDE), "Registro de Eventos del Sistema (Logs)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 10), COLOR_TEXTO_SECUNDARIO));

        panelDerecho.add(panelSuperiorDerecho, BorderLayout.NORTH);
        panelDerecho.add(scrollLogs, BorderLayout.CENTER);

        return panelDerecho;
    }

    private JPanel crearPanelBuffersGlobales() {
        panelBuffersGlobal = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        panelBuffersGlobal.setBackground(COLOR_PANEL);
        panelBuffersGlobal.setBorder(crearBordeTitulado("Buffers Compartidos y Semáforos (Sincronización Productor-Consumidor)"));

        refrescarPanelBuffers();
        return panelBuffersGlobal;
    }

    private void refrescarPanelBuffers() {
        panelBuffersGlobal.removeAll();
        ListaEnlazada<Buffer> bufs = cluster.getBuffers();

        if (bufs.estaVacia()) {
            JLabel lblVacio = new JLabel("No hay buffers configurados. Use el botón '+ Crear Buffer' para agregar uno.");
            lblVacio.setForeground(COLOR_TEXTO_SECUNDARIO);
            lblVacio.setFont(new Font("SansSerif", Font.ITALIC, 11));
            panelBuffersGlobal.add(lblVacio);
        } else {
            for (int i = 0; i < bufs.getTamano(); i++) {
                Buffer b = bufs.obtener(i);
                JPanel tarjetaBuf = new JPanel(new GridLayout(4, 1, 2, 2));
                tarjetaBuf.setBackground(COLOR_PANEL_INTERNO);
                tarjetaBuf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_BORDE, 1),
                        new EmptyBorder(4, 8, 4, 8)
                ));

                JLabel l1 = new JLabel(b.getId() + " [" + b.getComputadorAnfitrion() + "]");
                l1.setFont(new Font("SansSerif", Font.BOLD, 11));
                l1.setForeground(COLOR_VERDE);

                JLabel l2 = new JLabel("Ocupación: " + b.getElementosActuales() + " / " + b.getCapacidad() + " items");
                l2.setForeground(COLOR_TEXTO_PRINCIPAL);
                l2.setFont(new Font("Monospaced", Font.PLAIN, 10));

                JLabel l3 = new JLabel("Mutex: " + b.getMutex().getValor() + " | Vacíos: " + b.getVacios().getValor() + " | Llenos: " + b.getLlenos().getValor());
                l3.setForeground(COLOR_AZUL);
                l3.setFont(new Font("Monospaced", Font.PLAIN, 10));

                int bloqTotal = b.getMutex().getColaBloqueados().getTamano() + b.getVacios().getColaBloqueados().getTamano() + b.getLlenos().getColaBloqueados().getTamano();
                JLabel l4 = new JLabel("Procesos en Espera: " + bloqTotal);
                l4.setForeground(bloqTotal > 0 ? COLOR_NARANJA : COLOR_TEXTO_SECUNDARIO);
                l4.setFont(new Font("Monospaced", Font.PLAIN, 10));

                tarjetaBuf.add(l1);
                tarjetaBuf.add(l2);
                tarjetaBuf.add(l3);
                tarjetaBuf.add(l4);
                panelBuffersGlobal.add(tarjetaBuf);
            }
        }
        panelBuffersGlobal.revalidate();
        panelBuffersGlobal.repaint();
    }

    private void iniciarTimerRefresco() {
        timerRefrescoGUI = new javax.swing.Timer(250, e -> refrescarInterfaz());
        timerRefrescoGUI.start();
    }

    private void refrescarInterfaz() {
        int cicloActual = reloj.getCicloActual();
        lblReloj.setText("Reloj de Simulación: Ciclo " + cicloActual);

        int ramUsadaTotal = 0;
        int ramCapacidadTotal = 0;

        for (int i = 0; i < cluster.getCantidadComputadores(); i++) {
            Computador nodo = cluster.getComputador(i);
            ramUsadaTotal += nodo.getRamUsada();
            ramCapacidadTotal += nodo.getRamTotal();

            // Barra RAM del nodo
            JProgressBar barRAM = findComponentByName(this, "barRAM_" + nodo.getNombre(), JProgressBar.class);
            if (barRAM != null) {
                barRAM.setValue(nodo.getRamUsada());
                barRAM.setString("RAM: Usada " + nodo.getRamUsada() + " MB / Libre " + nodo.getRamLibre() + " MB (" +
                        (int)(((double) nodo.getRamUsada() / nodo.getRamTotal()) * 100) + "%)");
            }

            // Estado de CPU
            JLabel lblCPU = findComponentByName(this, "lblCPU_" + nodo.getNombre(), JLabel.class);
            if (lblCPU != null) {
                Proceso ejec = nodo.getPlanificador().getProcesoEnEjecucion();
                if (ejec != null) {
                    lblCPU.setText("CPU: " + ejec.getNombre() + " | PC: " + ejec.getPc() + " | MAR: " + ejec.getMar() + " | Modo: " + ejec.getModo());
                    lblCPU.setForeground(COLOR_VERDE);
                } else {
                    lblCPU.setText("CPU: [IDLE] | PC: " + nodo.getCpu().getPc() + " | MAR: " + nodo.getCpu().getMar() + " | Modo: SISTEMA_OPERATIVO");
                    lblCPU.setForeground(COLOR_AZUL);
                }
            }

            // Actualizar colas de forma NO destructiva (usando snapshot en ListaEnlazada)
            actualizarListaGUISegura("lstNuevos_" + nodo.getNombre(), nodo.getColaNuevos().obtenerListaElementos());
            actualizarListaGUISegura("lstListos_" + nodo.getNombre(), nodo.getPlanificador().getColaListos().obtenerListaElementos());
            actualizarListaGUISegura("lstBloq_" + nodo.getNombre(), nodo.getPlanificador().getColaBloqueados().obtenerListaElementos());
            actualizarListaGUISegura("lstTerm_" + nodo.getNombre(), nodo.getPlanificador().getColaTerminados().obtenerListaElementos());

            // Lista CPU
            JList<ProcesoWrapper> lstCPU = findComponentByName(this, "lstCPU_" + nodo.getNombre(), JList.class);
            if (lstCPU != null) {
                DefaultListModel<ProcesoWrapper> model = (DefaultListModel<ProcesoWrapper>) lstCPU.getModel();
                model.clear();
                Proceso ejec = nodo.getPlanificador().getProcesoEnEjecucion();
                if (ejec != null) {
                    model.addElement(new ProcesoWrapper(ejec, ">> " + ejec.getNombre() + " (PC:" + ejec.getPc() + ", D:" + ejec.getDeadline() + ")"));
                }
            }
        }

        // Refrescar barras globales
        if (ramCapacidadTotal > 0) {
            int pctRAM = (int) (((double) ramUsadaTotal / ramCapacidadTotal) * 100);
            barUsoRAMGlobal.setValue(pctRAM);
            barUsoRAMGlobal.setString("Ocupación RAM Clúster: " + pctRAM + "%");
        }

        double usoCPUProm = cluster.getUsoCPUGlobal();
        barUsoCPUGlobal.setValue((int) usoCPUProm);
        barUsoCPUGlobal.setString(String.format("Uso CPU Promedio: %.1f%%", usoCPUProm));

        // Registrar muestra en el gráfico en tiempo real
        panelGrafico.registrarMuestra();

        // Refrescar tarjetas de buffers
        refrescarPanelBuffers();
    }

    private void actualizarListaGUISegura(String nombreComponente, ListaEnlazada<Proceso> snapshot) {
        JList<ProcesoWrapper> lst = findComponentByName(this, nombreComponente, JList.class);
        if (lst == null) return;

        DefaultListModel<ProcesoWrapper> model = (DefaultListModel<ProcesoWrapper>) lst.getModel();
        model.clear();

        for (int i = 0; i < snapshot.getTamano(); i++) {
            Proceso p = snapshot.obtener(i);
            String etiqueta;
            if (p.getEstado() == EstadoProceso.BLOQUEADO) {
                etiqueta = p.getNombre() + " [" + p.getMotivoBloqueo().getDescripcion() + "]";
            } else if (p.getEstado() == EstadoProceso.TERMINADO) {
                etiqueta = p.getNombre() + " [Fin: C" + p.getCicloFinalizacion() + "]";
            } else {
                etiqueta = p.getNombre() + " (T:" + p.getTiempoRestante() + ", D:" + p.getDeadline() + ")";
            }
            model.addElement(new ProcesoWrapper(p, etiqueta));
        }
    }

    public void registrarLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtLogs.append("[Ciclo " + reloj.getCicloActual() + "] " + mensaje + "\n");
            txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
        });
    }

    // Clase auxiliar para encapsular proceso y mostrar texto en JList permitiendo abrir PCB
    private static class ProcesoWrapper {
        public Proceso proceso;
        public String texto;

        public ProcesoWrapper(Proceso proceso, String texto) {
            this.proceso = proceso;
            this.texto = texto;
        }

        @Override
        public String toString() {
            return texto;
        }
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

    private void estilarBoton(JButton b, Color fondo) {
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
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

    private TitledBorder crearBordeTitulado(String titulo) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDE, 1),
                titulo, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), COLOR_TEXTO_PRINCIPAL);
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