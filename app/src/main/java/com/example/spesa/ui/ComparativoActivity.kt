package com.example.spesa.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.adaptadores.ComparativoAdapter
import com.example.spesa.adaptadores.LinhaComparativo
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityComparativoBinding
import com.example.spesa.util.Formato
import com.example.spesa.util.visivel
import android.widget.TextView
import android.widget.Toast
import com.example.spesa.R
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Mercado

/** Quanto a lista custaria em cada mercado cadastrado, e quanto se economiza no mais barato. */
class ComparativoActivity : AppCompatActivity() {

    private lateinit var b: ActivityComparativoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityComparativoBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        val lista = Repositorio.lista(intent.getLongExtra(ListaActivity.EXTRA_LISTA_ID, -1)) ?: run { finish(); return }
        b.toolbar.setNavigationOnClickListener { finish() }
        b.subtitulo.text = "${lista.titulo} · ${lista.itens.size} itens"

        // Para cada mercado: total da lista lá + quantos itens não têm preço registrado nele
        val linhas = Repositorio.comparativo(lista).map { (mercado, total) ->
            val semPreco = lista.itens.count { item ->
                item.produtoId?.let { Repositorio.produto(it) }?.precoEm(mercado.id) == null
            }
            LinhaComparativo(mercado, total, semPreco)
        }

        val adapter = ComparativoAdapter()
        b.listaComparativo.layoutManager = LinearLayoutManager(this)
        b.listaComparativo.adapter = adapter
        adapter.atualizar(linhas)

        val daParaComparar = linhas.size >= 2 && lista.itens.isNotEmpty()
        b.textoVazio.visivel(linhas.isEmpty() || lista.itens.isEmpty())
        b.cartaoEconomia.visivel(daParaComparar)
        if (daParaComparar) {
            val maisBarato = linhas.first()
            val maisCaro = linhas.last()
            b.economiaTitulo.text = "Comprando tudo no ${maisBarato.mercado.nome} você economiza"
            b.economiaValor.text = Formato.reais(maisCaro.total - maisBarato.total)
            b.economiaDetalhe.text = "em relação ao ${maisCaro.mercado.nome} (${Formato.reais(maisCaro.total)})"
            mostrarDiferencas(lista, maisBarato.mercado, maisCaro.mercado)
        }
        b.blocoDiferencas.visivel(daParaComparar)
    }

    /** tabela com os itens que mais mudam de preço de um mercado pro outro */
    private fun mostrarDiferencas(lista: ListaDeCompras, barato: Mercado, caro: Mercado) {
        b.colunaA.text = barato.nome.take(12).uppercase()
        b.colunaB.text = caro.nome.take(12).uppercase()
        val linhas = lista.itens.mapNotNull { item ->
            val produto = item.produtoId?.let { Repositorio.produto(it) } ?: return@mapNotNull null
            val pa = produto.precoEm(barato.id)?.preco ?: return@mapNotNull null
            val pb = produto.precoEm(caro.id)?.preco ?: return@mapNotNull null
            Triple(item, pa * item.quantidade, pb * item.quantidade)
        }.sortedByDescending { kotlin.math.abs(it.third - it.second) }.take(5)

        b.diferencas.removeAllViews()
        linhas.forEach { (item, a, bb) ->
            val v = layoutInflater.inflate(R.layout.item_planilha, b.diferencas, false)
            v.findViewById<TextView>(R.id.dia).visivel(false)
            v.findViewById<TextView>(R.id.descricao).text = item.nome
            v.findViewById<TextView>(R.id.categoria).text = "${item.quantidade} un."
            // o mais barato de cada linha fica verde
            val ta = v.findViewById<TextView>(R.id.entrou)
            val tb = v.findViewById<TextView>(R.id.saiu)
            ta.text = Formato.reais(a); tb.text = Formato.reais(bb)
            ta.setTextColor(getColor(if (a <= bb) R.color.entrada else R.color.texto_suave))
            tb.setTextColor(getColor(if (bb < a) R.color.entrada else R.color.texto_suave))
            ta.layoutParams.width = (86 * resources.displayMetrics.density).toInt()
            tb.layoutParams.width = ta.layoutParams.width
            b.diferencas.addView(v)
        }

        b.usarMaisBarato.text = "Usar preços do ${barato.nome}"
        b.usarMaisBarato.setOnClickListener {
            lista.itens.forEach { item ->
                val preco = item.produtoId?.let { Repositorio.produto(it) }?.precoEm(barato.id)
                if (preco != null) {
                    item.precoUnitario = preco.preco
                    item.promocao = preco.promocao
                    item.mercadoId = barato.id
                }
            }
            Repositorio.salvar()
            Toast.makeText(this, "Lista atualizada com os preços do ${barato.nome}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
