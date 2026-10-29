package com.example.spesa.adaptadores

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ItemItemListaBinding
import com.example.spesa.model.ItemLista
import com.example.spesa.util.Formato
import com.example.spesa.util.mostrarCategoria
import com.example.spesa.util.visivel

/** Itens dentro de uma lista de compras, com a caixinha de "já peguei". */
class ItensAdapter(
    private val aoMarcar: (ItemLista, Boolean) -> Unit,
    private val aoClicar: (ItemLista) -> Unit,
    private val aoSegurar: (ItemLista) -> Unit,
) : RecyclerView.Adapter<ItensAdapter.Linha>() {

    private var itens: List<ItemLista> = emptyList()

    fun atualizar(novos: List<ItemLista>) {
        // Itens já no carrinho descem para o fim da lista
        itens = novos.sortedBy { it.noCarrinho }
        notifyDataSetChanged()
    }

    class Linha(val b: ItemItemListaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(ItemItemListaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(holder: Linha, position: Int) {
        val item = itens[position]
        val b = holder.b

        b.nome.text = item.nome
        b.detalhe.text = "${item.quantidade} × ${Formato.reais(item.precoUnitario)}"
        b.total.text = Formato.reais(item.total())
        b.tagCategoria.mostrarCategoria(item.categoria)
        b.tagPromo.visivel(item.promocao)

        val mercado = Repositorio.mercado(item.mercadoId)
        b.mercado.visivel(mercado != null)
        b.mercado.text = mercado?.nome

        // Riscado e mais apagado quando já está no carrinho
        b.marcado.isChecked = item.noCarrinho
        b.nome.paintFlags = if (item.noCarrinho)
            b.nome.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            b.nome.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        b.root.alpha = if (item.noCarrinho) 0.55f else 1f

        b.marcado.setOnClickListener { aoMarcar(item, b.marcado.isChecked) }
        b.root.setOnClickListener { aoClicar(item) }
        b.root.setOnLongClickListener { aoSegurar(item); true }
    }
}
