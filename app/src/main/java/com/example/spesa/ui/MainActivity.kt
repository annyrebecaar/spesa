package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.R
import com.example.spesa.adaptadores.ListasAdapter
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityMainBinding
import com.example.spesa.databinding.DialogNovaListaBinding
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.util.Formato
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

/** Tela principal: as listas ativas, o histórico e os atalhos para Despensa e Mercados. */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: ListasAdapter
    private var mostrandoHistorico = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        adapter = ListasAdapter(aoClicar = ::abrirLista, aoSegurar = ::menuDaLista)
        b.listaDeListas.layoutManager = LinearLayoutManager(this)
        b.listaDeListas.adapter = adapter

        b.abas.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                mostrandoHistorico = tab.position == 1
                atualizar()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        b.botaoNovaLista.setOnClickListener { dialogoLista(null) }
        b.botaoDespensa.setOnClickListener { startActivity(Intent(this, ProdutosActivity::class.java)) }
        b.botaoMercados.setOnClickListener { startActivity(Intent(this, MercadosActivity::class.java)) }
    }

    /** Toda vez que voltamos para esta tela, recarregamos (algo pode ter mudado). */
    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        val listas = Repositorio.listas.filter { it.finalizada == mostrandoHistorico }
        adapter.atualizar(listas)
        b.textoVazio.visivel(listas.isEmpty())
        b.textoVazio.setText(if (mostrandoHistorico) R.string.sem_historico else R.string.sem_listas)
        b.botaoNovaLista.visivel(!mostrandoHistorico)
    }

    private fun abrirLista(lista: ListaDeCompras) {
        startActivity(Intent(this, ListaActivity::class.java).putExtra(ListaActivity.EXTRA_LISTA_ID, lista.id))
    }

    /** Cria uma lista nova (lista == null) ou edita título/orçamento de uma existente. */
    private fun dialogoLista(lista: ListaDeCompras?) {
        val campos = DialogNovaListaBinding.inflate(layoutInflater)
        if (lista != null) {
            campos.campoTitulo.setText(lista.titulo)
            if (lista.orcamento > 0) campos.campoOrcamento.setText(lista.orcamento.toString().replace(".", ","))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(if (lista == null) R.string.nova_lista else R.string.editar)
            .setView(campos.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(if (lista == null) R.string.criar else R.string.salvar) { _, _ ->
                val titulo = campos.campoTitulo.text.toString().trim().ifBlank { "Minha lista" }
                val orcamento = Formato.lerReais(campos.campoOrcamento.text.toString())
                if (lista == null) {
                    val nova = ListaDeCompras(titulo = titulo, orcamento = orcamento)
                    Repositorio.adicionarLista(nova)
                    abrirLista(nova)
                } else {
                    lista.titulo = titulo
                    lista.orcamento = orcamento
                    Repositorio.salvar()
                    atualizar()
                }
            }
            .show()
    }

    /** Menu que aparece ao segurar o dedo num cartão de lista. */
    private fun menuDaLista(lista: ListaDeCompras) {
        val opcoes = arrayOf(
            getString(R.string.editar),
            getString(R.string.duplicar),
            getString(if (lista.finalizada) R.string.reabrir else R.string.finalizar),
            getString(R.string.excluir),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(lista.titulo)
            .setItems(opcoes) { _, qual ->
                when (qual) {
                    0 -> dialogoLista(lista)
                    1 -> {
                        Repositorio.adicionarLista(lista.clonar("${lista.titulo} (cópia)"))
                        b.abas.getTabAt(0)?.select()
                        atualizar()
                    }
                    2 -> {
                        lista.finalizada = !lista.finalizada
                        Repositorio.salvar()
                        atualizar()
                    }
                    3 -> confirmarExclusao(lista)
                }
            }
            .show()
    }

    private fun confirmarExclusao(lista: ListaDeCompras) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.excluir)
            .setMessage("Excluir a lista \"${lista.titulo}\"? Isso não pode ser desfeito.")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                Repositorio.removerLista(lista.id)
                atualizar()
            }
            .show()
    }
}
