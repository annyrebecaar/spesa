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

    private lateinit var b: ActivityProdutosBinding
    private lateinit var adapter: ProdutosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProdutosBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        b.toolbar.setNavigationOnClickListener { finish() }

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
        val busca = b.campoBusca.text.toString().trim()
        val filtrados = Repositorio.produtos
            .filter { busca.isEmpty() || it.nome.contains(busca, ignoreCase = true) || it.marca.contains(busca, ignoreCase = true) }
            .sortedWith(compareByDescending<Produto> { it.favorito }.thenBy { it.nome.lowercase() })
        adapter.atualizar(filtrados)
        b.textoVazio.visivel(filtrados.isEmpty())
    }

    private fun abrirFormulario(produto: Produto?) {
        val intent = Intent(this, ProdutoFormActivity::class.java)
        if (produto != null) intent.putExtra(ProdutoFormActivity.EXTRA_PRODUTO_ID, produto.id)
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
