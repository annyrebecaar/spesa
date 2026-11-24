package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.R
import com.example.spesa.adaptadores.ProdutosAdapter
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityProdutosBinding
import com.example.spesa.model.Produto
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Despensa: o catálogo de produtos, com busca e favoritos. */
class ProdutosActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_MERCADO_ID = "mercado_id"
        const val TODAS = "Todas"
    }

    // quando abre de dentro de um mercado, mostra só os produtos dele
    private var mercado: com.example.spesa.model.Mercado? = null

    // etiqueta escolhida na fileira de cima ("Todas" = sem filtro)
    private var etiqueta = TODAS

    private lateinit var b: ActivityProdutosBinding
    private lateinit var adapter: ProdutosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProdutosBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        b.toolbar.setNavigationOnClickListener { finish() }
        mercado = Repositorio.mercado(intent.getLongExtra(EXTRA_MERCADO_ID, -1))
        mercado?.let { b.toolbar.title = it.nome; b.toolbar.subtitle = it.endereco.ifBlank { null } }

        adapter = ProdutosAdapter(
            aoClicar = ::abrirFormulario,
            aoFavoritar = { produto ->
                produto.favorito = !produto.favorito
                Repositorio.salvarProduto(produto)
                atualizar()
            },
            aoSegurar = ::confirmarExclusao,
        )
        b.listaProdutos.layoutManager = LinearLayoutManager(this)
        b.listaProdutos.adapter = adapter

        b.campoBusca.doAfterTextChanged { atualizar() }
        b.botaoNovoProduto.setOnClickListener { abrirFormulario(null) }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        montarEtiquetas()
        val busca = b.campoBusca.text.toString().trim()
        val filtrados = Repositorio.produtos
            .filter { etiqueta == TODAS || it.nomeEtiqueta() == etiqueta }
            .filter { p -> mercado == null || p.precoEm(mercado!!.id) != null }
            .filter { busca.isEmpty() || it.nome.contains(busca, ignoreCase = true) || it.marca.contains(busca, ignoreCase = true) }
            // separa por etiqueta: favoritos primeiro, depois agrupa pela etiqueta e pelo nome
            .sortedWith(compareByDescending<Produto> { it.favorito }.thenBy { it.nomeEtiqueta() }.thenBy { it.nome.lowercase() })
        adapter.atualizar(filtrados)
        b.textoVazio.visivel(filtrados.isEmpty())
    }

    /** um chip por etiqueta que tem produto, com a cor dela */
    private fun montarEtiquetas() {
        val doMercado = Repositorio.produtos.filter { p -> mercado == null || p.precoEm(mercado!!.id) != null }
        val contagem = doMercado.groupingBy { it.nomeEtiqueta() }.eachCount()
        val nomes = listOf(TODAS) + Repositorio.todasEtiquetas().filter { (contagem[it] ?: 0) > 0 }
        if (etiqueta !in nomes) etiqueta = TODAS
        b.etiquetas.removeAllViews()
        nomes.forEach { nome ->
            val chip = com.example.spesa.util.Telas.chip(b.etiquetas, nome, nome == etiqueta)
            // bolinha com a cor da etiqueta ("Todas" não tem cor)
            if (nome != TODAS) {
                val pronta = com.example.spesa.model.Categoria.entries.find { it.rotulo == nome }
                val cor = getColor(pronta?.corRes ?: R.color.cat_outros)
                chip.chipIcon = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(cor)
                    setSize(36, 36)
                }
                chip.isChipIconVisible = true
            }
            chip.setOnClickListener { etiqueta = nome; atualizar() }
        }
    }

    private fun abrirFormulario(produto: Produto?) {
        val intent = Intent(this, ProdutoFormActivity::class.java)
        if (produto != null) intent.putExtra(ProdutoFormActivity.EXTRA_PRODUTO_ID, produto.id)
        mercado?.let { intent.putExtra(ProdutoFormActivity.EXTRA_MERCADO_ID, it.id) }
        startActivity(intent)
    }

    private fun confirmarExclusao(produto: Produto) {
        MaterialAlertDialogBuilder(this)
            .setTitle(produto.nome)
            .setMessage("Excluir este produto da despensa? Os itens que já estão em listas continuam lá.")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                Repositorio.removerProduto(produto.id)
                atualizar()
            }
            .show()
    }
}
