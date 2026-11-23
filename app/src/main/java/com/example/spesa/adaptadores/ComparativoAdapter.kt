package com.example.spesa.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.R
import com.example.spesa.databinding.ItemComparativoBinding
import com.example.spesa.model.Mercado
import com.example.spesa.util.Formato
import com.example.spesa.util.pintar

/** Uma linha do comparativo: quanto a lista custaria em cada mercado. */
data class LinhaComparativo(
    val mercado: Mercado,
    val total: Double,
    val itensSemPreco: Int,     // itens que usaram o preço da lista por não terem preço neste mercado
)

class ComparativoAdapter : RecyclerView.Adapter<ComparativoAdapter.Linha>() {

    private var linhas: List<LinhaComparativo> = emptyList()
    private var maior = 1.0

    fun atualizar(novas: List<LinhaComparativo>) {
        linhas = novas
        maior = novas.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
        notifyDataSetChanged()
    }

    class Linha(val b: ItemComparativoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(ItemComparativoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = linhas.size

    override fun onBindViewHolder(holder: Linha, position: Int) {
        val linha = linhas[position]
        val b = holder.b
        b.nomeMercado.text = linha.mercado.nome
        b.total.text = Formato.reais(linha.total)

        // Barra proporcional ao mercado mais caro (que fica com 100%)
        b.barra.progress = (linha.total / maior * 100).toInt().coerceIn(2, 100)
        b.barra.pintar(
            when (position) {
                0 -> R.color.orcamento_ok                      // o mais barato
                linhas.lastIndex -> R.color.orcamento_estouro  // o mais caro
                else -> R.color.cat_outros
            }
        )

        b.detalhe.text = when {
            position == 0 && linhas.size > 1 -> "Mais barato" + aviso(linha)
            linha.itensSemPreco > 0 -> aviso(linha).removePrefix(" · ")
            else -> "Preço registrado neste mercado"
        }
    }

    private fun aviso(l: LinhaComparativo) =
        if (l.itensSemPreco == 0) "" else " · ${l.itensSemPreco} item(ns) sem preço aqui, usando o da lista"
}
