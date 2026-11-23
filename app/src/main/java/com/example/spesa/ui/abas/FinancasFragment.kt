package com.example.spesa.ui.abas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentFinancasBinding
import com.example.spesa.model.Lancamento
import android.widget.LinearLayout
import com.example.spesa.databinding.ItemLinhaBinding
import com.example.spesa.model.CategoriaGasto
import com.example.spesa.model.Lembrete
import com.example.spesa.model.TipoLembrete
import com.example.spesa.ui.LancamentoFormActivity
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
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


        mostrarQuantoSaiu()
        mostrarAPagar()
        mostrarCategorias(doPeriodo)
        mostrarPlanilha(doPeriodo, entrou, saiu)
    }

    /** "saiu hoje / na semana / no mês": sempre conta a partir de hoje */
    private fun mostrarQuantoSaiu() {
        val agora = System.currentTimeMillis()
        fun saiuEntre(inicio: Long, fim: Long) =
            Repositorio.lancamentosEntre(inicio, fim).filter { !it.entrada }.sumOf { it.valor }
        val hoje = Datas.inicioDoDia(agora)
        val semana = Datas.inicioDaSemana(agora)
        val mes = Datas.inicioDoMes(agora)
        b.saiuHoje.text = Formato.reais(saiuEntre(hoje, Datas.somarDias(hoje, 1)))
        b.saiuSemana.text = Formato.reais(saiuEntre(semana, Datas.somarDias(semana, 7)))
        b.saiuMes.text = Formato.reais(saiuEntre(mes, Datas.somarMeses(mes, 1)))
    }

    /** contas dos próximos 30 dias (lembretes de "Pagar" que ainda não foram pagos) */
    private fun mostrarAPagar() {
        val ctx = requireContext()
        val hoje = Datas.inicioDoDia(System.currentTimeMillis())
        val limite = Datas.somarDias(hoje, 30)
        // conta atrasada também aparece, pra não esquecer
        val contas = Repositorio.lembretes
            .filter { it.tipo == TipoLembrete.PAGAR && !it.feito && it.data < limite }
            .sortedBy { it.data }

        b.aPagar.removeAllViews()
        b.aPagar.visivel(contas.isNotEmpty())
        b.aPagarVazio.visivel(contas.isEmpty())
        b.totalAPagar.text = if (contas.isEmpty()) "" else "próximos 30 dias · ${Formato.reais(contas.sumOf { it.valor })}"

        contas.forEach { conta ->
            val l = ItemLinhaBinding.inflate(layoutInflater, b.aPagar, true)
            l.root.strokeWidth = 0
            l.root.radius = 0f
            (l.root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            l.caixaIcone.visivel(true)
            l.caixaIcone.setBackgroundResource(R.drawable.bg_vermelho_claro)
            l.icone.setImageResource(R.drawable.ic_coin)
            l.icone.setColorFilter(Telas.cor(ctx, R.color.promo))
            l.titulo.text = conta.texto
            val atrasada = conta.data < hoje
            l.subtitulo.text = if (atrasada) "atrasada! era ${Datas.diaMes(conta.data)}" else "vence ${Datas.diaPorExtenso(conta.data).lowercase()}"
            if (atrasada || conta.data < Datas.somarDias(hoje, 3)) l.subtitulo.setTextColor(Telas.cor(ctx, R.color.promo))
            l.valor.text = Formato.reais(conta.valor)
            l.extra.visivel(true)
            l.extra.text = "Paguei ✓"
            // tocar: marca como paga e lança a saída nas finanças
            l.root.setOnClickListener { pagar(conta) }
        }
    }

    private fun pagar(conta: Lembrete) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pagou ${conta.texto}?")
            .setMessage("Vamos lançar ${Formato.reais(conta.valor)} como saída em Contas e tirar de A pagar.")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton("Paguei") { _, _ ->
                Repositorio.pagarConta(conta)
                atualizar()
            }.show()
    }

    /** uma barra só, dividida por categoria, e embaixo a legenda com valor e % */
    private fun mostrarCategorias(doPeriodo: List<Lancamento>) {
        val ctx = requireContext()
        b.barraEmpilhada.removeAllViews()
        b.categorias.removeAllViews()
        val porCategoria = Repositorio.gastosPorCategoria(doPeriodo)
        val total = porCategoria.sumOf { it.second }
        porCategoria.forEach { (cat, valor) ->
            // cada pedaço tem o tamanho (peso) do valor gasto
            b.barraEmpilhada.addView(View(ctx).apply { setBackgroundColor(Telas.cor(ctx, cat.corRes)) },
                LinearLayout.LayoutParams(0, -1, valor.toFloat()))
            val linha = layoutInflater.inflate(R.layout.item_legenda, b.categorias, false)
            linha.findViewById<View>(R.id.cor).backgroundTintList = android.content.res.ColorStateList.valueOf(Telas.cor(ctx, cat.corRes))
            linha.findViewById<TextView>(R.id.nome).text = cat.rotulo
            linha.findViewById<TextView>(R.id.pct).text = "${if (total > 0) (valor / total * 100).toInt() else 0}%"
            linha.findViewById<TextView>(R.id.valor).text = Formato.reais(valor)
            b.categorias.addView(linha)
        }
        b.barraEmpilhada.visivel(porCategoria.isNotEmpty())
        if (porCategoria.isEmpty()) {
            b.categorias.addView(TextView(ctx).apply {
                text = "Sem saídas neste período."
                setTextColor(Telas.cor(ctx, R.color.texto_suave))
            })
        }
    }

    /** a planilha: uma linha por lançamento, com total e saldo no fim */
    private fun mostrarPlanilha(doPeriodo: List<Lancamento>, entrou: Double, saiu: Double) {
        val ctx = requireContext()
        b.tituloPlanilha.text = when (escala) { Escala.DIA -> "Planilha do dia"; Escala.SEMANA -> "Planilha da semana"; else -> "Planilha do mês" }
        b.rotuloSaldo.text = when (escala) { Escala.DIA -> "Saldo do dia"; Escala.SEMANA -> "Saldo da semana"; else -> "Saldo do mês" }
        b.lancamentos.removeAllViews()
        b.vazio.visivel(doPeriodo.isEmpty())
        doPeriodo.sortedByDescending { it.data }.forEach { l ->
            val v = layoutInflater.inflate(R.layout.item_planilha, b.lancamentos, false)
            v.findViewById<TextView>(R.id.dia).text = Datas.diaBarraMes(l.data)
            v.findViewById<TextView>(R.id.descricao).text = l.descricao.ifBlank { l.categoria.rotulo }
            v.findViewById<TextView>(R.id.categoria).text = l.categoria.rotulo
            v.findViewById<TextView>(R.id.entrou).text = if (l.entrada) Formato.numero(l.valor) else ""
            v.findViewById<TextView>(R.id.saiu).text = if (!l.entrada) Formato.numero(l.valor) else ""
            v.setOnClickListener {
                startActivity(Intent(ctx, LancamentoFormActivity::class.java).putExtra(LancamentoFormActivity.EXTRA_ID, l.id))
            }
            v.setOnLongClickListener {
                MaterialAlertDialogBuilder(ctx).setTitle("Excluir lançamento?")
                    .setMessage("${l.descricao} · ${Formato.reais(l.valor)}")
                    .setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton(R.string.excluir) { _, _ -> Repositorio.removerLancamento(l.id); atualizar() }
                    .show()
                true
            }
            b.lancamentos.addView(v)
        }
        b.totalEntrou.text = Formato.numero(entrou)
        b.totalSaiu.text = Formato.numero(saiu)
        b.saldo.text = Formato.reais(entrou - saiu)
        b.exportar.setOnClickListener { exportar(doPeriodo) }
    }

    /** manda a planilha como texto separado por ";" (dá pra colar no Excel ou no Planilhas) */
    private fun exportar(doPeriodo: List<Lancamento>) {
        val csv = buildString {
            appendLine("Data;Descrição;Categoria;Entrou;Saiu")
            doPeriodo.sortedBy { it.data }.forEach { l ->
                appendLine("${Datas.curta(l.data)};${l.descricao};${l.categoria.rotulo};" +
                    "${if (l.entrada) Formato.numero(l.valor) else ""};${if (!l.entrada) Formato.numero(l.valor) else ""}")
            }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, csv), "Exportar planilha"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
