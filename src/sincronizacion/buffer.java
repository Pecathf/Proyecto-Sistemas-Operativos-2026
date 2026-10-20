/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sincronizacion;
import edd.ListaEnlazada;
/**
 *
 * @author PECA
 */
public class buffer {
    

public class Buffer {
    private String id;
    private int capacidad;
    private ListaEnlazada<Object> almacen;
    
    private Semaforo mutex;
    private Semaforo vacios;
    private Semaforo llenos;

    public Buffer(String id, int capacidad) {
        this.id = id;
        this.capacidad = capacidad;
        this.almacen = new ListaEnlazada<>();
        
        this.mutex = new Semaforo(id + "_mutex", 1);
        this.vacios = new Semaforo(id + "_vacios", capacidad);
        this.llenos = new Semaforo(id + "_llenos", 0);
    }

    public Semaforo getMutex() { return mutex; }
    public Semaforo getVacios() { return vacios; }
    public Semaforo getLlenos() { return llenos; }
    public int getElementosActuales() { return almacen.getTamano(); }
    public int getCapacidad() { return capacidad; }
    
    public void insertar(Object elemento) { almacen.agregar(elemento); }
    public Object extraer() { return almacen.eliminarCabeza(); }
}
    
}
