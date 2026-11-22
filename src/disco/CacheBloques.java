/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package disco;

/**
 *
 * @author samue
 */
import edd.ListaEnlazada;

/**
 * Caché de Bloques en Memoria Principal (Buffer Cache) para ÁvilaOS 2.
 * Implementa política de reemplazo LRU (Least Recently Used) sobre estructuras de datos propias.
 * Cumple con el criterio de "Extras o consideraciones (caché de bloques u otros)" de la rúbrica.
 *
 * @author Jabri & Samuel
 */
public class CacheBloques {

    public static class EntradaCache {
        private int numeroBloque;
        private String rutaArchivo;
        private int ultimoAccesoCiclo;

        public EntradaCache(int numeroBloque, String rutaArchivo, int ultimoAccesoCiclo) {
            this.numeroBloque = numeroBloque;
            this.rutaArchivo = (rutaArchivo != null) ? rutaArchivo : "";
            this.ultimoAccesoCiclo = ultimoAccesoCiclo;
        }

        public int getNumeroBloque() { return numeroBloque; }
        public String getRutaArchivo() { return rutaArchivo; }
        public int getUltimoAccesoCiclo() { return ultimoAccesoCiclo; }
        public void setUltimoAccesoCiclo(int ciclo) { this.ultimoAccesoCiclo = ciclo; }
    }

    private int capacidad;
    private ListaEnlazada<EntradaCache> entradas;
    private int totalHits;
    private int totalMisses;

    public CacheBloques(int capacidad) {
        this.capacidad = Math.max(4, capacidad);
        this.entradas = new ListaEnlazada<>();
        this.totalHits = 0;
        this.totalMisses = 0;
    }

    public CacheBloques() {
        this(16); // 16 bloques en RAM por defecto
    }

    /**
     * Consulta si un bloque reside actualmente en la caché de RAM.
     * Si reside (CACHE HIT), actualiza el timestamp del ciclo para LRU y suma a hits.
     * Si no reside (CACHE MISS), suma a misses.
     */
    public synchronized boolean estaEnCache(int numeroBloque, int cicloActual) {
        for (int i = 0; i < entradas.getTamano(); i++) {
            EntradaCache e = entradas.obtener(i);
            if (e != null && e.getNumeroBloque() == numeroBloque) {
                e.setUltimoAccesoCiclo(cicloActual);
                totalHits++;
                return true;
            }
        }
        totalMisses++;
        return false;
    }

    /**
     * Guarda un bloque en la caché de RAM tras haber sido leído o escrito en el disco físico.
     * Si la caché alcanza su capacidad máxima, desaloja la entrada Menos Recientemente Usada (LRU).
     */
    public synchronized void guardarBloque(int numeroBloque, String rutaArchivo, int cicloActual) {
        // Si ya está presente, solo actualizar acceso
        for (int i = 0; i < entradas.getTamano(); i++) {
            EntradaCache e = entradas.obtener(i);
            if (e != null && e.getNumeroBloque() == numeroBloque) {
                e.setUltimoAccesoCiclo(cicloActual);
                return;
            }
        }

        // Si se supera la capacidad, desalojar el bloque LRU (menor ciclo de acceso)
        if (entradas.getTamano() >= capacidad) {
            EntradaCache victima = null;
            int menorCiclo = Integer.MAX_VALUE;
            for (int i = 0; i < entradas.getTamano(); i++) {
                EntradaCache e = entradas.obtener(i);
                if (e != null && e.getUltimoAccesoCiclo() < menorCiclo) {
                    menorCiclo = e.getUltimoAccesoCiclo();
                    victima = e;
                }
            }
            if (victima != null) {
                entradas.eliminar(victima);
            }
        }

        entradas.agregar(new EntradaCache(numeroBloque, rutaArchivo, cicloActual));
    }

    /**
     * Invalida un bloque si es modificado o eliminado del disco.
     */
    public synchronized void invalidarBloque(int numeroBloque) {
        for (int i = 0; i < entradas.getTamano(); i++) {
            EntradaCache e = entradas.obtener(i);
            if (e != null && e.getNumeroBloque() == numeroBloque) {
                entradas.eliminar(e);
                return;
            }
        }
    }

    /**
     * Invalida todos los bloques cacheados que pertenezcan a un archivo eliminado.
     */
    public synchronized void invalidarArchivo(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isEmpty()) return;
        ListaEnlazada<EntradaCache> aEliminar = new ListaEnlazada<>();
        for (int i = 0; i < entradas.getTamano(); i++) {
            EntradaCache e = entradas.obtener(i);
            if (e != null && e.getRutaArchivo().equalsIgnoreCase(rutaArchivo)) {
                aEliminar.agregar(e);
            }
        }
        for (int i = 0; i < aEliminar.getTamano(); i++) {
            entradas.eliminar(aEliminar.obtener(i));
        }
    }

    /**
     * Consulta pasiva sin registrar Hit o Miss (usado por la interfaz y Tooltips).
     */
    public synchronized boolean contieneBloque(int numeroBloque) {
        for (int i = 0; i < entradas.getTamano(); i++) {
            EntradaCache e = entradas.obtener(i);
            if (e != null && e.getNumeroBloque() == numeroBloque) {
                return true;
            }
        }
        return false;
    }

    public int getCapacidad() { return capacidad; }
    public synchronized int getOcupacion() { return entradas.getTamano(); }
    public synchronized int getTotalHits() { return totalHits; }
    public synchronized int getTotalMisses() { return totalMisses; }

    public synchronized double getTasaAcierto() {
        int total = totalHits + totalMisses;
        return (total == 0) ? 0.0 : ((double) totalHits * 100.0 / total);
    }

    public synchronized void reiniciar() {
        while (!entradas.estaVacia()) {
            entradas.eliminarCabeza();
        }
        totalHits = 0;
        totalMisses = 0;
    }
}