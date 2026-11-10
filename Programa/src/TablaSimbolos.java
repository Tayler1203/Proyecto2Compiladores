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
     * Agrega un simbolo al ambito actual con tamaño de arreglo.
     *
     * @param nombre Nombre del simbolo.
     * @param tipo Tipo del simbolo.
     * @param linea Linea donde se declara.
     * @param categoria Categoria del simbolo.
     * @param tamano Tamaño del arreglo (null si no aplica).
     * @return true si el simbolo se agrego correctamente; false si ya existia en el ambito actual.
     */
    public boolean agregarSimbolo(
        String nombre,
        String tipo,
        int linea,
        String categoria,
        Integer tamano
    ) {
        boolean agregado = agregarSimbolo(nombre, tipo, linea, categoria);

        if (agregado) {
            Simbolo simbolo = buscarSimbolo(nombre);
            simbolo.setTamanoArreglo(tamano);
        }

        return agregado;
    }

    /**
     * Busca un identificador desde el ambito actual
     * hasta el ambito global.
     *
     * @param nombre Nombre del identificador.
     * @return Simbolo encontrado o null si no existe
     *         en los ambitos accesibles.
     */
    public Simbolo buscarSimbolo(String nombre) {
        for (Ambito ambito : pilaAmbitos) {
            for (Simbolo simbolo : ambito.getSimbolos()) {
                if (simbolo.getNombre().equals(nombre)) {
                    return simbolo;
                }
            }
        }

        return null;
    }

    /**
     * Obtiene el nombre de la funcion actual recorriendo la pila de ambitos.
     *
     * @return Nombre de la funcion actual ("principal" o nombre de funcion normal),
     *         o null si no estamos dentro de ninguna funcion.
     */
    public String obtenerNombreFuncionActual() {
        for (Ambito ambito : pilaAmbitos) {
            String nombre = ambito.getNombre();

            if ("principal".equals(nombre)) {
                return "principal";
            }

            if (nombre.startsWith("funcion ")) {
                return nombre.substring("funcion ".length());
            }
        }

        return null;
    }

    /**
     * Busca una funcion en el ambito global por su nombre.
     *
     * @param nombre Nombre de la funcion a buscar.
     * @return Simbolo de la funcion encontrada, o null si no existe.
     */
    public Simbolo buscarFuncionGlobal(String nombre) {
        for (Simbolo simbolo : ambitos.get(0).getSimbolos()) {
            if (simbolo.getNombre().equals(nombre)
                && "funcion".equals(simbolo.getCategoria())) {
                return simbolo;
            }
        }

        return null;
    }

    /**
     * Nombre: estaDentroDeCiclo
     * Descripcion: Comprueba si el ambito actual pertenece a un ciclo.
     * Salidas: No hay.
     *
     * @return true si existe un ciclo activo; false en otro caso.
     */
    public boolean estaDentroDeCiclo() {
        for (Ambito ambito : pilaAmbitos) {
            String nombre = ambito.getNombre();

            if (nombre.matches("(while|for) \\d+")) {
                return true;
            }
        }

        return false;
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

    /**
     * Registra la firma de parametros de una funcion declarada en el ambito global.
     *
     * @param nombre Nombre de la funcion.
     * @param tiposParametros Tipos de sus parametros, en orden.
     * @return true si se encontro una funcion y se registro su firma.
     */
    public boolean registrarParametrosFuncion(String nombre, List<String> tiposParametros) {
        if (tiposParametros == null) {
            return false;
        }

        for (Simbolo simbolo : ambitos.get(0).getSimbolos()) {
            if (simbolo.getNombre().equals(nombre)
                && "funcion".equals(simbolo.getCategoria())) {
                return simbolo.setTiposParametros(tiposParametros);
            }
        }
        return false;
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
        private List<String> tiposParametros = new ArrayList<>();
        private boolean parametrosRegistrados;
        private Integer tamanoArreglo;

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

        /**
         * Obtiene los tipos de los parametros declarados, en orden.
         *
         * @return Lista inmodificable de tipos de parametros.
         */
        public List<String> getTiposParametros() {
            return Collections.unmodifiableList(tiposParametros);
        }

        /**
         * Obtiene la cantidad de parametros declarados.
         *
         * @return Cantidad de parametros.
         */
        public int getCantidadParametros() {
            return tiposParametros.size();
        }

        /**
         * Actualiza los tipos de parametros de una funcion.
         * Solo puede invocarse desde la clase contenedora.
         *
         * @param tiposParametros Tipos de los parametros, en orden.
         * @return true si se registraron exitosamente; false si ya estaban registrados.
         */
        private boolean setTiposParametros(List<String> tiposParametros) {
            if (parametrosRegistrados) {
                return false;
            }

            this.tiposParametros = new ArrayList<>(tiposParametros);
            parametrosRegistrados = true;
            return true;
        }

        /**
         * Obtiene el tamaño del arreglo.
         *
         * @return Tamaño del arreglo, o null si no es un arreglo.
         */
        public Integer getTamanoArreglo() {
            return tamanoArreglo;
        }

        /**
         * Establece el tamaño del arreglo.
         *
         * @param tamanoArreglo Tamaño del arreglo.
         */
        public void setTamanoArreglo(Integer tamanoArreglo) {
            this.tamanoArreglo = tamanoArreglo;
        }

    }
}
