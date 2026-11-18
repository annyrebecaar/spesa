package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Tela de entrar. A conta fica salva só neste celular, então o login
 * confere o e-mail e a senha com o que foi cadastrado aqui.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        // Se já existe conta, deixa o e-mail preenchido.
        Repositorio.usuario?.let { b.campoEmail.setText(it.email) }

        b.botaoEntrar.setOnClickListener { entrar() }
        b.irCadastro.setOnClickListener { startActivity(Intent(this, CadastroActivity::class.java)) }
        b.esqueci.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Esqueci minha senha")
                .setMessage("Sua conta fica guardada só neste celular. Se esqueceu a senha, é preciso criar uma conta nova (suas listas continuam aqui).")
                .setPositiveButton("Criar conta nova") { _, _ -> startActivity(Intent(this, CadastroActivity::class.java)) }
                .setNegativeButton("Voltar", null)
                .show()
        }
    }

    private fun entrar() {
        val email = b.campoEmail.text.toString()
        val senha = b.campoSenha.text.toString()
        when {
            email.isBlank() || senha.isBlank() -> b.campoSenha.error = "Preencha e-mail e senha"
            Repositorio.usuario == null -> b.campoEmail.error = "Nenhuma conta neste celular. Toque em Criar conta."
            !Repositorio.entrar(email, senha) -> b.campoSenha.error = "E-mail ou senha incorretos"
            else -> {
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()   // fecha o login para o botão voltar não retornar aqui
            }
        }
    }
}
