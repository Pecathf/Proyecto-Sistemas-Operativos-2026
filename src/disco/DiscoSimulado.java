/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */
import arquitectura.ComponenteReloj;
import arquitectura.Computador;
import edd.Cola;
import edd.ListaEnlazada;
import procesos.EstadoProceso;
import procesos.MotivoBloqueo;
import procesos.Proceso;
import utilidades.RegistroEventos;

import java.awt.Color;
import java.util.concurrent.Semaphore;

public class DiscoSimulado implements ComponenteReloj {

    private int totalBloques;
    private int tamanoSwapBloques;
    private int inicioAreaSwap;
    private Bloque[] bloques;

    private int cabezalActual;
    private boolean[] direccionSubiendo; // true = hacia bloques mayores (↑), false = hacia bloques menores (↓)
    private int velocidadCabezalPorCiclo;

    private PoliticaPlanificacionDisco politica;
    private Cola<SolicitudIO> colaSolicitudes;
    private SolicitudIO solicitudEnCurso;
    private int ciclosRestantesSolicitud;

    private Computador computador;
    private RegistroEventos logger;
    private Semaphore mutexDisco;
    private CacheBloques cache;

    // Métricas del Disco
    private int desplazamientoTotal;
    private int totalSolicitudesAtendidas;
    private long sumaTiemposEsperaIO;

    private int atendidasFIFO;
    private int atendidasSSTF;
    private int atendidasSCAN;
    private int atendidasCSCAN;

    public DiscoSimulado(int totalBloques, int tamanoSwapBloques, int cabezalInicial, PoliticaPlanificacionDisco politicaInicial) {
        this.totalBloques = Math.max(20, totalBloques);
        this.tamanoSwapBloques = Math.min(Math.max(0, tamanoSwapBloques), this.totalBloques / 2);
        this.inicioAreaSwap = this.totalBloques - this.tamanoSwapBloques;
        this.bloques = new Bloque[this.totalBloques];

        for (int i = 0; i < this.totalBloques; i++) {
            boolean esSwap = (i >= this.inicioAreaSwap);
            this.bloques[i] = new Bloque(i, esSwap);
        }

        this.cabezalActual = Math.max(0, Math.min(cabezalInicial, this.totalBloques - 1));
        this.direccionSubiendo = new boolean[]{true}; // Por defecto inicia subiendo (↑)
        this.velocidadCabezalPorCiclo = 25; // Posiciones que recorre el cabezal por ciclo de reloj

        this.politica = (politicaInicial != null) ? politicaInicial : new PoliticaDiscoFIFO();
        this.colaSolicitudes = new Cola<>();
        this.solicitudEnCurso = null;
        this.ciclosRestantesSolicitud = 0;

        this.mutexDisco = new Semaphore(1);
        this.desplazamientoTotal = 0;
        this.totalSolicitudesAtendidas = 0;
        this.sumaTiemposEsperaIO = 0;
        this.cache = new CacheBloques(16);
    }

    public void setComputador(Computador computador) {
        this.computador = computador;
    }

    public void setLogger(RegistroEventos logger) {
        this.logger = logger;
    }

    private void log(String mensaje) {
        if (logger != null) {
            logger.registrar(mensaje);
        }
    }

    public synchronized void setPolitica(PoliticaPlanificacionDisco nuevaPolitica) {
        if (nuevaPolitica != null) {
            this.politica = nuevaPolitica;
            log("Política de planificación de disco cambiada a: " + nuevaPolitica.getTipo().getNombreLegible());
        }
    }

    public PoliticaPlanificacionDisco getPolitica() {
        return politica;
    }

    /**
     * Encola una nueva solicitud de E/S en la cola única del disco.
     */
    public void encolarSolicitud(SolicitudIO solicitud) {
        try {
            mutexDisco.acquire();
            colaSolicitudes.encolar(solicitud);
            log("Solicitud E/S encolada: " + solicitud.toString() + " (" + politica.getTipo().name() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexDisco.release();
        }
    }

    /**
     * Ejecuta exactamente 1 ciclo de trabajo del disco sincronizado con el Reloj Global.
     */
    @Override
    public void ejecutarCiclo(int cicloActual) {
        try {
            mutexDisco.acquire();

            // 1. Si no hay solicitud atendiéndose, seleccionar la siguiente según la política activa
            if (solicitudEnCurso == null && !colaSolicitudes.estaVacia()) {
                boolean subiendoAntes = direccionSubiendo[0];
                solicitudEnCurso = politica.seleccionarSiguiente(colaSolicitudes, cabezalActual, direccionSubiendo, totalBloques);

                if (solicitudEnCurso != null) {
                    int objetivo = Math.max(0, Math.min(solicitudEnCurso.getBloqueObjetivo(), totalBloques - 1));
                    int distancia = calcularDesplazamiento(cabezalActual, objetivo, subiendoAntes);

                    desplazamientoTotal += distancia;
                    cabezalActual = objetivo;

                    // Tiempo de espera en cola de E/S
                    int esperaEnCola = Math.max(0, cicloActual - solicitudEnCurso.getCicloLlegadaCola());
                    sumaTiemposEsperaIO += esperaEnCola;

                    // Duración: desplazamiento del cabezal + 1 ciclo por cada bloque leído/escrito
                    int ciclosMovimiento = (int) Math.ceil((double) distancia / velocidadCabezalPorCiclo);
                    int ciclosTransferencia = Math.max(1, solicitudEnCurso.getTamanoBloques());
                    ciclosRestantesSolicitud = Math.max(1, ciclosMovimiento + ciclosTransferencia);

                    String nomProc = (solicitudEnCurso.getProceso() != null) ? solicitudEnCurso.getProceso().getNombre() : "SO";
                    log("El disco atiende la solicitud de " + nomProc + " (" + solicitudEnCurso.getOperacion() +
                        ") en el bloque " + objetivo + " (" + politica.getTipo().name() + ", Δ=" + distancia + ")");
                }
            }

            // 2. Avanzar el trabajo de la solicitud en curso
            if (solicitudEnCurso != null) {
                ciclosRestantesSolicitud--;
                if (ciclosRestantesSolicitud <= 0) {
                    finalizarSolicitudActual(cicloActual);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexDisco.release();
        }
    }

    /**
     * Calcula el desplazamiento del cabezal respetando el recorrido hasta el extremo en SCAN / C-SCAN.
     */
    private int calcularDesplazamiento(int origen, int destino, boolean subiendoAntes) {
        TipoPoliticaDisco tipo = politica.getTipo();
        if (tipo == TipoPoliticaDisco.SCAN && subiendoAntes != direccionSubiendo[0]) {
            // En SCAN, si invirtió dirección, llegó hasta el extremo (totalBloques - 1 o 0) y regresó
            if (subiendoAntes) {
                return ((totalBloques - 1) - origen) + ((totalBloques - 1) - destino);
            } else {
                return origen + destino;
            }
        } else if (tipo == TipoPoliticaDisco.C_SCAN && destino < origen) {
            // En C-SCAN, llega al final (totalBloques - 1) y salta al 0 (sin contar el salto de regreso)
            return ((totalBloques - 1) - origen) + destino;
        }
        return Math.abs(destino - origen);
    }

    private void finalizarSolicitudActual(int cicloActual) {
        totalSolicitudesAtendidas++;
        switch (politica.getTipo()) {
            case FIFO: atendidasFIFO++; break;
            case SSTF: atendidasSSTF++; break;
            case SCAN: atendidasSCAN++; break;
            case C_SCAN: atendidasCSCAN++; break;
        }

        Proceso p = solicitudEnCurso.getProceso();
        TipoOperacionIO op = solicitudEnCurso.getOperacion();
        solicitudEnCurso = null;

        // Despertar o actualizar el estado del proceso asociado
        if (p != null && computador != null) {
            if (op == TipoOperacionIO.SUSPEND) {
                // El proceso ya quedó escrito en Swap; pasa a Bloqueado/Suspendido o Listo/Suspendido
                if (p.getEstado() == EstadoProceso.BLOQUEADO) {
                    p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
                } else {
                    p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                }
                log("Disco completó escritura en Swap para proceso suspendido: " + p.getNombre());
            } else if (op == TipoOperacionIO.RESUME) {
                // El proceso regresa de Swap a RAM -> pasa a Listo
                liberarBloquesSwap(p.getNombre());
                p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                computador.getPlanificador().desbloquearProcesoLocal(p);
                log("Disco reactivó desde Swap al proceso: " + p.getNombre());
            } else {
                // Operación CRUD sobre archivo completada: despertar al proceso
                if (p.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
                    p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                    p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                    log("Disco completó E/S de " + p.getNombre() + " (pasa a LISTO_SUSPENDIDO)");
                } else {
                    p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                    computador.getPlanificador().desbloquearProcesoLocal(p);
                    log("Disco completó E/S de " + p.getNombre() + " -> pasa a LISTO");
                }
            }
        }
    }

    // =========================================================================
    // GESTIÓN DE BLOQUES (ASIGNACIÓN ENCADENADA Y ÁREA DE SWAP)
    // =========================================================================

    public synchronized int contarBloquesLibresArchivos() {
        int libres = 0;
        for (int i = 0; i < inicioAreaSwap; i++) {
            if (!bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    public synchronized int contarBloquesLibresSwap() {
        int libres = 0;
        for (int i = inicioAreaSwap; i < totalBloques; i++) {
            if (!bloques[i].isOcupado()) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Asigna N bloques libres usando asignación encadenada.
     * @return Índice del primer bloque de la cadena, o -1 si no hay espacio suficiente.
     */
    public synchronized int asignarCadenaBloques(String rutaArchivo, int cantidadBloques, Color color) {
        if (cantidadBloques <= 0 || contarBloquesLibresArchivos() < cantidadBloques) {
            return -1;
        }

        int primerBloque = -1;
        int bloqueAnterior = -1;
        int asignados = 0;

        for (int i = 0; i < inicioAreaSwap && asignados < cantidadBloques; i++) {
            if (!bloques[i].isOcupado()) {
                bloques[i].asignar(rutaArchivo, -1, color);
                if (primerBloque == -1) {
                    primerBloque = i;
                }
                if (bloqueAnterior != -1) {
                    bloques[bloqueAnterior].setSiguienteBloque(i);
                }
                bloqueAnterior = i;
                asignados++;
            }
        }
        return primerBloque;
    }

    /**
     * Asigna una cadena iniciando en un bloque específico (usado al cargar el JSON inicial).
     */
    public synchronized boolean asignarCadenaDesdeBloque(String rutaArchivo, int startBlock, int cantidadBloques, Color color) {
        if (startBlock < 0 || startBlock >= inicioAreaSwap || contarBloquesLibresArchivos() < cantidadBloques) {
            return false;
        }
        if (bloques[startBlock].isOcupado()) {
            return false;
        }

        bloques[startBlock].asignar(rutaArchivo, -1, color);
        int bloqueAnterior = startBlock;
        int asignados = 1;

        for (int i = 0; i < inicioAreaSwap && asignados < cantidadBloques; i++) {
            if (i != startBlock && !bloques[i].isOcupado()) {
                bloques[i].asignar(rutaArchivo, -1, color);
                bloques[bloqueAnterior].setSiguienteBloque(i);
                bloqueAnterior = i;
                asignados++;
            }
        }
        return true;
    }

    /**
     * Libera toda la cadena de bloques enlazados a partir de primerBloque.
     */
    public synchronized void liberarCadenaBloques(int primerBloque) {
        int actual = primerBloque;
        int seguridad = 0;
        while (actual >= 0 && actual < inicioAreaSwap && seguridad < totalBloques) {
            int siguiente = bloques[actual].getSiguienteBloque();
            if (cache != null) { cache.invalidarBloque(actual); }
            bloques[actual].liberar();
            actual = siguiente;
            seguridad++;
        }
    }

    /**
     * Reserva bloques en el área de Swap para un proceso suspendido.
     * @return Primer bloque asignado en Swap, o -1 si el área de Swap está llena.
     */
    public synchronized int asignarBloquesSwap(String nombreProceso, int cantidadBloques) {
        int requeridos = Math.max(1, cantidadBloques);
        if (contarBloquesLibresSwap() < requeridos) {
            return -1;
        }

        int primerBloque = -1;
        int bloqueAnterior = -1;
        int asignados = 0;
        Color colorSwap = new Color(180, 90, 220);

        for (int i = inicioAreaSwap; i < totalBloques && asignados < requeridos; i++) {
            if (!bloques[i].isOcupado()) {
                bloques[i].asignar("SWAP:" + nombreProceso, -1, colorSwap);
                if (primerBloque == -1) {
                    primerBloque = i;
                }
                if (bloqueAnterior != -1) {
                    bloques[bloqueAnterior].setSiguienteBloque(i);
                }
                bloqueAnterior = i;
                asignados++;
            }
        }
        return primerBloque;
    }

    public synchronized void liberarBloquesSwap(String nombreProceso) {
        String etiqueta = "SWAP:" + nombreProceso;
        for (int i = inicioAreaSwap; i < totalBloques; i++) {
            if (bloques[i].isOcupado() && etiqueta.equals(bloques[i].getPropietario())) {
                bloques[i].liberar();
            }
        }
    }

    // =========================================================================
    // GETTERS Y MÉTRICAS
    // =========================================================================

    public int getTotalBloques() { return totalBloques; }
    public int getTamanoSwapBloques() { return tamanoSwapBloques; }
    public int getInicioAreaSwap() { return inicioAreaSwap; }
    public Bloque getBloque(int index) { return (index >= 0 && index < totalBloques) ? bloques[index] : null; }
    public CacheBloques getCache() { return cache; }
    public int getCabezalActual() { return cabezalActual; }
    public synchronized void setCabezalActual(int nuevaPosicion) { this.cabezalActual = Math.max(0, Math.min(nuevaPosicion, this.totalBloques - 1)); }
    public boolean isDireccionSubiendo() { return direccionSubiendo[0]; }
    public SolicitudIO getSolicitudEnCurso() { return solicitudEnCurso; }

    public synchronized ListaEnlazada<SolicitudIO> getColaSolicitudesSnapshot() {
        return colaSolicitudes.obtenerListaElementos();
    }

    public int getDesplazamientoTotal() { return desplazamientoTotal; }
    public int getTotalSolicitudesAtendidas() { return totalSolicitudesAtendidas; }

    public double getTiempoPromedioEsperaIO() {
        return (totalSolicitudesAtendidas == 0) ? 0.0 : ((double) sumaTiemposEsperaIO / totalSolicitudesAtendidas);
    }

    public int getAtendidasFIFO() { return atendidasFIFO; }
    public int getAtendidasSSTF() { return atendidasSSTF; }
    public int getAtendidasSCAN() { return atendidasSCAN; }
    public int getAtendidasCSCAN() { return atendidasCSCAN; }
}