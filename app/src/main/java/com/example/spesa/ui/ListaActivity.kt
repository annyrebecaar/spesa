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
                atualizar()
            },
            aoClicar = { item -> abrirFormulario(item.id) },
            aoSegurar = ::confirmarExclusao,
        )
        b.listaItens.layoutManager = LinearLayoutManager(this)
        b.listaItens.adapter = adapter

        b.botaoNovoItem.setOnClickListener { abrirFormulario(null) }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        b.toolbar.title = lista.titulo
        adapter.atualizar(lista.itens)
        b.textoVazio.visivel(lista.itens.isEmpty())

        // Rodapé
        b.valorCarrinho.text = Formato.reais(lista.totalNoCarrinho())
        b.valorFalta.text = "${getString(R.string.falta)}: ${Formato.reais(lista.totalRestante())}"
        b.valorTotal.text = "${getString(R.string.total)}: ${Formato.reais(lista.totalEstimado())}"

        // Teto de gastos: verde até 80%, amarelo até 100%, vermelho se estourar
        b.blocoOrcamento.visivel(lista.orcamento > 0)
        if (lista.orcamento > 0) {
            val gasto = lista.totalNoCarrinho()
            val pct = (gasto / lista.orcamento * 100).toInt()
            b.barraOrcamento.progress = pct.coerceIn(0, 100)
            when {
                pct > 100 -> {
                    b.barraOrcamento.pintar(R.color.orcamento_estouro)
                    b.textoOrcamento.text = "Estourou o teto de ${Formato.reais(lista.orcamento)} em ${Formato.reais(gasto - lista.orcamento)}"
                }
                pct >= 80 -> {
                    b.barraOrcamento.pintar(R.color.orcamento_atencao)
                    b.textoOrcamento.text = "Atenção: $pct% do teto de ${Formato.reais(lista.orcamento)}"
                }
                else -> {
                    b.barraOrcamento.pintar(R.color.orcamento_ok)
                    b.textoOrcamento.text = "$pct% do teto de ${Formato.reais(lista.orcamento)}"
                }
            }
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
            R.id.acao_comparar -> startActivity(
                Intent(this, ComparativoActivity::class.java).putExtra(EXTRA_LISTA_ID, lista.id)
            )
            R.id.acao_finalizar -> {
                lista.finalizada = true
                Repositorio.salvar()
                Toast.makeText(this, "Lista finalizada e guardada no histórico", Toast.LENGTH_SHORT).show()
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
