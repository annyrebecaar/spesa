package com.example.spesa.ui.abas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentListasBinding
import com.example.spesa.model.Categoria
import com.example.spesa.model.ItemLista
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel

/** Aba Listas: pastas (duas por linha), todas as listas, histórico e a tela de primeira lista. */
class ListasFragment : Fragment() {
    private var _b: FragmentListasBinding? = null
    private val b get() = _b!!
    private var historico = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentListasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.abas.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) { historico = id == R.id.abaHistorico; atualizar() }
        }
        b.buscar.setOnClickListener {
            b.caixaBusca.visivel(b.caixaBusca.visibility != View.VISIBLE)
            if (b.caixaBusca.visibility != View.VISIBLE) b.campoBusca.setText("")
        }
        b.campoBusca.doAfterTextChanged { atualizar() }
        b.novaPasta.setOnClickListener { Telas.dialogoPasta(requireActivity(), null) { atualizar() } }
        b.botaoNovaLista.setOnClickListener { novaLista() }
        b.criarPrimeira.setOnClickListener { novaLista() }

        // Listas prontas para quem está começando
        modelos.keys.forEach { nome ->
            Telas.chip(b.modelos, nome).setOnClickListener { criarDoModelo(nome) }
        }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun novaLista() = Telas.dialogoLista(requireActivity(), null) { atualizar() }

    private fun atualizar() {
        val busca = b.campoBusca.text.toString().trim().lowercase()
        val filtradas = Repositorio.listas.filter { busca.isBlank() || it.titulo.lowercase().contains(busca) }
        val ativas = filtradas.filter { !it.finalizada }
        val semNenhuma = Repositorio.listas.none { !it.finalizada } && busca.isBlank()

        b.blocoAtivas.visivel(!historico && !semNenhuma)
        b.vazio.visivel(!historico && semNenhuma)
        b.blocoHistorico.visivel(historico)
        b.botaoNovaLista.visivel(!historico && !semNenhuma)

        if (!historico) {
            montarPastas()
            b.listas.removeAllViews()
            ativas.forEach { Telas.linhaDaLista(b.listas, it, ::menu) }
        } else montarHistorico(filtradas.filter { it.finalizada })
    }

    /** Coloca as pastas em linhas de duas, com espaço entre elas. */
    private fun montarPastas() {
        b.pastas.removeAllViews()
        val dp = resources.displayMetrics.density
        Repositorio.pastas.chunked(2).forEach { par ->
            val linha = LinearLayout(requireContext())
            linha.orientation = LinearLayout.HORIZONTAL
            b.pastas.addView(linha, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = (16 * dp).toInt() })
            par.forEachIndexed { i, pasta ->
                val p = Telas.pasta(linha, pasta)
                p.root.setOnLongClickListener { Telas.dialogoPasta(requireActivity(), pasta) { atualizar() }; true }
                linha.addView(p.root, LinearLayout.LayoutParams(0, (100 * dp).toInt(), 1f).apply {
                    if (i == 0) marginEnd = (8 * dp).toInt() else marginStart = (8 * dp).toInt()
                })
            }
            // Se a última linha tem só uma pasta, um espaço vazio mantém o tamanho igual.
            if (par.size == 1) linha.addView(View(requireContext()), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = (8 * dp).toInt() })
        }
    }

    /** Histórico: listas finalizadas agrupadas por mês, com o total gasto no mês. */
    private fun montarHistorico(finalizadas: List<ListaDeCompras>) {
        b.blocoHistorico.removeAllViews()
        if (finalizadas.isEmpty()) {
            val t = TextView(requireContext())
            t.text = "Nenhuma compra finalizada ainda.\nNa lista, toque em Finalizar compra."
            t.setTextColor(Telas.cor(requireContext(), R.color.texto_suave))
            t.setPadding(0, 48, 0, 0)
            b.blocoHistorico.addView(t)
            return
        }
        val dp = resources.displayMetrics.density
        finalizadas.sortedByDescending { it.finalizadaEm ?: it.criadaEm }
            .groupBy { Datas.inicioDoMes(it.finalizadaEm ?: it.criadaEm) }
            .forEach { (mes, listas) ->
                val cab = LayoutInflater.from(requireContext()).inflate(R.layout.item_cabecalho_mes, b.blocoHistorico, false)
                cab.findViewById<TextView>(R.id.mes).text = Datas.mes(mes)
                cab.findViewById<TextView>(R.id.total).text = "gasto ${Formato.reais(listas.sumOf { it.totalPago ?: it.totalEstimado() })}"
                b.blocoHistorico.addView(cab)
                listas.forEach { Telas.linhaDaLista(b.blocoHistorico, it, ::menu) }
            }
        val dica = TextView(requireContext())
        dica.text = "Segure o dedo numa lista para duplicar e usar de novo."
        dica.setTextColor(Telas.cor(requireContext(), R.color.texto_suave))
        dica.gravity = android.view.Gravity.CENTER
        dica.setPadding(0, (8 * dp).toInt(), 0, 0)
        b.blocoHistorico.addView(dica)
    }

    private fun menu(lista: ListaDeCompras) = Telas.menuDaLista(requireActivity(), lista) { atualizar() }

    /** Itens das listas prontas. */
    private val modelos = mapOf(
        "Mercado do mês" to listOf("Arroz 5 kg" to Categoria.ALIMENTOS, "Feijão 1 kg" to Categoria.ALIMENTOS,
            "Leite 1 L" to Categoria.BEBIDAS, "Café 500 g" to Categoria.ALIMENTOS, "Banana" to Categoria.HORTIFRUTI),
        "Churrasco" to listOf("Picanha" to Categoria.ACOUGUE, "Linguiça" to Categoria.ACOUGUE,
            "Carvão" to Categoria.OUTROS, "Pão de alho" to Categoria.PADARIA, "Refrigerante" to Categoria.BEBIDAS),
        "Limpeza" to listOf("Detergente" to Categoria.LIMPEZA, "Sabão em pó" to Categoria.LIMPEZA,
            "Água sanitária" to Categoria.LIMPEZA, "Esponja" to Categoria.LIMPEZA),
    )

    private fun criarDoModelo(nome: String) {
        val pasta = when (nome) { "Churrasco" -> 2L; "Limpeza" -> 3L; else -> 1L }
        val lista = ListaDeCompras(titulo = nome, pastaId = Repositorio.pasta(pasta)?.id)
        modelos[nome]!!.forEach { (item, cat) -> lista.itens.add(ItemLista(id = System.nanoTime(), nome = item, categoria = cat)) }
        Repositorio.adicionarLista(lista)
        Repositorio.registrar("Você criou a lista $nome", "mais")
        Telas.abrirLista(requireActivity(), lista.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
