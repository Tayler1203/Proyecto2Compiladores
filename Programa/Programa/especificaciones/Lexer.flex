/* ==========================================
   PROYECTO 1 - COMPILADORES
   ANALIZADOR LEXICO
   ========================================== */

%%


/* ==========================================
   1. CONFIGURACION DEL ANALIZADOR
   ========================================== */

%public
%class Lexer
%unicode
%line
%column
%cup

%xstate COMENTARIO_MULTI


%eofval{
    return new java_cup.runtime.Symbol(
        sym.EOF,
        yyline + 1,
        yycolumn + 1
    );
%eofval}


%{

    private int lineaInicioComentario;
    private int columnaInicioComentario;

    private java.util.Queue<String> erroresPendientes =
        new java.util.ArrayDeque<>();


    /*
     * Devuelve la linea actual comenzando desde 1.
     */
    public int getLinea() {
        return yyline + 1;
    }


    /*
     * Crea un Symbol compatible con CUP.
     *
     * tipo  = identificador definido en sym.java
     * left  = linea del token
     * right = columna del token
     * value = lexema reconocido
     */
    private java_cup.runtime.Symbol simbolo(int tipo) {

        return new java_cup.runtime.Symbol(
            tipo,
            yyline + 1,
            yycolumn + 1,
            yytext()
        );
    }


    /*
     * Guarda temporalmente los errores lexicos.
     */
    private void registrarError(String mensaje) {
        erroresPendientes.add(mensaje);
    }


    private void registrarErrorLexico(String descripcion) {
        registrarError(
            "Error lexico en linea " + (yyline + 1)
            + ", columna " + (yycolumn + 1)
            + ": " + descripcion
        );
    }


    /*
     * Indica si existen errores lexicos pendientes.
     */
    public boolean hayErroresPendientes() {
        return !erroresPendientes.isEmpty();
    }


    /*
     * Obtiene y elimina el siguiente error pendiente.
     */
    public String obtenerSiguienteError() {
        return erroresPendientes.poll();
    }

%}


/* ==========================================
   2. DEFINICIONES REGULARES
   ========================================== */


/* ==========================================
   2.1 LETRAS, DIGITOS E IDENTIFICADORES
   ========================================== */

Letra = [a-zA-Z]

Digito = [0-9]

Digito_No_Cero = [1-9]

Caracter = {Letra}|{Digito}|"_"

Id = ({Letra}|"_"){Caracter}*


/* ==========================================
   2.2 LITERALES ENTEROS
   ========================================== */

Lit_Int = 0|{Digito_No_Cero}{Digito}*


/* ==========================================
   2.3 LITERALES FLOTANTES
   ========================================== */

Parte_Entera = 0|{Digito_No_Cero}{Digito}*

Parte_Decimal = 0|{Digito}*{Digito_No_Cero}

Lit_Float = {Parte_Entera}"."{Parte_Decimal}


/* ==========================================
   2.4 FLOTANTES INVALIDOS
   ========================================== */

Float_Sin_Decimal = {Parte_Entera}"."

Float_Sin_Entera = "."{Digito}+

Float_Entera_Invalida = 0{Digito}+"."{Digito}+

Float_Decimal_Invalida = {Parte_Entera}"."{Digito}+"0"


/* ==========================================
   2.5 LITERALES BOOLEANOS
   ========================================== */

Lit_Bool = "true"|"false"


/* ==========================================
   2.6 CARACTERES PERMITIDOS EN CHAR Y STRING
   ========================================== */

/* Caracteres internos y secuencias de escape */
Escape = \\[^\r\n]
Caracter_Char = [^'\\\r\n] | {Escape}
Caracter_String = [^\"\\\r\n] | {Escape}

/* Literales */
Lit_Char = "'" {Caracter_Char} "'"
Lit_String = "\"" {Caracter_String}* "\""

/* Char con longitud incorrecta */
Lit_Char_Invalido = "'" {Caracter_Char}* "'"


/* ==========================================
   2.9 COMENTARIO DE UNA LINEA
   ========================================== */

Comentario_Linea = \|[^\r\n]*


/* ==========================================
   2.10 LITERALES SIN CERRAR
   ========================================== */

String_Sin_Cerrar = "\"" {Caracter_String}*

Char_Sin_Cerrar = "'" {Caracter_Char}*


/* ==========================================
   2.11 COMILLAS MEZCLADAS
   ========================================== */

/* Esta sección está vacía - las reglas de comillas mezcladas
   fueron eliminadas porque no son necesarias para reconocer
   literales válidos y pueden consumir fragmentos que deberían
   analizarse como tokens separados. */


%%


/* ==========================================
   3. REGLAS LEXICAS
   ========================================== */


/* ==========================================
   3.1 TIPOS DE DATOS
   ========================================== */

"int" {
    return simbolo(sym.INT);
}

"float" {
    return simbolo(sym.FLOAT);
}

"bool" {
    return simbolo(sym.BOOL);
}

"char" {
    return simbolo(sym.CHAR);
}

"string" {
    return simbolo(sym.STRING);
}

"void" {
    return simbolo(sym.VOID);
}


/* ==========================================
   3.2 PALABRAS RESERVADAS
   ========================================== */

"principal" {
    return simbolo(sym.PRINCIPAL);
}

"if" {
    return simbolo(sym.IF);
}

"elif" {
    return simbolo(sym.ELIF);
}

"else" {
    return simbolo(sym.ELSE);
}

"while" {
    return simbolo(sym.WHILE);
}

"for" {
    return simbolo(sym.FOR);
}

"return" {
    return simbolo(sym.RETURN);
}

"break" {
    return simbolo(sym.BREAK);
}

"val" {
    return simbolo(sym.VAL);
}

"read" {
    return simbolo(sym.READ);
}

"write" {
    return simbolo(sym.WRITE);
}


/* ==========================================
   3.3 OPERADORES ARITMETICOS
   ========================================== */

"++" {
    return simbolo(sym.INCREMENTO);
}

"--" {
    return simbolo(sym.DECREMENTO);
}

"//" {
    return simbolo(sym.DIVISION_ENTERA);
}

"+" {
    return simbolo(sym.SUMA);
}

"-" {
    return simbolo(sym.RESTA);
}

"*" {
    return simbolo(sym.MULTIPLICACION);
}

"/" {
    return simbolo(sym.DIVISION);
}

"mod" {
    return simbolo(sym.MODULO);
}

"pot" {
    return simbolo(sym.POTENCIA);
}


/* ==========================================
   3.4 OPERADORES RELACIONALES
   ========================================== */

"<=" {
    return simbolo(sym.MENOR_IGUAL);
}

">=" {
    return simbolo(sym.MAYOR_IGUAL);
}

"==" {
    return simbolo(sym.IGUAL);
}

"!=" {
    return simbolo(sym.DIFERENTE);
}

"<" {
    return simbolo(sym.MENOR);
}

">" {
    return simbolo(sym.MAYOR);
}


/* ==========================================
   3.5 OPERADORES LOGICOS
   ========================================== */

"λ" {
    return simbolo(sym.AND);
}

"θ" {
    return simbolo(sym.OR);
}

"Σ" {
    return simbolo(sym.NOT);
}


/* ==========================================
   3.6 OPERADOR DE ASIGNACION
   ========================================== */

"Ͱ" {
    return simbolo(sym.ASIGNACION);
}


/* ==========================================
   3.7 DELIMITADORES Y SIMBOLOS
   ========================================== */

"¿:" {
    return simbolo(sym.BLOQUE_INI);
}

":?" {
    return simbolo(sym.BLOQUE_FIN);
}

"є:" {
    return simbolo(sym.PAR_INI);
}

":э" {
    return simbolo(sym.PAR_FIN);
}

"ʃ:" {
    return simbolo(sym.COR_INI);
}

":ʅ" {
    return simbolo(sym.COR_FIN);
}

"»" {
    return simbolo(sym.FIN_EXPR);
}

"," {
    return simbolo(sym.COMA);
}


/* ==========================================
   3.8 LITERALES VALIDOS
   ========================================== */

{Lit_Bool} {
    return simbolo(sym.LIT_BOOL);
}

{Lit_Char} {
    return simbolo(sym.LIT_CHAR);
}

{Lit_String} {
    return simbolo(sym.LIT_STRING);
}

{Float_Decimal_Invalida} {
    registrarErrorLexico(
        "literal float invalido '" + yytext() + "'"
    );
}

{Lit_Float} {
    return simbolo(sym.LIT_FLOAT);
}


/* ==========================================
   3.9 ERRORES DE FLOTANTES
   ========================================== */

{Float_Entera_Invalida} {
    registrarErrorLexico(
        "parte entera invalida en literal flotante "
        + yytext()
        + ". No se permiten ceros a la izquierda."
    );
}


{Float_Sin_Decimal} {
    registrarErrorLexico(
        "literal flotante sin parte decimal " + yytext()
    );
}


{Float_Sin_Entera} {
    registrarErrorLexico(
        "literal flotante sin parte entera " + yytext()
    );
}


/* ==========================================
   3.10 LITERALES ENTEROS
   ========================================== */

{Lit_Int} {
    return simbolo(sym.LIT_INT);
}


/* ==========================================
   3.11 CHAR INVALIDO
   ========================================== */

{Lit_Char_Invalido} {
    registrarErrorLexico(
        "literal char invalido "
        + yytext()
        + ". Un char debe contener exactamente un caracter."
    );
}


/* ==========================================
   3.12 COMILLAS MEZCLADAS
   ========================================== */

/* Esta sección está vacía - las reglas de comillas mezcladas
   y sus acciones de error fueron eliminadas. */


/* ==========================================
   3.13 STRING Y CHAR SIN CERRAR
   ========================================== */

{String_Sin_Cerrar} {
    registrarErrorLexico(
        "string sin cerrar " + yytext()
    );
}


{Char_Sin_Cerrar} {
    registrarErrorLexico(
        "char sin cerrar " + yytext()
    );
}


/* ==========================================
   3.14 IDENTIFICADORES
   ========================================== */

{Id} {
    return simbolo(sym.ID);
}


/* ==========================================
   3.15 COMENTARIOS
   ========================================== */

{Comentario_Linea} {
    /* Ignorar comentario de una linea */
}


"¡" {

    lineaInicioComentario = yyline + 1;
    columnaInicioComentario = yycolumn + 1;

    yybegin(COMENTARIO_MULTI);
}


<COMENTARIO_MULTI> "!" {

    yybegin(YYINITIAL);
}


<COMENTARIO_MULTI> [^!\r\n]+ {
    /* Ignorar contenido */
}


<COMENTARIO_MULTI> \r\n|\r|\n {
    /* Ignorar salto de linea */
}


<COMENTARIO_MULTI> <<EOF>> {

    registrarError(
        "Error lexico en linea "
        + lineaInicioComentario
        + ", columna "
        + columnaInicioComentario
        + ": comentario multilinea no fue cerrado con !"
    );

    return new java_cup.runtime.Symbol(
        sym.EOF,
        yyline + 1,
        yyline + 1
    );
}


/* ==========================================
   4. ESPACIOS EN BLANCO
   ========================================== */

[ \t\r\n]+ {
    /* Ignorar */
}


/* ==========================================
   5. ERROR LEXICO GENERAL
   ========================================== */

[^] {
    registrarErrorLexico(
        "caracter no reconocido '" + yytext() + "'"
    );
}