package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.R
import com.example.spesa.adaptadores.PrecosAdapter
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityProdutoFormBinding
import com.example.spesa.databinding.DialogPrecoBinding
import com.example.spesa.model.Categoria
import com.example.spesa.model.PrecoNoMercado
import com.example.spesa.model.Produto
import com.example.spesa.util.Formato
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Cadastro completo de um produto: dados fixos + "onde encontrar" (um preço por mercado).
 * Trabalhamos numa cópia e só gravamos no Repositorio quando o usuário toca em Salvar.
 */
class ProdutoFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUTO_ID = "produto_id"
        const val NOVA = "+ Nova etiqueta"
        const val EXTRA_MERCADO_ID = "mercado_id"   // veio de dentro de um mercado
    }

    private lateinit var b: ActivityProdutoFormBinding
    private lateinit var produto: Produto
    private lateinit var precosAdapter: PrecosAdapter
    private var etiquetaEscolhida = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProdutoFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        val original = Repositorio.produto(intent.getLongExtra(EXTRA_PRODUTO_ID, -1))
        // Cópia para poder cancelar sem estragar o original
        produto = original?.copy(precos = original.precos.map { it.copy() }.toMutableList()) ?: Produto()
        etiquetaEscolhida = produto.nomeEtiqueta()

        b.toolbar.title = getString(if (original == null) R.string.novo_produto else R.string.editar_produto)
        b.toolbar.setNavigationOnClickListener { finish() }

        b.campoNome.setText(produto.nome)
        b.campoMarca.setText(produto.marca)
        b.campoDescricao.setText(produto.descricao)
        b.trocaFavorito.isChecked = produto.favorito

        montarEtiquetas()

        precosAdapter = PrecosAdapter(aoClicar = { dialogoPreco(it) }, aoSegurar = ::removerPreco)
        b.listaPrecos.layoutManager = LinearLayoutManager(this)
        b.listaPrecos.adapter = precosAdapter
        atualizarPrecos()

        b.botaoAdicionarPreco.setOnClickListener { dialogoPreco(null) }
        b.botaoSalvar.setOnClickListener { salvar() }
        b.botaoComparar.setOnClickListener {
            startActivity(android.content.Intent(this, ComparativoActivity::class.java)
                .putExtra(ComparativoActivity.EXTRA_PRODUTO, produto.id))
        }
    }

    /** caixa de etiquetas: as prontas, as criadas e, no fim, "+ Nova etiqueta" */
    private fun montarEtiquetas() {
        val opcoes = Repositorio.todasEtiquetas() + NOVA
        b.campoCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, opcoes))
        b.campoCategoria.setText(etiquetaEscolhida, false)
        b.campoCategoria.setOnItemClickListener { _, _, posicao, _ ->
            if (opcoes[posicao] == NOVA) novaEtiqueta() else etiquetaEscolhida = opcoes[posicao]
        }
    }

    /** janelinha para digitar o nome da etiqueta nova */
    private fun novaEtiqueta() {
        val campo = android.widget.EditText(this).apply { hint = "Ex.: Pet, Festa, Bebê" }
        val caixa = android.widget.FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0); addView(campo)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova etiqueta")
            .setView(caixa)
            .setNegativeButton(R.string.cancelar) { _, _ -> b.campoCategoria.setText(etiquetaEscolhida, false) }
            .setPositiveButton("Criar") { _, _ ->
                val nome = campo.text.toString().trim()
                if (nome.isNotEmpty()) {
                    Repositorio.criarEtiqueta(nome)
                    etiquetaEscolhida = Repositorio.todasEtiquetas().find { it.equals(nome, true) } ?: nome
                }
                montarEtiquetas()
            }.show()
    }

    private fun atualizarPrecos() {
        precosAdapter.atualizar(produto.precos)
        b.textoSemPrecos.visivel(produto.precos.isEmpty())
    }

    /** Adiciona (preco == null) ou edita o preço num mercado. */
    private fun dialogoPreco(preco: PrecoNoMercado?) {
        if (Repositorio.mercados.isEmpty()) {
            Toast.makeText(this, "Cadastre um mercado primeiro", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MercadosActivity::class.java))
            return
        }
        val campos = DialogPrecoBinding.inflate(layoutInflater)
        val mercados = Repositorio.mercados
        // se veio de dentro de um mercado, ele já vem escolhido
        var escolhido = Repositorio.mercado(preco?.mercadoId)
            ?: Repositorio.mercado(intent.getLongExtra(EXTRA_MERCADO_ID, -1)) ?: mercados[0]

        campos.campoMercado.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, mercados.map { it.nome }))
        campos.campoMercado.setText(escolhido.nome, false)
        campos.campoMercado.setOnItemClickListener { _, _, posicao, _ -> escolhido = mercados[posicao] }
        campos.campoMercado.isEnabled = preco == null   // ao editar, o mercado não muda

        if (preco != null) {
            campos.campoPreco.setText(Formato.numero(preco.preco))
            campos.campoCorredor.setText(preco.corredor)
            campos.trocaPromocao.isChecked = preco.promocao
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.onde_encontrar)
            .setView(campos.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val valor = Formato.lerReais(campos.campoPreco.text.toString())
                val corredor = campos.campoCorredor.text.toString().trim()
                val promocao = campos.trocaPromocao.isChecked
                // Se já existe preço nesse mercado, atualiza em vez de duplicar
                val alvo = preco ?: produto.precoEm(escolhido.id)
                if (alvo != null) {
                    alvo.preco = valor
                    alvo.corredor = corredor
                    alvo.promocao = promocao
                    alvo.atualizadoEm = System.currentTimeMillis()
                } else {
                    produto.precos.add(PrecoNoMercado(escolhido.id, valor, promocao, corredor))
                }
                atualizarPrecos()
            }
            .show()
    }

    private fun removerPreco(preco: PrecoNoMercado) {
        val nome = Repositorio.mercado(preco.mercadoId)?.nome ?: "este mercado"
        MaterialAlertDialogBuilder(this)
            .setMessage("Remover o preço de $nome?")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                produto.precos.remove(preco)
                atualizarPrecos()
            }
            .show()
    }

    private fun salvar() {
        val nome = b.campoNome.text.toString().trim()
        if (nome.isEmpty()) {
            Toast.makeText(this, "Dê um nome ao produto", Toast.LENGTH_SHORT).show()
            return
        }
        produto.nome = nome
        produto.marca = b.campoMarca.text.toString().trim()
        produto.descricao = b.campoDescricao.text.toString().trim()
        // etiqueta pronta vira a categoria; etiqueta criada fica em "etiqueta"
        val pronta = Categoria.entries.find { it.rotulo == etiquetaEscolhida }
        produto.categoria = pronta ?: Categoria.OUTROS
        produto.etiqueta = if (pronta == null) etiquetaEscolhida else ""
        produto.favorito = b.trocaFavorito.isChecked
        Repositorio.salvarProduto(produto)
        finish()
    }
}
