package com.example.spesa.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivitySplashBinding

/** Tela de abertura: mostra a marca, carrega os dados e vai para a principal. */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivitySplashBinding.inflate(layoutInflater).root)

        Repositorio.iniciar(this)
        // Aplica o tema escolhido em Ajustes (claro ou escuro).
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (Repositorio.preferencia(Repositorio.CHAVE_TEMA_ESCURO, false))
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )

        Handler(Looper.getMainLooper()).postDelayed({
            // Com conta e logado: vai direto para o app. Senão, para a tela de entrar.
            val destino = if (Repositorio.estaLogado()) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, destino))
            finish()
        }, 1200)
    }
}
