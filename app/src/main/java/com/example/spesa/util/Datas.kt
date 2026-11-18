package com.example.spesa.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Contas com datas usadas em Finanças e Agenda. Tudo em milissegundos. */
object Datas {
    private val brasil = Locale("pt", "BR")

    fun inicioDoDia(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun mesmoDia(a: Long, b: Long) = inicioDoDia(a) == inicioDoDia(b)

    /** Primeiro dia do mês de "millis", às 0h. */
    fun inicioDoMes(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDoDia(millis); set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    /** Soma (ou subtrai) meses. */
    fun somarMeses(millis: Long, meses: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis; add(Calendar.MONTH, meses)
    }.timeInMillis

    fun somarDias(millis: Long, dias: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis; add(Calendar.DAY_OF_MONTH, dias)
    }.timeInMillis

    /** Domingo da semana de "millis", às 0h. */
    fun inicioDaSemana(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDoDia(millis); set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }.timeInMillis

    private fun fmt(padrao: String, millis: Long): String =
        SimpleDateFormat(padrao, brasil).format(Date(millis))

    /** "Novembro 2026" */
    fun mesAno(millis: Long) = fmt("MMMM yyyy", millis).replaceFirstChar { it.uppercase() }
    /** "Nov 2026" */
    fun mesAnoCurto(millis: Long) = fmt("MMM yyyy", millis).replace(".", "").replaceFirstChar { it.uppercase() }
    /** "Novembro" */
    fun mes(millis: Long) = fmt("MMMM", millis).replaceFirstChar { it.uppercase() }
    /** "Quarta, 18 de novembro" */
    fun diaPorExtenso(millis: Long) = fmt("EEEE, d 'de' MMMM", millis)
        .replace("-feira", "").replaceFirstChar { it.uppercase() }
    /** "Sábado, 21 de novembro" */
    fun diaCompleto(millis: Long) = diaPorExtenso(millis)
    /** "QUI" */
    fun diaSemanaCurto(millis: Long) = fmt("EEE", millis).replace(".", "").uppercase()
    /** "19" */
    fun dia(millis: Long) = fmt("d", millis)
    /** "18 nov 2026" */
    fun curta(millis: Long) = fmt("d MMM yyyy", millis).replace(".", "")
    /** "17 nov" */
    fun diaMes(millis: Long) = fmt("dd MMM", millis).replace(".", "")
    /** "09:00" */
    fun hora(millis: Long) = fmt("HH:mm", millis)

    /** "há 2 h", "ontem", "17 nov" */
    fun haQuanto(millis: Long): String {
        val minutos = (System.currentTimeMillis() - millis) / 60000
        return when {
            minutos < 1 -> "agora"
            minutos < 60 -> "há $minutos min"
            minutos < 24 * 60 && mesmoDia(millis, System.currentTimeMillis()) -> "há ${minutos / 60} h"
            mesmoDia(millis, somarDias(System.currentTimeMillis(), -1)) -> "ontem"
            else -> diaMes(millis)
        }
    }
}
