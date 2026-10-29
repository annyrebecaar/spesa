package com.example.spesa.model

data class Produto(
    var id: Int = 0,
    var nome: String = "",
    var preco: Double = 0.0,
    var promocao: Boolean = false,
    var quantidade: Int = 0,
    var categoria: Categoria = Categoria.OUTROS,
    var dataVencimento: String = "",
    var vencimento: Boolean = false,
    var imagemUrl: String = "",
    var descricao: String = "",
    var favorito: Boolean = false
)

enum class Categoria(val codigo: Int) {
    ALIMENTOS(0),
    BEBIDAS(1),
    HORTIFRUTI(2),
    LIMPEZA(3),
    HIGIENE(4),
    PADARIA(5),
    ACOUGUE(6),
    BELEZA(7),
    OUTROS(8)
}