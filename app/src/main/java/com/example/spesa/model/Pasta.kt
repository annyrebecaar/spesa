package com.example.spesa.model

import com.example.spesa.R

/** As cores que uma pasta pode ter. corEscura é a "orelha" da pasta; texto é a cor das letras. */
enum class CorPasta(val rotulo: String, val corRes: Int, val textoClaro: Boolean) {
    VERDE("Verde", R.color.pasta_verde, true),
    VERMELHA("Vermelha", R.color.pasta_vermelha, true),
    AMARELA("Amarela", R.color.pasta_amarela, false),
    SALVIA("Sálvia", R.color.pasta_salvia, true),
}

/** Uma pasta agrupa várias listas (ex.: "Mercado do mês", "Festas e churrascos"). */
data class Pasta(
    val id: Long = System.currentTimeMillis(),
    var nome: String = "",
    var cor: CorPasta = CorPasta.VERDE,
)
