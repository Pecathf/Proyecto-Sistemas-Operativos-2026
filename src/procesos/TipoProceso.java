package procesos;

/**
 *
 * @author Jabri
 */
public enum TipoProceso {
    CPU_BOUND("CPU-Bound (Cómputo)"),
    IO_BOUND("I/O-Bound (Archivos / Disco)"),
    PRODUCTOR("Productor"),
    CONSUMIDOR("Consumidor");

    private final String descripcion;

    TipoProceso(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getNombreLegible() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
