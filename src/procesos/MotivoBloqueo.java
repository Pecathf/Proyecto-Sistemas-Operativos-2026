package procesos;

/**
 *
 * @author Jabri
 */
public enum MotivoBloqueo {
    NINGUNO("Ninguno"),
    ENTRADA_SALIDA("Operación de E/S en Disco"),
    SUSPENSION_SWAP("Suspendido en Área de Swap"),
    REACTIVACION_SWAP("Reactivando desde Swap a RAM"),
    SEMAFORO_MUTEX("Semáforo Mutex"),
    SEMAFORO_LLENOS("Semáforo Llenos"),
    SEMAFORO_VACIOS("Semáforo Vacíos"),
    LATENCIA_RED("Latencia de Red");

    private final String descripcion;

    MotivoBloqueo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
