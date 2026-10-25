/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sincronizacion;

/**
 *
 * @author PECA
 */

import edd.ListaEnlazada;

public class Buffer {
    private String id;
    private int capacidad;
    private int memoriaOcupadaMb;
    private String computadorAnfitrion;
    private ListaEnlazada<String> almacen;

    private Semaforo mutex;
    private Semaforo vacios;
    private Semaforo llenos;

    // Métricas del Buffer
    private int totalProducidos;
    private int totalConsumidos;

    public Buffer(String id, int capacidad, String computadorAnfitrion, int memoriaOcupadaMb) {
        this.id = id;
        this.capacidad = capacidad;
        this.computadorAnfitrion = computadorAnfitrion;
        this.memoriaOcupadaMb = memoriaOcupadaMb > 0 ? memoriaOcupadaMb : capacidad * 64; // Default razonable si no se especifica
        this.almacen = new ListaEnlazada<>();

        this.mutex = new Semaforo(id + "_mutex", 1);
        this.vacios = new Semaforo(id + "_vacios", capacidad);
        this.llenos = new Semaforo(id + "_llenos", 0);

        this.totalProducidos = 0;
        this.totalConsumidos = 0;
    }

    public Buffer(String id, int capacidad, String computadorAnfitrion) {
        this(id, capacidad, computadorAnfitrion, capacidad * 64);
    }

    public synchronized void insertar(String elemento) {
        almacen.agregar(elemento);
        totalProducidos++;
    }

    public synchronized String extraer() {
        if (!almacen.estaVacia()) {
            totalConsumidos++;
            return almacen.eliminarCabeza();
        }
        return null;
    }

    public String getId() { return id; }
    public int getCapacidad() { return capacidad; }
    public int getMemoriaOcupadaMb() { return memoriaOcupadaMb; }
    public String getComputadorAnfitrion() { return computadorAnfitrion; }
    public Semaforo getMutex() { return mutex; }
    public Semaforo getVacios() { return vacios; }
    public Semaforo getLlenos() { return llenos; }
    public synchronized int getElementosActuales() { return almacen.getTamano(); }
    public int getTotalProducidos() { return totalProducidos; }
    public int getTotalConsumidos() { return totalConsumidos; }
    public synchronized ListaEnlazada<String> getAlmacenCopia() { return almacen.clonar(); }
}