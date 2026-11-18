package com.example.spesa.model

import com.example.spesa.R

/** Categorias de dinheiro (diferentes das categorias de produto do mercado). */
enum class CategoriaGasto(val rotulo: String, val corRes: Int) {
    MERCADO("Mercado", R.color.fin_mercado),
    MORADIA("Moradia", R.color.fin_moradia),
    TRANSPORTE("Transporte", R.color.fin_transporte),
    LAZER("Lazer", R.color.fin_lazer),
    CONTAS("Contas", R.color.fin_contas),
    RENDA("Renda", R.color.fin_renda),
}

/** Quantas vezes algo se repete. */
enum class Repeticao(val rotulo: String) {
    NAO("Não repete"), SEMANAL("Toda semana"), MENSAL("Todo mês"),
}

/**
 * Um registro de dinheiro que entrou ou saiu.
 * É só anotação: o app não mexe em conta de banco nenhuma.
 */
data class Lancamento(
    val id: Long = System.currentTimeMillis(),
    var entrada: Boolean = false,           // true = entrou dinheiro; false = saiu
    var valor: Double = 0.0,
    var descricao: String = "",
    var categoria: CategoriaGasto = CategoriaGasto.MERCADO,
    var data: Long = System.currentTimeMillis(),
    var repeticao: Repeticao = Repeticao.NAO,
)
