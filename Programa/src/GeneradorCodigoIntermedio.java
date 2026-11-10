
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

  public ResultadoExpresion crearResultado(String tipo, String lugar) {
    return tipo == null ? null : new ResultadoExpresion(tipo, lugar);
  }

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

  public String nuevoTemporal() {
    return "t" + contadorTemporales++;
  }

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

  public String nuevaEtiqueta() {
    return "L" + contadorEtiquetas++;
  }

  public void iniciarIf(ResultadoExpresion condicion, String funcion) {
    String base = funcion + "_if_" + contadorEtiquetas++;

    ContextoIf contexto = new ContextoIf(base);
    contextosIf.push(contexto);

    emitirSaltoCondicionalVerdadero(condicion, base + "_bloque");
    emitir("goto " + contexto.etiquetaFalsa);
    emitirEtiqueta(base + "_bloque");
  }

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

  public void iniciarElse() {
    ContextoIf contexto = obtenerContextoIf();

    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaFalsa);

    contexto.etiquetaFalsa = null;
    contexto.tieneSaltoAFin = true;

    emitirEtiqueta(contexto.base + "_else_bloque");
  }

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

  public void iniciarFor(String funcion) {
    String base = funcion + "_for_" + contadorEtiquetas++;

    ContextoFor contexto = new ContextoFor(base);

    contextosFor.push(contexto);
    etiquetasFinCiclos.push(contexto.etiquetaFin);

    emitirEtiqueta(contexto.etiquetaCondicion);
  }

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

  public void terminarFor() {
    ContextoFor contexto = obtenerContextoFor();

    emitirEtiqueta(contexto.etiquetaIncremento);
    instrucciones.addAll(contexto.instruccionesIncremento);

    emitir("goto " + contexto.etiquetaCondicion);
    emitirEtiqueta(contexto.etiquetaFin);

    etiquetasFinCiclos.pop();
    contextosFor.pop();
  }

  public void comenzarWhile(String funcion) {
    String base = funcion + "_while_" + contadorEtiquetas++;

    ContextoWhile contexto = new ContextoWhile(base);

    contextosWhile.push(contexto);
    etiquetasFinCiclos.push(base + "_fin");

    emitirEtiqueta(base + "_inicio");
  }

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

  public void terminarWhile() {
    ContextoWhile contexto = contextosWhile.pop();

    emitir("goto " + contexto.base + "_inicio");
    emitirEtiqueta(contexto.base + "_fin");

    etiquetasFinCiclos.pop();
  }

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

  public void emitir(String instruccion) {
    if (instruccionesDiferidas == null) {
      instrucciones.add(instruccion);
    } else {
      instruccionesDiferidas.add(instruccion);
    }
  }

  public void emitirEtiqueta(String etiqueta) {
    emitir(etiqueta + ":");
  }

  public void generarAsignacion(String identificador, String lugar) {
    emitir(identificador + " = " + lugar);
  }

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

  public void generarDeclaracion(String tipo, String nombre) {
    emitir("data_" + tipo + " " + nombre);
  }

  public void generarDeclaracionArreglo(
      String tipo,
      String nombre,
      int tamanio
  ) {
    emitir("data_" + tipo + " " + nombre + "[" + tamanio + "]");
  }

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

  public void generarRead(String identificador) {
    emitir("read " + identificador);
  }

  public void generarWrite(ResultadoExpresion expresion) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }
    String lugar = obtenerLugarOperando(expresion.getLugar());

    emitir("write " + lugar);
  }

  public void registrarParametro(String tipo, String nombre) {
    parametrosPendientes.addLast("param_" + tipo + " " + nombre);
  }

  public void emitirParametros() {
    while (!parametrosPendientes.isEmpty()) {
      emitir(parametrosPendientes.removeFirst());
    }
  }

  public void iniciarFuncion(String nombre) {
    emitir("func " + nombre + ":");
  }

  public void terminarFuncion(String nombre) {
    emitir("endfunc " + nombre);
  }

  public void generarReturn(ResultadoExpresion expresion) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }

    emitir("return " + expresion.getLugar());
  }

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

  public void guardar(String archivo) throws IOException {
    Files.write(
        Path.of(archivo),
        instrucciones,
        StandardCharsets.UTF_8
    );
  }

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
