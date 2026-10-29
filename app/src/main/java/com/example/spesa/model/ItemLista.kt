package com.example.spesa.model

/**
 * Um item dentro de uma lista de compras.
 * Guarda uma cópia do nome/categoria (e não só o id do produto) para a lista
 * continuar fazendo sentido mesmo se o produto for apagado do catálogo.
 */
data class ItemLista(
    val id: Long = System.currentTimeMillis(),
    var produtoId: Long? = null,        // null quando o item foi digitado direto, sem catálogo
    var nome: String = "",
    var categoria: Categoria = Categoria.OUTROS,
    var quantidade: Int = 1,
    var precoUnitario: Double = 0.0,
    var promocao: Boolean = false,
    var noCarrinho: Boolean = false,
    var mercadoId: Long? = null,        // onde pretendo comprar este item
) {
    /** Quanto este item custa no total (preço × quantidade). */
    fun total(): Double = precoUnitario * quantidade
}
