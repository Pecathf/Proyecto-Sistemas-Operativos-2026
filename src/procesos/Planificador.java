package procesos;
/**
 *
 * @author Jabri
 */

import arquitectura.Cluster;
import arquitectura.Computador;
import edd.Cola;
import edd.ListaEnlazada;
import politicas.PoliticaPlanificacion;
import sincronizacion.Buffer;

public class Planificador {

    private Computador computador;
    private Cluster cluster;

    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaTerminados;
    private ListaEnlazada<Proceso> procesosTerminadosEnCiclo;

    private Proceso procesoEnEjecucion;
    private int quantum;
    private int quantumActual;
    private PoliticaPlanificacion politica;

    public Planificador(int quantum, PoliticaPlanificacion politica) {
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.procesosTerminadosEnCiclo = new ListaEnlazada<>();
        this.procesoEnEjecucion = null;
        this.quantum = quantum;
        this.quantumActual = 0;
        this.politica = politica;
    }

    public void setComputador(Computador computador) {
        this.computador = computador;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public synchronized void setPolitica(PoliticaPlanificacion nuevaPolitica) {
        this.politica = nuevaPolitica;
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            procesoEnEjecucion = null;
        }
        if (cluster != null && computador != null) {
            cluster.log(computador.getNombre() + " cambió política de planificación a: " + nuevaPolitica.getTipo());
        }
    }

    public synchronized void admitirProcesoListo(Proceso p) {
        p.setEstado(EstadoProceso.LISTO);
        p.setModo(ModoEjecucion.USUARIO);
        colaListos.encolar(p);
    }

    public synchronized void ejecutarCiclo(int cicloActual) {
        procesosTerminadosEnCiclo = new ListaEnlazada<>();

        // 1. Aumentar tiempo de espera en Listo para métricas
        ListaEnlazada<Proceso> listosSnapshot = colaListos.obtenerListaElementos();
        for (int i = 0; i < listosSnapshot.getTamano(); i++) {
            listosSnapshot.obtener(i).incrementarTiempoEsperaListo();
        }

        // 2. Incrementar tiempo de bloqueo en semáforos para métricas
        ListaEnlazada<Proceso> bloqSnapshot = colaBloqueados.obtenerListaElementos();
        for (int i = 0; i < bloqSnapshot.getTamano(); i++) {
            Proceso pb = bloqSnapshot.obtener(i);
            if (pb.getMotivoBloqueo() == MotivoBloqueo.SEMAFORO_MUTEX ||
                pb.getMotivoBloqueo() == MotivoBloqueo.SEMAFORO_LLENOS ||
                pb.getMotivoBloqueo() == MotivoBloqueo.SEMAFORO_VACIOS) {
                pb.incrementarTiempoBloqueadoSem();
            }
        }

        // 3. Control Universal de Deadlines en TODOS los procesos activos
        revisarDeadlines(cicloActual);

        // 4. Procesar E/S y Latencia de Red en procesos bloqueados
        procesarBloqueadosPorTiempo(cicloActual);

        // 5. Verificar desalojo preventivo (Apropiación polimórfica)
        if (procesoEnEjecucion != null && politica.debeApropiar(procesoEnEjecucion, colaListos)) {
            if (cluster != null) {
                cluster.log(computador.getNombre() + ": Desalojo por política apropiativa del proceso " + procesoEnEjecucion.getNombre());
            }
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        }

        // 6. Selección de proceso a ejecutar si la CPU está libre
        if (procesoEnEjecucion == null && !colaListos.estaVacia()) {
            procesoEnEjecucion = politica.seleccionarSiguienteProceso(colaListos);
            if (procesoEnEjecucion != null) {
                procesoEnEjecucion.setEstado(EstadoProceso.EJECUCION);
                quantumActual = 0;
                if (procesoEnEjecucion.getCicloPrimeraEjecucion() == -1) {
                    procesoEnEjecucion.setCicloPrimeraEjecucion(cicloActual);
                }
                computador.getCpu().cargarProceso(procesoEnEjecucion);
            }
        }

        // 7. Ejecutar ciclo del proceso en CPU
        if (procesoEnEjecucion != null) {
            computador.getCpu().ejecutarCiclo(cicloActual);
            ejecutarInstruccionProceso(cicloActual);
        } else {
            computador.getCpu().ejecutarCiclo(cicloActual);
        }
    }

    private void ejecutarInstruccionProceso(int cicloActual) {
        switch (procesoEnEjecucion.getTipo()) {
            case CPU_BOUND:
                ejecutarCpuBound(cicloActual);
                break;
            case IO_BOUND:
                ejecutarIoBound(cicloActual);
                break;
            case PRODUCTOR:
                ejecutarProductor(cicloActual);
                break;
            case CONSUMIDOR:
                ejecutarConsumidor(cicloActual);
                break;
        }
    }

    private void ejecutarCpuBound(int cicloActual) {
        procesoEnEjecucion.setModo(ModoEjecucion.USUARIO);
        procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);
        quantumActual++;

        if (procesoEnEjecucion.getTiempoRestante() <= 0) {
            finalizarProceso(procesoEnEjecucion, cicloActual, true);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        } else if (politica.usaQuantum() && quantumActual >= quantum) {
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        }
    }

    private void ejecutarIoBound(int cicloActual) {
        procesoEnEjecucion.setModo(ModoEjecucion.USUARIO);
        procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);
        procesoEnEjecucion.setContadorParaIO(procesoEnEjecucion.getContadorParaIO() - 1);
        quantumActual++;

        if (procesoEnEjecucion.getTiempoRestante() <= 0) {
            finalizarProceso(procesoEnEjecucion, cicloActual, true);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        } else if (procesoEnEjecucion.getContadorParaIO() <= 0) {
            // Solicita operación de E/S
            procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
            procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.ENTRADA_SALIDA);
            procesoEnEjecucion.setTiempoRestanteIO(procesoEnEjecucion.getDuracionIO());
            procesoEnEjecucion.setContadorParaIO(procesoEnEjecucion.getCiclosEntreIO());
            colaBloqueados.encolar(procesoEnEjecucion);
            if (cluster != null) {
                cluster.log(procesoEnEjecucion.getNombre() + " bloqueado por operación de E/S (" + procesoEnEjecucion.getDuracionIO() + " ciclos)");
            }
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        } else if (politica.usaQuantum() && quantumActual >= quantum) {
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        }
    }

    private void ejecutarProductor(int cicloActual) {
        Buffer buffer = (cluster != null) ? cluster.buscarBuffer(procesoEnEjecucion.getBufferObjetivo()) : null;
        if (buffer == null) {
            ejecutarCpuBound(cicloActual);
            return;
        }

        boolean esRemoto = !buffer.getComputadorAnfitrion().equalsIgnoreCase(computador.getNombre());
        int fase = procesoEnEjecucion.getFaseSincronizacion();

        switch (fase) {
            case 0: // Produciendo en modo usuario
                procesoEnEjecucion.setModo(ModoEjecucion.USUARIO);
                procesoEnEjecucion.setContadorCiclosOperacion(procesoEnEjecucion.getContadorCiclosOperacion() - 1);
                if (procesoEnEjecucion.getContadorCiclosOperacion() <= 0) {
                    procesoEnEjecucion.setFaseSincronizacion(1);
                }
                quantumActual++;
                verificarQuantum();
                break;

            case 1: // Verificación de latencia de red si es buffer remoto
                if (esRemoto && cluster != null && cluster.getLatenciaRedCiclos() > 0) {
                    procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.LATENCIA_RED);
                    procesoEnEjecucion.setCiclosLatenciaRestantes(cluster.getLatenciaRedCiclos());
                    procesoEnEjecucion.setFaseSincronizacion(2);
                    colaBloqueados.encolar(procesoEnEjecucion);
                    cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " accede a " + buffer.getId() + " remotamente (bloqueado por red " + cluster.getLatenciaRedCiclos() + " ciclos)");
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                } else {
                    procesoEnEjecucion.setFaseSincronizacion(2);
                    ejecutarProductor(cicloActual);
                }
                break;

            case 2: // semWait(vacios) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                boolean pasoVacios = buffer.getVacios().semWait(procesoEnEjecucion);
                if (!pasoVacios) {
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.SEMAFORO_VACIOS);
                    procesoEnEjecucion.setFaseSincronizacion(3); // Al despertar ya tiene el recurso 'vacios' y pasa a mutex
                    colaBloqueados.encolar(procesoEnEjecucion);
                    if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " bloqueado en semáforo vacíos de " + buffer.getId());
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                    return;
                }
                procesoEnEjecucion.setFaseSincronizacion(3);
                quantumActual++;
                verificarQuantum();
                break;

            case 3: // semWait(mutex) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                boolean pasoMutex = buffer.getMutex().semWait(procesoEnEjecucion);
                if (!pasoMutex) {
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.SEMAFORO_MUTEX);
                    procesoEnEjecucion.setFaseSincronizacion(4); // Al despertar ya tiene mutex y pasa a anyadir
                    colaBloqueados.encolar(procesoEnEjecucion);
                    if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " bloqueado en semáforo mutex de " + buffer.getId());
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                    return;
                }
                procesoEnEjecucion.setFaseSincronizacion(4);
                quantumActual++;
                verificarQuantum();
                break;

            case 4: // anyadir() a buffer en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                buffer.insertar("Item_" + procesoEnEjecucion.getId() + "_" + (procesoEnEjecucion.getElementosProcesados() + 1));
                if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " depositó elemento en " + buffer.getId() + " (" + buffer.getElementosActuales() + "/" + buffer.getCapacidad() + ")");
                procesoEnEjecucion.setFaseSincronizacion(5);
                quantumActual++;
                verificarQuantum();
                break;

            case 5: // semSignal(mutex) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                Proceso despiertoMutex = buffer.getMutex().semSignal();
                if (despiertoMutex != null) {
                    despertarProcesoCluster(despiertoMutex);
                    if (cluster != null) cluster.log("Semáforo mutex de " + buffer.getId() + " desbloqueó a " + despiertoMutex.getNombre());
                }
                procesoEnEjecucion.setFaseSincronizacion(6);
                quantumActual++;
                verificarQuantum();
                break;

            case 6: // semSignal(llenos) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                Proceso despiertoLlenos = buffer.getLlenos().semSignal();
                if (despiertoLlenos != null) {
                    despertarProcesoCluster(despiertoLlenos);
                    if (cluster != null) cluster.log("Semáforo llenos de " + buffer.getId() + " desbloqueó a " + despiertoLlenos.getNombre());
                }
                procesoEnEjecucion.incrementarElementosProcesados();
                procesoEnEjecucion.setContadorCiclosOperacion(procesoEnEjecucion.getCiclosPorOperacion());
                procesoEnEjecucion.setFaseSincronizacion(0);
                procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);

                if (procesoEnEjecucion.getElementosProcesados() >= procesoEnEjecucion.getElementosMeta() || procesoEnEjecucion.getTiempoRestante() <= 0) {
                    finalizarProceso(procesoEnEjecucion, cicloActual, true);
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                } else {
                    quantumActual++;
                    verificarQuantum();
                }
                break;
        }
    }

    private void ejecutarConsumidor(int cicloActual) {
        Buffer buffer = (cluster != null) ? cluster.buscarBuffer(procesoEnEjecucion.getBufferObjetivo()) : null;
        if (buffer == null) {
            ejecutarCpuBound(cicloActual);
            return;
        }

        boolean esRemoto = !buffer.getComputadorAnfitrion().equalsIgnoreCase(computador.getNombre());
        int fase = procesoEnEjecucion.getFaseSincronizacion();

        switch (fase) {
            case 0: // Verificación de latencia de red si es remoto
                if (esRemoto && cluster != null && cluster.getLatenciaRedCiclos() > 0) {
                    procesoEnEjecucion.setEstado(EstadoProceso.BLOQUEADO);
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.LATENCIA_RED);
                    procesoEnEjecucion.setCiclosLatenciaRestantes(cluster.getLatenciaRedCiclos());
                    procesoEnEjecucion.setFaseSincronizacion(1);
                    colaBloqueados.encolar(procesoEnEjecucion);
                    cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " accede a " + buffer.getId() + " remotamente (bloqueado por red " + cluster.getLatenciaRedCiclos() + " ciclos)");
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                } else {
                    procesoEnEjecucion.setFaseSincronizacion(1);
                    ejecutarConsumidor(cicloActual);
                }
                break;

            case 1: // semWait(llenos) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                boolean pasoLlenos = buffer.getLlenos().semWait(procesoEnEjecucion);
                if (!pasoLlenos) {
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.SEMAFORO_LLENOS);
                    procesoEnEjecucion.setFaseSincronizacion(2); // Al despertar ya tiene llenos y pasa a mutex
                    colaBloqueados.encolar(procesoEnEjecucion);
                    if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " bloqueado en semáforo llenos de " + buffer.getId());
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                    return;
                }
                procesoEnEjecucion.setFaseSincronizacion(2);
                quantumActual++;
                verificarQuantum();
                break;

            case 2: // semWait(mutex) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                boolean pasoMutex = buffer.getMutex().semWait(procesoEnEjecucion);
                if (!pasoMutex) {
                    procesoEnEjecucion.setMotivoBloqueo(MotivoBloqueo.SEMAFORO_MUTEX);
                    procesoEnEjecucion.setFaseSincronizacion(3); // Al despertar ya tiene mutex y pasa a extraer
                    colaBloqueados.encolar(procesoEnEjecucion);
                    if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " bloqueado en semáforo mutex de " + buffer.getId());
                    computador.getCpu().liberarProceso();
                    procesoEnEjecucion = null;
                    return;
                }
                procesoEnEjecucion.setFaseSincronizacion(3);
                quantumActual++;
                verificarQuantum();
                break;

            case 3: // extraer() de buffer en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                String item = buffer.extraer();
                if (cluster != null) cluster.log("Proceso " + procesoEnEjecucion.getNombre() + " extrajo " + item + " de " + buffer.getId());
                procesoEnEjecucion.setFaseSincronizacion(4);
                quantumActual++;
                verificarQuantum();
                break;

            case 4: // semSignal(mutex) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                Proceso despiertoMutex = buffer.getMutex().semSignal();
                if (despiertoMutex != null) {
                    despertarProcesoCluster(despiertoMutex);
                    if (cluster != null) cluster.log("Semáforo mutex de " + buffer.getId() + " desbloqueó a " + despiertoMutex.getNombre());
                }
                procesoEnEjecucion.setFaseSincronizacion(5);
                quantumActual++;
                verificarQuantum();
                break;

            case 5: // semSignal(vacios) en modo Sistema Operativo
                procesoEnEjecucion.setModo(ModoEjecucion.SISTEMA_OPERATIVO);
                Proceso despiertoVacios = buffer.getVacios().semSignal();
                if (despiertoVacios != null) {
                    despertarProcesoCluster(despiertoVacios);
                    if (cluster != null) cluster.log("Semáforo vacíos de " + buffer.getId() + " desbloqueó a " + despiertoVacios.getNombre());
                }
                procesoEnEjecucion.setFaseSincronizacion(6);
                quantumActual++;
                verificarQuantum();
                break;

            case 6: // Consumiendo en modo usuario
                procesoEnEjecucion.setModo(ModoEjecucion.USUARIO);
                procesoEnEjecucion.setContadorCiclosOperacion(procesoEnEjecucion.getContadorCiclosOperacion() - 1);
                if (procesoEnEjecucion.getContadorCiclosOperacion() <= 0) {
                    procesoEnEjecucion.incrementarElementosProcesados();
                    procesoEnEjecucion.setContadorCiclosOperacion(procesoEnEjecucion.getCiclosPorOperacion());
                    procesoEnEjecucion.setFaseSincronizacion(0);
                    procesoEnEjecucion.setTiempoRestante(procesoEnEjecucion.getTiempoRestante() - 1);

                    if (procesoEnEjecucion.getElementosProcesados() >= procesoEnEjecucion.getElementosMeta() || procesoEnEjecucion.getTiempoRestante() <= 0) {
                        finalizarProceso(procesoEnEjecucion, cicloActual, true);
                        computador.getCpu().liberarProceso();
                        procesoEnEjecucion = null;
                        return;
                    }
                }
                quantumActual++;
                verificarQuantum();
                break;
        }
    }

    private void verificarQuantum() {
        if (procesoEnEjecucion != null && politica.usaQuantum() && quantumActual >= quantum) {
            procesoEnEjecucion.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(procesoEnEjecucion);
            computador.getCpu().liberarProceso();
            procesoEnEjecucion = null;
        }
    }

    public synchronized void despertarProcesoCluster(Proceso p) {
        if (p == null) return;
        p.setEstado(EstadoProceso.LISTO);
        p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);

        // Si es de esta misma máquina:
        if (computador != null && p.getComputadorAsignado().equalsIgnoreCase(computador.getNombre())) {
            desbloquearProcesoLocal(p);
        } else if (cluster != null) {
            // Es de otra máquina del clúster (comunicación distribuida)
            Computador otro = cluster.buscarComputadorPorNombre(p.getComputadorAsignado());
            if (otro != null) {
                otro.getPlanificador().desbloquearProcesoLocal(p);
            }
        }
    }

    public synchronized void desbloquearProcesoLocal(Proceso p) {
        // Removerlo de la cola de bloqueados de esta máquina y pasarlo a listos
        Cola<Proceso> aux = new Cola<>();
        while (!colaBloqueados.estaVacia()) {
            Proceso actual = colaBloqueados.desencolar();
            if (actual.getId() != p.getId()) {
                aux.encolar(actual);
            }
        }
        while (!aux.estaVacia()) {
            colaBloqueados.encolar(aux.desencolar());
        }

        p.setEstado(EstadoProceso.LISTO);
        colaListos.encolar(p);
    }

    private void procesarBloqueadosPorTiempo(int cicloActual) {
        Cola<Proceso> aux = new Cola<>();
        while (!colaBloqueados.estaVacia()) {
            Proceso p = colaBloqueados.desencolar();

            if (p.getMotivoBloqueo() == MotivoBloqueo.ENTRADA_SALIDA) {
                p.setTiempoRestanteIO(p.getTiempoRestanteIO() - 1);
                if (p.getTiempoRestanteIO() <= 0) {
                    p.setEstado(EstadoProceso.LISTO);
                    p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                    colaListos.encolar(p);
                    if (cluster != null) cluster.log("Proceso " + p.getNombre() + " terminó su operación de E/S y pasa a Listos");
                } else {
                    aux.encolar(p);
                }
            } else if (p.getMotivoBloqueo() == MotivoBloqueo.LATENCIA_RED) {
                p.setCiclosLatenciaRestantes(p.getCiclosLatenciaRestantes() - 1);
                if (p.getCiclosLatenciaRestantes() <= 0) {
                    p.setEstado(EstadoProceso.LISTO);
                    p.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
                    colaListos.encolar(p);
                    if (cluster != null) cluster.log("Proceso " + p.getNombre() + " completó transferencia de red y pasa a Listos");
                } else {
                    aux.encolar(p);
                }
            } else {
                aux.encolar(p); // Permanece bloqueado en semáforo
            }
        }

        while (!aux.estaVacia()) {
            colaBloqueados.encolar(aux.desencolar());
        }
    }

    private void revisarDeadlines(int cicloActual) {
        // 1. Proceso en CPU
        if (procesoEnEjecucion != null) {
            procesoEnEjecucion.setDeadline(procesoEnEjecucion.getDeadline() - 1);
            if (procesoEnEjecucion.getDeadline() <= 0) {
                if (cluster != null) cluster.log("DEADLINE EXPIRADO: Proceso " + procesoEnEjecucion.getNombre() + " terminado forzosamente en " + computador.getNombre());
                finalizarProceso(procesoEnEjecucion, cicloActual, false);
                computador.getCpu().liberarProceso();
                procesoEnEjecucion = null;
            }
        }

        // 2. Procesos en Cola de Listos
        Cola<Proceso> auxListos = new Cola<>();
        while (!colaListos.estaVacia()) {
            Proceso p = colaListos.desencolar();
            p.setDeadline(p.getDeadline() - 1);
            if (p.getDeadline() <= 0) {
                if (cluster != null) cluster.log("DEADLINE EXPIRADO: Proceso " + p.getNombre() + " en cola de listos terminado forzosamente");
                finalizarProceso(p, cicloActual, false);
            } else {
                auxListos.encolar(p);
            }
        }
        while (!auxListos.estaVacia()) {
            colaListos.encolar(auxListos.desencolar());
        }

        // 3. Procesos en Cola de Bloqueados
        Cola<Proceso> auxBloq = new Cola<>();
        while (!colaBloqueados.estaVacia()) {
            Proceso p = colaBloqueados.desencolar();
            p.setDeadline(p.getDeadline() - 1);
            if (p.getDeadline() <= 0) {
                if (cluster != null) cluster.log("DEADLINE EXPIRADO: Proceso " + p.getNombre() + " en estado bloqueado terminado forzosamente");
                finalizarProceso(p, cicloActual, false);
            } else {
                auxBloq.encolar(p);
            }
        }
        while (!auxBloq.estaVacia()) {
            colaBloqueados.encolar(auxBloq.desencolar());
        }
    }

    private void finalizarProceso(Proceso p, int cicloActual, boolean cumplioDeadline) {
        p.setEstado(EstadoProceso.TERMINADO);
        p.setCicloFinalizacion(cicloActual);
        p.setCumplioDeadline(cumplioDeadline);
        colaTerminados.encolar(p);
        procesosTerminadosEnCiclo.agregar(p);
    }

    public synchronized ListaEnlazada<Proceso> extraerProcesosTerminadosEnCiclo() {
        ListaEnlazada<Proceso> copia = procesosTerminadosEnCiclo;
        procesosTerminadosEnCiclo = new ListaEnlazada<>();
        return copia;
    }

    // --- Getters y Setters ---
    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public PoliticaPlanificacion getPolitica() { return politica; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Proceso getProcesoEnEjecucion() { return procesoEnEjecucion; }
}