package procesos;
/**
 *
 * @author Jabri
 */
import archivos.EntradaFS;
import archivos.PermisoArchivo;
import archivos.SistemaArchivos;
import arquitectura.CPU;
import arquitectura.Computador;
import disco.DiscoSimulado;
import disco.SolicitudIO;
import disco.TipoOperacionIO;
import edd.Cola;
import edd.ListaEnlazada;
import politicas.PoliticaPlanificacion;
import utilidades.RegistroEventos;

import java.util.concurrent.Semaphore;

public class Planificador {

    private Computador computador;
    private PoliticaPlanificacion politica;
    private int quantum;
    private RegistroEventos logger;
    private Semaphore mutexColas;

    // Métricas de Swap (Planificador a Mediano Plazo)
    private int totalSuspensiones;
    private int totalReactivaciones;

    public Planificador(Computador computador, PoliticaPlanificacion politica, int quantum) {
        this.computador = computador;
        this.politica = politica;
        this.quantum = Math.max(1, quantum);
        this.mutexColas = new Semaphore(1);
        this.totalSuspensiones = 0;
        this.totalReactivaciones = 0;
    }

    public void setLogger(RegistroEventos logger) {
        this.logger = logger;
    }

    private void log(String msg) {
        if (logger != null) {
            logger.registrar(msg);
        }
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
        if (politica != null) {
            log("Política de planificación CPU cambiada a: " + politica.getTipo().name());
        }
    }

    public PoliticaPlanificacion getPolitica() {
        return politica;
    }

    public void setQuantum(int quantum) {
        this.quantum = Math.max(1, quantum);
    }

    public int getQuantum() {
        return quantum;
    }

    /**
     * Admite un proceso nuevo: si hay RAM va a LISTO; si no hay RAM, intenta suspender
     * un proceso bloqueado o lo envía directamente a LISTO_SUSPENDIDO.
     */
    public void admitirProceso(Proceso p, int cicloActual) {
        try {
            mutexColas.acquire();
            p.setTiempoLlegada(cicloActual);
            p.setIdComputadorAsignado(computador.getId());

            if (computador.asignarMemoria(p.getMemoriaRequerida())) {
                p.setEstado(EstadoProceso.LISTO);
                computador.getColaListos().encolar(p);
                log("Proceso " + p.getNombre() + " admitido en RAM -> LISTO (" + p.getMemoriaRequerida() + " MB)");
            } else {
                // Intentar liberar RAM suspendiendo un proceso bloqueado (Mediano Plazo)
                boolean espacioLiberado = intentarSuspenderParaLiberarRAM(p.getMemoriaRequerida(), cicloActual);
                if (espacioLiberado && computador.asignarMemoria(p.getMemoriaRequerida())) {
                    p.setEstado(EstadoProceso.LISTO);
                    computador.getColaListos().encolar(p);
                    log("Proceso " + p.getNombre() + " entró a RAM tras swapping -> LISTO");
                } else {
                    // Si aún no cabe, enviarlo a Swap en estado LISTO_SUSPENDIDO
                    DiscoSimulado disco = computador.getDisco();
                    int bloquesSwap = Math.max(1, (int) Math.ceil(p.getMemoriaRequerida() / 16.0));
                    int bloqueInicio = (disco != null) ? disco.asignarBloquesSwap(p.getNombre(), bloquesSwap) : 0;
                    p.setBloqueInicioSwap(bloqueInicio);
                    p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                    p.setMotivoBloqueo(MotivoBloqueo.SUSPENSION_SWAP);
                    computador.getColaListoSuspendidos().encolar(p);
                    totalSuspensiones++;
                    if (disco != null) {
                        disco.encolarSolicitud(new SolicitudIO(p, TipoOperacionIO.SUSPEND, p.getNombre(),
                                Math.max(0, bloqueInicio), bloquesSwap, cicloActual));
                    }
                    log("RAM insuficiente: " + p.getNombre() + " enviado a Swap -> LISTO_SUSPENDIDO");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Planificador a Mediano Plazo: selecciona un proceso en BLOQUEADO (el que lleva más tiempo bloqueado),
     * libera su RAM y lo transfiere al área de Swap del disco.
     */
    private boolean intentarSuspenderParaLiberarRAM(int memoriaNecesaria, int cicloActual) {
        Cola<Proceso> colaBloqueados = computador.getColaBloqueados();
        DiscoSimulado disco = computador.getDisco();
        if (colaBloqueados.estaVacia() || disco == null) {
            return false;
        }

        ListaEnlazada<Proceso> candidatos = colaBloqueados.obtenerListaElementos();
        Proceso victima = null;
        int mayorTiempoBloqueado = -1;

        for (int i = 0; i < candidatos.getTamano(); i++) {
            Proceso c = candidatos.obtener(i);
            if (c.getTiempoBloqueado() > mayorTiempoBloqueado) {
                mayorTiempoBloqueado = c.getTiempoBloqueado();
                victima = c;
            }
        }

        if (victima == null) return false;

        int bloquesSwap = Math.max(1, (int) Math.ceil(victima.getMemoriaRequerida() / 16.0));
        int bloqueInicio = disco.asignarBloquesSwap(victima.getNombre(), bloquesSwap);
        if (bloqueInicio == -1) {
            return false; // Área de Swap llena
        }

        colaBloqueados.eliminarElemento(victima);
        computador.liberarMemoria(victima.getMemoriaRequerida());
        victima.setBloqueInicioSwap(bloqueInicio);
        victima.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
        victima.setMotivoBloqueo(MotivoBloqueo.SUSPENSION_SWAP);
        computador.getColaBloqueadoSuspendidos().encolar(victima);
        totalSuspensiones++;

        disco.encolarSolicitud(new SolicitudIO(victima, TipoOperacionIO.SUSPEND, victima.getNombre(),
                bloqueInicio, bloquesSwap, cicloActual));
        log("Planificador Mediano Plazo suspendió a " + victima.getNombre() +
            " -> BLOQUEADO_SUSPENDIDO (liberados " + victima.getMemoriaRequerida() + " MB)");

        return computador.getMemoriaDisponible() >= memoriaNecesaria;
    }

    /**
     * Revisa si hay procesos en LISTO_SUSPENDIDO que puedan volver a RAM tras liberarse memoria.
     */
    private void intentarReactivarSuspendidos(int cicloActual) {
        Cola<Proceso> colaListoSusp = computador.getColaListoSuspendidos();
        DiscoSimulado disco = computador.getDisco();
        if (colaListoSusp.estaVacia()) return;

        ListaEnlazada<Proceso> suspendidos = colaListoSusp.obtenerListaElementos();
        for (int i = 0; i < suspendidos.getTamano(); i++) {
            Proceso p = suspendidos.obtener(i);
            if (computador.asignarMemoria(p.getMemoriaRequerida())) {
                colaListoSusp.eliminarElemento(p);
                p.setEstado(EstadoProceso.BLOQUEADO);
                p.setMotivoBloqueo(MotivoBloqueo.REACTIVACION_SWAP);
                computador.getColaBloqueados().encolar(p);
                totalReactivaciones++;

                int bloquesSwap = Math.max(1, (int) Math.ceil(p.getMemoriaRequerida() / 16.0));
                int bloqueOrigen = Math.max(0, p.getBloqueInicioSwap());
                if (disco != null) {
                    disco.encolarSolicitud(new SolicitudIO(p, TipoOperacionIO.RESUME, p.getNombre(),
                            bloqueOrigen, bloquesSwap, cicloActual));
                } else {
                    p.setEstado(EstadoProceso.LISTO);
                    p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                    colaBloqueados.eliminarElemento(p);
                    computador.getColaListos().encolar(p);
                }
                log("Planificador Mediano Plazo reactivando desde Swap a " + p.getNombre());
            }
        }
    }

    /**
     * Ejecuta un ciclo de planificación sobre la CPU y las colas del computador.
     */
    public void ejecutarCicloPlanificacion(int cicloActual) {
        try {
            mutexColas.acquire();

            CPU cpu = computador.getCpu();
            Cola<Proceso> colaListos = computador.getColaListos();
            Cola<Proceso> colaBloqueados = computador.getColaBloqueados();
            Cola<Proceso> colaBloqSusp = computador.getColaBloqueadoSuspendidos();

            // 1. Verificar desalojo por política apropiativa (ej. Prioridad Apropiativa)
            if (!cpu.estaLibre() && politica != null && politica.esApropiativa() && !colaListos.estaVacia()) {
                Proceso actual = cpu.getProcesoActual();
                Proceso candidato = politica.seleccionarSiguiente(colaListos);
                if (candidato != null && candidato.getPrioridad() < actual.getPrioridad()) {
                    cpu.desalojarProceso();
                    actual.setEstado(EstadoProceso.LISTO);
                    colaListos.encolar(actual);
                    log("Desalojo por prioridad: " + actual.getNombre() + " -> LISTO");
                }
            }

            // 2. Despachar siguiente proceso si la CPU está libre
            if (cpu.estaLibre() && !colaListos.estaVacia() && politica != null) {
                Proceso siguiente = politica.seleccionarSiguiente(colaListos);
                if (siguiente != null) {
                    siguiente.setEstado(EstadoProceso.EJECUCION);
                    siguiente.setQuantumRestante(quantum);
                    cpu.asignarProceso(siguiente);
                    log("CPU ejecuta proceso: " + siguiente.getNombre());
                }
            }

            // 3. Ejecutar instrucción en CPU
            if (!cpu.estaLibre()) {
                Proceso enEjecucion = cpu.getProcesoActual();

                // Si es un proceso IO_BOUND con operación pendiente, emitir solicitud al disco en su primer ciclo
                if (enEjecucion.getTipo() == TipoProceso.IO_BOUND
                        && enEjecucion.getOperacionSolicitada() != null
                        && !enEjecucion.isOperacionIOEjecutada()) {
                    enEjecucion.setOperacionIOEjecutada(true);
                    iniciarOperacionDisco(enEjecucion, cicloActual);
                } else {
                    boolean termino = enEjecucion.ejecutarInstruccion(cicloActual);
                    if (termino) {
                        cpu.liberarProceso();
                        enEjecucion.setEstado(EstadoProceso.TERMINADO);
                        enEjecucion.setTiempoFinalizacion(cicloActual);
                        computador.liberarMemoria(enEjecucion.getMemoriaRequerida());
                        computador.getColaTerminados().encolar(enEjecucion);
                        log("Proceso terminado: " + enEjecucion.getNombre() + " (liberados " + enEjecucion.getMemoriaRequerida() + " MB)");
                        intentarReactivarSuspendidos(cicloActual);
                    } else if (politica != null && politica.usaQuantum() && enEjecucion.getQuantumRestante() <= 0) {
                        cpu.liberarProceso();
                        enEjecucion.setEstado(EstadoProceso.LISTO);
                        colaListos.encolar(enEjecucion);
                        log("Quantum agotado para " + enEjecucion.getNombre() + " -> LISTO");
                    }
                }
            }

            // 4. Actualizar tiempos de espera y bloqueo en todas las colas
            ListaEnlazada<Proceso> listos = colaListos.obtenerListaElementos();
            for (int i = 0; i < listos.getTamano(); i++) {
                listos.obtener(i).incrementarEsperaListo();
            }

            ListaEnlazada<Proceso> bloqueados = colaBloqueados.obtenerListaElementos();
            for (int i = 0; i < bloqueados.getTamano(); i++) {
                bloqueados.obtener(i).incrementarTiempoBloqueado();
            }

            ListaEnlazada<Proceso> bloqSusp = colaBloqSusp.obtenerListaElementos();
            for (int i = 0; i < bloqSusp.getTamano(); i++) {
                bloqSusp.obtener(i).incrementarTiempoBloqueado();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    /**
     * Traduce la operación CRUD del proceso en una solicitud real al Sistema de Archivos y al Disco Simulado.
     */
    private void iniciarOperacionDisco(Proceso p, int cicloActual) {
        CPU cpu = computador.getCpu();
        DiscoSimulado disco = computador.getDisco();
        SistemaArchivos fs = computador.getSistemaArchivos();

        cpu.liberarProceso();
        p.setEstado(EstadoProceso.BLOQUEADO);
        p.setMotivoBloqueo(MotivoBloqueo.ENTRADA_SALIDA);
        computador.getColaBloqueados().encolar(p);

        TipoOperacionIO op = p.getOperacionSolicitada();
        String ruta = p.getRutaArchivo();
        int bloqueObjetivo = 0;
        int tamanoBloques = Math.max(1, p.getTamanoArchivoBloques());

        if (fs != null) {
            EntradaFS existente = fs.buscarPorRuta(ruta);
            if (op == TipoOperacionIO.CREATE) {
                if (ruta.endsWith("/") || p.getTamanoArchivoBloques() == 0) {
                    fs.crearDirectorio(ruta, fs.getUsuarioActivo(), PermisoArchivo.PUBLICO);
                } else {
                    boolean creado = fs.crearArchivo(ruta, tamanoBloques, fs.getUsuarioActivo(), PermisoArchivo.PUBLICO, p.getNombre());
                    if (creado) {
                        EntradaFS nuevo = fs.buscarPorRuta(ruta);
                        if (nuevo != null) bloqueObjetivo = Math.max(0, nuevo.getPrimerBloque());
                    }
                }
            } else if (op == TipoOperacionIO.READ) {
                if (existente != null && !existente.isEsDirectorio()) {
                    bloqueObjetivo = Math.max(0, existente.getPrimerBloque());
                    tamanoBloques = existente.getTamanoBloques();
                }
            } else if (op == TipoOperacionIO.UPDATE) {
                if (existente != null) {
                    bloqueObjetivo = Math.max(0, existente.getPrimerBloque());
                    if (p.getNuevoNombreArchivo() != null && !p.getNuevoNombreArchivo().isEmpty()) {
                        fs.actualizarNombre(ruta, p.getNuevoNombreArchivo());
                    }
                }
            } else if (op == TipoOperacionIO.DELETE) {
                if (existente != null) {
                    bloqueObjetivo = Math.max(0, existente.getPrimerBloque());
                    tamanoBloques = Math.max(1, existente.getTamanoBloques());
                    fs.eliminarEntrada(ruta);
                }
            }
        }

        if (disco != null) {
            disco.encolarSolicitud(new SolicitudIO(p, op, ruta, bloqueObjetivo, tamanoBloques, cicloActual));
            log("Proceso " + p.getNombre() + " bloqueado por E/S (" + op + " -> " + ruta + ")");
        } else {
            // Fallback si no hubiera disco conectado
            p.setEstado(EstadoProceso.LISTO);
            p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
            computador.getColaBloqueados().eliminarElemento(p);
            computador.getColaListos().encolar(p);
        }
    }

    /**
     * Invocado por DiscoSimulado cuando finaliza la solicitud de E/S o reactivación de un proceso.
     */
    public void desbloquearProcesoLocal(Proceso p) {
        if (p == null) return;
        try {
            mutexColas.acquire();
            computador.getColaBloqueados().eliminarElemento(p);
            computador.getColaBloqueadoSuspendidos().eliminarElemento(p);
            computador.getColaListoSuspendidos().eliminarElemento(p);

            p.setEstado(EstadoProceso.LISTO);
            p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
            computador.getColaListos().encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexColas.release();
        }
    }

    public int getTotalSuspensiones() { return totalSuspensiones; }
    public int getTotalReactivaciones() { return totalReactivaciones; }
}