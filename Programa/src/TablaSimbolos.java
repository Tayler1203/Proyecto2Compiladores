import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nombre: TablaSimbolos
 * Descripcion: Gestiona los ambitos y simbolos del compilador para llevar el control
 * de variables, funciones y otros identificadores durante la compilacion.
 * Salidas: No hay.
 */
public final class TablaSimbolos {

    private final List<Ambito> ambitos = new ArrayList<>();
    private final Deque<Ambito> pilaAmbitos = new ArrayDeque<>();
    private int contadorBloques;

    /**
     * Nombre: TablaSimbolos
     * Descripcion: Inicializa la tabla de simbolos con el ambito global requerido por el compilador.
     * Salidas: No hay.
     */
    public TablaSimbolos() {
        Ambito global = new Ambito("Global", "Global", 0);
        ambitos.add(global);
        pilaAmbitos.push(global);
    }

    /**
     * Nombre: abrirAmbito
     * Descripcion: Crea y activa un nuevo ambito hijo dentro del ambito actual.
     * Salidas: No hay.
     *
     * @param nombre Nombre del nuevo ambito.
     */
    public void abrirAmbito(String nombre) {
        Ambito padre = pilaAmbitos.peek();
        String ruta = padre.getRuta() + " / " + nombre;
        Ambito ambito = new Ambito(nombre, ruta, padre.getNivel() + 1);
        ambitos.add(ambito);
        pilaAmbitos.push(ambito);
    }

    /**
     * Nombre: abrirAmbitoBloque
     * Descripcion: Abre un ambito adicional para representar un bloque de codigo dentro del ambito actual.
     * Salidas: No hay.
     */
    public void abrirAmbitoBloque() {
        contadorBloques++;
        abrirAmbito("Bloque " + contadorBloques);
    }

    /**
     * Nombre: cerrarAmbito
     * Descripcion: Cierra el ambito activo y vuelve al ambito padre, evitando cerrar el ambito global.
     * Salidas: No hay.
     */
    public void cerrarAmbito() {
        if (pilaAmbitos.size() == 1) {
            throw new IllegalStateException("No se puede cerrar el ambito global.");
        }
        pilaAmbitos.pop();
    }

    /**
     * Nombre: agregarSimbolo
     * Descripcion: Agrega un simbolo al ambito actual si aun no existe en ese mismo contexto.
     * Salidas: No hay.
     *
     * @param nombre Nombre del simbolo.
     * @param tipo Tipo del simbolo.
     * @param linea Numero de linea donde aparece.
     * @param categoria Categoria del simbolo.
     * @return true si el simbolo se agrego correctamente; false si ya existia en el ambito actual.
     */
    public boolean agregarSimbolo(String nombre, String tipo, int linea, String categoria) {
        return pilaAmbitos.peek().agregarSimbolo(nombre, tipo, linea, categoria);
    }

    /**
     * Nombre: getAmbitos
     * Descripcion: Devuelve una vista inmodificable de todos los ambitos registrados en la tabla.
     * Salidas: No hay.
     *
     * @return Lista de ambitos almacenados.
     */
    public List<Ambito> getAmbitos() {
        return Collections.unmodifiableList(new ArrayList<>(ambitos));
    }

    public static final class Ambito {
        private final String nombre;
        private final String ruta;
        private final int nivel;
        private final List<Simbolo> simbolos = new ArrayList<>();
        private final Set<String> nombresSimbolos = new HashSet<>();

        /**
         * Nombre: Ambito
         * Descripcion: Constructor interno que crea un ambito con su nombre, ruta y nivel de profundidad.
         * Salidas: No hay.
         *
         * @param nombre Nombre del ambito.
         * @param ruta Ruta jerarquica del ambito.
         * @param nivel Nivel de profundidad en el arbol de ambitos.
         */
        private Ambito(String nombre, String ruta, int nivel) {
            this.nombre = nombre;
            this.ruta = ruta;
            this.nivel = nivel;
        }

        /**
         * Nombre: agregarSimbolo
         * Descripcion: Registra un simbolo en el ambito actual si no existe en ese mismo alcance.
         * Salidas: No hay.
         *
         * @param nombre Nombre del simbolo.
         * @param tipo Tipo del simbolo.
         * @param linea Linea de declaracion.
         * @param categoria Categoria del simbolo.
         * @return true si se agrego con exito; false si ya existe.
         */
        private boolean agregarSimbolo(String nombre, String tipo, int linea, String categoria) {
            if (!nombresSimbolos.add(nombre)) {
                return false;
            }
            simbolos.add(new Simbolo(nombre, tipo, linea, categoria));
            return true;
        }

        /**
         * Nombre: getNombre
         * Descripcion: Obtiene el nombre del ambito.
         * Salidas: No hay.
         *
         * @return Nombre del ambito.
         */
        public String getNombre() {
            return nombre;
        }

        /**
         * Nombre: getRuta
         * Descripcion: Devuelve la ruta jerarquica del ambito dentro del arbol de alcance.
         * Salidas: No hay.
         *
         * @return Ruta del ambito.
         */
        public String getRuta() {
            return ruta;
        }

        /**
         * Nombre: getNivel
         * Descripcion: Obtiene el nivel de profundidad del ambito.
         * Salidas: No hay.
         *
         * @return Nivel del ambito.
         */
        public int getNivel() {
            return nivel;
        }

        /**
         * Nombre: getSimbolos
         * Descripcion: Devuelve la lista de simbolos definidos en este ambito.
         * Salidas: No hay.
         *
         * @return Simbolos del ambito.
         */
        public List<Simbolo> getSimbolos() {
            return Collections.unmodifiableList(simbolos);
        }
    }

    public static final class Simbolo {
        private final String nombre;
        private final String tipo;
        private final int linea;
        private final String categoria;

        /**
         * Nombre: Simbolo
         * Descripcion: Constructor interno que almacena la informacion basica del simbolo declarado.
         * Salidas: No hay.
         *
         * @param nombre Nombre del simbolo.
         * @param tipo Tipo del simbolo.
         * @param linea Linea donde se declara.
         * @param categoria Categoria del simbolo.
         */
        private Simbolo(String nombre, String tipo, int linea, String categoria) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.linea = linea;
            this.categoria = categoria;
        }

        /**
         * Nombre: getNombre
         * Descripcion: Obtiene el nombre del simbolo.
         * Salidas: No hay.
         *
         * @return Nombre del simbolo.
         */
        public String getNombre() {
            return nombre;
        }

        /**
         * Nombre: getTipo
         * Descripcion: Retorna el tipo asociado al simbolo.
         * Salidas: No hay.
         *
         * @return Tipo del simbolo.
         */
        public String getTipo() {
            return tipo;
        }

        /**
         * Nombre: getLinea
         * Descripcion: Devuelve la linea en la que fue declarado el simbolo.
         * Salidas: No hay.
         *
         * @return Numero de linea.
         */
        public int getLinea() {
            return linea;
        }

        /**
         * Nombre: getCategoria
         * Descripcion: Proporciona la categoria del simbolo, como variable, funcion o constante.
         * Salidas: No hay.
         *
         * @return Categoria del simbolo.
         */
        public String getCategoria() {
            return categoria;
        }
    }
}
