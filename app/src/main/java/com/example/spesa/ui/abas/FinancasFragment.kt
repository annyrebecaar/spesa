package com.example.spesa.ui.abas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentFinancasBinding
import com.example.spesa.model.Lancamento
import com.example.spesa.ui.LancamentoFormActivity
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.pintar
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Aba Finanças: quanto entrou, saiu e sobrou no dia/semana/mês, por categoria. */
class FinancasFragment : Fragment() {
    private var _b: FragmentFinancasBinding? = null
    private val b get() = _b!!

    private enum class Escala { DIA, SEMANA, MES }
    private var escala = Escala.MES
    private var referencia = System.currentTimeMillis()   // qual dia/semana/mês está na tela

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentFinancasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        b.escala.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            escala = when (id) { R.id.escalaDia -> Escala.DIA; R.id.escalaSemana -> Escala.SEMANA; else -> Escala.MES }
            referencia = System.currentTimeMillis()
            atualizar()
        }
        b.anterior.setOnClickListener { andar(-1) }
        b.proximo.setOnClickListener { andar(1) }
        b.lancar.setOnClickListener { startActivity(Intent(requireContext(), LancamentoFormActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun andar(passo: Int) {
        referencia = when (escala) {
            Escala.DIA -> Datas.somarDias(referencia, passo)
            Escala.SEMANA -> Datas.somarDias(referencia, 7 * passo)
            Escala.MES -> Datas.somarMeses(referencia, passo)
        }
        atualizar()
    }

    /** Começo e fim (em milissegundos) do período mostrado. */
    private fun intervalo(): Pair<Long, Long> = when (escala) {
        Escala.DIA -> Datas.inicioDoDia(referencia).let { it to Datas.somarDias(it, 1) }
        Escala.SEMANA -> Datas.inicioDaSemana(referencia).let { it to Datas.somarDias(it, 7) }
        Escala.MES -> Datas.inicioDoMes(referencia).let { it to Datas.somarMeses(it, 1) }
    }

    private fun atualizar() {
        val (inicio, fim) = intervalo()
        b.periodo.text = when (escala) {
            Escala.DIA -> Datas.diaMes(inicio)
            Escala.SEMANA -> "${Datas.diaMes(inicio)} – ${Datas.diaMes(Datas.somarDias(fim, -1))}"
            Escala.MES -> Datas.mesAnoCurto(inicio)
        }
        b.rotuloSobrou.text = when (escala) { Escala.DIA -> "Sobrou no dia"; Escala.SEMANA -> "Sobrou na semana"; else -> "Sobrou no mês" }

        val doPeriodo = Repositorio.lancamentosEntre(inicio, fim)
        val entrou = doPeriodo.filter { it.entrada }.sumOf { it.valor }
        val saiu = doPeriodo.filter { !it.entrada }.sumOf { it.valor }
        b.sobrou.text = Formato.reais(entrou - saiu)
        b.entrou.text = Formato.reais(entrou)
        b.saiu.text = Formato.reais(saiu)
        b.pctRenda.visivel(entrou > 0)
        if (entrou > 0) b.pctRenda.text = "${((entrou - saiu) / entrou * 100).toInt()}% da renda"

        // Barras por categoria (a maior ocupa a barra inteira)
        b.categorias.removeAllViews()
        val porCategoria = Repositorio.gastosPorCategoria(doPeriodo)
        val maior = porCategoria.maxOfOrNull { it.second } ?: 0.0
        porCategoria.forEach { (cat, valor) ->
            val v = layoutInflater.inflate(R.layout.item_barra_categoria, b.categorias, false)
            v.findViewById<TextView>(R.id.nome).text = cat.rotulo
            v.findViewById<TextView>(R.id.valor).text = Formato.reais(valor)
            v.findViewById<ProgressBar>(R.id.barra).apply {
                progress = if (maior > 0) (valor / maior * 100).toInt() else 0
                pintar(cat.corRes)
            }
            b.categorias.addView(v)
        }
        if (porCategoria.isEmpty()) {
            b.categorias.addView(TextView(requireContext()).apply {
                text = "Sem saídas neste período."
                setTextColor(Telas.cor(requireContext(), R.color.texto_suave))
            })
        }

        // Lista de lançamentos
        b.lancamentos.removeAllViews()
        b.vazio.visivel(doPeriodo.isEmpty())
        doPeriodo.sortedByDescending { it.data }.forEach { linhaLancamento(it) }
    }

    private fun linhaLancamento(l: Lancamento) {
        val ctx = requireContext()
        val linha = Telas.linha(b.lancamentos)
        linha.caixaIcone.visivel(true)
        linha.caixaIcone.setBackgroundResource(if (l.entrada) R.drawable.bg_verde_claro else R.drawable.bg_vermelho_claro)
        linha.icone.setImageResource(if (l.entrada) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up)
        linha.icone.setColorFilter(Telas.cor(ctx, if (l.entrada) R.color.entrada else R.color.promo))
        linha.titulo.text = l.descricao.ifBlank { l.categoria.rotulo }
        linha.subtitulo.text = "${Datas.diaMes(l.data)} · ${l.categoria.rotulo}"
        linha.valor.text = (if (l.entrada) "+" else "−") + Formato.reais(l.valor)
        linha.valor.setTextColor(Telas.cor(ctx, if (l.entrada) R.color.entrada else R.color.promo))
        linha.root.setOnClickListener {
            startActivity(Intent(ctx, LancamentoFormActivity::class.java).putExtra(LancamentoFormActivity.EXTRA_ID, l.id))
        }
        linha.root.setOnLongClickListener {
            MaterialAlertDialogBuilder(ctx).setTitle("Excluir lançamento?")
                .setMessage("${l.descricao} · ${Formato.reais(l.valor)}")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.excluir) { _, _ -> Repositorio.removerLancamento(l.id); atualizar() }
                .show()
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
