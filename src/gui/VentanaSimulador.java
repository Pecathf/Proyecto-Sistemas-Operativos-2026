/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gui;

/**
 *
 * @author Jabri
 */

import javax.swing.*;
import java.awt.*;
import politicas.*;
import procesos.*;

public class VentanaSimulador extends JFrame {

    private Planificador planificador;

    // Componentes visuales para las colas
    private DefaultListModel<String> modeloTrabajos;
    private DefaultListModel<String> modeloListos;
    private DefaultListModel<String> modeloBloqueados;
    private DefaultListModel<String> modeloTerminados;
    private JLabel lblCPU;

    // Componentes de control
    private JTextField txtNombre, txtTiempo, txtPrioridad, txtQuantum;
    private JComboBox<String> comboPolitica;
    private int contadorPID = 1;

    public VentanaSimulador() {
        setTitle("ÁvilaOS - Simulador de Planificación de Procesos");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Inicializar planificador por defecto (Round Robin, Quantum = 3)
        planificador = new Planificador(3, new PoliticaRoundRobin());

        // --- PANEL SUPERIOR: Formularios y Controles ---
        JPanel panelSuperior = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Fila 1: Crear Proceso
        JPanel panelCrear = new JPanel(new FlowLayout());
        panelCrear.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Proceso"));
        
        txtNombre = new JTextField("Proceso " + contadorPID, 10);
        txtTiempo = new JTextField("5", 4);
        txtPrioridad = new JTextField("1", 4);
        JButton btnAgregar = new JButton("Agregar Proceso");

        panelCrear.add(new JLabel("Nombre:"));
        panelCrear.add(txtNombre);
        panelCrear.add(new JLabel("Tiempo (CPU):"));
        panelCrear.add(txtTiempo);
        panelCrear.add(new JLabel("Prioridad:"));
        panelCrear.add(txtPrioridad);
        panelCrear.add(btnAgregar);

        // Fila 2: Configuración de Política y Ejecución
        JPanel panelControl = new JPanel(new FlowLayout());
        panelControl.setBorder(BorderFactory.createTitledBorder("Control del Sistema"));

        comboPolitica = new JComboBox<>(new String[]{"Round Robin", "FCFS", "SJF"});
        txtQuantum = new JTextField("3", 4);
        JButton btnPaso = new JButton("Ejecutar Ciclo (Paso a Paso)");
        JButton btnAdmitir = new JButton("Admitir a Listos");

        panelControl.add(new JLabel("Política:"));
        panelControl.add(comboPolitica);
        panelControl.add(new JLabel("Quantum:"));
        panelControl.add(txtQuantum);
        panelControl.add(btnAdmitir);
        panelControl.add(btnPaso);

        panelSuperior.add(panelCrear);
        panelSuperior.add(panelControl);
        add(panelSuperior, BorderLayout.NORTH);

        // --- PANEL CENTRAL: Visualización de Colas y CPU ---
        JPanel panelCentral = new JPanel(new GridLayout(1, 5, 5, 5));

        modeloTrabajos = new DefaultListModel<>();
        modeloListos = new DefaultListModel<>();
        modeloBloqueados = new DefaultListModel<>();
        modeloTerminados = new DefaultListModel<>();

        panelCentral.add(crearPanelCola("1. Cola Trabajos", modeloTrabajos));
        panelCentral.add(crearPanelCola("2. Cola Listos", modeloListos));
        
        // Panel CPU dedicado
        JPanel panelCPU = new JPanel(new BorderLayout());
        panelCPU.setBorder(BorderFactory.createTitledBorder("CPU (Ejecución)"));
        lblCPU = new JLabel("CPU LIBRE", SwingConstants.CENTER);
        lblCPU.setFont(new Font("Arial", Font.BOLD, 13));
        panelCPU.add(lblCPU, BorderLayout.CENTER);
        panelCentral.add(panelCPU);

        panelCentral.add(crearPanelCola("3. Cola Bloqueados", modeloBloqueados));
        panelCentral.add(crearPanelCola("4. Terminados", modeloTerminados));

        add(panelCentral, BorderLayout.CENTER);

        // --- EVENTOS ---
        btnAgregar.addActionListener(e -> agregarProceso());
        btnAdmitir.addActionListener(e -> {
            planificador.admitirProcesos();
            actualizarVistas();
        });
        btnPaso.addActionListener(e -> {
            planificador.ejecutarCiclo();
            actualizarVistas();
        });
        comboPolitica.addActionListener(e -> cambiarPolitica());
    }

    private JPanel crearPanelCola(String titulo, DefaultListModel<String> modelo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        JList<String> lista = new JList<>(modelo);
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    private void agregarProceso() {
        try {
            String nombre = txtNombre.getText();
            int tiempo = Integer.parseInt(txtTiempo.getText());
            int prioridad = Integer.parseInt(txtPrioridad.getText());

            Proceso p = new Proceso(contadorPID++, nombre, tiempo, prioridad, 1000, 0);
            planificador.crearProceso(p);

            txtNombre.setText("Proceso " + contadorPID);
            actualizarVistas();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese valores numéricos válidos en tiempo y prioridad.");
        }
    }

    private void cambiarPolitica() {
        String seleccion = (String) comboPolitica.getSelectedItem();
        try {
            int q = Integer.parseInt(txtQuantum.getText());
            planificador.setQuantum(q);
        } catch (NumberFormatException e) {
            // Mantiene el quantum actual si el texto es inválido
        }

        switch (seleccion) {
            case "FCFS":
                planificador.setPolitica(new PoliticaFCFS());
                break;
            case "SJF":
                planificador.setPolitica(new PoliticaSJF());
                break;
            default:
                planificador.setPolitica(new PoliticaRoundRobin());
                break;
        }
    }

    private void actualizarVistas() {
        modeloTrabajos.clear();
        modeloListos.clear();
        modeloBloqueados.clear();
        modeloTerminados.clear();

        // 1. Recorrer Cola de Trabajos
        edd.Cola<Proceso> auxTrabajos = new edd.Cola<>();
        while (!planificador.getColaTrabajos().estaVacia()) {
            Proceso p = planificador.getColaTrabajos().desencolar();
            modeloTrabajos.addElement(p.getNombre() + " (T: " + p.getTiempoRestante() + ")");
            auxTrabajos.encolar(p);
        }
        while (!auxTrabajos.estaVacia()) {
            planificador.getColaTrabajos().encolar(auxTrabajos.desencolar());
        }

        // 2. Recorrer Cola de Listos
        edd.Cola<Proceso> auxListos = new edd.Cola<>();
        while (!planificador.getColaListos().estaVacia()) {
            Proceso p = planificador.getColaListos().desencolar();
            modeloListos.addElement(p.getNombre() + " (T: " + p.getTiempoRestante() + ")");
            auxListos.encolar(p);
        }
        while (!auxListos.estaVacia()) {
            planificador.getColaListos().encolar(auxListos.desencolar());
        }

        // 3. Recorrer Cola de Bloqueados
        edd.Cola<Proceso> auxBloqueados = new edd.Cola<>();
        while (!planificador.getColaBloqueados().estaVacia()) {
            Proceso p = planificador.getColaBloqueados().desencolar();
            modeloBloqueados.addElement(p.getNombre() + " (T: " + p.getTiempoRestante() + ")");
            auxBloqueados.encolar(p);
        }
        while (!auxBloqueados.estaVacia()) {
            planificador.getColaBloqueados().encolar(auxBloqueados.desencolar());
        }

        // 4. Recorrer Cola de Terminados
        edd.Cola<Proceso> auxTerminados = new edd.Cola<>();
        while (!planificador.getColaTerminados().estaVacia()) {
            Proceso p = planificador.getColaTerminados().desencolar();
            modeloTerminados.addElement(p.getNombre());
            auxTerminados.encolar(p);
        }
        while (!auxTerminados.estaVacia()) {
            planificador.getColaTerminados().encolar(auxTerminados.desencolar());
        }

        // 5. Estado de la CPU
        Proceso enEjecucion = planificador.getProcesoEnEjecucion();
        if (enEjecucion != null) {
            lblCPU.setText("<html><center><b>" + enEjecucion.getNombre() + "</b><br>Modo: " + enEjecucion.getModo() + "<br>T. Restante: " + enEjecucion.getTiempoRestante() + "</center></html>");
        } else {
            lblCPU.setText("CPU LIBRE");
        }
    }
}