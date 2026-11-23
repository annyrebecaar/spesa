package com.example.spesa.dados

import android.content.Context
import android.content.SharedPreferences
import com.example.spesa.model.Atividade
import com.example.spesa.model.CategoriaGasto
import com.example.spesa.model.CorPasta
import com.example.spesa.model.ItemLista
import com.example.spesa.model.Lancamento
import com.example.spesa.model.Lembrete
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Mercado
import com.example.spesa.model.Pasta
import com.example.spesa.model.Produto
import com.example.spesa.model.Usuario
import java.security.MessageDigest
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
    private const val CHAVE_PASTAS = "pastas"
    private const val CHAVE_LANCAMENTOS = "lancamentos"
    private const val CHAVE_LEMBRETES = "lembretes"
    private const val CHAVE_ATIVIDADES = "atividades"
    private const val CHAVE_USUARIO = "usuario"
    private const val CHAVE_LOGADO = "logado"
    private const val CHAVE_PRIMEIRA_VEZ = "pastas_criadas"
    const val CHAVE_LEMBRETES_LIGADOS = "lembretes_ligados"
    const val CHAVE_TEMA_ESCURO = "tema_escuro"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    val listas = mutableListOf<ListaDeCompras>()
    val produtos = mutableListOf<Produto>()
    val mercados = mutableListOf<Mercado>()
    val pastas = mutableListOf<Pasta>()
    val lancamentos = mutableListOf<Lancamento>()
    val lembretes = mutableListOf<Lembrete>()
    val atividades = mutableListOf<Atividade>()
    var usuario: Usuario? = null

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
        pastas.clear(); pastas.addAll(ler(CHAVE_PASTAS))
        lancamentos.clear(); lancamentos.addAll(ler(CHAVE_LANCAMENTOS))
        lembretes.clear(); lembretes.addAll(ler(CHAVE_LEMBRETES))
        atividades.clear(); atividades.addAll(ler(CHAVE_ATIVIDADES))
        usuario = prefs.getString(CHAVE_USUARIO, null)?.let { gson.fromJson(it, Usuario::class.java) }

        // Na primeira vez, já cria as 4 pastas do protótipo para o app não começar vazio.
        if (!prefs.getBoolean(CHAVE_PRIMEIRA_VEZ, false)) {
            pastas.addAll(listOf(
                Pasta(1, "Mercado do mês", CorPasta.VERDE),
                Pasta(2, "Festas e churrascos", CorPasta.VERMELHA),
                Pasta(3, "Casa e limpeza", CorPasta.AMARELA),
                Pasta(4, "Farmácia e higiene", CorPasta.SALVIA),
            ))
            prefs.edit().putBoolean(CHAVE_PRIMEIRA_VEZ, true).apply()
            salvar()
        }
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
            .putString(CHAVE_PASTAS, gson.toJson(pastas))
            .putString(CHAVE_LANCAMENTOS, gson.toJson(lancamentos))
            .putString(CHAVE_LEMBRETES, gson.toJson(lembretes))
            .putString(CHAVE_ATIVIDADES, gson.toJson(atividades))
            .putString(CHAVE_USUARIO, usuario?.let { gson.toJson(it) })
            .apply()
    }

    /** Tudo em um texto JSON só, para o backup (Ajustes > Backup). */
    fun exportarTudo(): String = gson.toJson(mapOf(
        "listas" to listas, "produtos" to produtos, "mercados" to mercados, "pastas" to pastas,
        "lancamentos" to lancamentos, "lembretes" to lembretes,
    ))

    // ---------- Preferências simples (liga/desliga) ----------

    fun preferencia(chave: String, padrao: Boolean): Boolean = prefs.getBoolean(chave, padrao)
    fun mudarPreferencia(chave: String, valor: Boolean) = prefs.edit().putBoolean(chave, valor).apply()

    // ---------- Conta (login local) ----------

    /** Embaralha a senha com SHA-256 para não guardar o texto puro. */
    fun hash(senha: String): String =
        MessageDigest.getInstance("SHA-256").digest(senha.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun estaLogado(): Boolean = usuario != null && prefs.getBoolean(CHAVE_LOGADO, false)

    fun criarConta(nome: String, email: String, senha: String) {
        usuario = Usuario(nome, email.trim().lowercase(), hash(senha))
        prefs.edit().putBoolean(CHAVE_LOGADO, true).apply()
        salvar()
    }

    /** Devolve true se o e-mail e a senha batem com a conta salva neste celular. */
    fun entrar(email: String, senha: String): Boolean {
        val u = usuario ?: return false
        val ok = u.email == email.trim().lowercase() && u.senhaHash == hash(senha)
        if (ok) prefs.edit().putBoolean(CHAVE_LOGADO, true).apply()
        return ok
    }

    fun sair() = prefs.edit().putBoolean(CHAVE_LOGADO, false).apply()

    // ---------- Atividade recente ----------

    fun registrar(texto: String, tipo: String = "check") {
        atividades.add(0, Atividade(texto, System.currentTimeMillis(), tipo))
        while (atividades.size > 30) atividades.removeAt(atividades.lastIndex)
        salvar()
    }

    // ---------- Pastas ----------

    fun pasta(id: Long?): Pasta? = if (id == null) null else pastas.find { it.id == id }

    fun listasDaPasta(id: Long): List<ListaDeCompras> = listas.filter { it.pastaId == id && !it.finalizada }

    fun salvarPasta(pasta: Pasta) {
        val posicao = pastas.indexOfFirst { it.id == pasta.id }
        if (posicao >= 0) pastas[posicao] = pasta else pastas.add(pasta)
        salvar()
    }

    /** Apaga a pasta, mas não as listas: elas só ficam "sem pasta". */
    fun removerPasta(id: Long) {
        pastas.removeAll { it.id == id }
        listas.filter { it.pastaId == id }.forEach { it.pastaId = null }
        salvar()
    }

    // ---------- Finanças ----------

    fun salvarLancamento(l: Lancamento) {
        val posicao = lancamentos.indexOfFirst { it.id == l.id }
        if (posicao >= 0) lancamentos[posicao] = l else lancamentos.add(l)
        lancamentos.sortByDescending { it.data }
        salvar()
    }

    fun removerLancamento(id: Long) {
        lancamentos.removeAll { it.id == id }
        salvar()
    }

    /** Lançamentos entre duas datas (em milissegundos). */
    fun lancamentosEntre(inicio: Long, fim: Long): List<Lancamento> =
        lancamentos.filter { it.data in inicio until fim }

    /** Soma por categoria das saídas, da maior para a menor. */
    fun gastosPorCategoria(lista: List<Lancamento>): List<Pair<CategoriaGasto, Double>> =
        lista.filter { !it.entrada }.groupBy { it.categoria }
            .map { it.key to it.value.sumOf { l -> l.valor } }
            .sortedByDescending { it.second }

    // ---------- Agenda ----------

    fun lembrete(id: Long): Lembrete? = lembretes.find { it.id == id }

    fun salvarLembrete(l: Lembrete) {
        val posicao = lembretes.indexOfFirst { it.id == l.id }
        if (posicao >= 0) lembretes[posicao] = l else lembretes.add(l)
        lembretes.sortBy { it.data }
        salvar()
    }

    fun removerLembrete(id: Long) {
        lembretes.removeAll { it.id == id }
        salvar()
    }

    /** dia do próximo lembrete de "Comprar" ligado a essa lista (ou null se não tem) */
    fun dataDeCompra(listaId: Long): Long? =
        lembretes.filter { it.listaId == listaId && !it.feito }.minOfOrNull { it.data }

    /** Próximos compromissos ainda não feitos, a partir de agora (começo do dia). */
    fun proximosLembretes(): List<Lembrete> {
        val hoje = com.example.spesa.util.Datas.inicioDoDia(System.currentTimeMillis())
        return lembretes.filter { !it.feito && it.data >= hoje }.sortedBy { it.data }
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
