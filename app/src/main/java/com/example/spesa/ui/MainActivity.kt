package com.example.spesa.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityMainBinding
import com.example.spesa.ui.abas.AgendaFragment
import com.example.spesa.ui.abas.AjustesFragment
import com.example.spesa.ui.abas.FinancasFragment
import com.example.spesa.ui.abas.InicioFragment
import com.example.spesa.ui.abas.ListasFragment

/**
 * Tela principal com o menu de baixo (5 abas).
 * Cada aba é um Fragment: um "pedaço de tela" que trocamos dentro da moldura.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Para outras telas pedirem para abrir numa aba específica. */
        const val EXTRA_ABA = "aba"
    }

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)

        b.menuInferior.setOnItemSelectedListener { item ->
            mostrar(
                when (item.itemId) {
                    R.id.aba_listas -> ListasFragment()
                    R.id.aba_financas -> FinancasFragment()
                    R.id.aba_agenda -> AgendaFragment()
                    R.id.aba_ajustes -> AjustesFragment()
                    else -> InicioFragment()
                }
            )
            true
        }
        // Android 13 ou mais novo: pede permissão para mostrar as notificações dos lembretes
        com.example.spesa.util.Lembretes.criarCanal(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (savedInstanceState == null) {
            b.menuInferior.selectedItemId = intent.getIntExtra(EXTRA_ABA, R.id.aba_inicio)
        }
    }

    /** Troca o conteúdo da moldura. */
    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.conteudo, fragment).commit()
    }

    /** Usado pelos atalhos da tela de Início (ex.: o cartão do saldo abre Finanças). */
    fun irPara(abaId: Int) {
        b.menuInferior.selectedItemId = abaId
    }
}
