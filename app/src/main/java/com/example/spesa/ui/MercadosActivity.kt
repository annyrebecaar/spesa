package com.example.spesa.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spesa.R
import com.example.spesa.adaptadores.MercadosAdapter
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityMercadosBinding
import com.example.spesa.databinding.DialogMercadoBinding
import com.example.spesa.model.Mercado
import com.example.spesa.util.visivel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Cadastro dos mercados, lojas e feiras onde o usuário compra. */
class MercadosActivity : AppCompatActivity() {

    private lateinit var b: ActivityMercadosBinding
    private lateinit var adapter: MercadosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMercadosBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        b.toolbar.setNavigationOnClickListener { finish() }

        adapter = MercadosAdapter(aoClicar = { dialogoMercado(it) }, aoSegurar = ::confirmarExclusao)
        b.listaMercados.layoutManager = LinearLayoutManager(this)
        b.listaMercados.adapter = adapter

        b.botaoNovoMercado.setOnClickListener { dialogoMercado(null) }
        atualizar()
    }

    private fun atualizar() {
        adapter.atualizar(Repositorio.mercados.sortedBy { it.nome.lowercase() })
        b.textoVazio.visivel(Repositorio.mercados.isEmpty())
    }

    /** Mesmo diálogo para criar (mercado == null) e editar. */
    private fun dialogoMercado(mercado: Mercado?) {
        val campos = DialogMercadoBinding.inflate(layoutInflater)
        if (mercado != null) {
            campos.campoNome.setText(mercado.nome)
            campos.campoEndereco.setText(mercado.endereco)
            campos.campoObservacao.setText(mercado.observacao)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(if (mercado == null) R.string.novo_mercado else R.string.editar_mercado)
            .setView(campos.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val nome = campos.campoNome.text.toString().trim()
                if (nome.isEmpty()) {
                    Toast.makeText(this, "O mercado precisa de um nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val alvo = mercado ?: Mercado()
                alvo.nome = nome
                alvo.endereco = campos.campoEndereco.text.toString().trim()
                alvo.observacao = campos.campoObservacao.text.toString().trim()
                Repositorio.salvarMercado(alvo)
                atualizar()
            }
            .show()
    }

    private fun confirmarExclusao(mercado: Mercado) {
        MaterialAlertDialogBuilder(this)
            .setTitle(mercado.nome)
            .setMessage("Excluir este mercado? Os preços registrados nele também somem.")
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.excluir) { _, _ ->
                Repositorio.removerMercado(mercado.id)
                atualizar()
            }
            .show()
    }
}
