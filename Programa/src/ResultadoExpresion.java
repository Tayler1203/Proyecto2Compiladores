/**
 * Nombre: ResultadoExpresion
 * Descripcion: Representa el tipo y el lugar del resultado de una expresion.
 * Salidas: No hay.
 */
public class ResultadoExpresion {

  private final String tipo;
  private final String lugar;

  /**
   * Nombre: ResultadoExpresion
   * Descripcion: Inicializa el resultado de una expresion.
   * Salidas: No hay.
   *
   * @param tipo Tipo de la expresion.
   * @param lugar Lugar del resultado.
   */
  public ResultadoExpresion(String tipo, String lugar) {
    this.tipo = tipo;
    this.lugar = lugar;
  }

  /**
   * Nombre: getTipo
   * Descripcion: Obtiene el tipo de la expresion.
   * Salidas: No hay.
   *
   * @return Tipo de la expresion.
   */
  public String getTipo() {
    return tipo;
  }

  /**
   * Nombre: getLugar
   * Descripcion: Obtiene el lugar del resultado.
   * Salidas: No hay.
   *
   * @return Lugar del resultado.
   */
  public String getLugar() {
    return lugar;
  }
}
