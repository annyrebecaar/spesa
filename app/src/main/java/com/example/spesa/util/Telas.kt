package com.example.spesa.util

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.spesa.R
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.DialogNovaListaBinding
import com.example.spesa.databinding.ItemLinhaBinding
import com.example.spesa.databinding.ItemPastaBinding
import com.example.spesa.model.CorPasta
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Pasta
import com.example.spesa.ui.ListaActivity
import com.example.spesa.ui.PastaActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Pedaços de tela e diálogos usados em mais de uma tela.
 * Ficam aqui para não repetir o mesmo código em vários lugares.
 */
object Telas {

    fun cor(ctx: android.content.Context, corRes: Int) = ContextCompat.getColor(ctx, corRes)

    /** Cria uma linha (item_linha.xml) dentro de um container e devolve para preencher. */
    fun linha(container: ViewGroup): ItemLinhaBinding =
        ItemLinhaBinding.inflate(LayoutInflater.from(container.context), container, true)

    /** Linha de uma lista de compras (usada em Listas, Pasta e Histórico). */
    fun linhaDaLista(container: ViewGroup, lista: ListaDeCompras, aoSegurar: (ListaDeCompras) -> Unit) {
        val ctx = container.context
        val l = linha(container)
        val pasta = Repositorio.pasta(lista.pastaId)
        l.barra.visivel(true)
        l.barra.backgroundTintList = ColorStateList.valueOf(cor(ctx, pasta?.cor?.corRes ?: R.color.borda_forte))
        l.titulo.text = lista.titulo

        val partes = mutableListOf<String>()
        pasta?.let { partes.add(it.nome) }
        if (lista.finalizada) {
            lista.finalizadaEm?.let { partes.add(Datas.diaMes(it)) }
            Repositorio.mercado(lista.mercadoCompraId)?.let { partes.add(it.nome) } ?: partes.add("${lista.itens.size} itens")
            l.valor.text = Formato.reais(lista.totalPago ?: lista.totalEstimado())
            l.extra.visivel(true)
            l.extra.text = "✓ finalizada"
        } else {
            val pegos = lista.quantosNoCarrinho()
            partes.add(if (pegos > 0) "$pegos de ${lista.itens.size} no carrinho" else "${lista.itens.size} itens")
            val promos = lista.itens.count { it.promocao }
            if (promos > 0) partes.add("$promos em promoção")
            l.valor.text = Formato.reais(lista.totalEstimado())
            if (pegos > 0 && lista.itens.isNotEmpty()) {
                l.progresso.visivel(true)
                l.progresso.progress = pegos * 100 / lista.itens.size
            }
        }
        l.subtitulo.text = partes.joinToString(" · ")
        l.root.setOnClickListener { abrirLista(ctx as Activity, lista.id) }
        l.root.setOnLongClickListener { aoSegurar(lista); true }
    }

    /** Preenche o desenho de pasta com nome, cor e número de listas. */
    fun pasta(container: ViewGroup, pasta: Pasta): ItemPastaBinding {
        val ctx = container.context
        val p = ItemPastaBinding.inflate(LayoutInflater.from(ctx), container, false)
        val c = ColorStateList.valueOf(cor(ctx, pasta.cor.corRes))
        p.orelha.backgroundTintList = c
        p.fundo.backgroundTintList = c
        p.frente.backgroundTintList = c
        p.fundo.alpha = 0.85f
        val corTexto = if (pasta.cor.textoClaro) R.color.white else R.color.texto
        p.nome.setTextColor(cor(ctx, corTexto))
        p.quantas.setTextColor(cor(ctx, corTexto))
        p.nome.text = pasta.nome
        val n = Repositorio.listasDaPasta(pasta.id).size
        p.quantas.text = if (n == 1) "1 lista" else "$n listas"
        p.root.setOnClickListener {
            ctx.startActivity(Intent(ctx, PastaActivity::class.java).putExtra(PastaActivity.EXTRA_PASTA_ID, pasta.id))
        }
        return p
    }

    fun abrirLista(activity: Activity, id: Long) {
        activity.startActivity(Intent(activity, ListaActivity::class.java).putExtra(ListaActivity.EXTRA_LISTA_ID, id))
    }

    /** Monta um chip de escolha (usado em todos os grupos de chips criados no código). */
    fun chip(grupo: ChipGroup, texto: String, marcado: Boolean = false): Chip {
        val chip = Chip(grupo.context, null, com.google.android.material.R.attr.chipStyle)
        chip.setChipBackgroundColorResource(R.color.chip_fundo)
        chip.setChipStrokeColorResource(R.color.chip_borda)
        chip.chipStrokeWidth = grupo.resources.displayMetrics.density * 1.5f
        chip.setTextColor(ContextCompat.getColorStateList(grupo.context, R.color.chip_texto))
        chip.isCheckable = true
        chip.isCheckedIconVisible = false
        chip.text = texto
        chip.id = android.view.View.generateViewId()
        grupo.addView(chip)
        chip.isChecked = marcado
        return chip
    }

    /**
     * Diálogo de criar (lista == null) ou editar uma lista.
     * pastaInicial: pasta já marcada (quando criamos de dentro de uma pasta).
     */
    fun dialogoLista(activity: Activity, lista: ListaDeCompras?, pastaInicial: Long? = null, depois: () -> Unit) {
        val campos = DialogNovaListaBinding.inflate(activity.layoutInflater)
        val chips = Repositorio.pastas.associateWith { p ->
            chip(campos.grupoPastas, p.nome, p.id == (lista?.pastaId ?: pastaInicial))
        }
        if (lista != null) {
            campos.campoTitulo.setText(lista.titulo)
            if (lista.orcamento > 0) campos.campoOrcamento.setText(lista.orcamento.toString().replace(".", ","))
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(if (lista == null) "Nova lista" else "Editar lista")
            .setView(campos.root)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(if (lista == null) R.string.criar else R.string.salvar) { _, _ ->
                val titulo = campos.campoTitulo.text.toString().trim().ifBlank { "Minha lista" }
                val orcamento = Formato.lerReais(campos.campoOrcamento.text.toString())
                val pastaId = chips.entries.find { it.value.isChecked }?.key?.id
                if (lista == null) {
                    val nova = ListaDeCompras(titulo = titulo, orcamento = orcamento, pastaId = pastaId)
                    Repositorio.adicionarLista(nova)
                    Repositorio.registrar("Você criou a lista $titulo", "mais")
                    abrirLista(activity, nova.id)
                } else {
                    lista.titulo = titulo
                    lista.orcamento = orcamento
                    lista.pastaId = pastaId
                    Repositorio.salvar()
                }
                depois()
            }
            .show()
    }

    /** Menu ao segurar o dedo numa lista: editar, duplicar, finalizar/reabrir, excluir. */
    fun menuDaLista(activity: Activity, lista: ListaDeCompras, depois: () -> Unit) {
        val opcoes = arrayOf("Editar", "Duplicar", if (lista.finalizada) "Reabrir" else "Finalizar", "Excluir")
        MaterialAlertDialogBuilder(activity)
            .setTitle(lista.titulo)
            .setItems(opcoes) { _, qual ->
                when (qual) {
                    0 -> dialogoLista(activity, lista, depois = depois)
                    1 -> { Repositorio.adicionarLista(lista.clonar("${lista.titulo} (cópia)")); depois() }
                    2 -> {
                        lista.finalizada = !lista.finalizada
                        lista.finalizadaEm = if (lista.finalizada) System.currentTimeMillis() else null
                        Repositorio.salvar(); depois()
                    }
                    3 -> MaterialAlertDialogBuilder(activity)
                        .setTitle("Excluir")
                        .setMessage("Excluir a lista \"${lista.titulo}\"? Isso não pode ser desfeito.")
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.excluir) { _, _ -> Repositorio.removerLista(lista.id); depois() }
                        .show()
                }
            }
            .show()
    }

    /** Diálogo de criar (pasta == null) ou editar uma pasta: nome + cor. */
    fun dialogoPasta(activity: Activity, pasta: Pasta?, depois: () -> Unit) {
        val ctx = activity
        val dp = ctx.resources.displayMetrics.density
        val caixa = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)
        }
        val campoLayout = TextInputLayout(ctx, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply { hint = "Nome da pasta" }
        val campo = TextInputEditText(campoLayout.context).apply { setText(pasta?.nome ?: "") }
        campoLayout.addView(campo)
        caixa.addView(campoLayout)
        val grupo = ChipGroup(ctx).apply { isSingleSelection = true; isSelectionRequired = true }
        caixa.addView(grupo)
        val chips = CorPasta.entries.associateWith { c -> chip(grupo, c.rotulo, c == (pasta?.cor ?: CorPasta.VERDE)) }
        chips.forEach { (c, chip) -> chip.chipIcon = null; chip.setChipIconResource(R.drawable.bg_avatar); chip.chipIconTint = ColorStateList.valueOf(cor(ctx, c.corRes)); chip.isChipIconVisible = true }

        val dialogo = MaterialAlertDialogBuilder(ctx)
            .setTitle(if (pasta == null) "Nova pasta" else "Editar pasta")
            .setView(caixa)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.salvar) { _, _ ->
                val nome = campo.text.toString().trim().ifBlank { "Nova pasta" }
                val corEscolhida = chips.entries.find { it.value.isChecked }?.key ?: CorPasta.VERDE
                val p = pasta ?: Pasta()
                p.nome = nome; p.cor = corEscolhida
                Repositorio.salvarPasta(p)
                depois()
            }
        if (pasta != null) dialogo.setNeutralButton("Excluir") { _, _ ->
            Repositorio.removerPasta(pasta.id); depois()
        }
        dialogo.show()
    }
}
