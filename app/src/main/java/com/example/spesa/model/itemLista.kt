package com.example.spesa.model

data class ItemLista(
    var id: Int = 0,
    var produto: Produto = Produto(),
    var quantidade: Int = 1,
    var precoUnitario: Double = 0.0,
    var precoTotal: Double = 0.0,
    var desconto: Double = 0.0,
    var noCarrinho: Boolean = false,
)