/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package archivos;

/**
 *
 * @author Jabri
 */
import edd.ListaEnlazada;
import java.awt.Color;

public class EntradaFS {
    private String nombre;
    private String rutaCompleta;
    private boolean esDirectorio;
    private String dueno;
    private PermisoArchivo permiso;
    private int tamanoBloques;
    private int primerBloque;
    private String procesoCreador;
    private Color color;
    private ListaEnlazada<EntradaFS> hijos;

    public EntradaFS(String nombre, String rutaCompleta, boolean esDirectorio, String dueno,
                     PermisoArchivo permiso, int tamanoBloques, int primerBloque,
                     String procesoCreador, Color color) {
        this.nombre = nombre;
        this.rutaCompleta = rutaCompleta;
        this.esDirectorio = esDirectorio;
        this.dueno = (dueno != null && !dueno.isEmpty()) ? dueno : "admin";
        this.permiso = (permiso != null) ? permiso : PermisoArchivo.PUBLICO;
        this.tamanoBloques = esDirectorio ? 0 : Math.max(1, tamanoBloques);
        this.primerBloque = esDirectorio ? -1 : primerBloque;
        this.procesoCreador = (procesoCreador != null && !procesoCreador.isEmpty()) ? procesoCreador : "Sistema (Inicial)";
        this.color = color;
        this.hijos = esDirectorio ? new ListaEnlazada<>() : null;
    }

    public void agregarHijo(EntradaFS hijo) {
        if (esDirectorio && hijo != null) {
            hijos.agregar(hijo);
        }
    }

    public boolean eliminarHijo(EntradaFS hijo) {
        if (esDirectorio && hijo != null) {
            return hijos.eliminar(hijo);
        }
        return false;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRutaCompleta() { return rutaCompleta; }
    public void setRutaCompleta(String rutaCompleta) { this.rutaCompleta = rutaCompleta; }

    public boolean isEsDirectorio() { return esDirectorio; }
    public String getDueno() { return dueno; }
    public void setDueno(String dueno) { this.dueno = dueno; }

    public PermisoArchivo getPermiso() { return permiso; }
    public void setPermiso(PermisoArchivo permiso) { this.permiso = permiso; }

    public int getTamanoBloques() { return tamanoBloques; }
    public void setTamanoBloques(int tamanoBloques) { this.tamanoBloques = tamanoBloques; }

    public int getPrimerBloque() { return primerBloque; }
    public void setPrimerBloque(int primerBloque) { this.primerBloque = primerBloque; }

    public String getProcesoCreador() { return procesoCreador; }
    public void setProcesoCreador(String procesoCreador) { this.procesoCreador = procesoCreador; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public ListaEnlazada<EntradaFS> getHijos() { return hijos; }

    @Override
    public String toString() {
        return nombre + (esDirectorio ? "/" : " (" + tamanoBloques + " blq)");
    }
}