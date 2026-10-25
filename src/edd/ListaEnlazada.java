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

    public void agregar(T elemento) {
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

    public T eliminarCabeza() {
        if (estaVacia()) return null;
        T dato = cabeza.getDato();
        cabeza = cabeza.getSiguiente();
        if (cabeza == null) cola = null;
        tamano--;
        return dato;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) return null;
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    public boolean estaVacia() { 
        return tamano == 0; 
    }

    public int getTamano() { 
        return tamano; 
    }
}
