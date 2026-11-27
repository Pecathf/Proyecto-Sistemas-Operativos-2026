/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package archivos;

/**
 *
 * @author Jabri
 */

import disco.DiscoSimulado;
import edd.ListaEnlazada;
import utilidades.RegistroEventos;

import java.awt.Color;
import java.util.concurrent.Semaphore;

public class SistemaArchivos {

    public static final int MAX_USUARIOS = 5;

    private EntradaFS raiz;
    private DiscoSimulado disco;
    private ModoUsuario modoActual;
    private String usuarioActivo;
    private RegistroEventos logger;
    private Semaphore mutexFS;
    private int indiceColor;

    private static final Color[] PALETA_ARCHIVOS = new Color[]{
        new Color(41, 121, 255),   // Azul
        new Color(0, 200, 115),    // Verde
        new Color(255, 145, 0),    // Naranja
        new Color(0, 229, 255),    // Cyan
        new Color(255, 214, 0),    // Amarillo
        new Color(255, 82, 82),    // Coral
        new Color(105, 240, 174),  // Menta
        new Color(68, 138, 255)    // Celeste
    };

    public SistemaArchivos(DiscoSimulado disco) {
        this.disco = disco;
        this.raiz = new EntradaFS("/", "/", true, "admin", PermisoArchivo.PUBLICO, 0, -1, "Sistema", null);
        this.modoActual = ModoUsuario.ADMINISTRADOR;
        this.usuarioActivo = "user1";
        this.mutexFS = new Semaphore(1);
        this.indiceColor = 0;
    }

    public void setLogger(RegistroEventos logger) {
        this.logger = logger;
    }

    private void log(String mensaje) {
        if (logger != null) {
            logger.registrar(mensaje);
        }
    }

    public Color generarSiguienteColor() {
        Color c = PALETA_ARCHIVOS[indiceColor % PALETA_ARCHIVOS.length];
        indiceColor++;
        return c;
    }

    // =========================================================================
    // VALIDACIÓN DE PERMISOS (MODO ADMINISTRADOR VS MODO USUARIO)
    // =========================================================================

    public String validarPermisoCrear(String rutaDirectorioPadre, boolean esDirectorio) {
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return null; // Permitido
        }
        if (esDirectorio) {
            return "Operación denegada: En modo Usuario no se permite crear directorios.";
        }
        EntradaFS padre = buscarPorRuta(rutaDirectorioPadre);
        if (padre == null || !padre.isEsDirectorio()) {
            return "Ruta de directorio destino inválida: " + rutaDirectorioPadre;
        }
        boolean esPropio = padre.getDueno().equalsIgnoreCase(usuarioActivo);
        boolean esZonaPermitida = rutaDirectorioPadre.startsWith("/usuarios")
                || rutaDirectorioPadre.startsWith("/tmp")
                || rutaDirectorioPadre.startsWith("/home/" + usuarioActivo);

        if (!esPropio && !esZonaPermitida) {
            return "Operación denegada: En modo Usuario solo puede crear archivos en sus propios directorios (dueño: " 
                    + padre.getDueno() + ", usuario activo: " + usuarioActivo + ").";
        }
        return null;
    }

    public String validarPermisoLeer(String ruta) {
        EntradaFS entrada = buscarPorRuta(ruta);
        if (entrada == null) {
            return "El archivo o directorio no existe: " + ruta;
        }
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (entrada.getPermiso() == PermisoArchivo.PUBLICO || entrada.getDueno().equalsIgnoreCase(usuarioActivo)) {
            return null;
        }
        return "Acceso denegado: El archivo '" + entrada.getNombre() + "' es privado y pertenece a '" + entrada.getDueno() + "'.";
    }

    public String validarPermisoActualizar(String ruta) {
        EntradaFS entrada = buscarPorRuta(ruta);
        if (entrada == null) {
            return "La ruta no existe: " + ruta;
        }
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (entrada.getDueno().equalsIgnoreCase(usuarioActivo)) {
            return null; // El usuario puede renombrar o modificar sus propios archivos
        }
        return "Operación denegada: En modo Usuario solo puede modificar elementos de su propiedad (dueño: " 
                + entrada.getDueno() + ", usuario activo: " + usuarioActivo + ").";
    }

    public String validarPermisoEliminar(String ruta) {
        if ("/".equals(ruta)) {
            return "No se puede eliminar el directorio raíz '/'.";
        }
        EntradaFS entrada = buscarPorRuta(ruta);
        if (entrada == null) {
            return "La ruta no existe: " + ruta;
        }
        if (modoActual == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (entrada.getDueno().equalsIgnoreCase(usuarioActivo)) {
            return null; // El usuario puede eliminar sus propios archivos
        }
        return "Operación denegada: En modo Usuario solo puede eliminar elementos de su propiedad (dueño: " 
                + entrada.getDueno() + ", usuario activo: " + usuarioActivo + ").";
    }

    // =========================================================================
    // OPERACIONES CRUD SOBRE EL ÁRBOL Y EL DISCO
    // =========================================================================

    public boolean crearDirectorio(String rutaCompleta, String dueno, PermisoArchivo permiso) {
        try {
            mutexFS.acquire();
            String rutaLimpia = normalizarRuta(rutaCompleta);
            if ("/".equals(rutaLimpia) || buscarPorRutaSinLock(rutaLimpia) != null) {
                return false;
            }
            String rutaPadre = obtenerRutaPadre(rutaLimpia);
            String nombre = obtenerNombreDesdeRuta(rutaLimpia);

            EntradaFS padre = buscarPorRutaSinLock(rutaPadre);
            if (padre == null) {
                // Crear directorios intermedios si no existen
                crearDirectorioSinLock(rutaPadre, dueno, permiso);
                padre = buscarPorRutaSinLock(rutaPadre);
            }
            if (padre == null || !padre.isEsDirectorio()) {
                return false;
            }

            EntradaFS nuevoDir = new EntradaFS(nombre, rutaLimpia, true, dueno, permiso, 0, -1, "Sistema", null);
            padre.agregarHijo(nuevoDir);
            log("Directorio creado: " + rutaLimpia + " (Dueño: " + dueno + ")");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            mutexFS.release();
        }
    }

    private void crearDirectorioSinLock(String rutaCompleta, String dueno, PermisoArchivo permiso) {
        String rutaLimpia = normalizarRuta(rutaCompleta);
        if ("/".equals(rutaLimpia) || buscarPorRutaSinLock(rutaLimpia) != null) return;
        String rutaPadre = obtenerRutaPadre(rutaLimpia);
        String nombre = obtenerNombreDesdeRuta(rutaLimpia);
        EntradaFS padre = buscarPorRutaSinLock(rutaPadre);
        if (padre == null) {
            crearDirectorioSinLock(rutaPadre, dueno, permiso);
            padre = buscarPorRutaSinLock(rutaPadre);
        }
        if (padre != null && padre.isEsDirectorio()) {
            padre.agregarHijo(new EntradaFS(nombre, rutaLimpia, true, dueno, permiso, 0, -1, "Sistema", null));
        }
    }

    /**
     * Crea un archivo asignando bloques encadenados en el disco.
     */
    public boolean crearArchivo(String rutaCompleta, int tamanoBloques, String dueno, PermisoArchivo permiso, String procesoCreador) {
        try {
            mutexFS.acquire();
            String rutaLimpia = normalizarRuta(rutaCompleta);
            if (buscarPorRutaSinLock(rutaLimpia) != null) {
                log("Error al crear archivo: ya existe " + rutaLimpia);
                return false;
            }

            String rutaPadre = obtenerRutaPadre(rutaLimpia);
            String nombre = obtenerNombreDesdeRuta(rutaLimpia);
            EntradaFS padre = buscarPorRutaSinLock(rutaPadre);
            if (padre == null || !padre.isEsDirectorio()) {
                log("Error al crear archivo: directorio padre inexistente " + rutaPadre);
                return false;
            }

            Color colorArchivo = generarSiguienteColor();
            int primerBloque = disco.asignarCadenaBloques(rutaLimpia, tamanoBloques, colorArchivo);
            if (primerBloque == -1) {
                log("Error al crear archivo " + rutaLimpia + ": espacio insuficiente en disco (" + tamanoBloques + " bloques requeridos).");
                return false;
            }

            EntradaFS nuevoArchivo = new EntradaFS(nombre, rutaLimpia, false, dueno, permiso,
                    tamanoBloques, primerBloque, procesoCreador, colorArchivo);
            padre.agregarHijo(nuevoArchivo);
            log("Archivo creado: " + rutaLimpia + " [" + tamanoBloques + " blq, inicio: " + primerBloque + ", creador: " + procesoCreador + "]");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            mutexFS.release();
        }
    }

    /**
     * Carga un archivo desde el JSON inicial respetando su bloque inicial.
     */
    public boolean cargarArchivoDesdeJSON(String rutaCompleta, int startBlock, int tamanoBloques, String dueno, PermisoArchivo permiso) {
        try {
            mutexFS.acquire();
            String rutaLimpia = normalizarRuta(rutaCompleta);
            String rutaPadre = obtenerRutaPadre(rutaLimpia);
            String nombre = obtenerNombreDesdeRuta(rutaLimpia);

            if (buscarPorRutaSinLock(rutaPadre) == null) {
                crearDirectorioSinLock(rutaPadre, "admin", PermisoArchivo.PUBLICO);
            }
            EntradaFS padre = buscarPorRutaSinLock(rutaPadre);
            if (padre == null || !padre.isEsDirectorio()) return false;

            Color colorArchivo = generarSiguienteColor();
            boolean asignado = disco.asignarCadenaDesdeBloque(rutaLimpia, startBlock, tamanoBloques, colorArchivo);
            if (!asignado) {
                startBlock = disco.asignarCadenaBloques(rutaLimpia, tamanoBloques, colorArchivo);
                if (startBlock == -1) return false;
            }

            EntradaFS archivo = new EntradaFS(nombre, rutaLimpia, false, dueno, permiso,
                    tamanoBloques, startBlock, "JSON Inicial", colorArchivo);
            padre.agregarHijo(archivo);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            mutexFS.release();
        }
    }

    public boolean actualizarNombre(String rutaActual, String nuevoNombre) {
        try {
            mutexFS.acquire();
            String rutaLimpia = normalizarRuta(rutaActual);
            if ("/".equals(rutaLimpia) || nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                return false;
            }
            EntradaFS entrada = buscarPorRutaSinLock(rutaLimpia);
            if (entrada == null) return false;

            String rutaPadre = obtenerRutaPadre(rutaLimpia);
            String nuevaRuta = ("/".equals(rutaPadre)) ? "/" + nuevoNombre.trim() : rutaPadre + "/" + nuevoNombre.trim();

            if (buscarPorRutaSinLock(nuevaRuta) != null) {
                return false; // Ya existe otro elemento con ese nombre
            }

            entrada.setNombre(nuevoNombre.trim());
            actualizarRutasRecursivo(entrada, nuevaRuta);
            log("Renombrado: " + rutaLimpia + " -> " + nuevaRuta);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            mutexFS.release();
        }
    }

    private void actualizarRutasRecursivo(EntradaFS nodo, String nuevaRuta) {
        nodo.setRutaCompleta(nuevaRuta);
        if (nodo.isEsDirectorio() && nodo.getHijos() != null) {
            for (int i = 0; i < nodo.getHijos().getTamano(); i++) {
                EntradaFS hijo = nodo.getHijos().obtener(i);
                actualizarRutasRecursivo(hijo, nuevaRuta + "/" + hijo.getNombre());
            }
        }
    }

    public boolean eliminarEntrada(String rutaCompleta) {
        try {
            mutexFS.acquire();
            String rutaLimpia = normalizarRuta(rutaCompleta);
            if ("/".equals(rutaLimpia)) return false;

            EntradaFS objetivo = buscarPorRutaSinLock(rutaLimpia);
            EntradaFS padre = buscarPorRutaSinLock(obtenerRutaPadre(rutaLimpia));
            if (objetivo == null || padre == null) return false;

            liberarRecursosRecursivo(objetivo);
            padre.eliminarHijo(objetivo);
            log("Eliminado del sistema de archivos: " + rutaLimpia);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            mutexFS.release();
        }
    }

    private void liberarRecursosRecursivo(EntradaFS nodo) {
        if (nodo.isEsDirectorio() && nodo.getHijos() != null) {
            for (int i = 0; i < nodo.getHijos().getTamano(); i++) {
                liberarRecursosRecursivo(nodo.getHijos().obtener(i));
            }
        } else if (!nodo.isEsDirectorio() && nodo.getPrimerBloque() >= 0) {
            disco.liberarCadenaBloques(nodo.getPrimerBloque());
        }
    }

    // =========================================================================
    // BÚSQUEDAS Y RECORRIDOS
    // =========================================================================

    public EntradaFS buscarPorRuta(String ruta) {
        try {
            mutexFS.acquire();
            return buscarPorRutaSinLock(normalizarRuta(ruta));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            mutexFS.release();
        }
    }

    private EntradaFS buscarPorRutaSinLock(String ruta) {
        if ("/".equals(ruta)) return raiz;
        return buscarRecursivo(raiz, ruta);
    }

    private EntradaFS buscarRecursivo(EntradaFS actual, String rutaObjetivo) {
        if (actual == null) return null;
        if (actual.getRutaCompleta().equals(rutaObjetivo)) return actual;

        if (actual.isEsDirectorio() && actual.getHijos() != null) {
            for (int i = 0; i < actual.getHijos().getTamano(); i++) {
                EntradaFS encontrado = buscarRecursivo(actual.getHijos().obtener(i), rutaObjetivo);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    /**
     * Devuelve todos los archivos existentes para poblar la JTable de asignación.
     */
    public ListaEnlazada<EntradaFS> obtenerTodosLosArchivos() {
        ListaEnlazada<EntradaFS> lista = new ListaEnlazada<>();
        try {
            mutexFS.acquire();
            recolectarArchivos(raiz, lista);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexFS.release();
        }
        return lista;
    }

    private void recolectarArchivos(EntradaFS nodo, ListaEnlazada<EntradaFS> lista) {
        if (nodo == null) return;
        if (!nodo.isEsDirectorio()) {
            lista.agregar(nodo);
        } else if (nodo.getHijos() != null) {
            for (int i = 0; i < nodo.getHijos().getTamano(); i++) {
                recolectarArchivos(nodo.getHijos().obtener(i), lista);
            }
        }
    }

    /**
     * Devuelve todas las entradas (directorios y archivos) para guardar el JSON.
     */
    public ListaEnlazada<EntradaFS> obtenerTodasLasEntradas() {
        ListaEnlazada<EntradaFS> lista = new ListaEnlazada<>();
        try {
            mutexFS.acquire();
            recolectarTodas(raiz, lista);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexFS.release();
        }
        return lista;
    }

    private void recolectarTodas(EntradaFS nodo, ListaEnlazada<EntradaFS> lista) {
        if (nodo == null) return;
        if (!"/".equals(nodo.getRutaCompleta())) {
            lista.agregar(nodo);
        }
        if (nodo.isEsDirectorio() && nodo.getHijos() != null) {
            for (int i = 0; i < nodo.getHijos().getTamano(); i++) {
                recolectarTodas(nodo.getHijos().obtener(i), lista);
            }
        }
    }

    private String normalizarRuta(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) return "/";
        String r = ruta.trim();
        if (!r.startsWith("/")) r = "/" + r;
        if (r.length() > 1 && r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }

    private String obtenerRutaPadre(String rutaNormalizada) {
        int idx = rutaNormalizada.lastIndexOf('/');
        if (idx <= 0) return "/";
        return rutaNormalizada.substring(0, idx);
    }

    private String obtenerNombreDesdeRuta(String rutaNormalizada) {
        int idx = rutaNormalizada.lastIndexOf('/');
        return rutaNormalizada.substring(idx + 1);
    }

    // --- Getters y Setters ---
    public EntradaFS getRaiz() { return raiz; }
    public DiscoSimulado getDisco() { return disco; }
    public ModoUsuario getModoActual() { return modoActual; }
    public void setModoActual(ModoUsuario modoActual) {
        this.modoActual = modoActual;
        log("Modo de usuario cambiado a: " + modoActual.getDescripcion());
    }
    public String getUsuarioActivo() { return usuarioActivo; }
    public void setUsuarioActivo(String usuarioActivo) { this.usuarioActivo = usuarioActivo; }

    public ListaEnlazada<String> obtenerUsuariosExistentes() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        recolectarUsuarios(raiz, lista);
        return lista;
    }

    private void recolectarUsuarios(EntradaFS nodo, ListaEnlazada<String> lista) {
        if (nodo == null) return;
        String dueno = nodo.getDueno();
        if (dueno != null && !dueno.trim().isEmpty()
                && !dueno.equalsIgnoreCase("admin")
                && !dueno.equalsIgnoreCase("Sistema")) {
            boolean existe = false;
            for (int i = 0; i < lista.getTamano(); i++) {
                if (lista.obtener(i).equalsIgnoreCase(dueno.trim())) {
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                lista.agregar(dueno.trim());
            }
        }
        if (nodo.isEsDirectorio() && nodo.getHijos() != null) {
            for (int i = 0; i < nodo.getHijos().getTamano(); i++) {
                recolectarUsuarios(nodo.getHijos().obtener(i), lista);
            }
        }
    }

    public boolean existeUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) return false;
        ListaEnlazada<String> usuarios = obtenerUsuariosExistentes();
        for (int i = 0; i < usuarios.getTamano(); i++) {
            if (usuarios.obtener(i).equalsIgnoreCase(nombreUsuario.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Como máximo se permiten 5 usuarios.
     * Si el usuario es nuevo, el Administrador crea automáticamente su carpeta personal.
     * @return null si la operación fue exitosa, o un mensaje de error si no se pudo crear.
     */
    public String registrarNuevoUsuarioConCarpetaAdmin(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return "El nombre de usuario no puede estar vacío.";
        }
        String usuarioLimpio = nombreUsuario.trim();
        if (usuarioLimpio.equalsIgnoreCase("admin") || usuarioLimpio.equalsIgnoreCase("Sistema")) {
            return "No puede usar nombres reservados del sistema.";
        }

        ListaEnlazada<String> usuarios = obtenerUsuariosExistentes();
        boolean yaExiste = false;
        for (int i = 0; i < usuarios.getTamano(); i++) {
            if (usuarios.obtener(i).equalsIgnoreCase(usuarioLimpio)) {
                yaExiste = true;
                break;
            }
        }

        if (yaExiste) {
            this.usuarioActivo = usuarioLimpio;
            return null; // Ya existe, simplemente cambiamos la sesión a este usuario
        }

        if (usuarios.getTamano() >= MAX_USUARIOS) {
            return "Límite alcanzado: El sistema solo permite un máximo de " + MAX_USUARIOS + " usuarios registrados.\n" +
                   "Usuarios actuales: " + formatearListaUsuarios(usuarios);
        }

        // Determinar ruta de la carpeta personal (usar /home o /usuarios según lo que exista en el FS)
        String rutaBase = "/home";
        if (buscarPorRuta("/usuarios") != null) {
            rutaBase = "/usuarios";
        } else if (buscarPorRuta("/home") == null) {
            crearDirectorio("/home", "admin", PermisoArchivo.PUBLICO);
        }

        String rutaCarpetaUsuario = rutaBase + "/" + usuarioLimpio;
        boolean creada = crearDirectorio(rutaCarpetaUsuario, usuarioLimpio, PermisoArchivo.PRIVADO);
        if (creada) {
            this.usuarioActivo = usuarioLimpio;
            log("ADMIN: Carpeta personal creada automáticamente en '" + rutaCarpetaUsuario + "' para el usuario '" + usuarioLimpio + "' (" + (usuarios.getTamano() + 1) + "/" + MAX_USUARIOS + ").");
            return null;
        } else {
            return "Error al intentar crear la carpeta personal en: " + rutaCarpetaUsuario;
        }
    }

    private String formatearListaUsuarios(ListaEnlazada<String> lista) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.getTamano(); i++) {
            sb.append(lista.obtener(i));
            if (i < lista.getTamano() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}