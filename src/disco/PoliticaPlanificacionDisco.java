/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author Jabri
 */

import edd.Cola;

public interface PoliticaPlanificacionDisco {
    /**
     * Selecciona y extrae de la cola la siguiente solicitud a atender.
     * Puede actualizar la dirección del cabezal si aplica (SCAN / C-SCAN).
     */
    SolicitudIO seleccionarSiguiente(Cola<SolicitudIO> colaSolicitudes, int cabezalActual, boolean[] subiendo, int totalBloques);

    TipoPoliticaDisco getTipo();
}