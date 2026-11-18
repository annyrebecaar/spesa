package com.example.spesa.model

/** O tipo do lembrete muda o ícone e a cor na agenda. */
enum class TipoLembrete(val rotulo: String) {
    COMPRAR("Comprar"), PAGAR("Pagar"), FAZER("Fazer"),
}

/** Quanto tempo antes avisar. */
enum class Aviso(val rotulo: String, val minutosAntes: Long) {
    NA_HORA("Na hora", 0), UMA_HORA("1 h antes", 60), UM_DIA("1 dia antes", 24 * 60),
}

/** Um compromisso da agenda: comprar algo, pagar uma conta ou uma tarefa. */
data class Lembrete(
    val id: Long = System.currentTimeMillis(),
    var tipo: TipoLembrete = TipoLembrete.COMPRAR,
    var texto: String = "",
    var data: Long = System.currentTimeMillis(),   // dia e hora juntos, em milissegundos
    var listaId: Long? = null,                     // lista que abre ao tocar (só para "Comprar")
    var valor: Double = 0.0,                       // valor da conta (só para "Pagar")
    var aviso: Aviso = Aviso.UM_DIA,
    var repeticao: Repeticao = Repeticao.NAO,
    var feito: Boolean = false,
)
