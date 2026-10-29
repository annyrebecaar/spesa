package com.example.spesa.model

data class ListaCompra(
    var id: Int = 0,
    var titulo: String = "",
    var dataCriacao: String = "",
    var finalizada: Boolean = false,
    var valorTotal: Double = 0.0,
    var itens: List<ItemLista> = emptyList(),
)
