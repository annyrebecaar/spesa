package com.example.spesa.dados

import android.content.Context
import android.content.SharedPreferences
import com.example.spesa.model.ItemLista
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Mercado
import com.example.spesa.model.Produto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Único lugar do app que lê e grava dados.
 *
 * Como funciona: mantemos as três listas em memória e, a cada mudança,
 * transformamos tudo em texto JSON (com o Gson) e guardamos no SharedPreferences,
 * que é um arquivinho de preferências que o Android salva no celular.
 * Ao abrir o app, fazemos o caminho inverso.
 *
 * É um `object`, ou seja, existe uma única instância compartilhada por todas as telas.
 */
object Repositorio {
    private const val ARQUIVO = "spesa_dados"
    private const val CHAVE_LISTAS = "listas"
    private const val CHAVE_PRODUTOS = "produtos"
    private const val CHAVE_MERCADOS = "mercados"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    val listas = mutableListOf<ListaDeCompras>()
    val produtos = mutableListOf<Produto>()
    val mercados = mutableListOf<Mercado>()

    /** Chamar uma vez, na tela de abertura, antes de usar qualquer outra função. */
    fun iniciar(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        carregar()
    }

    private fun carregar() {
        listas.clear(); listas.addAll(ler(CHAVE_LISTAS))
        produtos.clear(); produtos.addAll(ler(CHAVE_PRODUTOS))
        mercados.clear(); mercados.addAll(ler(CHAVE_MERCADOS))
    }

    /** Lê uma lista de objetos do tipo T gravada na chave informada. */
    private inline fun <reified T> ler(chave: String): List<T> {
        val json = prefs.getString(chave, null) ?: return emptyList()
        val tipo = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, tipo)
    }

    fun salvar() {
        prefs.edit()
            .putString(CHAVE_LISTAS, gson.toJson(listas))
            .putString(CHAVE_PRODUTOS, gson.toJson(produtos))
            .putString(CHAVE_MERCADOS, gson.toJson(mercados))
            .apply()
    }

    // ---------- Listas ----------

    fun lista(id: Long): ListaDeCompras? = listas.find { it.id == id }

    fun adicionarLista(lista: ListaDeCompras) {
        listas.add(0, lista)
        salvar()
    }

    fun removerLista(id: Long) {
        listas.removeAll { it.id == id }
        salvar()
    }

    // ---------- Produtos (catálogo / despensa) ----------

    fun produto(id: Long): Produto? = produtos.find { it.id == id }

    fun produtoPorNome(nome: String): Produto? =
        produtos.find { it.nome.equals(nome.trim(), ignoreCase = true) }

    fun salvarProduto(produto: Produto) {
        val posicao = produtos.indexOfFirst { it.id == produto.id }
        if (posicao >= 0) produtos[posicao] = produto else produtos.add(0, produto)
        salvar()
    }

    fun removerProduto(id: Long) {
        produtos.removeAll { it.id == id }
        salvar()
    }

    fun favoritos(): List<Produto> = produtos.filter { it.favorito }

    /**
     * Quando um item é digitado numa lista e ainda não existe no catálogo,
     * criamos o produto automaticamente. Assim a "despensa" vai se formando sozinha.
     */
    fun garantirProduto(item: ItemLista): Produto {
        produtoPorNome(item.nome)?.let { return it }
        val novo = Produto(nome = item.nome.trim(), categoria = item.categoria)
        produtos.add(0, novo)
        return novo
    }

    // ---------- Mercados ----------

    fun mercado(id: Long?): Mercado? = if (id == null) null else mercados.find { it.id == id }

    fun salvarMercado(mercado: Mercado) {
        val posicao = mercados.indexOfFirst { it.id == mercado.id }
        if (posicao >= 0) mercados[posicao] = mercado else mercados.add(mercado)
        salvar()
    }

    fun removerMercado(id: Long) {
        mercados.removeAll { it.id == id }
        // Limpa as referências a esse mercado para não sobrar "fantasma".
        produtos.forEach { p -> p.precos.removeAll { it.mercadoId == id } }
        listas.forEach { l -> l.itens.filter { it.mercadoId == id }.forEach { it.mercadoId = null } }
        salvar()
    }

    /**
     * Para a tela de comparativo: quanto a lista custaria em cada mercado.
     * Para cada item, usa o preço registrado naquele mercado; se não houver,
     * usa o preço que está na lista. Retorna pares (mercado, total).
     */
    fun comparativo(lista: ListaDeCompras): List<Pair<Mercado, Double>> =
        mercados.map { mercado ->
            val total = lista.itens.sumOf { item ->
                val precoLa = item.produtoId?.let { produto(it) }?.precoEm(mercado.id)?.preco
                (precoLa ?: item.precoUnitario) * item.quantidade
            }
            mercado to total
        }.sortedBy { it.second }
}
