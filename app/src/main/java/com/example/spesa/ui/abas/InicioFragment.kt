package com.example.spesa.ui.abas

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentInicioBinding
import com.example.spesa.databinding.ItemItemListaBinding
import com.example.spesa.ui.LancamentoFormActivity
import com.example.spesa.ui.LembreteFormActivity
import com.example.spesa.ui.MainActivity
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel

/** aba Início: resumo das finanças, atalhos, a lista em andamento e a agenda */
class InicioFragment : Fragment() {
    private var _b: FragmentInicioBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentInicioBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val main = requireActivity() as MainActivity
        b.avatar.setOnClickListener { main.irPara(R.id.aba_ajustes) }
        b.cartaoSaldo.setOnClickListener { (requireActivity() as MainActivity).irPara(R.id.aba_financas) }
        b.verListas.setOnClickListener { main.irPara(R.id.aba_listas) }
        b.verAgenda.setOnClickListener { main.irPara(R.id.aba_agenda) }
        b.agendaVazia.setOnClickListener { startActivity(Intent(requireContext(), LembreteFormActivity::class.java)) }
        b.atalhoLista.setOnClickListener { Telas.dialogoLista(requireActivity(), null) { atualizar() } }
        b.atalhoGasto.setOnClickListener { startActivity(Intent(requireContext(), LancamentoFormActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        val agora = System.currentTimeMillis()
        val nome = Repositorio.usuario?.nome ?: "você"
        b.avatar.text = nome.take(1).uppercase()
        b.ola.text = "Olá, ${nome.substringBefore(" ")}!"
        b.hoje.text = Datas.diaPorExtenso(agora)
        mostrarAvisos()
        mostrarFinancas(agora)
        mostrarLista()
        mostrarAgenda()
    }

    /** faixa vermelha pra cada conta vencida ou que vence hoje: toque pra marcar como paga */
    private fun mostrarAvisos() {
        val ctx = requireContext()
        b.avisos.removeAllViews()
        val hoje = Datas.inicioDoDia(System.currentTimeMillis())
        Repositorio.contasAtrasadas().forEach { conta ->
            val l = Telas.linha(b.avisos)
            l.root.setCardBackgroundColor(Telas.cor(ctx, R.color.promo_clara))
            l.root.strokeColor = Telas.cor(ctx, R.color.promo)
            l.caixaIcone.visivel(true)
            l.caixaIcone.setBackgroundResource(R.drawable.bg_avatar)
            l.caixaIcone.backgroundTintList = ColorStateList.valueOf(Telas.cor(ctx, R.color.promo))
            l.icone.setImageResource(R.drawable.ic_bell)
            l.icone.setColorFilter(Telas.cor(ctx, R.color.white))
            val dias = ((hoje - Datas.inicioDoDia(conta.data)) / 86_400_000L).toInt()
            l.titulo.text = when (dias) {
                0 -> "${conta.texto} vence hoje"
                1 -> "${conta.texto} venceu ontem"
                else -> "${conta.texto} venceu há $dias dias"
            }
            l.subtitulo.text = "Toque quando pagar"
            l.valor.text = Formato.reais(conta.valor)
            l.valor.setTextColor(Telas.cor(ctx, R.color.promo))
            l.root.setOnClickListener {
                Telas.confirmar(requireActivity(), "Pagou ${conta.texto}?",
                    "Vamos lançar ${Formato.reais(conta.valor)} como saída e já marcar a do próximo mês.", "Paguei") {
                    Repositorio.pagarConta(conta); atualizar()
                }
            }
        }
        // faixa amarela pra categoria que passou do teto do mês
        Repositorio.tetosEstourados().forEach { (cat, gasto) ->
            val l = Telas.linha(b.avisos)
            l.root.setCardBackgroundColor(Telas.cor(ctx, R.color.promo_clara))
            l.root.strokeColor = Telas.cor(ctx, R.color.promo)
            l.titulo.text = "${cat.rotulo} passou do teto"
            l.subtitulo.text = "teto de ${Formato.reais(Repositorio.teto(cat))} no mês"
            l.valor.text = Formato.reais(gasto)
            l.valor.setTextColor(Telas.cor(ctx, R.color.promo))
            l.root.setOnClickListener { (requireActivity() as MainActivity).irPara(R.id.aba_financas) }
        }
    }

    /** sobrou no mês + barrinha de quanto do que entrou já foi gasto */
    private fun mostrarFinancas(agora: Long) {
        val inicio = Datas.inicioDoMes(agora)
        val doMes = Repositorio.lancamentosEntre(inicio, Datas.somarMeses(inicio, 1))
        val entrou = doMes.filter { it.entrada }.sumOf { it.valor }
        val saiu = doMes.filter { !it.entrada }.sumOf { it.valor }
        b.rotuloSaldo.text = "Sobrou em ${Datas.mes(agora).lowercase()}"
        b.saldo.text = Formato.reais(entrou - saiu)
        b.entrou.text = "↓ entrou ${Formato.reais(entrou)}"
        b.saiu.text = "↑ saiu ${Formato.reais(saiu)}"
        b.barraGasto.progress = if (entrou > 0) (saiu / entrou * 100).toInt().coerceIn(0, 100) else 0
    }

    /** a lista ativa mais recente, com os 3 próximos itens que faltam */
    private fun mostrarLista() {
        val lista = Repositorio.listas.firstOrNull { !it.finalizada }
        b.itensLista.removeAllViews()
        if (lista == null) {
            b.barraLista.visivel(false)
            b.abrirLista.text = "+ Criar sua primeira lista"
            b.abrirLista.setOnClickListener { Telas.dialogoLista(requireActivity(), null) { atualizar() } }
            return
        }
        b.barraLista.visivel(true)
        val pasta = Repositorio.pasta(lista.pastaId)
        b.barraLista.backgroundTintList = ColorStateList.valueOf(Telas.cor(requireContext(), pasta?.cor?.corRes ?: R.color.primaria))
        val corTexto = Telas.cor(requireContext(), if (pasta?.cor?.textoClaro == false) R.color.texto else R.color.white)
        b.nomeLista.setTextColor(corTexto)
        b.progressoLista.setTextColor(corTexto)
        b.nomeLista.text = lista.titulo
        b.progressoLista.text = "${lista.quantosNoCarrinho()} de ${lista.itens.size}"

        // marca o item aqui mesmo, sem abrir a lista
        lista.itens.filter { !it.noCarrinho }.take(3).forEach { item ->
            val l = ItemItemListaBinding.inflate(layoutInflater, b.itensLista, true)
            l.nome.text = item.nome
            l.detalhe.text = "${item.quantidade} × ${Formato.reais(item.precoUnitario)}"
            l.total.text = Formato.reais(item.total())
            l.tagPromo.visivel(item.promocao)
            l.tagCategoria.visivel(false)
            l.marcado.setOnClickListener {
                item.noCarrinho = true
                Repositorio.salvar()
                Repositorio.registrar("Você marcou ${item.nome} em ${lista.titulo}", "check")
                l.nome.paintFlags = l.nome.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                l.root.animate().alpha(0f).setDuration(350).withEndAction { atualizar() }
            }
            l.root.setOnClickListener { Telas.abrirLista(requireActivity(), lista.id) }
        }
        if (lista.itens.none { !it.noCarrinho }) {
            b.abrirLista.text = "Tudo no carrinho! Finalizar compra ›"
        } else {
            b.abrirLista.text = "Abrir lista · ${Formato.reais(lista.totalEstimado())} ›"
        }
        b.abrirLista.setOnClickListener { Telas.abrirLista(requireActivity(), lista.id) }
    }

    /** os 3 próximos compromissos */
    private fun mostrarAgenda() {
        b.agenda.removeAllViews()
        val proximos = Repositorio.proximosLembretes().take(3)
        b.agendaVazia.visivel(proximos.isEmpty())
        proximos.forEach { lem ->
            val l = Telas.linha(b.agenda)
            l.caixaData.visivel(true)
            l.dataSemana.text = Datas.diaSemanaCurto(lem.data)
            l.dataDia.text = Datas.dia(lem.data)
            l.titulo.text = "${lem.tipo.rotulo}: ${lem.texto}"
            val lista = lem.listaId?.let { Repositorio.lista(it) }
            l.subtitulo.text = when {
                lista != null -> "${Datas.hora(lem.data)} · ${lista.itens.size} itens · ${Formato.reais(lista.totalEstimado())}"
                lem.valor > 0 -> "${Datas.hora(lem.data)} · ${Formato.reais(lem.valor)}"
                else -> Datas.hora(lem.data)
            }
            l.root.setOnClickListener {
                if (lista != null) Telas.abrirLista(requireActivity(), lista.id)
                else startActivity(Intent(requireContext(), LembreteFormActivity::class.java).putExtra(LembreteFormActivity.EXTRA_ID, lem.id))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
