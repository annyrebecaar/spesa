package com.example.spesa.ui.abas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentInicioBinding
import com.example.spesa.ui.LancamentoFormActivity
import com.example.spesa.ui.LembreteFormActivity
import com.example.spesa.ui.MainActivity
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel

/** Aba Início: saudação, saldo do mês, atalhos, próximos compromissos e atividade recente. */
class InicioFragment : Fragment() {
    private var _b: FragmentInicioBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentInicioBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        val main = requireActivity() as MainActivity
        b.cartaoSaldo.setOnClickListener { main.irPara(R.id.aba_financas) }
        b.verAgenda.setOnClickListener { main.irPara(R.id.aba_agenda) }
        b.sino.setOnClickListener { main.irPara(R.id.aba_agenda) }
        b.atalhoLista.setOnClickListener { Telas.dialogoLista(requireActivity(), null) { atualizar() } }
        b.atalhoGasto.setOnClickListener { startActivity(Intent(requireContext(), LancamentoFormActivity::class.java)) }
        b.atalhoContinuar.setOnClickListener {
            // Abre a lista ativa mais recente
            val ultima = Repositorio.listas.firstOrNull { !it.finalizada }
            if (ultima == null) Toast.makeText(requireContext(), "Nenhuma lista ativa. Crie uma em Nova lista.", Toast.LENGTH_SHORT).show()
            else Telas.abrirLista(requireActivity(), ultima.id)
        }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        val agora = System.currentTimeMillis()
        val nome = Repositorio.usuario?.nome ?: "você"
        b.avatar.text = nome.take(1).uppercase()
        b.hoje.text = Datas.diaPorExtenso(agora)
        b.ola.text = "Olá, ${nome.substringBefore(" ")}"

        // Saldo do mês: entradas menos saídas
        val inicio = Datas.inicioDoMes(agora)
        val doMes = Repositorio.lancamentosEntre(inicio, Datas.somarMeses(inicio, 1))
        val saldo = doMes.filter { it.entrada }.sumOf { it.valor } - doMes.filter { !it.entrada }.sumOf { it.valor }
        b.rotuloSaldo.text = "Sobrou em ${Datas.mes(agora).lowercase()}"
        b.saldo.text = Formato.reais(saldo)

        // Próximos 3 compromissos
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
                lista != null -> "${lista.itens.size} itens · ${Formato.reais(lista.totalEstimado())}"
                lem.valor > 0 -> "${Formato.reais(lem.valor)} · ${Datas.hora(lem.data)}"
                else -> Datas.hora(lem.data)
            }
            l.root.setOnClickListener {
                if (lista != null) Telas.abrirLista(requireActivity(), lista.id)
                else startActivity(Intent(requireContext(), LembreteFormActivity::class.java).putExtra(LembreteFormActivity.EXTRA_ID, lem.id))
            }
        }

        // Atividade recente (últimas 4)
        b.atividades.removeAllViews()
        val atv = Repositorio.atividades.take(4)
        b.atividadesVazia.visivel(atv.isEmpty())
        atv.forEach { a ->
            val l = Telas.linha(b.atividades)
            l.caixaIcone.visivel(true)
            l.icone.setImageResource(when (a.tipo) { "mais" -> R.drawable.ic_plus; "saida" -> R.drawable.ic_arrow_up; else -> R.drawable.ic_check_line })
            l.icone.setColorFilter(Telas.cor(requireContext(), if (a.tipo == "saida") R.color.promo else R.color.primaria))
            if (a.tipo == "saida") l.caixaIcone.setBackgroundResource(R.drawable.bg_vermelho_claro)
            l.titulo.text = a.texto
            l.titulo.textSize = 14f
            l.titulo.setTypeface(null, android.graphics.Typeface.NORMAL)
            l.subtitulo.visivel(false)
            l.valor.text = Datas.haQuanto(a.quando)
            l.valor.textSize = 12f
            l.valor.setTextColor(Telas.cor(requireContext(), R.color.texto_suave))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
