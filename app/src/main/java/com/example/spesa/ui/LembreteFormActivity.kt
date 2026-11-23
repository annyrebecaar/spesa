package com.example.spesa.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityLembreteFormBinding
import com.example.spesa.model.Aviso
import com.example.spesa.model.Lembrete
import com.example.spesa.model.Repeticao
import com.example.spesa.model.TipoLembrete
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Lembretes
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/** Formulário de lembrete: comprar (com lista vinculada), pagar (com valor) ou fazer. */
class LembreteFormActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "lembrete_id"
        const val EXTRA_DATA = "data"
    }

    private lateinit var b: ActivityLembreteFormBinding
    private lateinit var lembrete: Lembrete
    private var quando = Calendar.getInstance()
    private var listaId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLembreteFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        val existente = Repositorio.lembrete(intent.getLongExtra(EXTRA_ID, -1))
        lembrete = existente ?: Lembrete()
        if (existente != null) {
            b.tituloTela.text = "Editar lembrete"
            b.campoTexto.setText(existente.texto)
            if (existente.valor > 0) b.campoValor.setText(Formato.numero(existente.valor))
            quando.timeInMillis = existente.data
            listaId = existente.listaId
            b.tipo.check(when (existente.tipo) { TipoLembrete.PAGAR -> R.id.tipoPagar; TipoLembrete.FAZER -> R.id.tipoFazer; else -> R.id.tipoComprar })
        } else {
            // Novo: começa no dia tocado na agenda, às 8h
            quando.timeInMillis = intent.getLongExtra(EXTRA_DATA, System.currentTimeMillis())
            quando.set(Calendar.HOUR_OF_DAY, 8); quando.set(Calendar.MINUTE, 0)
        }

        val chipsAviso = Aviso.entries.associateWith { Telas.chip(b.avisar, it.rotulo, it == lembrete.aviso) }
        val chipsRepetir = Repeticao.entries.associateWith { Telas.chip(b.repetir, it.rotulo, it == lembrete.repeticao) }

        b.tipo.addOnButtonCheckedListener { _, _, marcado -> if (marcado) mostrarCamposDoTipo() }
        mostrarCamposDoTipo()
        mostrarDataHora()
        mostrarLista()

        b.data.setOnClickListener {
            DatePickerDialog(this, { _, a, m, d -> quando.set(a, m, d); mostrarDataHora() },
                quando.get(Calendar.YEAR), quando.get(Calendar.MONTH), quando.get(Calendar.DAY_OF_MONTH)).show()
        }
        b.hora.setOnClickListener {
            TimePickerDialog(this, { _, h, min -> quando.set(Calendar.HOUR_OF_DAY, h); quando.set(Calendar.MINUTE, min); mostrarDataHora() },
                quando.get(Calendar.HOUR_OF_DAY), quando.get(Calendar.MINUTE), true).show()
        }
        b.lista.setOnClickListener { escolherLista() }
        b.fechar.setOnClickListener { finish() }

        b.salvar.setOnClickListener {
            val texto = b.campoTexto.text.toString().trim()
            if (texto.isBlank()) { b.campoTexto.error = "Escreva o que lembrar"; return@setOnClickListener }
            lembrete.tipo = tipoMarcado()
            lembrete.texto = texto
            lembrete.data = quando.timeInMillis
            lembrete.listaId = if (lembrete.tipo == TipoLembrete.COMPRAR) listaId else null
            lembrete.valor = if (lembrete.tipo == TipoLembrete.PAGAR) Formato.lerReais(b.campoValor.text.toString()) else 0.0
            lembrete.aviso = chipsAviso.entries.find { it.value.isChecked }?.key ?: Aviso.UM_DIA
            lembrete.repeticao = chipsRepetir.entries.find { it.value.isChecked }?.key ?: Repeticao.NAO
            Repositorio.salvarLembrete(lembrete)
            Lembretes.agendar(this, lembrete)   // marca a notificação no celular
            finish()
        }
    }

    private fun tipoMarcado() = when (b.tipo.checkedButtonId) {
        R.id.tipoPagar -> TipoLembrete.PAGAR
        R.id.tipoFazer -> TipoLembrete.FAZER
        else -> TipoLembrete.COMPRAR
    }

    /** Lista só aparece em "Comprar"; valor só em "Pagar". */
    private fun mostrarCamposDoTipo() {
        val tipo = tipoMarcado()
        b.blocoLista.visivel(tipo == TipoLembrete.COMPRAR)
        b.caixaValor.visivel(tipo == TipoLembrete.PAGAR)
    }

    private fun mostrarDataHora() {
        b.data.text = Datas.curta(quando.timeInMillis)
        b.hora.text = Datas.hora(quando.timeInMillis)
    }

    private fun mostrarLista() {
        val lista = listaId?.let { Repositorio.lista(it) }
        b.lista.text = lista?.let { "${it.titulo} · ${it.itens.size} itens · ${Formato.reais(it.totalEstimado())}" } ?: "Nenhuma (toque para escolher)"
    }

    private fun escolherLista() {
        val ativas = Repositorio.listas.filter { !it.finalizada }
        val nomes = (listOf("Nenhuma") + ativas.map { it.titulo }).toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle("Lista vinculada")
            .setItems(nomes) { _, i ->
                listaId = if (i == 0) null else ativas[i - 1].id
                if (i > 0 && b.campoTexto.text.isNullOrBlank()) b.campoTexto.setText(ativas[i - 1].titulo)
                mostrarLista()
            }.show()
    }
}
