package com.example.spesa.model

/**
 * Produto do catálogo ("despensa"). Guarda as informações fixas do produto
 * e a lista de preços em cada mercado onde ele foi encontrado.
 */
data class Produto(
    val id: Long = System.currentTimeMillis(),
    var nome: String = "",
    var marca: String = "",
    var categoria: Categoria = Categoria.OUTROS,
    var descricao: String = "",
    var favorito: Boolean = false,
    var precos: MutableList<PrecoNoMercado> = mutableListOf(),
) {
    /** O menor preço registrado entre todos os mercados, ou null se não há nenhum. */
    fun menorPreco(): PrecoNoMercado? = precos.minByOrNull { it.preco }

    /** Preço deste produto num mercado específico, ou null se não foi registrado lá. */
    fun precoEm(mercadoId: Long): PrecoNoMercado? = precos.find { it.mercadoId == mercadoId }
}
