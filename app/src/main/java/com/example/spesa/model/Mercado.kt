package com.example.spesa.model

/**
 * Um estabelecimento onde se compra: supermercado, feira, padaria, loja...
 * É aqui que o usuário registra "onde encontrar" cada produto.
 */
data class Mercado(
    val id: Long = System.currentTimeMillis(),
    var nome: String = "",
    var endereco: String = "",
    var observacao: String = "",
)
