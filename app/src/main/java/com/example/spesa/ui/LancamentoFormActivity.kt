package com.example.spesa.ui

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityLancamentoFormBinding
import com.example.spesa.model.CategoriaGasto
import com.example.spesa.model.Lancamento
import com.example.spesa.model.Repeticao
import com.example.spesa.util.Datas
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import java.util.Calendar

/** Formulário de entrada ou saída de dinheiro (novo ou editando um existente). */
class LancamentoFormActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "lancamento_id"
    }

    private lateinit var b: ActivityLancamentoFormBinding
    private lateinit var lancamento: Lancamento
    private var data = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLancamentoFormBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        val existente = Repositorio.lancamentos.find { it.id == intent.getLongExtra(EXTRA_ID, -1) }
        lancamento = existente ?: Lancamento()
        if (existente != null) {
            b.tituloTela.text = "Editar lançamento"
            b.campoValor.setText(Formato.reais(existente.valor).replace("R$", "").trim())
            b.campoDescricao.setText(existente.descricao)
            data = existente.data
            b.tipo.check(if (existente.entrada) R.id.tipoEntrada else R.id.tipoSaida)
        }

        // Chips criados a partir das listas de opções (enum)
        val chipsCategoria = CategoriaGasto.entries.associateWith { Telas.chip(b.categorias, it.rotulo, it == lancamento.categoria) }
        val chipsRepetir = Repeticao.entries.associateWith { Telas.chip(b.repetir, it.rotulo, it == lancamento.repeticao) }

        // Saída fica vermelha, entrada fica verde (e a categoria muda para Renda)
        b.tipo.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val entrada = id == R.id.tipoEntrada
            b.campoValor.setTextColor(Telas.cor(this, if (entrada) R.color.entrada else R.color.promo))
            if (entrada) chipsCategoria[CategoriaGasto.RENDA]?.isChecked = true
        }

        mostrarData()
        b.data.setOnClickListener {
            val c = Calendar.getInstance().apply { timeInMillis = data }
            DatePickerDialog(this, { _, ano, mes, dia ->
                data = Calendar.getInstance().apply { set(ano, mes, dia, 12, 0) }.timeInMillis
                mostrarData()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.fechar.setOnClickListener { finish() }
        b.salvar.setOnClickListener {
            val valor = Formato.lerReais(b.campoValor.text.toString())
            if (valor <= 0) { b.campoValor.error = "Digite o valor"; return@setOnClickListener }
            lancamento.entrada = b.tipo.checkedButtonId == R.id.tipoEntrada
            lancamento.valor = valor
            lancamento.descricao = b.campoDescricao.text.toString().trim()
            lancamento.categoria = chipsCategoria.entries.find { it.value.isChecked }?.key ?: CategoriaGasto.MERCADO
            lancamento.repeticao = chipsRepetir.entries.find { it.value.isChecked }?.key ?: Repeticao.NAO
            lancamento.data = data
            Repositorio.salvarLancamento(lancamento)
            if (existente == null) {
                val sinal = if (lancamento.entrada) "Entrada" else "Saída"
                Repositorio.registrar("$sinal: ${lancamento.descricao.ifBlank { lancamento.categoria.rotulo }} ${Formato.reais(valor)}",
                    if (lancamento.entrada) "check" else "saida")
            }
            finish()
        }
    }

    private fun mostrarData() {
        b.data.text = Datas.curta(data)
    }
}
