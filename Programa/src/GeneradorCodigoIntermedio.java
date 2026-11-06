
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GeneradorCodigoIntermedio {

  private int contadorTemporales = 0;
  private int contadorEtiquetas = 0;

  private final List<String> instrucciones = new ArrayList<>();

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

  public void emitir(String instruccion) {
    instrucciones.add(instruccion);
  }

  public void emitirEtiqueta(String etiqueta) {
    emitir(etiqueta + ":");
  }

  public void generarAsignacion(String identificador, String lugar) {
    emitir(identificador + " = " + lugar);
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
  }
}
