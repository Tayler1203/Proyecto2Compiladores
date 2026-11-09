
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
    private final String etiquetaFin;
    private String etiquetaFalsa;
    private boolean tieneSaltoAFin;

    private ContextoIf(String etiquetaFin, String etiquetaFalsa) {
      this.etiquetaFin = etiquetaFin;
      this.etiquetaFalsa = etiquetaFalsa;
    }
  }

  private static class ContextoFor {
    private final String etiquetaCondicion;
    private final String etiquetaFin;
    private List<String> instruccionesIncremento = new ArrayList<>();

    private ContextoFor(String etiquetaCondicion, String etiquetaFin) {
      this.etiquetaCondicion = etiquetaCondicion;
      this.etiquetaFin = etiquetaFin;
    }
  }

  private int contadorTemporales = 0;
  private int contadorEtiquetas = 0;

  private final List<String> instrucciones = new ArrayList<>();
  private final Deque<ContextoIf> contextosIf = new ArrayDeque<>();
  private final Deque<ContextoFor> contextosFor = new ArrayDeque<>();
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
    if (tipo == null) {
      return null;
    }

    StringBuilder lugar = new StringBuilder(nombre + "є:");
    boolean lugaresCompletos = true;

    for (int i = 0; i < argumentos.size(); i++) {
      ResultadoExpresion argumento = argumentos.get(i);

      if (argumento == null || argumento.getLugar() == null) {
        lugaresCompletos = false;
      } else {
        if (i > 0) {
          lugar.append(", ");
        }

        lugar.append(argumento.getLugar());
      }
    }

    lugar.append(":э");

    return crearResultado(
        tipo,
        lugaresCompletos ? lugar.toString() : null
    );
  }

  public ResultadoExpresion crearAccesoArreglo(
      String nombre,
      ResultadoExpresion indice,
      String tipo
  ) {
    if (tipo == null || indice == null) {
      return null;
    }

    String lugar = indice.getLugar() == null
        ? null
        : nombre + "ʃ:" + indice.getLugar() + ":ʅ";

    return crearResultado(tipo, lugar);
  }

  public String nuevoTemporal() {
    return "t" + contadorTemporales++;
  }

  public String generarLiteral(String valor) {
    String temporal = nuevoTemporal();
    emitir(temporal + " = " + valor);
    return temporal;
  }

  public String nuevaEtiqueta() {
    return "L" + contadorEtiquetas++;
  }

  public void iniciarIf(ResultadoExpresion condicion) {
    ContextoIf contexto = new ContextoIf(
        nuevaEtiqueta(),
        nuevaEtiqueta()
    );
    contextosIf.push(contexto);
    emitirSaltoCondicionalFalso(condicion, contexto.etiquetaFalsa);
  }

  public void iniciarElif() {
    ContextoIf contexto = obtenerContextoIf();
    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaFalsa);
    contexto.etiquetaFalsa = nuevaEtiqueta();
    contexto.tieneSaltoAFin = true;
  }

  public void evaluarElif(ResultadoExpresion condicion) {
    ContextoIf contexto = obtenerContextoIf();
    emitirSaltoCondicionalFalso(condicion, contexto.etiquetaFalsa);
  }

  public void iniciarElse() {
    ContextoIf contexto = obtenerContextoIf();
    emitir("goto " + contexto.etiquetaFin);
    emitirEtiqueta(contexto.etiquetaFalsa);
    contexto.etiquetaFalsa = null;
    contexto.tieneSaltoAFin = true;
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

  public void iniciarFor() {
    ContextoFor contexto = new ContextoFor(
        nuevaEtiqueta(),
        nuevaEtiqueta()
    );
    contextosFor.push(contexto);
    emitirEtiqueta(contexto.etiquetaCondicion);
  }

  public void evaluarCondicionFor(ResultadoExpresion condicion) {
    ContextoFor contexto = obtenerContextoFor();
    emitirSaltoCondicionalFalso(condicion, contexto.etiquetaFin);
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
    instrucciones.addAll(contexto.instruccionesIncremento);
    emitir("goto " + contexto.etiquetaCondicion);
    emitirEtiqueta(contexto.etiquetaFin);
    contextosFor.pop();
  }

  private void emitirSaltoCondicionalFalso(
      ResultadoExpresion condicion,
      String etiquetaFalsa
  ) {
    if (condicion != null
        && "bool".equals(condicion.getTipo())
        && condicion.getLugar() != null) {
      emitir(
          "ifFalse " + condicion.getLugar()
          + " goto " + etiquetaFalsa
      );
    } else {
      emitir("goto " + etiquetaFalsa);
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

  public void generarAsignacionInicial(
      String nombre,
      ResultadoExpresion expresion
  ) {
    if (expresion == null || expresion.getLugar() == null) {
      return;
    }

    String lugar = expresion.getLugar();

    if (lugar.equals("true")
        || lugar.equals("false")
        || lugar.startsWith("\"")
        || lugar.matches("-?[0-9]+(\\.[0-9]+)?")) {
      lugar = generarLiteral(lugar);
    }

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
    instruccionesDiferidas = null;
  }
}
