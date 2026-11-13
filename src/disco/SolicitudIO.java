/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */

import procesos.Proceso;

public class SolicitudIO {
    private Proceso proceso;
    private TipoOperacionIO operacion;
    private int bloqueObjetivo;
    private String rutaArchivo;
    private int tamanoBloques;
    private int cicloLlegadaCola;

    public SolicitudIO(Proceso proceso, TipoOperacionIO operacion, int bloqueObjetivo, String rutaArchivo, int tamanoBloques, int cicloLlegadaCola) {
        this.proceso = proceso;
        this.operacion = operacion;
        this.bloqueObjetivo = bloqueObjetivo;
        this.rutaArchivo = rutaArchivo;
        this.tamanoBloques = Math.max(1, tamanoBloques);
        this.cicloLlegadaCola = cicloLlegadaCola;
    }

    public Proceso getProceso() { return proceso; }
    public TipoOperacionIO getOperacion() { return operacion; }
    public int getBloqueObjetivo() { return bloqueObjetivo; }
    public void setBloqueObjetivo(int bloqueObjetivo) { this.bloqueObjetivo = bloqueObjetivo; }
    public String getRutaArchivo() { return rutaArchivo; }
    public int getTamanoBloques() { return tamanoBloques; }
    public int getCicloLlegadaCola() { return cicloLlegadaCola; }

    @Override
    public String toString() {
        String nomProc = (proceso != null) ? proceso.getNombre() : "SO";
        return nomProc + " [" + operacion + " -> Blq " + bloqueObjetivo + "]";
    }
}