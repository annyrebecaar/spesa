package com.example.spesa.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ItemProdutoBinding
import com.example.spesa.model.Produto
import com.example.spesa.util.Formato
import com.example.spesa.util.mostrarCategoria
import com.example.spesa.util.visivel

class ProdutosAdapter(
    private val aoClicar: (Produto) -> Unit,
    private val aoFavoritar: (Produto) -> Unit,
    private val aoSegurar: (Produto) -> Unit,
) : RecyclerView.Adapter<ProdutosAdapter.Linha>() {

    private var produtos: List<Produto> = emptyList()

    fun atualizar(novos: List<Produto>) {
        produtos = novos
        notifyDataSetChanged()
    }

    class Linha(val b: ItemProdutoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(ItemProdutoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = produtos.size

    override fun onBindViewHolder(holder: Linha, position: Int) {
        val produto = produtos[position]
        val b = holder.b

        b.nome.text = if (produto.marca.isBlank()) produto.nome else "${produto.nome} · ${produto.marca}"
        b.tagCategoria.mostrarCategoria(produto.categoria)

        val menor = produto.menorPreco()
        val mercado = Repositorio.mercado(menor?.mercadoId)
        b.ondeEncontrar.text = when {
            menor == null -> "Sem preço registrado"
            mercado == null -> "Mais barato: ${Formato.reais(menor.preco)}"
            produto.precos.size == 1 -> "${Formato.reais(menor.preco)} no ${mercado.nome}"
            else -> "Mais barato: ${Formato.reais(menor.preco)} no ${mercado.nome} (${produto.precos.size} mercados)"
        }
        b.tagPromo.visivel(produto.precos.any { it.promocao })

        b.botaoFavorito.setImageResource(
            if (produto.favorito) R.drawable.ic_star else R.drawable.ic_star_border
        )

        b.botaoFavorito.setOnClickListener { aoFavoritar(produto) }
        b.root.setOnClickListener { aoClicar(produto) }
        b.root.setOnLongClickListener { aoSegurar(produto); true }
    }
}
