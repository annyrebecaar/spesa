package com.example.spesa.ui.abas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.FragmentAjustesBinding
import com.example.spesa.ui.LoginActivity
import com.example.spesa.util.Lembretes
import com.example.spesa.util.Telas
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Aba Ajustes: perfil, preferências, atalhos para Mercados/Despensa/Pastas e conta. */
class AjustesFragment : Fragment() {
    private var _b: FragmentAjustesBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentAjustesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        mostrarPerfil()

        b.lembretes.isChecked = Repositorio.preferencia(Repositorio.CHAVE_LEMBRETES_LIGADOS, true)
        b.lembretes.setOnCheckedChangeListener { _, ligado ->
            Repositorio.mudarPreferencia(Repositorio.CHAVE_LEMBRETES_LIGADOS, ligado)
            Lembretes.reagendarTodos(requireContext())
        }

        val escuro = Repositorio.preferencia(Repositorio.CHAVE_TEMA_ESCURO, false)
        b.tema.check(if (escuro) R.id.temaEscuro else R.id.temaClaro)
        b.tema.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val querEscuro = id == R.id.temaEscuro
            if (querEscuro == Repositorio.preferencia(Repositorio.CHAVE_TEMA_ESCURO, false)) return@addOnButtonCheckedListener
            Repositorio.mudarPreferencia(Repositorio.CHAVE_TEMA_ESCURO, querEscuro)
            // Troca o tema do app inteiro (a tela é recriada sozinha)
            AppCompatDelegate.setDefaultNightMode(if (querEscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        b.editarPerfil.setOnClickListener { editarPerfil() }
        b.gerenciarPastas.setOnClickListener { gerenciarPastas() }
        b.senha.setOnClickListener { trocarSenha() }
        b.backup.setOnClickListener { exportar() }
        b.exemplo.setOnClickListener {
            Telas.confirmar(requireActivity(), "Preencher com exemplo?",
                "Vamos acrescentar mercados, listas, finanças e lembretes de exemplo. Nada do que você já tem é apagado.", "Preencher") {
                com.example.spesa.dados.Exemplo.preencher()
                Lembretes.reagendarTodos(requireContext())
                Toast.makeText(requireContext(), "Pronto! Dá uma olhada no Início.", Toast.LENGTH_SHORT).show()
            }
        }
        b.ajuda.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Sobre o Spesa")
                .setMessage("Spesa é uma lista de compras inteligente: soma em tempo real, teto de gastos, comparação de preços entre mercados, finanças do mês e agenda de compras e contas.\n\nDicas:\n• Segure o dedo numa lista, pasta ou lançamento para ver mais opções.\n• Ao finalizar uma compra, o valor pode ir direto para as Finanças.\n\nTodos os dados ficam só neste celular.")
                .setPositiveButton("Entendi", null).show()
        }
        b.sair.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Sair da conta?")
                .setMessage("Suas listas continuam salvas neste celular.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Sair") { _, _ ->
                    Repositorio.sair()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finishAffinity()
                }.show()
        }
    }

    private fun mostrarPerfil() {
        val u = Repositorio.usuario
        b.nome.text = u?.nome ?: "Sem nome"
        b.email.text = u?.email ?: ""
        b.avatar.text = (u?.nome ?: "?").take(1).uppercase()
    }

    /** Cria uma caixinha de texto para usar dentro dos diálogos. */
    private fun campo(dica: String, valor: String = "", senha: Boolean = false): EditText = EditText(requireContext()).apply {
        hint = dica; setText(valor)
        if (senha) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun caixa(vararg campos: View): LinearLayout = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        val p = (24 * resources.displayMetrics.density).toInt()
        setPadding(p, p / 3, p, 0)
        campos.forEach { addView(it) }
    }

    private fun editarPerfil() {
        val u = Repositorio.usuario ?: return
        val nome = campo("Nome", u.nome)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Editar perfil").setView(caixa(nome))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                u.nome = nome.text.toString().trim().ifBlank { u.nome }
                Repositorio.salvar(); mostrarPerfil()
            }.show()
    }

    private fun trocarSenha() {
        val u = Repositorio.usuario ?: return
        val atual = campo("Senha atual", senha = true)
        val nova = campo("Nova senha (mínimo 8)", senha = true)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Trocar senha").setView(caixa(atual, nova))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val msg = when {
                    Repositorio.hash(atual.text.toString()) != u.senhaHash -> "Senha atual incorreta"
                    nova.text.length < 8 -> "A nova senha precisa de 8 caracteres"
                    else -> { u.senhaHash = Repositorio.hash(nova.text.toString()); Repositorio.salvar(); "Senha trocada" }
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun gerenciarPastas() {
        val nomes = (Repositorio.pastas.map { it.nome } + "+ Nova pasta").toTypedArray()
        MaterialAlertDialogBuilder(requireContext()).setTitle("Pastas (toque para editar)")
            .setItems(nomes) { _, i ->
                Telas.dialogoPasta(requireActivity(), Repositorio.pastas.getOrNull(i)) {}
            }.show()
    }

    /** Backup: manda todos os dados em texto (JSON) para onde o usuário escolher (e-mail, Drive, WhatsApp...). */
    private fun exportar() {
        val envio = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Backup do Spesa")
            .putExtra(Intent.EXTRA_TEXT, Repositorio.exportarTudo())
        startActivity(Intent.createChooser(envio, "Salvar backup em..."))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
