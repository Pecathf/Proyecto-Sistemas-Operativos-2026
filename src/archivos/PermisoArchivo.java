/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package archivos;

/**
 *
 * @author Jabri
 */

public enum PermisoArchivo {
    PUBLICO("public"),
    PRIVADO("private");

    private final String valorJson;

    PermisoArchivo(String valorJson) {
        this.valorJson = valorJson;
    }

    public String getValorJson() {
        return valorJson;
    }

    public static PermisoArchivo desdeTexto(String texto) {
        if (texto != null && (texto.equalsIgnoreCase("private") || texto.equalsIgnoreCase("privado"))) {
            return PRIVADO;
        }
        return PUBLICO;
    }

    @Override
    public String toString() {
        return valorJson;
    }
}