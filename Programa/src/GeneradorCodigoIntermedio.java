
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Nombre: GeneradorCodigoIntermedio
 * Descripcion: Genera las instrucciones de codigo intermedio del programa y
 * administra temporales, etiquetas y contextos de control.
 * Salidas: Codigo intermedio almacenado en memoria o en un archivo.
 */
public class GeneradorCodigoIntermedio {

  private static class ContextoIf {
    private final String base;
    private final String etiquetaFin;
    private String etiquetaFalsa;
    private boolean tieneSaltoAFin;
    private int contadorElif;

    private ContextoIf(String base) {
      this.base = base;
      this.etiquetaFin = base + "_fin";
      this.etiquetaFalsa = base + "_siguiente";
    }
  }

  private static class ContextoFor {
    private final String etiquetaCondicion;
    private final String etiquetaCuerpo;
    private final String etiquetaIncremento;
    private final String etiquetaFin;
    private List<String> instruccionesIncremento = new ArrayList<>();

    private ContextoFor(String base) {
      this.etiquetaCondicion = base + "_comparacion";
      this.etiquetaCuerpo = base + "_cuerpo";
      this.etiquetaIncremento = base + "_incremento";
      this.etiquetaFin = base + "_fin";
    }
  }

  private static class ContextoWhile {
    private final String base;

    private ContextoWhile(String base) {
      this.base = base;
    }
  }

  private int contadorTemporales = 0;
  private int contadorEtiquetas = 0;

  private final List<String> instrucciones = new ArrayList<>();
  private final Deque<ContextoIf> contextosIf = new ArrayDeque<>();
  private final Deque<ContextoFor> contextosFor = new ArrayDeque<>();
  private final Deque<ContextoWhile> contextosWhile = new ArrayDeque<>();
  private final Deque<String> etiquetasFinCiclos = new ArrayDeque<>();
  private final Deque<String> parametrosPendientes = new ArrayDeque<>();
  private List<String> instruccionesDiferidas;

  /**
   * Nombre: crearResultado
   * Descripcion: Crea el resultado de una expresion.
   * Salidas: No hay.
   *
   * @param tipo Tipo de la expresion.
   * @param lugar Lugar donde se encuentra el resultado.
   * @return Resultado creado o null si el tipo es nulo.
   */
  public ResultadoExpresion crearResultado(String tipo, String lugar) {
    return tipo == null ? null : new ResultadoExpresion(tipo, lugar);
  }

  /**
   * Nombre: crearOperacionBinaria
   * Descripcion: Genera una operacion binaria entre dos expresiones.
   * Salidas: Agrega la instruccion de operacion al codigo intermedio.
   *
   * @param izquierdo Operando izquierdo.
   * @param derecho Operando derecho.
   * @param operador Operador de la operacion.
   * @param tipo Tipo del resultado.
   * @return Resultado de la operacion.
   */
  public ResultadoExpresion crearOperacionBinaria(
      ResultadoExpresion izquierdo,
      ResultadoExpresion derecho,
      String operador,
      String tipo
  ) {
    if (tipo == null || izquierdo == null || derecho == null) {
      return null;
    }

    String lugar = null;

    if (izquierdo.getLugar() != null && derecho.getLugar() != null) {
      lugar = nuevoTemporal();

      emitir(
          lugar + " = " + izquierdo.getLugar()
          + " " + operador + " " + derecho.getLugar()
      );
    }

    return crearResultado(tipo, lugar);
  }

  /**
   * Nombre: crearNegacion
   * Descripcion: Genera la negacion de una expresion.
   * Salidas: Agrega la instruccion de negacion al codigo intermedio.
   *
   * @param operando Expresion que se negara.
   * @param tipo Tipo del resultado.
   * @return Resultado de la negacion.
   */
  public ResultadoExpresion crearNegacion(
      ResultadoExpresion operando,
      String tipo
  ) {
    if (tipo == null || operando == null) {
      return null;
    }

    String lugar = null;

    if (operando.getLugar() != null) {
      lugar = nuevoTemporal();
      emitir(lugar + " = Σ " + operando.getLugar());
    }

    return crearResultado(tipo, lugar);
  }

  /**
   * Nombre: crearLlamadaFuncion
   * Descripcion: Genera una llamada a funcion y sus parametros.
   * Salidas: Agrega las instrucciones de parametros y llamada.
   *
   * @param nombre Nombre de la funcion.
   * @param argumentos Argumentos de la llamada.
   * @param tipo Tipo retornado por la funcion.
   * @return Resultado de la llamada.
   */
  public ResultadoExpresion crearLlamadaFuncion(
      String nombre,
      List<ResultadoExpresion> argumentos,
      String tipo
  ) {
    if (tipo == null || argumentos == null) {
      return null;
    }

    for (ResultadoExpresion argumento : argumentos) {
      if (argumento == null || argumento.getLugar() == null) {
        return null;
      }
    }
    List<String> lugaresArgumentos = new ArrayList<>();
    for (ResultadoExpresion argumento : argumentos) {
      String lugar = obtenerLugarOperando(argumento.getLugar());

      lugaresArgumentos.add(lugar);
    }

    for (String lugar : lugaresArgumentos) {
      emitir("param " + lugar);
    }

    if ("void".equals(tipo)) {
      emitir("call " + nombre + ", " + argumentos.size());
      return crearResultado(tipo, null);
    }

    String temporalResultado = nuevoTemporal();

    emitir(
        temporalResultado + " = call " + nombre + ", " + argumentos.size()
    );

    return crearResultado(tipo, temporalResultado);
  }

  /**
   * Nombre: crearAccesoArreglo
   * Descripcion: Genera el acceso a un elemento de arreglo.
   * Salidas: Agrega la instruccion de acceso al codigo intermedio.
   *
   * @param nombre Nombre del arreglo.
   * @param indice Indice del elemento.
   * @param tipo Tipo del elemento.
   * @return Resultado del acceso.
   */
  public ResultadoExpresion crearAccesoArreglo(
      String nombre,
      ResultadoExpresion indice,
      String tipo
  ) {
    if (tipo == null || indice == null
        || indice.getLugar() == null) {
      return null;
    }

    String temporal = nuevoTemporal();

    emitir(
        temporal + " = " + nombre
        + "[" + indice.getLugar() + "]"
    );

    return crearResultado(tipo, temporal);
  }

  /**
   * Nombre: nuevoTemporal
   * Descripcion: Genera un identificador temporal unico.
   * Salidas: No hay.
   *
   * @return Nombre del temporal generado.
   */
  public String nuevoTemporal() {
    return "t" + contadorTemporales++;
  }

  /**
   * Nombre: generarLiteral
   * Descripcion: Guarda un literal en un temporal.
   * Salidas: Agrega la asignacion del literal al codigo intermedio.
   *
   * @param valor Valor del literal.
   * @return Temporal que contiene el literal.
   */
  public String generarLiteral(String valor) {
    String temporal = nuevoTemporal();
    emitir(temporal + " = " + valor);
    return temporal;
  }

  private boolean esLiteral(String lugar) {
    if (lugar == null) {
      return false;
    }

    return lugar.equals("true")
        || lugar.equals("false")
        || lugar.startsWith("\"")
        || lugar.matches("-?[0-9]+(\\.[0-9]+)?");
  }

  private String obtenerLugarOperando(String lugar) {
    if (esLiteral(lugar)) {
      return generarLiteral(lugar);
    }

    return lugar;
  }

  /**
   * Nombre: nuevaEtiqueta
   * Descripcion: Genera una etiqueta unica para saltos.
   * Salidas: No hay.
   *
   * @return Nombre de la etiqueta.
   */
  public String nuevaEtiqueta() {
    return "L" + contadorEtiquetas++;
  }

  /**
   * Nombre: iniciarIf
   * Descripcion: Inicia la generacion de un bloque if.
   * Salidas: Agrega las instrucciones de control del bloque.
   *
   * @param condicion Condicion del bloque.
   * @param funcion Funcion que contiene el bloque.
   */
  public void iniciarIf(ResultadoExpresion condicion, String funcion) {
    String base = funcion + "_if_" + contadorEtiquetas++;

    ContextoIf contexto = new ContextoIf(base);
    contextosIf.push(contexto);

    emitirSaltoCondicionalVerdadero(condicion, base + "_bloque");
    emitir("goto " + contexto.etiquetaFalsa);
    emitirEtiqueta(base + "_bloque");
  }

  /**
   * Nombre: iniciarElif
   * Descripcion: Inicia una rama elif del bloque actual.
   * Salidas: Agrega las etiquetas y saltos de la rama.
   */
  public void iniciarElif() {
    ContextoIf contexto = obtenerContextoIf();

    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaFalsa);

    contexto.contadorElif++;

    contexto.etiquetaFalsa =
        contexto.base + "_elif_"
        + contexto.contadorElif + "_siguiente";

    contexto.tieneSaltoAFin = true;
  }

  /**
   * Nombre: evaluarElif
   * Descripcion: Evalua la condicion de una rama elif.
   * Salidas: Agrega las instrucciones de evaluacion.
   *
   * @param condicion Condicion de la rama.
   */
  public void evaluarElif(ResultadoExpresion condicion) {
    ContextoIf contexto = obtenerContextoIf();

    emitirSaltoCondicionalVerdadero(
        condicion,
        contexto.base + "_elif_"
        + contexto.contadorElif + "_bloque"
    );
    emitir("goto " + contexto.etiquetaFalsa);

    emitirEtiqueta(
        contexto.base + "_elif_"
        + contexto.contadorElif + "_bloque"
    );
  }

  /**
   * Nombre: iniciarElse
   * Descripcion: Inicia la rama else del bloque actual.
   * Salidas: Agrega las etiquetas y saltos de la rama.
   */
  public void iniciarElse() {
    ContextoIf contexto = obtenerContextoIf();

    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaFalsa);

    contexto.etiquetaFalsa = null;
    contexto.tieneSaltoAFin = true;

    emitirEtiqueta(contexto.base + "_else_bloque");
  }

  /**
   * Nombre: terminarIf
   * Descripcion: Finaliza el bloque if activo.
   * Salidas: Agrega las etiquetas de cierre del bloque.
   */
  public void terminarIf() {
    ContextoIf contexto = obtenerContextoIf();

    if (contexto.etiquetaFalsa != null) {
      emitirEtiqueta(contexto.etiquetaFalsa);
    }

    if (contexto.tieneSaltoAFin) {
      emitirEtiqueta(contexto.etiquetaFin);
    }

    contextosIf.pop();
  }

  /**
   * Nombre: iniciarFor
   * Descripcion: Inicia la generacion de un ciclo for.
   * Salidas: Agrega la etiqueta de comparacion del ciclo.
   *
   * @param funcion Funcion que contiene el ciclo.
   */
  public void iniciarFor(String funcion) {
    String base = funcion + "_for_" + contadorEtiquetas++;

    ContextoFor contexto = new ContextoFor(base);

    contextosFor.push(contexto);
    etiquetasFinCiclos.push(contexto.etiquetaFin);

    emitirEtiqueta(contexto.etiquetaCondicion);
  }

  /**
   * Nombre: evaluarCondicionFor
   * Descripcion: Emite el salto condicional del ciclo for.
   * Salidas: Agrega las instrucciones de control del ciclo.
   *
   * @param condicion Condicion del ciclo.
   */
  public void evaluarCondicionFor(ResultadoExpresion condicion) {
    ContextoFor contexto = obtenerContextoFor();

    if (condicion != null
        && "bool".equals(condicion.getTipo())
        && condicion.getLugar() != null) {
      emitir(
          "if " + condicion.getLugar()
          + " goto " + contexto.etiquetaCuerpo
      );
    }

    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaCuerpo);

    instruccionesDiferidas = new ArrayList<>();
  }

  /**
   * Nombre: finalizarEncabezadoFor
   * Descripcion: Finaliza el encabezado del ciclo for.
   * Salidas: No hay.
   */
  public void finalizarEncabezadoFor() {
    ContextoFor contexto = obtenerContextoFor();

    if (instruccionesDiferidas == null) {
      throw new IllegalStateException(
          "No hay instrucciones de incremento pendientes para el for."
      );
    }

    contexto.instruccionesIncremento = instruccionesDiferidas;
    instruccionesDiferidas = null;
  }

  /**
   * Nombre: terminarFor
   * Descripcion: Finaliza el ciclo for activo.
   * Salidas: Agrega las instrucciones de incremento y cierre.
   */
  public void terminarFor() {
    ContextoFor contexto = obtenerContextoFor();

    emitirEtiqueta(contexto.etiquetaIncremento);
    instrucciones.addAll(contexto.instruccionesIncremento);

    emitir("goto " + contexto.etiquetaCondicion);
    emitirEtiqueta(contexto.etiquetaFin);

    etiquetasFinCiclos.pop();
    contextosFor.pop();
  }

  /**
   * Nombre: comenzarWhile
   * Descripcion: Inicia la generacion de un ciclo while.
   * Salidas: Agrega la etiqueta de inicio del ciclo.
   *
   * @param funcion Funcion que contiene el ciclo.
   */
  public void comenzarWhile(String funcion) {
    String base = funcion + "_while_" + contadorEtiquetas++;

    ContextoWhile contexto = new ContextoWhile(base);

    contextosWhile.push(contexto);
    etiquetasFinCiclos.push(base + "_fin");

    emitirEtiqueta(base + "_inicio");
  }

  /**
   * Nombre: evaluarWhile
   * Descripcion: Emite el salto condicional del ciclo while.
   * Salidas: Agrega las instrucciones de control del ciclo.
   *
   * @param condicion Condicion del ciclo.
   */
  public void evaluarWhile(ResultadoExpresion condicion) {
    ContextoWhile contexto = contextosWhile.peek();

    if (condicion != null
        && "bool".equals(condicion.getTipo())
        && condicion.getLugar() != null) {
      emitir(
          "if " + condicion.getLugar()
          + " goto " + contexto.base + "_bloque"
      );
    }

    emitir("goto " + contexto.base + "_fin");
    emitirEtiqueta(contexto.base + "_bloque");
  }

  /**
   * Nombre: terminarWhile
   * Descripcion: Finaliza el ciclo while activo.
   * Salidas: Agrega las instrucciones de cierre del ciclo.
   */
  public void terminarWhile() {
    ContextoWhile contexto = contextosWhile.pop();

    emitir("goto " + contexto.base + "_inicio");
    emitirEtiqueta(contexto.base + "_fin");

    etiquetasFinCiclos.pop();
  }

  /**
   * Nombre: generarBreak
   * Descripcion: Genera un salto hacia el final del ciclo mas interno.
   * Salidas: Agrega una instruccion de salto si existe un ciclo activo.
   */
  public void generarBreak() {
    if (etiquetasFinCiclos.isEmpty()) {
      return;
    }

    emitir("goto " + etiquetasFinCiclos.peek());
  }

  private void emitirSaltoCondicionalVerdadero(
      ResultadoExpresion condicion,
      String etiquetaVerdadera
  ) {
    if (condicion != null
        && "bool".equals(condicion.getTipo())
        && condicion.getLugar() != null) {
      emitir(
          "if " + condicion.getLugar()
          + " goto " + etiquetaVerdadera
      );
    }
  }

  private ContextoIf obtenerContextoIf() {
    if (contextosIf.isEmpty()) {
      throw new IllegalStateException(
          "No hay un contexto if activo para generar codigo intermedio."
      );
    }

    return contextosIf.peek();
  }

  private ContextoFor obtenerContextoFor() {
    if (contextosFor.isEmpty()) {
      throw new IllegalStateException(
          "No hay un contexto for activo para generar codigo intermedio."
      );
    }

    return contextosFor.peek();
  }

  /**
   * Nombre: emitir
   * Descripcion: Agrega una instruccion al contexto activo.
   * Salidas: Almacena la instruccion en el codigo intermedio.
   *
   * @param instruccion Instruccion que se agregara.
   */
  public void emitir(String instruccion) {
    if (instruccionesDiferidas == null) {
      instrucciones.add(instruccion);
    } else {
      instruccionesDiferidas.add(instruccion);
    }
  }

  /**
   * Nombre: emitirEtiqueta
   * Descripcion: Emite una etiqueta para instrucciones de salto.
   * Salidas: Agrega la etiqueta al codigo intermedio.
   *
   * @param etiqueta Nombre de la etiqueta.
   */
  public void emitirEtiqueta(String etiqueta) {
    emitir(etiqueta + ":");
  }

  /**
   * Nombre: generarAsignacion
   * Descripcion: Genera una asignacion entre un identificador y un lugar.
   * Salidas: Agrega una instruccion de asignacion.
   *
   * @param identificador Identificador que recibira el valor.
   * @param lugar Lugar que contiene el valor.
   */
  public void generarAsignacion(String identificador, String lugar) {
    emitir(identificador + " = " + lugar);
  }

  /**
   * Nombre: generarIncrementoDecremento
   * Descripcion: Genera el incremento o decremento de un identificador.
   * Salidas: Agrega las instrucciones de calculo y asignacion.
   *
   * @param identificador Identificador que se modificara.
   * @param operador Operador de incremento o decremento.
   */
  public void generarIncrementoDecremento(
      String identificador,
      String operador
  ) {
    String operadorAritmetico = "++".equals(operador) ? "+" : "-";
    String temporal = nuevoTemporal();
    emitir(temporal + " = " + identificador + " "
        + operadorAritmetico + " 1");
    generarAsignacion(identificador, temporal);
  }

  /**
   * Nombre: generarDeclaracion
   * Descripcion: Genera la declaracion de una variable.
   * Salidas: Agrega la declaracion al codigo intermedio.
   *
   * @param tipo Tipo de la variable.
   * @param nombre Nombre de la variable.
   */
  public void generarDeclaracion(String tipo, String nombre) {
    emitir("data_" + tipo + " " + nombre);
  }

  /**
   * Nombre: generarDeclaracionArreglo
   * Descripcion: Genera la declaracion de un arreglo.
   * Salidas: Agrega la declaracion al codigo intermedio.
   *
   * @param tipo Tipo de los elementos.
   * @param nombre Nombre del arreglo.
   * @param tamanio Cantidad de elementos.
   */
  public void generarDeclaracionArreglo(
      String tipo,
      String nombre,
      int tamanio
  ) {
    emitir("data_" + tipo + " " + nombre + "[" + tamanio + "]");
  }

  /**
   * Nombre: generarInicializacionArreglo
   * Descripcion: Genera las asignaciones iniciales de un arreglo.
   * Salidas: Agrega las asignaciones validas al codigo intermedio.
   *
   * @param nombre Nombre del arreglo.
   * @param elementos Elementos iniciales.
   */
  public void generarInicializacionArreglo(
      String nombre,
      List<ResultadoExpresion> elementos
  ) {
    if (elementos == null) {
      return;
    }

    for (int i = 0; i < elementos.size(); i++) {
      ResultadoExpresion elemento = elementos.get(i);

      if (elemento == null || elemento.getLugar() == null) {
        continue;
      }

      String lugar = obtenerLugarOperando(elemento.getLugar());

      emitir(nombre + "[" + i + "] = " + lugar);
    }
  }

  /**
   * Nombre: generarAsignacionElementoArreglo
   * Descripcion: Genera la asignacion de un elemento de arreglo.
   * Salidas: Agrega una instruccion de asignacion.
   *
   * @param nombre Nombre del arreglo.
   * @param indice Indice del elemento.
   * @param valor Valor que se asignara.
   */
  public void generarAsignacionElementoArreglo(
      String nombre,
      ResultadoExpresion indice,
      ResultadoExpresion valor
  ) {
    if (indice == null || valor == null
        || indice.getLugar() == null
        || valor.getLugar() == null) {
      return;
    }

    String lugarIndice = indice.getLugar();
    String lugarValor = obtenerLugarOperando(valor.getLugar());

    emitir(nombre + "[" + lugarIndice + "] = " + lugarValor);
  }

  /**
   * Nombre: generarRead
   * Descripcion: Genera una instruccion de lectura.
   * Salidas: Agrega una instruccion read al codigo intermedio.
   *
   * @param identificador Identificador que recibira la entrada.
   */
  public void generarRead(String identificador) {
    emitir("read " + identificador);
  }

  /**
   * Nombre: generarWrite
   * Descripcion: Genera una instruccion de escritura.
   * Salidas: Agrega una instruccion write al codigo intermedio.
   *
   * @param expresion Expresion que se escribira.
   */
  public void generarWrite(ResultadoExpresion expresion) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }
    String lugar = obtenerLugarOperando(expresion.getLugar());

    emitir("write " + lugar);
  }

  /**
   * Nombre: registrarParametro
   * Descripcion: Registra un parametro para su emision posterior.
   * Salidas: No hay.
   *
   * @param tipo Tipo del parametro.
   * @param nombre Nombre del parametro.
   */
  public void registrarParametro(String tipo, String nombre) {
    parametrosPendientes.addLast("param_" + tipo + " " + nombre);
  }

  /**
   * Nombre: emitirParametros
   * Descripcion: Emite los parametros pendientes de la funcion actual.
   * Salidas: Agrega los parametros al codigo intermedio.
   */
  public void emitirParametros() {
    while (!parametrosPendientes.isEmpty()) {
      emitir(parametrosPendientes.removeFirst());
    }
  }

  /**
   * Nombre: iniciarFuncion
   * Descripcion: Emite el inicio de una funcion.
   * Salidas: Agrega la instruccion de inicio de funcion.
   *
   * @param nombre Nombre de la funcion.
   */
  public void iniciarFuncion(String nombre) {
    emitir("func " + nombre + ":");
  }

  /**
   * Nombre: terminarFuncion
   * Descripcion: Emite el final de una funcion.
   * Salidas: Agrega la instruccion de final de funcion.
   *
   * @param nombre Nombre de la funcion.
   */
  public void terminarFuncion(String nombre) {
    emitir("endfunc " + nombre);
  }

  /**
   * Nombre: generarReturn
   * Descripcion: Genera una instruccion de retorno.
   * Salidas: Agrega una instruccion return al codigo intermedio.
   *
   * @param expresion Expresion que se retornara.
   */
  public void generarReturn(ResultadoExpresion expresion) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }

    emitir("return " + expresion.getLugar());
  }

  /**
   * Nombre: generarAsignacionInicial
   * Descripcion: Genera la asignacion inicial de una variable.
   * Salidas: Agrega una instruccion de asignacion.
   *
   * @param nombre Nombre de la variable.
   * @param expresion Expresion inicial.
   */
  public void generarAsignacionInicial(
      String nombre,
      ResultadoExpresion expresion
  ) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }

    String lugar = obtenerLugarOperando(expresion.getLugar());

    generarAsignacion(nombre, lugar);
  }

  /**
   * Nombre: guardar
   * Descripcion: Guarda las instrucciones generadas en un archivo UTF-8.
   * Salidas: Genera un archivo con el codigo intermedio.
   *
   * @param archivo Ruta del archivo de salida.
   * @throws IOException si no es posible escribir el archivo.
   */
  public void guardar(String archivo) throws IOException {
    Files.write(
        Path.of(archivo),
        instrucciones,
        StandardCharsets.UTF_8
    );
  }

  /**
   * Nombre: reiniciar
   * Descripcion: Restablece contadores, contextos e instrucciones.
   * Salidas: No hay.
   */
  public void reiniciar() {
    contadorTemporales = 0;
    contadorEtiquetas = 0;
    instrucciones.clear();
    contextosIf.clear();
    contextosFor.clear();
    contextosWhile.clear();
    etiquetasFinCiclos.clear();
    parametrosPendientes.clear();
    instruccionesDiferidas = null;
  }
}
