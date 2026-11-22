/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author Jabri
 */

import archivos.EntradaFS;
import archivos.PermisoArchivo;
import archivos.SistemaArchivos;
import disco.*;
import edd.ListaEnlazada;
import politicas.TipoPolitica;
import procesos.Proceso;
import procesos.TipoProceso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class GestorJSON {

    /**
     * Contenedor para devolver tanto el DiscoSimulado como el SistemaArchivos cargados desde el JSON 1.
     */
    public static class ResultadoCargaFS {
        public DiscoSimulado disco;
        public SistemaArchivos sistemaArchivos;

        public ResultadoCargaFS(DiscoSimulado disco, SistemaArchivos sistemaArchivos) {
            this.disco = disco;
            this.sistemaArchivos = sistemaArchivos;
        }
    }

    /**
     * Contenedor para devolver la configuración del computador único, swap, política de disco y procesos desde el JSON 2.
     */
    public static class ResultadoCargaProcesos {
        public int cycleDurationMs = 300;
        public int swapSizeBlocks = 50;
        public TipoPoliticaDisco initialDiskPolicy = TipoPoliticaDisco.FIFO;
        public int ramCapacity = 1024;
        public TipoPolitica cpuPolicy = TipoPolitica.FCFS;
        public int quantum = 3;
        public ListaEnlazada<Proceso> procesos = new ListaEnlazada<>();
    }

    // =========================================================================
    // 1. CARGAR Y GUARDAR JSON DEL SISTEMA DE ARCHIVOS Y DISCO
    // =========================================================================

    public static ResultadoCargaFS cargarSistemaArchivosJSON(File archivo, int swapSizeBlocks) throws IOException {
        String contenido = leerArchivoCompleto(archivo);

        int totalBlocks = extraerInt(contenido, "totalBlocks", 200);
        int initialHead = extraerInt(contenido, "initialHead", 0);

        DiscoSimulado disco = new DiscoSimulado(totalBlocks, swapSizeBlocks, initialHead, new PoliticaDiscoFIFO());
        SistemaArchivos fs = new SistemaArchivos(disco);

        String bloqueArray = extraerBloqueCorchetes(contenido, "fileSystem");
        ListaEnlazada<String> objetos = dividirObjetosJSON(bloqueArray);

        for (int i = 0; i < objetos.getTamano(); i++) {
            String obj = objetos.obtener(i);
            String path = extraerString(obj, "path", "");
            String type = extraerString(obj, "type", "FILE");
            String owner = extraerString(obj, "owner", "admin");
            String permissions = extraerString(obj, "permissions", "public");

            if (path.isEmpty()) continue;

            if ("DIRECTORY".equalsIgnoreCase(type)) {
                fs.crearDirectorio(path, owner, PermisoArchivo.desdeTexto(permissions));
            } else {
                int startBlock = extraerInt(obj, "startBlock", 0);
                int blocks = extraerInt(obj, "blocks", 1);
                fs.cargarArchivoDesdeJSON(path, startBlock, blocks, owner, PermisoArchivo.desdeTexto(permissions));
            }
        }

        return new ResultadoCargaFS(disco, fs);
    }

    public static void guardarSistemaArchivosJSON(SistemaArchivos fs, File destino) throws IOException {
        if (fs == null || destino == null) return;
        DiscoSimulado disco = fs.getDisco();
        ListaEnlazada<EntradaFS> entradas = fs.obtenerTodasLasEntradas();

        try (PrintWriter pw = new PrintWriter(new FileWriter(destino))) {
            pw.println("{");
            pw.println("  \"diskConfig\": {");
            pw.println("    \"totalBlocks\": " + disco.getTotalBloques() + ",");
            pw.println("    \"initialHead\": " + disco.getCabezalActual());
            pw.println("  },");
            pw.println("  \"fileSystem\": [");

            for (int i = 0; i < entradas.getTamano(); i++) {
                EntradaFS e = entradas.obtener(i);
                pw.println("    {");
                pw.println("      \"path\": \"" + e.getRutaCompleta() + "\",");
                if (e.isEsDirectorio()) {
                    pw.println("      \"type\": \"DIRECTORY\",");
                } else {
                    pw.println("      \"type\": \"FILE\",");
                    pw.println("      \"startBlock\": " + e.getPrimerBloque() + ",");
                    pw.println("      \"blocks\": " + e.getTamanoBloques() + ",");
                }
                pw.println("      \"owner\": \"" + e.getDueno() + "\",");
                pw.println("      \"permissions\": \"" + e.getPermiso().getValorJson() + "\"");
                pw.print("    }");
                if (i < entradas.getTamano() - 1) pw.println(",");
                else pw.println();
            }

            pw.println("  ]");
            pw.println("}");
        }
    }

    // =========================================================================
    // 2. CARGAR JSON DE PROCESOS Y CONFIGURACIÓN
    // =========================================================================

    public static ResultadoCargaProcesos cargarProcesosJSON(File archivo) throws IOException {
        String contenido = leerArchivoCompleto(archivo);
        ResultadoCargaProcesos res = new ResultadoCargaProcesos();

        res.cycleDurationMs = Math.max(20, extraerInt(contenido, "cycleDurationMs", 300));
        res.swapSizeBlocks = Math.max(10, extraerInt(contenido, "swapSizeBlocks", 50));

        String diskPolStr = extraerString(contenido, "initialDiskPolicy", "FIFO").toUpperCase().replace("-", "_");
        try {
            res.initialDiskPolicy = TipoPoliticaDisco.valueOf(diskPolStr);
        } catch (Exception e) {
            res.initialDiskPolicy = TipoPoliticaDisco.FIFO;
        }

        res.ramCapacity = Math.max(64, extraerInt(contenido, "ramCapacity", 1024));
        res.quantum = Math.max(0, extraerInt(contenido, "quantum", 3));

        String cpuPolStr = extraerString(contenido, "policy", "FCFS").toUpperCase().replace(" ", "_");
        try {
            res.cpuPolicy = TipoPolitica.valueOf(cpuPolStr);
        } catch (Exception e) {
            res.cpuPolicy = TipoPolitica.FCFS;
        }

        String bloqueProcesos = extraerBloqueCorchetes(contenido, "processes");
        ListaEnlazada<String> objetos = dividirObjetosJSON(bloqueProcesos);

        for (int i = 0; i < objetos.getTamano(); i++) {
            String obj = objetos.obtener(i);
            String name = extraerString(obj, "name", "Proc_" + (i + 1));
            int memory = Math.max(1, extraerInt(obj, "memory", 16));
            int deadline = Math.max(1, extraerInt(obj, "deadline", 500));
            int instructions = Math.max(1, extraerInt(obj, "instructions", 2));
            int priority = Math.max(1, extraerInt(obj, "priority", 1));
            String targetPath = extraerString(obj, "targetPath", "");
            String opStr = extraerString(obj, "operation", "").toUpperCase();
            int fileSizeBlocks = Math.max(0, extraerInt(obj, "fileSizeBlocks", 1));

            TipoProceso tipo = targetPath.isEmpty() ? TipoProceso.CPU_BOUND : TipoProceso.IO_BOUND;
            Proceso p = new Proceso(i + 1, name, instructions, priority, memory, deadline, tipo);

            if (!targetPath.isEmpty()) {
                p.setBufferObjetivo(opStr + "|" + targetPath + "|" + fileSizeBlocks);
            }
            res.procesos.agregar(p);
        }

        return res;
    }

    // =========================================================================
    // HELPERS DE PARSEO JSON SIN COLECCIONES PROHIBIDAS
    // =========================================================================

    private static String leerArchivoCompleto(File archivo) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea).append("\n");
            }
        }
        return sb.toString();
    }

    private static String extraerString(String json, String clave, String defecto) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return defecto;
        int dosPuntos = json.indexOf(':', idx + patron.length());
        if (dosPuntos == -1) return defecto;
        int comilla1 = json.indexOf('"', dosPuntos + 1);
        if (comilla1 == -1) return defecto;
        int comilla2 = json.indexOf('"', comilla1 + 1);
        if (comilla2 == -1) return defecto;
        return json.substring(comilla1 + 1, comilla2).trim();
    }

    private static int extraerInt(String json, String clave, int defecto) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return defecto;
        int dosPuntos = json.indexOf(':', idx + patron.length());
        if (dosPuntos == -1) return defecto;

        int i = dosPuntos + 1;
        while (i < json.length() && !Character.isDigit(json.charAt(i)) && json.charAt(i) != '-') {
            i++;
        }
        int inicio = i;
        while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '-')) {
            i++;
        }
        if (inicio >= i) return defecto;
        try {
            return Integer.parseInt(json.substring(inicio, i));
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static String extraerBloqueCorchetes(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx == -1) return "";
        int inicio = json.indexOf('[', idx);
        if (inicio == -1) return "";

        int nivel = 0;
        for (int i = inicio; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') nivel++;
            else if (c == ']') {
                nivel--;
                if (nivel == 0) {
                    return json.substring(inicio + 1, i);
                }
            }
        }
        return "";
    }

    private static ListaEnlazada<String> dividirObjetosJSON(String contenidoArray) {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        int nivel = 0;
        int inicioObj = -1;

        for (int i = 0; i < contenidoArray.length(); i++) {
            char c = contenidoArray.charAt(i);
            if (c == '{') {
                if (nivel == 0) inicioObj = i;
                nivel++;
            } else if (c == '}') {
                nivel--;
                if (nivel == 0 && inicioObj != -1) {
                    lista.agregar(contenidoArray.substring(inicioObj, i + 1));
                    inicioObj = -1;
                }
            }
        }
        return lista;
    }
}