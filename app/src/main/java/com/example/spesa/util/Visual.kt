package com.example.spesa.util

import android.content.res.ColorStateList
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.spesa.model.Categoria

/** Atalhos visuais usados em várias telas (funções de extensão). */

fun View.visivel(mostrar: Boolean) {
    visibility = if (mostrar) View.VISIBLE else View.GONE
}

/** Preenche uma etiqueta com o nome e a cor da categoria. */
fun TextView.mostrarCategoria(categoria: Categoria) {
    text = categoria.rotulo
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, categoria.corRes))
}

/** Mostra a etiqueta de um produto (as criadas pelo usuário ficam na cor de "Outros"). */
fun TextView.mostrarEtiqueta(produto: com.example.spesa.model.Produto) {
    mostrarCategoria(if (produto.etiqueta.isBlank()) produto.categoria else Categoria.OUTROS)
    text = produto.nomeEtiqueta()
}

/** Troca a cor de uma barra de progresso (verde, amarelo, vermelho...). */
fun ProgressBar.pintar(corRes: Int) {
    progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, corRes))
}
