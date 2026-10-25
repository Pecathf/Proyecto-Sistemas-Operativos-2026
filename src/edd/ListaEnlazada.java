package edd;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Jabri
 */

public class ListaEnlazada<T> {
    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    public synchronized void agregar(T elemento) {
        Nodo<T> nuevoNodo = new Nodo<>(elemento);
        if (cabeza == null) {
            cabeza = nuevoNodo;
            cola = nuevoNodo;
        } else {
            cola.setSiguiente(nuevoNodo);
            cola = nuevoNodo;
        }
        tamano++;
    }

    public synchronized T eliminarCabeza() {
        if (estaVacia())
            return null;
        T dato = cabeza.getDato();
        cabeza = cabeza.getSiguiente();
        if (cabeza == null) {
            cola = null;
        }
        tamano--;
        return dato;
    }

    public synchronized boolean eliminar(T elemento) {
        if (estaVacia() || elemento == null)
            return false;

        if (cabeza.getDato().equals(elemento)) {
            eliminarCabeza();
            return true;
        }

        Nodo<T> actual = cabeza;
        while (actual.getSiguiente() != null) {
            if (elemento.equals(actual.getSiguiente().getDato())) {
                if (actual.getSiguiente() == cola) {
                    cola = actual;
                }
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamano--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    public synchronized T obtener(int indice) {
        if (indice < 0 || indice >= tamano)
            return null;
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    public synchronized boolean estaVacia() {
        return tamano == 0;
    }

    public synchronized int getTamano() {
        return tamano;
    }

    public synchronized ListaEnlazada<T> clonar() {
        ListaEnlazada<T> copia = new ListaEnlazada<>();
        Nodo<T> actual = cabeza;
        while (actual != null) {
            copia.agregar(actual.getDato());
            actual = actual.getSiguiente();
        }
        return copia;
    }
}