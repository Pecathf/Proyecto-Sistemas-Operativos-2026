/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hardware;

/**
 *
 * @author PECA
 */
import procesos.Planificador;
import procesos.Proceso;
import politicas.PoliticaPlanificacion;
public class Computador {
    private String idComputador;
    private int memoriaTotal;
    private int memoriaDisponible;
    private Planificador kernelLocal;

    public Computador(String idComputador, int memoriaTotal, PoliticaPlanificacion politicaInicial) {
        this.idComputador = idComputador;
        this.memoriaTotal = memoriaTotal;
        this.memoriaDisponible = memoriaTotal;
        this.kernelLocal= new Planificador(3, politicaInicial);
        this.kernelLocal.setPolitica(politicaInicial);
    }

    public boolean admitirProceso(Proceso p) {
        if (p.getMemoriaRequerida() <= this.memoriaDisponible) {
        this.memoriaDisponible -= p.getMemoriaRequerida();
        
        // ❌ Antes: this.kernelLocal.crearProceso(p); (Lo devolvía a Trabajos)
        // ✅ Ahora: Lo encolamos directamente en la Cola de Listos
        this.kernelLocal.getColaListos().encolar(p); 
        
        System.out.println("[MEMORIA " + idComputador + "] Proceso " + p.getId() + 
                           " admitido (" + p.getMemoriaRequerida() + " MB). RAM libre: " + 
                           memoriaDisponible + " MB");
        return true;
    }
    System.out.println("[RECHAZO " + idComputador + "] RAM insuficiente para Proceso " + p.getId() + 
                       " (Requiere: " + p.getMemoriaRequerida() + " MB, Libre: " + memoriaDisponible + " MB)");
    return false;
    }

    public void liberarMemoria(Proceso p) {
        this.memoriaDisponible += p.getMemoriaRequerida();
        System.out.println("[MEMORIA " + idComputador + "] Proceso " + p.getId() + 
                           " liberó " + p.getMemoriaRequerida() + " MB. RAM libre: " + memoriaDisponible + " MB");
    }

    public void procesarCicloReloj() {
        if (this.kernelLocal != null) {
            this.kernelLocal.ejecutarCiclo();
        }
    }
    public void liberarProceso(Proceso p) {
    if (p != null) {
        this.memoriaDisponible += p.getMemoriaRequerida();
        // Asegurarse de que no supere el total de RAM del equipo
        if (this.memoriaDisponible > this.memoriaTotal) {
            this.memoriaDisponible = this.memoriaTotal;
        }
    }
}

    public Planificador getKernelLocal() { return kernelLocal; }
    public String getIdComputador() { return idComputador; }
    public int getMemoriaDisponible() { return memoriaDisponible; }
    public int getMemoriaTotal() { return memoriaTotal; }
}
