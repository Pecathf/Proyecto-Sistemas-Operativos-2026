/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author samue
 */
public class RegistroEventos {
    private final StringBuilder historial = new StringBuilder();

    public synchronized void registrar(String evento) {
        historial.append("• ").append(evento).append("\n");
        System.out.println(evento);
    }

    public synchronized String obtenerHistorialCompleto() {
        return historial.toString();
    }

    @Override
    public String toString() {
        return obtenerHistorialCompleto();
    }
}