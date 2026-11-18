package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.R
import com.example.spesa.adaptadores.ItensAdapter
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityListaBinding
import com.example.spesa.model.ItemLista
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.util.Formato
import com.example.spesa.util.pintar
import com.example.spesa.util.visivel
import android.widget.TextView
import com.example.spesa.databinding.SheetFinalizarBinding
import com.example.spesa.model.CategoriaGasto
import com.example.spesa.model.Lancamento
import com.example.spesa.util.Telas
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Tela de uma lista: os itens, a caixinha "já peguei" e o rodapé com a soma em tempo real. */
class ListaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LISTA_ID = "lista_id"
    }

    private lateinit var b: ActivityListaBinding
    private lateinit var adapter: ItensAdapter
    private lateinit var lista: ListaDeCompras

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListaBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        val id = intent.getLongExtra(EXTRA_LISTA_ID, -1)
        lista = Repositorio.lista(id) ?: run { finish(); return }

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        b.toolbar.setNavigationOnClickListener { finish() }

        adapter = ItensAdapter(
            aoMarcar = { item, marcado ->
                item.noCarrinho = marcado
                Repositorio.salvar()
                if (marcado) Repositorio.registrar("Você marcou ${item.nome} em ${lista.titulo}", "check")
                atualizar()
            },
            aoClicar = { item -> abrirFormulario(item.id) },
            aoSegurar = ::confirmarExclusao,
        )
        b.listaItens.layoutManager = LinearLayoutManager(this)
        b.listaItens.adapter = adapter

        b.botaoNovoItem.setOnClickListener { abrirFormulario(null) }
        b.botaoFinalizar.setOnClickListener { finalizarCompra() }
        b.botaoComparar.setOnClickListener {
            startActivity(Intent(this, ComparativoActivity::class.java).putExtra(EXTRA_LISTA_ID, lista.id))
        }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        b.toolbar.title = lista.titulo
        adapter.atualizar(lista.itens)
        b.textoVazio.visivel(lista.itens.isEmpty())

        // Rodapé: no carrinho, falta e total
        b.valorCarrinho.text = Formato.reais(lista.totalNoCarrinho())
        b.valorFalta.text = Formato.reais(lista.totalRestante())
        b.valorTotal.text = Formato.reais(lista.totalEstimado())
        b.botaoFinalizar.isEnabled = !lista.finalizada
        b.botaoFinalizar.text = if (lista.finalizada) "Compra finalizada" else "Finalizar compra"

        // Teto de gastos: verde até 80%, amarelo até 100%, vermelho se estourar
        b.blocoOrcamento.visivel(lista.orcamento > 0)
        if (lista.orcamento > 0) {
            val estimado = lista.totalEstimado()
            val pct = (estimado / lista.orcamento * 100).toInt()
            b.barraOrcamento.progress = pct.coerceIn(0, 100)
            b.textoOrcamento.text = "Orçamento ${Formato.reais(lista.orcamento)}"
            val sobra = lista.orcamento - estimado
            b.textoSobra.text = if (sobra >= 0) "Sobram ${Formato.reais(sobra)}" else "Passou ${Formato.reais(-sobra)}"
            b.barraOrcamento.pintar(when {
                pct > 100 -> R.color.orcamento_estouro
                pct >= 80 -> R.color.orcamento_atencao
                else -> R.color.amarelo
            })
        }
    }

    private fun abrirFormulario(itemId: Long?) {
        val intent = Intent(this, ItemFormActivity::class.java)
            .putExtra(ItemFormActivity.EXTRA_LISTA_ID, lista.id)
        if (itemId != null) intent.putExtra(ItemFormActivity.EXTRA_ITEM_ID, itemId)
        startActivity(intent)
    }

    private fun confirmarExclusao(item: ItemLista) {
        MaterialAlertDialogBuilder(this)
            .setTitle(item.nome)
            .setMessage("Tirar este item da lista?")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                lista.itens.remove(item)
                Repositorio.salvar()
                Repositorio.registrar("Você tirou ${item.nome} de ${lista.titulo}", "check")
                atualizar()
            }
            .show()
    }

    // ---------- Menu da barra superior ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_lista, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.acao_despensa -> adicionarDaDespensa()
            R.id.acao_compartilhar -> compartilhar()
            R.id.acao_editar -> Telas.dialogoLista(this, lista) { atualizar() }
            R.id.acao_comparar -> startActivity(
                Intent(this, ComparativoActivity::class.java).putExtra(EXTRA_LISTA_ID, lista.id)
            )
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /** Manda a lista como texto (WhatsApp, e-mail...), com ✓ no que já foi pego. */
    private fun compartilhar() {
        val texto = buildString {
            appendLine("🛒 ${lista.titulo}")
            lista.itens.forEach { appendLine("${if (it.noCarrinho) "✓" else "•"} ${it.quantidade}× ${it.nome} — ${Formato.reais(it.total())}") }
            append("Total: ${Formato.reais(lista.totalEstimado())}")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Compartilhar lista"))
    }

    /**
     * Janela de "Compra finalizada!": confirma o total, pergunta onde comprou
     * e, se a caixinha estiver marcada, lança o valor como saída nas Finanças.
     */
    private fun finalizarCompra() {
        val f = SheetFinalizarBinding.inflate(layoutInflater)
        val janela = BottomSheetDialog(this)
        janela.setContentView(f.root)

        val total = if (lista.totalNoCarrinho() > 0) lista.totalNoCarrinho() else lista.totalEstimado()
        f.quantos.text = "${lista.quantosNoCarrinho()} de ${lista.itens.size} itens no carrinho"
        f.campoTotal.setText(Formato.reais(total).replace("R$", "").trim())
        f.orcamento.text = if (lista.orcamento > 0)
            "Orçamento ${Formato.reais(lista.orcamento)}\nSobraram ${Formato.reais(lista.orcamento - total)}" else ""
        val chips = Repositorio.mercados.associateWith { Telas.chip(f.mercados, it.nome) }
        chips.values.firstOrNull()?.isChecked = true
        if (chips.isEmpty()) f.mercados.addView(TextView(this).apply { text = "Cadastre mercados em Ajustes > Mercados."; setTextColor(Telas.cor(context, R.color.texto_suave)) })
        f.lancar.text = "Lançar nas Finanças como saída em Mercado"

        f.voltar.setOnClickListener { janela.dismiss() }
        f.concluir.setOnClickListener {
            val pago = Formato.lerReais(f.campoTotal.text.toString())
            val mercado = chips.entries.find { it.value.isChecked }?.key
            lista.finalizada = true
            lista.finalizadaEm = System.currentTimeMillis()
            lista.totalPago = pago
            lista.mercadoCompraId = mercado?.id
            Repositorio.salvar()
            if (f.lancar.isChecked && pago > 0) {
                Repositorio.salvarLancamento(Lancamento(
                    entrada = false, valor = pago, categoria = CategoriaGasto.MERCADO,
                    descricao = listOfNotNull(lista.titulo, mercado?.nome).joinToString(" · "),
                ))
            }
            Repositorio.registrar("Você finalizou ${lista.titulo}: ${Formato.reais(pago)}", "check")
            janela.dismiss()
            Toast.makeText(this, "Compra guardada no Histórico", Toast.LENGTH_SHORT).show()
            finish()
        }
        janela.show()
    }

    /** Mostra os favoritos que ainda não estão na lista e deixa marcar vários de uma vez. */
    private fun adicionarDaDespensa() {
        val nomesNaLista = lista.itens.map { it.nome.lowercase() }
        val candidatos = Repositorio.favoritos().filter { it.nome.lowercase() !in nomesNaLista }
        if (candidatos.isEmpty()) {
            Toast.makeText(this, "Marque produtos como favoritos na Despensa para eles aparecerem aqui", Toast.LENGTH_LONG).show()
            return
        }
        val marcados = BooleanArray(candidatos.size)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.da_despensa)
            .setMultiChoiceItems(candidatos.map { it.nome }.toTypedArray(), marcados) { _, i, marcado ->
                marcados[i] = marcado
            }
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.adicionar) { _, _ ->
                candidatos.forEachIndexed { i, produto ->
                    if (!marcados[i]) return@forEachIndexed
                    val maisBarato = produto.menorPreco()
                    lista.itens.add(
                        ItemLista(
                            id = System.nanoTime(),
                            produtoId = produto.id,
                            nome = produto.nome,
                            categoria = produto.categoria,
                            precoUnitario = maisBarato?.preco ?: 0.0,
                            promocao = maisBarato?.promocao ?: false,
                            mercadoId = maisBarato?.mercadoId,
                        )
                    )
                }
                Repositorio.salvar()
                atualizar()
            }
            .show()
    }
}
