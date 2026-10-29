package com.example.spesa.model

/**
 * Liga um produto a um mercado: "no mercado X esse produto custa Y".
 * Uma lista disso dentro do Produto responde "onde encontrar e por quanto".
 */
data class PrecoNoMercado(
    val mercadoId: Long,
    var preco: Double = 0.0,
    var promocao: Boolean = false,
    var corredor: String = "",          // ex.: "corredor 5, perto dos laticínios"
    var atualizadoEm: Long = System.currentTimeMillis(),
)
