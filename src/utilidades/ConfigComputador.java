/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author PECA
 */
import politicas.TipoPolitica;
public class ConfigComputador {
    public String nombre;
    public int ramMb;
    public TipoPolitica politica;
    public int quantum;

    public ConfigComputador(String nombre, int ramMb, TipoPolitica politica, int quantum) {
        this.nombre = nombre;
        this.ramMb = ramMb;
        this.politica = (politica != null) ? politica : TipoPolitica.ROUND_ROBIN;
        this.quantum = (quantum > 0) ? quantum : 3;
    }

    public ConfigComputador() {
        this("Computador", 8192, TipoPolitica.ROUND_ROBIN, 3);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getRamMb() { return ramMb; }
    public void setRamMb(int ramMb) { this.ramMb = ramMb; }

    public TipoPolitica getPolitica() { return politica; }
    public void setPolitica(TipoPolitica politica) { this.politica = politica; }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
}
