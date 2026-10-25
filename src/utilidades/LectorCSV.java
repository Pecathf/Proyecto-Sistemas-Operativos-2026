/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilidades;

/**
 *
 * @author PECA
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LectorCSV {
    
    // Carga un archivo "config.csv" donde la primera línea es: duracionCicloMs,numComputadores,ramPorComputador,quantumPorDefecto,latenciaRedCiclos
    // Por ejemplo la linea: 1000,3,8192,4,2
    public static ConfiguracionInicial cargarConfiguracion(String rutaArchivo) {
        ConfiguracionInicial config = new ConfiguracionInicial();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea = br.readLine(); // Leer encabezados y saltar
            linea = br.readLine();        // Leer valores reales
            
            if (linea != null) {
                String[] datos = linea.split(",");
                if (datos.length >= 5) {
                    config.duracionCicloMs = Integer.parseInt(datos[0].trim());
                    config.numComputadores = Integer.parseInt(datos[1].trim());
                    config.ramPorComputador = Integer.parseInt(datos[2].trim());
                    config.quantumPorDefecto = Integer.parseInt(datos[3].trim());
                    config.latenciaRedCiclos = Integer.parseInt(datos[4].trim());
                }
            }
            System.out.println("Configuración cargada exitosamente desde CSV.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error cargando CSV. Usando configuración por defecto. " + e.getMessage());
        }
        
        return config;
    }
}