package com.example.spesa.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ItemPrecoMercadoBinding
import com.example.spesa.model.PrecoNoMercado
import com.example.spesa.util.Formato
import com.example.spesa.util.visivel

/** "Onde encontrar": um cartão por mercado dentro da tela do produto. */
class PrecosAdapter(
    private val aoClicar: (PrecoNoMercado) -> Unit,
    private val aoSegurar: (PrecoNoMercado) -> Unit,
) : RecyclerView.Adapter<PrecosAdapter.Linha>() {

    private var precos: List<PrecoNoMercado> = emptyList()

    fun atualizar(novos: List<PrecoNoMercado>) {
        precos = novos.sortedBy { it.preco }
        notifyDataSetChanged()
    }

    class Linha(val b: ItemPrecoMercadoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(ItemPrecoMercadoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = precos.size

    override fun onBindViewHolder(holder: Linha, position: Int) {
        val preco = precos[position]
        val b = holder.b
        b.nomeMercado.text = Repositorio.mercado(preco.mercadoId)?.nome ?: "Mercado removido"
        b.preco.text = Formato.reais(preco.preco)
        b.tagPromo.visivel(preco.promocao)
        b.corredor.text = preco.corredor.ifBlank { "Atualizado em ${Formato.data(preco.atualizadoEm)}" }

        b.root.setOnClickListener { aoClicar(preco) }
        b.root.setOnLongClickListener { aoSegurar(preco); true }
    }
}
