package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityCadastroBinding

/** Tela de criar conta: valida os campos e salva o usuário no celular. */
class CadastroActivity : AppCompatActivity() {
    private lateinit var b: ActivityCadastroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        b.voltar.setOnClickListener { finish() }
        b.irLogin.setOnClickListener { finish() }
        b.botaoCriar.setOnClickListener { criar() }
    }

    private fun criar() {
        val nome = b.campoNome.text.toString().trim()
        val email = b.campoEmail.text.toString().trim()
        val senha = b.campoSenha.text.toString()
        val uso = when (b.grupoUso.checkedChipId) {
            R.id.usoRepublica -> "República"
            R.id.usoSo -> "Só eu"
            else -> "Família"
        }
        // Cada if confere um campo e mostra o erro embaixo dele.
        if (nome.isBlank()) { b.campoNome.error = "Diga como quer ser chamado"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.campoEmail.error = "E-mail inválido"; return }
        if (senha.length < 8) { b.campoSenha.error = "A senha precisa de pelo menos 8 caracteres"; return }
        if (!b.termos.isChecked) { b.termos.error = "Aceite os termos para continuar"; return }

        Repositorio.criarConta(nome, email, senha, uso)
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
