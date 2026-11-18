package com.example.spesa.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.ActivityPastaBinding
import com.example.spesa.model.Pasta
import com.example.spesa.util.Formato
import com.example.spesa.util.Telas
import com.example.spesa.util.visivel

/** Pasta aberta: as listas que estão dentro dela e o botão de criar uma nova lista ali. */
class PastaActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PASTA_ID = "pasta_id"
    }

    private lateinit var b: ActivityPastaBinding
    private lateinit var pasta: Pasta

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPastaBinding.inflate(layoutInflater)
        setContentView(b.root)
        Repositorio.iniciar(this)
        pasta = Repositorio.pasta(intent.getLongExtra(EXTRA_PASTA_ID, -1)) ?: run { finish(); return }

        b.voltar.setOnClickListener { finish() }
        b.editar.setOnClickListener {
            Telas.dialogoPasta(this, pasta) {
                // Se a pasta foi excluída, volta para a tela anterior.
                if (Repositorio.pasta(pasta.id) == null) finish() else atualizar()
            }
        }
        b.novaLista.setOnClickListener { Telas.dialogoLista(this, null, pasta.id) { atualizar() } }
    }

    override fun onResume() {
        super.onResume()
        atualizar()
    }

    private fun atualizar() {
        val listas = Repositorio.listasDaPasta(pasta.id)
        b.nome.text = pasta.nome
        b.iconePasta.setColorFilter(Telas.cor(this, pasta.cor.corRes))
        b.resumo.text = "${listas.size} ${if (listas.size == 1) "lista" else "listas"} · ${Formato.reais(listas.sumOf { it.totalEstimado() })} no total"
        b.listas.removeAllViews()
        listas.forEach { Telas.linhaDaLista(b.listas, it) { l -> Telas.menuDaLista(this, l) { atualizar() } } }
        b.vazio.visivel(listas.isEmpty())
    }
}
