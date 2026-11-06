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
