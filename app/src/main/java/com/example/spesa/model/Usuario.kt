package com.example.spesa.model

/**
 * A conta de quem usa o app. Fica guardada só no celular (não há servidor).
 * A senha é guardada como "hash" (um código embaralhado), nunca o texto puro.
 */
data class Usuario(
    var nome: String = "",
    var email: String = "",
    var senhaHash: String = "",
)

/** Uma linha de "Atividade recente" na tela de Início. */
data class Atividade(
    val texto: String = "",
    val quando: Long = System.currentTimeMillis(),
    val tipo: String = "check",        // check, mais ou saida (escolhe o ícone)
)
