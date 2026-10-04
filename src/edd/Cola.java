/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Jabri
 */
package edd;

public class Cola<T> {
    private final ListaEnlazada<T> lista;

    public Cola() {
        this.lista = new ListaEnlazada<>();
    }

    public void encolar(T elemento) {
        lista.agregar(elemento);
    }

    public T desencolar() {
        return lista.eliminarCabeza();
    }

    public T obtenerFrente() {
        if (lista.estaVacia()) return null;
        return lista.obtener(0);
    }

    public boolean estaVacia() {
        return lista.estaVacia();
    }

    public int getTamano() {
        return lista.getTamano();
    }
}
