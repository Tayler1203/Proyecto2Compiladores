public class ResultadoExpresion {

  private final String tipo;
  private final String lugar;

  public ResultadoExpresion(String tipo, String lugar) {
    this.tipo = tipo;
    this.lugar = lugar;
  }

  public String getTipo() {
    return tipo;
  }

  public String getLugar() {
    return lugar;
  }
}
