package com.example.spesa.adaptadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ItemMercadoBinding
import com.example.spesa.model.Mercado
import com.example.spesa.util.visivel

class MercadosAdapter(
    private val aoClicar: (Mercado) -> Unit,
    private val aoMenu: (Mercado) -> Unit,
) : RecyclerView.Adapter<MercadosAdapter.Linha>() {

    private var mercados: List<Mercado> = emptyList()

    fun atualizar(novos: List<Mercado>) {
        mercados = novos
        notifyDataSetChanged()
    }

    class Linha(val b: ItemMercadoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Linha =
        Linha(ItemMercadoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = mercados.size

    override fun onBindViewHolder(holder: Linha, position: Int) {
        val mercado = mercados[position]
        val b = holder.b
        b.nome.text = mercado.nome
        b.endereco.text = mercado.endereco.ifBlank { mercado.observacao }
        b.endereco.visivel(b.endereco.text.isNotBlank())

        val quantos = Repositorio.produtos.count { p -> p.precos.any { it.mercadoId == mercado.id } }
        b.resumo.text = when (quantos) {
            0 -> "Nenhum produto registrado aqui"
            1 -> "1 produto registrado"
            else -> "$quantos produtos registrados"
        }

        b.root.setOnClickListener { aoClicar(mercado) }
        b.mais.setOnClickListener { aoMenu(mercado) }
    }
}
