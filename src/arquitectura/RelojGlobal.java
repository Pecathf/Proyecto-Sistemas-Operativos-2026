package arquitectura;

/**
 *
 * @author PECA
 */
import edd.ListaEnlazada;
import java.util.concurrent.Semaphore;

public class RelojGlobal extends Thread {

    private int cicloActual;
    private int duracionCicloMs;
    private volatile boolean ejecutando;
    private volatile boolean pausado;

    private ListaEnlazada<ComponenteReloj> componentes;
    private Semaphore mutexComponentes;
    private Runnable listenerActualizacionGUI;

    public RelojGlobal(int duracionCicloMs) {
        this.cicloActual = 0;
        this.duracionCicloMs = Math.max(20, duracionCicloMs);
        this.ejecutando = false;
        this.pausado = true;
        this.componentes = new ListaEnlazada<>();
        this.mutexComponentes = new Semaphore(1);
        setName("Hilo-RelojGlobal");
    }

    public void registrarComponente(ComponenteReloj componente) {
        if (componente == null) return;
        try {
            mutexComponentes.acquire();
            componentes.agregar(componente);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexComponentes.release();
        }
    }

    public void agregarComputador(Computador c) {
        registrarComponente(c);
    }

    public void setListenerActualizacionGUI(Runnable listener) {
        this.listenerActualizacionGUI = listener;
    }

    public void setDuracionCicloMs(int duracionCicloMs) {
        this.duracionCicloMs = Math.max(20, duracionCicloMs);
    }

    public int getDuracionCicloMs() {
        return duracionCicloMs;
    }

    public int getCicloActual() {
        return cicloActual;
    }

    public boolean isPausado() {
        return pausado;
    }

    public void iniciarSimulacion() {
        this.pausado = false;
        if (!isAlive()) {
            this.ejecutando = true;
            start();
        }
    }

    public void pausarSimulacion() {
        this.pausado = true;
    }

    public void reanudarSimulacion() {
        this.pausado = false;
    }

    public void detenerSimulacion() {
        this.ejecutando = false;
        this.pausado = false;
        interrupt();
    }

    // --- Métodos de compatibilidad con versiones previas ---
    public void pausar() {
        pausarSimulacion();
    }

    public void reanudar() {
        reanudarSimulacion();
    }

    public void detener() {
        detenerSimulacion();
    }

    public void esperarInicioCiclo() throws InterruptedException {
        // Compatibilidad con invocaciones antiguas
    }

    public void notificarFinCiclo() {
        // Compatibilidad con invocaciones antiguas
    }

    @Override
    public void run() {
        while (ejecutando) {
            try {
                if (!pausado) {
                    avanzarUnCiclo();
                }
                Thread.sleep(duracionCicloMs);
            } catch (InterruptedException e) {
                if (!ejecutando) break;
            }
        }
    }

    public void avanzarUnCiclo() {
        cicloActual++;
        try {
            mutexComponentes.acquire();
            for (int i = 0; i < componentes.getTamano(); i++) {
                componentes.obtener(i).ejecutarCiclo(cicloActual);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexComponentes.release();
        }

        if (listenerActualizacionGUI != null) {
            listenerActualizacionGUI.run();
        }
    }
}
