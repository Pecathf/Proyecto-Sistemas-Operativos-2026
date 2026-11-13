/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */

import java.awt.Color;

public class Bloque {
    private int numero;
    private boolean ocupado;
    private boolean esAreaSwap;
    private String propietario; // Ruta del archivo o "SWAP:Proceso"
    private int siguienteBloque; // -1 si es el último bloque de la cadena
    private Color color;

    public Bloque(int numero, boolean esAreaSwap) {
        this.numero = numero;
        this.esAreaSwap = esAreaSwap;
        this.ocupado = false;
        this.propietario = "";
        this.siguienteBloque = -1;
        this.color = null;
    }

    public void asignar(String propietario, int siguienteBloque, Color color) {
        this.ocupado = true;
        this.propietario = propietario;
        this.siguienteBloque = siguienteBloque;
        this.color = color;
    }

    public void liberar() {
        this.ocupado = false;
        this.propietario = "";
        this.siguienteBloque = -1;
        this.color = null;
    }

    public int getNumero() { return numero; }
    public boolean isOcupado() { return ocupado; }
    public boolean isEsAreaSwap() { return esAreaSwap; }
    public String getPropietario() { return propietario; }
    public int getSiguienteBloque() { return siguienteBloque; }
    public void setSiguienteBloque(int siguienteBloque) { this.siguienteBloque = siguienteBloque; }
    public Color getColor() { return color; }
}