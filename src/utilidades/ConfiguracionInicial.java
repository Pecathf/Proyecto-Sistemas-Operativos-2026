package utilidades;

/**
 *
 * @author PECA
 */
import edd.ListaEnlazada;
import politicas.TipoPolitica;
import procesos.Proceso;
import procesos.TipoProceso;
import sincronizacion.Buffer;

public class ConfiguracionInicial {
    public int duracionCicloMs;
    public int latenciaRedCiclos;

    // Campos auxiliares usados por LectorCSV / GestorArchivos para carga simple
    public int numComputadores;
    public int ramPorComputador;
    public int quantumPorDefecto;

    public ListaEnlazada<ConfigComputador> computadores;
    public ListaEnlazada<Buffer> buffersIniciales;
    public ListaEnlazada<Proceso> procesosIniciales;

    public ConfiguracionInicial() {
        this.duracionCicloMs = 1000;
        this.latenciaRedCiclos = 2;
        this.numComputadores = 2;
        this.ramPorComputador = 8192;
        this.quantumPorDefecto = 3;
        this.computadores = new ListaEnlazada<>();
        this.buffersIniciales = new ListaEnlazada<>();
        this.procesosIniciales = new ListaEnlazada<>();
    }

    public static ConfiguracionInicial crearConfiguracionPorDefecto() {
        ConfiguracionInicial config = new ConfiguracionInicial();
        config.duracionCicloMs = 1000;
        config.latenciaRedCiclos = 2;

        config.computadores.agregar(new ConfigComputador("Computador 1", 8192, TipoPolitica.ROUND_ROBIN, 3));
        config.computadores.agregar(new ConfigComputador("Computador 2", 16384, TipoPolitica.ROUND_ROBIN, 3));

        Buffer b1 = new Buffer("Buffer_1", 5, "Computador 1", 512);
        config.buffersIniciales.agregar(b1);

        Proceso p1 = new Proceso(1, "Proceso_CPU_1", 8, 1, 1024, 16, TipoProceso.CPU_BOUND);
        p1.setComputadorAsignado("Computador 1");

        Proceso p2 = new Proceso(2, "Productor_1", 10, 2, 512, 25, TipoProceso.PRODUCTOR);
        p2.setBufferObjetivo("Buffer_1");
        p2.setCiclosPorOperacion(2);
        p2.setElementosMeta(4);
        p2.setComputadorAsignado("Computador 1");

        Proceso p3 = new Proceso(3, "Consumidor_Remoto_2", 12, 2, 512, 30, TipoProceso.CONSUMIDOR);
        p3.setBufferObjetivo("Buffer_1");
        p3.setCiclosPorOperacion(2);
        p3.setElementosMeta(4);
        p3.setComputadorAsignado("Computador 2");

        config.procesosIniciales.agregar(p1);
        config.procesosIniciales.agregar(p2);
        config.procesosIniciales.agregar(p3);

        return config;
    }
}
