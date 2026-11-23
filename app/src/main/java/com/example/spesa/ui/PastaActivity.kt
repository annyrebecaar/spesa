package com.example.spesa.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityPastaBinding
import com.example.spesa.model.Pasta
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel
import android.content.res.ColorStateList
import android.graphics.Paint
import android.text.Html
import com.example.spesa.databinding.ItemCartaoListaBinding
import com.example.spesa.databinding.ItemItemListaBinding
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.util.Datas

/** Pasta aberta: as listas que estão dentro dela e o botão de criar uma nova lista ali. */
class PastaActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PASTA_ID = "pasta_id"
    }

    private lateinit var b: ActivityPastaBinding
    private lateinit var pasta: Pasta

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPastaBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)
        pasta = Repositorio.pasta(intent.getLongExtra(EXTRA_PASTA_ID, -1)) ?: run { finish(); return }

        b.voltar.setOnClickListener { finish() }
        b.editar.setOnClickListener {
            Telas.menuDaPasta(this, pasta) {
                // Se a pasta foi excluída, volta para a tela anterior.
                if (Repositorio.pasta(pasta.id) == null) finish() else atualizar()
            }
        }
        b.novaLista.setOnClickListener { Telas.dialogoLista(this, null, pasta.id) { atualizar() } }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        val listas = Repositorio.listasDaPasta(pasta.id)
        b.nome.text = pasta.nome
        b.iconePasta.setColorFilter(Telas.cor(this, pasta.cor.corRes))
        b.resumo.text = "${listas.size} ${if (listas.size == 1) "lista" else "listas"} · ${Formato.reais(listas.sumOf { it.totalEstimado() })} no total"
        b.listas.removeAllViews()
        listas.forEach { cartao(it) }
        b.vazio.visivel(listas.isEmpty())
    }

    /** cartão da lista: barra com datas na cor da pasta e os 4 primeiros itens (dá pra marcar aqui mesmo) */
    private fun cartao(lista: ListaDeCompras) {
        val c = ItemCartaoListaBinding.inflate(layoutInflater, b.listas, true)
        c.barra.backgroundTintList = ColorStateList.valueOf(Telas.cor(this, pasta.cor.corRes))
        val corTexto = Telas.cor(this, if (pasta.cor.textoClaro) R.color.white else R.color.texto)
        listOf(c.dataCriada, c.dataCompra, c.abrir).forEach { it.setTextColor(corTexto) }

        c.dataCriada.text = Datas.diaSemanaData(lista.criadaEm)
        val compra = Repositorio.dataDeCompra(lista.id)
        c.dataCompra.visivel(compra != null)
        compra?.let { c.dataCompra.text = "Compra: ${Datas.diaSemanaCurtoDia(it)}" }
        c.titulo.text = lista.titulo

        // itens que faltam primeiro, os do carrinho depois
        val ordenados = lista.itens.sortedBy { it.noCarrinho }
        ordenados.take(4).forEach { item ->
            val l = ItemItemListaBinding.inflate(layoutInflater, c.itens, true)
            l.nome.text = item.nome
            l.detalhe.text = "${item.quantidade} × ${Formato.reais(item.precoUnitario)}"
            l.total.text = Formato.reais(item.total())
            l.tagPromo.visivel(item.promocao)
            l.tagCategoria.visivel(false)
            l.marcado.isChecked = item.noCarrinho
            if (item.noCarrinho) {
                l.nome.paintFlags = l.nome.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                l.root.alpha = 0.55f
            }
            l.marcado.setOnClickListener {
                item.noCarrinho = l.marcado.isChecked
                Repositorio.salvar()
                atualizar()
            }
            l.root.setOnClickListener { Telas.abrirLista(this, lista.id) }
        }
        val resto = lista.itens.size - 4
        c.mais.visivel(resto > 0 || lista.itens.isEmpty())
        c.mais.text = if (lista.itens.isEmpty()) "lista vazia: toque para adicionar itens" else "+ $resto ${if (resto == 1) "item" else "itens"}"

        c.carrinho.text = Html.fromHtml("No carrinho <b>${Formato.reais(lista.totalNoCarrinho())}</b>", 0)
        c.total.text = Html.fromHtml("Total <b>${Formato.reais(lista.totalEstimado())}</b>", 0)

        c.root.setOnClickListener { Telas.abrirLista(this, lista.id) }
        c.root.setOnLongClickListener { Telas.menuDaLista(this, lista) { atualizar() }; true }
    }
}
