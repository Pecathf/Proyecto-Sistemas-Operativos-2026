/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sincronizacion;
import edd.Cola;
import procesos.Proceso;
import procesos.Planificador;
/**
 *
 * @author Jabri
 */

public class Semaforo {
    private String nombre;
    private int valor;
    private Cola<Proceso> colaBloqueados;

    public Semaforo(String nombre, int valorInicial) {
        this.nombre = nombre;
        this.valor = valorInicial;
        this.colaBloqueados = new Cola<>();
    }

    public synchronized void semWait(Proceso p, Planificador planificadorAnfitrion) {
        valor--;
        if (valor < 0) {
            colaBloqueados.encolar(p);
            planificadorAnfitrion.bloquearProcesoActual("Esperando Semáforo: " + nombre);
        }
    }

    public synchronized void semSignal(Planificador planificadorAnfitrion) {
        valor++;
        if (valor <= 0) {
            if (!colaBloqueados.estaVacia()) {
                Proceso liberado = colaBloqueados.desencolar();
                planificadorAnfitrion.desbloquearProcesoEspecifico(liberado);
            }
        }
    }

    public int getValor() { return valor; }
    public String getNombre() { return nombre; }
}