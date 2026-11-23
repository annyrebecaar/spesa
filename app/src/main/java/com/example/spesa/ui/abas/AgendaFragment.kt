package com.example.spesa.ui.abas

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentAgendaBinding
import com.example.spesa.model.Lembrete
import com.example.spesa.model.TipoLembrete
import com.example.spesa.ui.LembreteFormActivity
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/** Aba Agenda: calendário do mês com bolinhas nos dias com compromisso e a lista do dia escolhido. */
class AgendaFragment : Fragment() {
    private var _b: FragmentAgendaBinding? = null
    private val b get() = _b!!

    private var mes = Datas.inicioDoMes(System.currentTimeMillis())   // mês na tela
    private var escolhido = Datas.inicioDoDia(System.currentTimeMillis())  // dia tocado

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentAgendaBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        // Linha com as iniciais dos dias da semana
        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { letra ->
            b.semana.addView(TextView(requireContext()).apply {
                text = letra; gravity = Gravity.CENTER; textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Telas.cor(requireContext(), R.color.texto_suave))
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        b.legendaPagar.backgroundTintList = ColorStateList.valueOf(Telas.cor(requireContext(), R.color.promo))
        b.anterior.setOnClickListener { mes = Datas.somarMeses(mes, -1); atualizar() }
        b.proximo.setOnClickListener { mes = Datas.somarMeses(mes, 1); atualizar() }
        b.novo.setOnClickListener {
            startActivity(Intent(requireContext(), LembreteFormActivity::class.java).putExtra(LembreteFormActivity.EXTRA_DATA, escolhido))
        }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        b.mes.text = Datas.mesAno(mes)
        montarCalendario()
        montarDia()
    }

    private fun montarCalendario() {
        b.calendario.removeAllViews()
        val cal = Calendar.getInstance().apply { timeInMillis = mes }
        val diasNoMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val vazioAntes = cal.get(Calendar.DAY_OF_WEEK) - 1   // domingo = 0 espaços
        val hoje = Datas.inicioDoDia(System.currentTimeMillis())
        var linha: LinearLayout? = null

        for (pos in 0 until vazioAntes + diasNoMes) {
            if (pos % 7 == 0) {
                linha = LinearLayout(requireContext())
                b.calendario.addView(linha)
            }
            val celula = layoutInflater.inflate(R.layout.item_dia, linha, false)
            linha!!.addView(celula)
            if (pos < vazioAntes) continue

            val dia = Datas.somarDias(mes, pos - vazioAntes)
            val numero = celula.findViewById<TextView>(R.id.numero)
            val ponto = celula.findViewById<View>(R.id.ponto)
            numero.text = Datas.dia(dia)
            when {
                dia == escolhido -> {
                    numero.setBackgroundResource(R.drawable.bg_avatar)
                    numero.setTextColor(Telas.cor(requireContext(), R.color.white))
                    numero.setTypeface(null, Typeface.BOLD)
                }
                dia == hoje -> numero.setBackgroundResource(R.drawable.bg_dia_hoje)
            }
            // Bolinha: verde se tem compra, vermelha se tem conta/tarefa
            val doDia = Repositorio.lembretes.filter { Datas.mesmoDia(it.data, dia) && !it.feito }
            if (doDia.isNotEmpty()) {
                ponto.visibility = View.VISIBLE
                val cor = if (doDia.all { it.tipo == TipoLembrete.COMPRAR }) R.color.primaria else R.color.promo
                ponto.backgroundTintList = ColorStateList.valueOf(Telas.cor(requireContext(), cor))
            }
            celula.setOnClickListener { escolhido = dia; atualizar() }
        }
        // Completa a última semana para as colunas ficarem alinhadas
        val sobra = (7 - (vazioAntes + diasNoMes) % 7) % 7
        repeat(sobra) { linha?.addView(View(requireContext()), LinearLayout.LayoutParams(0, 1, 1f)) }
    }

    private fun montarDia() {
        val doDia = Repositorio.lembretes.filter { Datas.mesmoDia(it.data, escolhido) }.sortedBy { it.data }
        b.diaEscolhido.text = Datas.diaCompleto(escolhido)
        b.quantos.text = when (doDia.size) { 0 -> ""; 1 -> "1 compromisso"; else -> "${doDia.size} compromissos" }
        b.vazio.visivel(doDia.isEmpty())
        b.compromissos.removeAllViews()
        doDia.forEach { linhaLembrete(it) }
    }

    private fun linhaLembrete(lem: Lembrete) {
        val ctx = requireContext()
        val l = Telas.linha(b.compromissos)
        val lista = lem.listaId?.let { Repositorio.lista(it) }
        l.caixaIcone.visivel(true)
        l.icone.setImageResource(when (lem.tipo) { TipoLembrete.COMPRAR -> R.drawable.ic_lists; TipoLembrete.PAGAR -> R.drawable.ic_coin; else -> R.drawable.ic_check_line })
        val vermelho = lem.tipo != TipoLembrete.COMPRAR
        if (vermelho) l.caixaIcone.setBackgroundResource(R.drawable.bg_vermelho_claro)
        l.icone.setColorFilter(Telas.cor(ctx, if (vermelho) R.color.promo else R.color.primaria))
        l.titulo.text = "${lem.tipo.rotulo}: ${lem.texto}"
        if (lem.feito) l.titulo.paintFlags = l.titulo.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        val partes = mutableListOf(Datas.hora(lem.data))
        lista?.let { partes.add("${it.itens.size} itens"); partes.add(Formato.reais(it.totalEstimado())) }
        if (lem.valor > 0) partes.add(Formato.reais(lem.valor))
        if (lem.feito) partes.add("feito")
        l.subtitulo.text = partes.joinToString(" · ")
        l.seta.visivel(true)
        l.root.setOnClickListener {
            if (lista != null) Telas.abrirLista(requireActivity(), lista.id)
            else startActivity(Intent(ctx, LembreteFormActivity::class.java).putExtra(LembreteFormActivity.EXTRA_ID, lem.id))
        }
        l.root.setOnLongClickListener {
            MaterialAlertDialogBuilder(ctx).setTitle(lem.texto)
                .setItems(arrayOf(if (lem.feito) "Marcar como não feito" else "Marcar como feito", "Editar", "Excluir")) { _, q ->
                    when (q) {
                        0 -> {
                            if (!lem.feito && lem.tipo == TipoLembrete.PAGAR) Repositorio.pagarConta(lem)
                            else { lem.feito = !lem.feito; Repositorio.salvarLembrete(lem); Repositorio.gerarRepeticoes() }
                            atualizar()
                        }
                        1 -> startActivity(Intent(ctx, LembreteFormActivity::class.java).putExtra(LembreteFormActivity.EXTRA_ID, lem.id))
                        2 -> { Repositorio.removerLembrete(lem.id); atualizar() }
                    }
                }.show()
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
