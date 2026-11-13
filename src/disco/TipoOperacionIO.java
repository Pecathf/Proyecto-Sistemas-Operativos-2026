/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */

public enum TipoOperacionIO {
    CREATE("Crear Archivo"),
    READ("Leer Archivo"),
    UPDATE("Actualizar Archivo"),
    DELETE("Eliminar Archivo"),
    SUSPEND("Suspender a Swap"),
    RESUME("Reactivar desde Swap");

    private final String descripcion;

    TipoOperacionIO(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return name();
    }
}