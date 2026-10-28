/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arquitectura;
import procesos.ModoEjecucion;
import procesos.Proceso;

/**
 *
 * @author Jabri
 */


public class CPU implements ComponenteReloj {
    private Proceso procesoActual;
    private int pc;
    private int mar;
    private ModoEjecucion modo;
    private int ciclosActivos;
    private int ciclosTotal;

    public CPU() {
        this.procesoActual = null;
        this.pc = 0;
        this.mar = 0;
        this.modo = ModoEjecucion.USUARIO;
        this.ciclosActivos = 0;
        this.ciclosTotal = 0;
    }

    @Override
    public void ejecutarCiclo(int cicloActual) {
        ciclosTotal++;
        if (procesoActual != null) {
            ciclosActivos++;
            pc++;
            mar++;
            procesoActual.setPc(pc);
            procesoActual.setMar(mar);
            this.modo = procesoActual.getModo();
        } else {
            this.modo = ModoEjecucion.SISTEMA_OPERATIVO; // Idle / OS kernel esperando
        }
    }

    public void cargarProceso(Proceso p) {
        this.procesoActual = p;
        if (p != null) {
            this.pc = p.getPc();
            this.mar = p.getMar();
            this.modo = p.getModo();
        }
    }

    public void liberarProceso() {
        if (procesoActual != null) {
            procesoActual.setPc(pc);
            procesoActual.setMar(mar);
            procesoActual.setModo(modo);
        }
        this.procesoActual = null;
    }

    public Proceso getProcesoActual() { return procesoActual; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public ModoEjecucion getModo() { return modo; }
    public void setModo(ModoEjecucion modo) { 
        this.modo = modo; 
        if (procesoActual != null) procesoActual.setModo(modo);
    }
    public int getCiclosActivos() { return ciclosActivos; }
    public int getCiclosTotal() { return ciclosTotal; }
    public double getPorcentajeUso() {
        return (ciclosTotal == 0) ? 0.0 : ((double) ciclosActivos / ciclosTotal) * 100.0;
    }
}