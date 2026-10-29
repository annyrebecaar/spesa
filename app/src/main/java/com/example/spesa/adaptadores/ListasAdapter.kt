package com.example.spesa.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.R
import com.example.spesa.databinding.ItemListaComprasBinding
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.util.Formato
import com.example.spesa.util.pintar

/**
 * Mostra os cartões de lista na tela principal.
 * Um Adapter é a "ponte" entre a lista de dados e os cartões na tela:
 * ele cria um cartão (onCreateViewHolder) e preenche cada um (onBindViewHolder).
 */
class ListasAdapter(
    private val aoClicar: (ListaDeCompras) -> Unit,
    private val aoSegurar: (ListaDeCompras) -> Unit,
) : RecyclerView.Adapter<ListasAdapter.Cartao>() {

    private var listas: List<ListaDeCompras> = emptyList()

    fun atualizar(novas: List<ListaDeCompras>) {
        listas = novas
        notifyDataSetChanged()
    }

    class Cartao(val b: ItemListaComprasBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Cartao {
        val b = ItemListaComprasBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Cartao(b)
    }

    override fun getItemCount() = listas.size

    override fun onBindViewHolder(holder: Cartao, position: Int) {
        val lista = listas[position]
        val b = holder.b
        b.titulo.text = lista.titulo
        b.valor.text = Formato.reais(lista.totalEstimado())

        val pegos = lista.quantosNoCarrinho()
        val texto = StringBuilder("${lista.itens.size} itens · $pegos no carrinho")
        if (lista.orcamento > 0) texto.append(" · teto ${Formato.reais(lista.orcamento)}")
        texto.append(" · ${Formato.data(lista.criadaEm)}")
        b.resumo.text = texto

        // Barra: progresso de itens pegos (ou do orçamento, se tiver)
        if (lista.orcamento > 0) {
            val pct = (lista.totalNoCarrinho() / lista.orcamento * 100).toInt()
            b.progresso.progress = pct.coerceIn(0, 100)
            b.progresso.pintar(
                when {
                    pct > 100 -> R.color.orcamento_estouro
                    pct >= 80 -> R.color.orcamento_atencao
                    else -> R.color.orcamento_ok
                }
            )
        } else {
            b.progresso.progress = if (lista.itens.isEmpty()) 0 else pegos * 100 / lista.itens.size
            b.progresso.pintar(R.color.orcamento_ok)
        }

        b.root.setOnClickListener { aoClicar(lista) }
        b.root.setOnLongClickListener { aoSegurar(lista); true }
    }
}
