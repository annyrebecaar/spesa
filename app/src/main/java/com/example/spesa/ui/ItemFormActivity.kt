package com.example.spesa.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityItemFormBinding
import com.example.spesa.model.Categoria
import com.example.spesa.model.ItemLista
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Mercado
import com.example.spesa.model.PrecoNoMercado
import com.example.spesa.model.Produto
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel

/** Formulário para adicionar ou editar um item de uma lista. */
class ItemFormActivity : AppCompatActivity() {
    private val NOVO_MERCADO = "+ Novo mercado"

    companion object {
        const val EXTRA_LISTA_ID = "lista_id"
        const val EXTRA_ITEM_ID = "item_id"
    }

    private lateinit var b: ActivityItemFormBinding
    private lateinit var lista: ListaDeCompras
    private var itemEditando: ItemLista? = null

    private var quantidade = 1
    private var categoriaEscolhida = Categoria.OUTROS
    private var mercadoEscolhido: Mercado? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityItemFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        lista = Repositorio.lista(intent.getLongExtra(EXTRA_LISTA_ID, -1)) ?: run { finish(); return }
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        itemEditando = lista.itens.find { it.id == itemId }

        b.toolbar.title = getString(if (itemEditando == null) R.string.novo_item else R.string.editar_item)
        b.toolbar.setNavigationOnClickListener { finish() }

        prepararCampos()
        itemEditando?.let { preencher(it) }

        b.botaoSalvar.setOnClickListener { salvar() }

        // total sempre atualizado: quantidade × preço
        b.campoPreco.doAfterTextChanged { mostrarTotal() }
        mostrarTotal()

        // remover só aparece quando está editando um item que já existe
        b.botaoRemover.visivel(itemEditando != null)
        b.botaoRemover.setOnClickListener {
            val item = itemEditando ?: return@setOnClickListener
            Telas.confirmar(this, "Remover item?", "Tirar \"${item.nome}\" da lista?", "Remover") {
                lista.itens.remove(item)
                Repositorio.salvar()
                finish()
            }
        }
    }

    /** mostra "2 × R$ 5,90" e o total do item */
    private fun mostrarTotal() {
        val preco = Formato.lerReais(b.campoPreco.text.toString())
        b.contaTotal.text = "$quantidade × ${Formato.reais(preco)}"
        b.valorTotal.text = Formato.reais(preco * quantidade)
    }

    private fun prepararCampos() {
        // Nome: sugere produtos já conhecidos
        val nomes = Repositorio.produtos.map { it.nome }
        b.campoNome.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nomes))
        b.campoNome.setOnItemClickListener { _, _, _, _ ->
            Repositorio.produtoPorNome(b.campoNome.text.toString())?.let { preencherDoProduto(it) }
        }
        b.campoNome.doAfterTextChanged { atualizarDica() }

        // Categoria
        b.campoCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, Categoria.rotulos()))
        b.campoCategoria.setText(categoriaEscolhida.rotulo, false)
        b.campoCategoria.setOnItemClickListener { _, _, posicao, _ ->
            categoriaEscolhida = Categoria.entries[posicao]
        }

        montarMercados()

        // Quantidade
        b.botaoMenos.setOnClickListener { mudarQuantidade(-1) }
        b.botaoMais.setOnClickListener { mudarQuantidade(+1) }
    }

    private fun mudarQuantidade(delta: Int) {
        quantidade = (quantidade + delta).coerceAtLeast(1)
        b.textoQuantidade.text = quantidade.toString()
        mostrarTotal()
    }

    /** Preenche o formulário a partir de um item já existente (modo edição). */
    private fun preencher(item: ItemLista) {
        b.campoNome.setText(item.nome)
        quantidade = item.quantidade
        b.textoQuantidade.text = quantidade.toString()
        if (item.precoUnitario > 0) b.campoPreco.setText(Formato.numero(item.precoUnitario))
        b.trocaPromocao.isChecked = item.promocao
        categoriaEscolhida = item.categoria
        b.campoCategoria.setText(item.categoria.rotulo, false)
        mercadoEscolhido = Repositorio.mercado(item.mercadoId)
        mercadoEscolhido?.let { b.campoMercado.setText(it.nome, false) }
        atualizarDica()
    }

    /** Quando o usuário escolhe um produto do catálogo, puxamos categoria e o melhor preço. */
    private fun preencherDoProduto(produto: Produto) {
        categoriaEscolhida = produto.categoria
        b.campoCategoria.setText(produto.categoria.rotulo, false)
        val melhor = produto.menorPreco()
        if (melhor != null) {
            b.campoPreco.setText(Formato.numero(melhor.preco))
            b.trocaPromocao.isChecked = melhor.promocao
            mercadoEscolhido = Repositorio.mercado(melhor.mercadoId)
            mercadoEscolhido?.let { b.campoMercado.setText(it.nome, false) }
        }
        atualizarDica()
    }

    /** Caixinha verde: em quais mercados esse produto já foi visto e por quanto. */
    private fun atualizarDica() {
        val produto = Repositorio.produtoPorNome(b.campoNome.text.toString())
        val precos = produto?.precos.orEmpty().sortedBy { it.preco }
        b.dicaPreco.visivel(precos.isNotEmpty())
        if (precos.isEmpty()) return
        b.dicaPreco.text = precos.joinToString("\n") { p ->
            val nome = Repositorio.mercado(p.mercadoId)?.nome ?: "?"
            val promo = if (p.promocao) " (promoção)" else ""
            val onde = if (p.corredor.isNotBlank()) " · ${p.corredor}" else ""
            "${Formato.reais(p.preco)} no $nome$promo$onde"
        } + "\nToque para comparar os mercados ›"
        // abre a comparação só deste produto
        b.dicaPreco.setOnClickListener {
            startActivity(android.content.Intent(this, ComparativoActivity::class.java)
                .putExtra(ComparativoActivity.EXTRA_PRODUTO, produto!!.id))
        }
    }

    /** Mercado: "Nenhum" + os cadastrados + "+ Novo mercado" no fim */
    private fun montarMercados() {
        val nomes = listOf(getString(R.string.nenhum_mercado)) + Repositorio.mercados.map { it.nome } + NOVO_MERCADO
        b.campoMercado.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nomes))
        b.campoMercado.setText(mercadoEscolhido?.nome ?: nomes[0], false)
        b.campoMercado.setOnItemClickListener { _, _, posicao, _ ->
            if (nomes[posicao] == NOVO_MERCADO) { novoMercado(); return@setOnItemClickListener }
            mercadoEscolhido = if (posicao == 0) null else Repositorio.mercados[posicao - 1]
            // Se já sabemos o preço deste produto nesse mercado, preenche
            val produto = Repositorio.produtoPorNome(b.campoNome.text.toString())
            val conhecido = mercadoEscolhido?.let { produto?.precoEm(it.id) }
            if (conhecido != null) {
                b.campoPreco.setText(Formato.numero(conhecido.preco))
                b.trocaPromocao.isChecked = conhecido.promocao
            }
        }
    }

    /** janelinha para cadastrar um mercado sem sair do item */
    private fun novoMercado() {
        val campo = android.widget.EditText(this).apply { hint = "Nome do mercado" }
        val caixa = android.widget.FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0); addView(campo)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Novo mercado")
            .setView(caixa)
            .setNegativeButton(R.string.cancelar) { _, _ -> montarMercados() }
            .setPositiveButton("Criar") { _, _ ->
                val nome = campo.text.toString().trim()
                if (nome.isNotEmpty()) {
                    val mercado = Mercado().apply { this.nome = nome }
                    Repositorio.salvarMercado(mercado)
                    mercadoEscolhido = mercado
                }
                montarMercados()
            }.show()
    }

    private fun salvar() {
        val nome = b.campoNome.text.toString().trim()
        if (nome.isEmpty()) {
            Toast.makeText(this, "Dê um nome ao produto", Toast.LENGTH_SHORT).show()
            return
        }
        val preco = Formato.lerReais(b.campoPreco.text.toString())
        val promocao = b.trocaPromocao.isChecked

        val item = itemEditando ?: ItemLista().also { lista.itens.add(it) }
        item.nome = nome
        item.categoria = categoriaEscolhida
        item.quantidade = quantidade
        item.precoUnitario = preco
        item.promocao = promocao
        item.mercadoId = mercadoEscolhido?.id

        // Garante que o produto exista na despensa e liga o item a ele
        val produto = Repositorio.garantirProduto(item)
        item.produtoId = produto.id

        // O app "aprende": se o item tem mercado e preço, registra isso no produto
        val mercado = mercadoEscolhido
        if (mercado != null && preco > 0) {
            val existente = produto.precoEm(mercado.id)
            if (existente != null) {
                existente.preco = preco
                existente.promocao = promocao
                existente.atualizadoEm = System.currentTimeMillis()
            } else {
                produto.precos.add(PrecoNoMercado(mercadoId = mercado.id, preco = preco, promocao = promocao))
            }
        }

        Repositorio.salvar()
        finish()
    }
}
