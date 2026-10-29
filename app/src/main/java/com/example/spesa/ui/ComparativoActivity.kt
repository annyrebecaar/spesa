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
        }
    }
}
