package edd;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Jabri
 */

public class Cola<T> {
    private final ListaEnlazada<T> lista;

    public Cola() {
        this.lista = new ListaEnlazada<>();
    }

    public synchronized void encolar(T elemento) {
        lista.agregar(elemento);
    }

    public synchronized T desencolar() {
        return lista.eliminarCabeza();
    }

    public synchronized T obtenerFrente() {
        if (lista.estaVacia()) return null;
        return lista.obtener(0);
    }

    public synchronized boolean estaVacia() {
        return lista.estaVacia();
    }

    public synchronized int getTamano() {
        return lista.getTamano();
    }

    // Retorna una copia de la lista interna sin vaciar ni alterar la cola
    public synchronized ListaEnlazada<T> obtenerListaElementos() {
        return lista.clonar();
    }
}
