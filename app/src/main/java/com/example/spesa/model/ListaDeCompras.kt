package com.example.spesa.model

data class ListaDeCompras(
    val id: Long = System.currentTimeMillis(),
    var titulo: String = "",
    var criadaEm: Long = System.currentTimeMillis(),
    var finalizada: Boolean = false,
    var orcamento: Double = 0.0,        // 0 = sem teto de gastos
    var itens: MutableList<ItemLista> = mutableListOf(),
) {
    /** Soma de tudo que já está no carrinho. */
    fun totalNoCarrinho(): Double = itens.filter { it.noCarrinho }.sumOf { it.total() }

    /** Soma do que ainda falta pegar. */
    fun totalRestante(): Double = itens.filter { !it.noCarrinho }.sumOf { it.total() }

    /** Soma de todos os itens, pegos ou não. */
    fun totalEstimado(): Double = itens.sumOf { it.total() }

    fun quantosNoCarrinho(): Int = itens.count { it.noCarrinho }

    /**
     * Cópia desta lista com novo id, novo título e todos os itens desmarcados.
     * Usada para "clonar" uma lista antiga sem redigitar tudo.
     */
    fun clonar(novoTitulo: String): ListaDeCompras = ListaDeCompras(
        titulo = novoTitulo,
        orcamento = orcamento,
        itens = itens.map { it.copy(id = System.nanoTime(), noCarrinho = false) }.toMutableList(),
    )
}
