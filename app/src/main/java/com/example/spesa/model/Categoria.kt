package com.example.spesa.model

import com.example.spesa.R

/**
 * Categorias de produto. Cada uma tem um nome para mostrar na tela
 * e uma cor (definida em res/values/colors.xml) para a etiqueta.
 */
enum class Categoria(val rotulo: String, val corRes: Int) {
    ALIMENTOS("Alimentos", R.color.cat_alimentos),
    BEBIDAS("Bebidas", R.color.cat_bebidas),
    HORTIFRUTI("Hortifruti", R.color.cat_hortifruti),
    LIMPEZA("Limpeza", R.color.cat_limpeza),
    HIGIENE("Higiene", R.color.cat_higiene),
    PADARIA("Padaria", R.color.cat_padaria),
    ACOUGUE("Açougue", R.color.cat_acougue),
    BELEZA("Beleza", R.color.cat_beleza),
    OUTROS("Outros", R.color.cat_outros);

    companion object {
        /** Lista só com os nomes, para preencher spinners (caixas de seleção). */
        fun rotulos(): List<String> = entries.map { it.rotulo }
    }
}
