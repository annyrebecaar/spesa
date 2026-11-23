package com.example.spesa.util

import android.util.Base64
import com.example.spesa.dados.Repositorio
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Pasta
import com.google.gson.Gson

/**
 * compartilhar listas e pastas com outras pessoas.
 * como o app não tem servidor, a lista vira um "código" (texto) que vai pelo whatsapp ou e-mail.
 * quem recebe cola o código em Listas > Importar e a lista aparece pronta, sem digitar nada de novo.
 */
object Compartilhar {
    private const val PREFIXO = "SPESA:"
    private val gson = Gson()

    // o que vai dentro do código: a pasta (se for uma pasta) e as listas
    private data class Pacote(val pasta: Pasta? = null, val listas: List<ListaDeCompras> = emptyList())

    /** transforma em código: json -> base64 (só letras e números, pra não quebrar no whatsapp) */
    fun codigo(listas: List<ListaDeCompras>, pasta: Pasta? = null): String {
        val json = gson.toJson(Pacote(pasta, listas))
        return PREFIXO + Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
    }

    /** mensagem pronta pra mandar */
    fun mensagem(titulo: String, codigo: String): String =
        "🛒 Te mandei \"$titulo\" no Spesa!\nAbra o app, vá em Listas > Importar e cole este código:\n\n$codigo"

    /**
     * lê o código colado e cria cópias das listas (e da pasta) neste celular.
     * devolve quantas listas entraram, ou -1 se o código não for válido.
     */
    fun importar(texto: String): Int {
        val inicio = texto.indexOf(PREFIXO)
        if (inicio < 0) return -1
        return try {
            val base = texto.substring(inicio + PREFIXO.length).trim().split(Regex("\\s"))[0]
            val json = String(Base64.decode(base, Base64.NO_WRAP))
            val pacote = gson.fromJson(json, Pacote::class.java)
            // ids novos pra não bater com o que já existe aqui
            var pastaId: Long? = null
            pacote.pasta?.let { p ->
                val nova = p.copy(id = System.nanoTime())
                Repositorio.salvarPasta(nova)
                pastaId = nova.id
            }
            pacote.listas.forEach { l ->
                Repositorio.adicionarLista(l.copy(id = System.nanoTime(), pastaId = pastaId ?: l.pastaId?.takeIf { Repositorio.pasta(it) != null }))
            }
            Repositorio.registrar("Você recebeu ${pacote.listas.size} lista(s) compartilhada(s)", "mais")
            pacote.listas.size
        } catch (e: Exception) {
            -1
        }
    }
}
