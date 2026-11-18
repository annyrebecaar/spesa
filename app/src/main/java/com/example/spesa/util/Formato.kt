package com.example.spesa.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Funções pequenas de formatação usadas em várias telas. */
object Formato {
    private val brasil = Locale("pt", "BR")
    private val moeda = NumberFormat.getCurrencyInstance(brasil)
    private val data = SimpleDateFormat("dd/MM/yyyy", brasil)

    /** 12.5 -> "R$ 12,50" */
    fun reais(valor: Double): String = moeda.format(valor)

    /** Lê o que o usuário digitou aceitando vírgula ou ponto: "12,50" -> 12.5 */
    fun lerReais(texto: String): Double {
        var t = texto.replace("R$", "").trim()
        // "1.234,56": o ponto é separador de milhar, então tiramos ele antes
        if (t.contains(",") && t.contains(".")) t = t.replace(".", "")
        return t.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    fun data(millis: Long): String = data.format(Date(millis))
}
