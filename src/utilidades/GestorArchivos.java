/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author PECA
 */
import edd.ListaEnlazada;
import politicas.TipoPolitica;
import procesos.Proceso;
import procesos.TipoProceso;
import sincronizacion.Buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GestorArchivos {

    public static void guardarConfiguracionCSV(ConfiguracionInicial config, File archivo) throws IOException {
        if (config == null || archivo == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            // Sección GENERAL
            pw.println("[GENERAL]");
            pw.println("duracionCicloMs,latenciaRedCiclos");
            pw.println(config.duracionCicloMs + "," + config.latenciaRedCiclos);

            // Sección COMPUTADORES
            pw.println("[COMPUTADORES]");
            pw.println("nombre,ramMb,politica,quantum");
            if (config.computadores != null) {
                for (int i = 0; i < config.computadores.getTamano(); i++) {
                    ConfigComputador c = config.computadores.obtener(i);
                    if (c != null) {
                        String polNom = (c.politica != null) ? c.politica.name() : "ROUND_ROBIN";
                        pw.println(c.nombre + "," + c.ramMb + "," + polNom + "," + c.quantum);
                    }
                }
            }

            // Sección BUFFERS
            pw.println("[BUFFERS]");
            pw.println("id,capacidad,computadorAnfitrion,ramMb");
            if (config.buffersIniciales != null) {
                for (int i = 0; i < config.buffersIniciales.getTamano(); i++) {
                    Buffer b = config.buffersIniciales.obtener(i);
                    if (b != null) {
                        pw.println(b.getId() + "," + b.getCapacidad() + "," + b.getComputadorAnfitrion() + "," + b.getMemoriaOcupadaMb());
                    }
                }
            }

            // Sección PROCESOS
            pw.println("[PROCESOS]");
            pw.println("id,nombre,tiempo,prioridad,ram,deadline,tipo,computadorAsignado,bufferObjetivo,ciclosOp,elementosMeta");
            if (config.procesosIniciales != null) {
                for (int i = 0; i < config.procesosIniciales.getTamano(); i++) {
                    Proceso p = config.procesosIniciales.obtener(i);
                    if (p != null) {
                        String tipoNom = (p.getTipo() != null) ? p.getTipo().name() : "CPU_BOUND";
                        pw.println(p.getId() + "," + p.getNombre() + "," + p.getTiempoEjecucion() + "," + p.getPrioridad() + "," +
                                p.getMemoriaRequerida() + "," + p.getDeadline() + "," + tipoNom + "," +
                                p.getComputadorAsignado() + "," + p.getBufferObjetivo() + "," +
                                p.getCiclosPorOperacion() + "," + p.getElementosMeta());
                    }
                }
            }
        }
    }

    public static ConfiguracionInicial cargarConfiguracionCSV(File archivo) throws IOException {
        ConfiguracionInicial config = new ConfiguracionInicial();
        if (archivo == null || !archivo.exists()) {
            return config;
        }

        String seccionActual = "";
        boolean tieneSecciones = false;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                if (linea.startsWith("[") && linea.endsWith("]")) {
                    seccionActual = linea.toUpperCase();
                    tieneSecciones = true;
                    continue;
                }

                // Si no tiene secciones y es la línea de valores simples: 1000,2,8192,3,2
                if (!tieneSecciones && !linea.startsWith("duracionCicloMs") && linea.contains(",")) {
                    String[] partes = linea.split(",");
                    if (partes.length >= 5) {
                        try {
                            config.duracionCicloMs = Math.max(20, Integer.parseInt(partes[0].trim()));
                            config.numComputadores = Math.max(2, Integer.parseInt(partes[1].trim()));
                            config.ramPorComputador = Math.max(512, Integer.parseInt(partes[2].trim()));
                            config.quantumPorDefecto = Math.max(1, Integer.parseInt(partes[3].trim()));
                            config.latenciaRedCiclos = Math.max(0, Integer.parseInt(partes[4].trim()));

                            config.computadores = new ListaEnlazada<>();
                            for (int k = 1; k <= config.numComputadores; k++) {
                                config.computadores.agregar(new ConfigComputador(
                                        "Computador " + k, config.ramPorComputador, TipoPolitica.ROUND_ROBIN, config.quantumPorDefecto));
                            }
                            return config;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                if (linea.startsWith("duracionCicloMs") || linea.startsWith("nombre") || linea.startsWith("id")) {
                    continue; // Saltar encabezados
                }

                String[] partes = linea.split(",");
                try {
                    switch (seccionActual) {
                        case "[GENERAL]":
                            if (partes.length >= 2) {
                                config.duracionCicloMs = Math.max(20, Integer.parseInt(partes[0].trim()));
                                config.latenciaRedCiclos = Math.max(0, Integer.parseInt(partes[1].trim()));
                            }
                            break;

                        case "[COMPUTADORES]":
                            if (partes.length >= 4) {
                                String nom = partes[0].trim();
                                int ram = Integer.parseInt(partes[1].trim());
                                TipoPolitica pol;
                                try {
                                    pol = TipoPolitica.valueOf(partes[2].trim().toUpperCase());
                                } catch (Exception e) {
                                    pol = TipoPolitica.ROUND_ROBIN;
                                }
                                int q = Integer.parseInt(partes[3].trim());
                                config.computadores.agregar(new ConfigComputador(nom, ram, pol, q));
                            }
                            break;

                        case "[BUFFERS]":
                            if (partes.length >= 3) {
                                String id = partes[0].trim();
                                int cap = Integer.parseInt(partes[1].trim());
                                String anf = partes[2].trim();
                                int ramMb = (partes.length >= 4) ? Integer.parseInt(partes[3].trim()) : cap * 64;
                                config.buffersIniciales.agregar(new Buffer(id, cap, anf, ramMb));
                            }
                            break;

                        case "[PROCESOS]":
                            if (partes.length >= 7) {
                                int id = Integer.parseInt(partes[0].trim());
                                String nom = partes[1].trim();
                                int t = Integer.parseInt(partes[2].trim());
                                int prio = Integer.parseInt(partes[3].trim());
                                int ram = Integer.parseInt(partes[4].trim());
                                int dead = Integer.parseInt(partes[5].trim());
                                TipoProceso tp;
                                try {
                                    tp = TipoProceso.valueOf(partes[6].trim().toUpperCase());
                                } catch (Exception e) {
                                    tp = TipoProceso.CPU_BOUND;
                                }
                                Proceso p = new Proceso(id, nom, t, prio, ram, dead, tp);
                                if (partes.length >= 8) p.setComputadorAsignado(partes[7].trim());
                                if (partes.length >= 9) p.setBufferObjetivo(partes[8].trim());
                                if (partes.length >= 10) p.setCiclosPorOperacion(Integer.parseInt(partes[9].trim()));
                                if (partes.length >= 11) p.setElementosMeta(Integer.parseInt(partes[10].trim()));
                                config.procesosIniciales.agregar(p);
                            }
                            break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return config;
    }
}
