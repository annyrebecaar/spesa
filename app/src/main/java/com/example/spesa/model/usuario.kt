package com.example.spesa.model

data class Usuario(
    var id: Int = 0,
    var nome: String = "",
    var nickName: String = "",
    var email: String = "",
    var senha: String = "",
    var imagemUrl: String = "",
    var listasDeCompras: List<ListaDeCompras> = emptyList(),
    var cargo: ECargos = ECargos.MEMBRO
)
enum class ECargos(val codigo: Int) {
    ADMIN(0),
    MEMBRO(1)
}
