package com.example.spesa.util

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.spesa.R
import com.example.spesa.util.Formato
import com.example.spesa.dados.Repositorio
import com.example.spesa.databinding.SheetCompartilharBinding
import com.example.spesa.databinding.SheetListaBinding
import com.example.spesa.databinding.SheetMenuBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        // ⋮ no canto: abre o menu (editar, compartilhar, apagar...)
        l.seta.visivel(true)
        l.seta.setImageResource(R.drawable.ic_more)
        l.seta.setOnClickListener { aoSegurar(lista) }
        if (!lista.pessoas.isNullOrEmpty()) l.subtitulo.append(" · com ${lista.pessoas!!.joinToString()}")
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

    /** chip de escolha (vem do item_chip.xml, já com as cores do app) */
    fun chip(grupo: ChipGroup, texto: String, marcado: Boolean = false): Chip {
        val chip = LayoutInflater.from(grupo.context).inflate(R.layout.item_chip, grupo, false) as Chip
        chip.text = texto
        chip.id = android.view.View.generateViewId()
        grupo.addView(chip)
        chip.isChecked = marcado
        return chip
    }

    /** abre uma janela que sobe de baixo com o layout dado */
    private fun janela(activity: Activity, conteudo: android.view.View): BottomSheetDialog {
        val j = BottomSheetDialog(activity)
        j.setContentView(conteudo)
        j.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        j.behavior.skipCollapsed = true
        j.show()
        return j
    }

    /** menu que sobe de baixo: lista de (ícone, texto, o que fazer) */
    fun menu(activity: Activity, titulo: String, opcoes: List<Triple<Int, String, () -> Unit>>, perigo: String? = null) {
        val m = SheetMenuBinding.inflate(activity.layoutInflater)
        m.titulo.text = titulo
        lateinit var j: BottomSheetDialog
        opcoes.forEach { (icone, texto, acao) ->
            val op = activity.layoutInflater.inflate(R.layout.item_opcao, m.opcoes, false) as android.widget.TextView
            op.text = texto
            op.setCompoundDrawablesRelativeWithIntrinsicBounds(icone, 0, 0, 0)
            if (texto == perigo) {
                op.setTextColor(cor(activity, R.color.promo))
                op.compoundDrawableTintList = ColorStateList.valueOf(cor(activity, R.color.promo))
            }
            op.setOnClickListener { j.dismiss(); acao() }
            m.opcoes.addView(op)
        }
        j = janela(activity, m.root)
    }

    /** pergunta "tem certeza?" antes de apagar */
    fun confirmar(activity: Activity, titulo: String, mensagem: String, botao: String, acao: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(titulo).setMessage(mensagem)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(botao) { _, _ -> acao() }
            .show()
    }

    /** criar (lista == null) ou editar uma lista: nome, teto de gastos e pasta */
    fun dialogoLista(activity: Activity, lista: ListaDeCompras?, pastaInicial: Long? = null, depois: () -> Unit) {
        val f = SheetListaBinding.inflate(activity.layoutInflater)
        f.titulo.text = if (lista == null) "Nova lista" else "Editar lista"
        f.confirmar.text = if (lista == null) "Criar lista" else "Salvar"
        val pastaAtual = lista?.pastaId ?: pastaInicial
        val semPasta = chip(f.escolhas, "Sem pasta", pastaAtual == null)
        val chips = Repositorio.pastas.associateWith { p -> chip(f.escolhas, p.nome, p.id == pastaAtual) }
        if (lista != null) {
            f.campoNome.setText(lista.titulo)
            if (lista.orcamento > 0) f.campoOrcamento.setText(Formato.numero(lista.orcamento))
        }
        val j = janela(activity, f.root)
        f.confirmar.setOnClickListener {
            val titulo = f.campoNome.text.toString().trim()
            if (titulo.isBlank()) { f.caixaNome.error = "Dê um nome pra lista"; return@setOnClickListener }
            val orcamento = Formato.lerReais(f.campoOrcamento.text.toString())
            val pastaId = if (semPasta.isChecked) null else chips.entries.find { it.value.isChecked }?.key?.id
            if (lista == null) {
                val nova = ListaDeCompras(titulo = titulo, orcamento = orcamento, pastaId = pastaId)
                Repositorio.adicionarLista(nova)
                Repositorio.registrar("Você criou a lista $titulo", "mais")
                abrirLista(activity, nova.id)
            } else {
                lista.titulo = titulo; lista.orcamento = orcamento; lista.pastaId = pastaId
                Repositorio.salvar()
            }
            j.dismiss(); depois()
        }
    }

    /** menu ao tocar no ⋮ ou segurar o dedo numa lista */
    fun menuDaLista(activity: Activity, lista: ListaDeCompras, depois: () -> Unit) {
        menu(activity, lista.titulo, listOf(
            Triple(R.drawable.ic_lists, "Abrir") { abrirLista(activity, lista.id) },
            Triple(R.drawable.ic_sliders, "Editar nome, teto e pasta") { dialogoLista(activity, lista, depois = depois) },
            Triple(R.drawable.ic_share, "Compartilhar com pessoas") { compartilhar(activity, lista.titulo, listOf(lista), null, depois) },
            Triple(R.drawable.ic_copy, "Duplicar") {
                Repositorio.adicionarLista(lista.clonar("${lista.titulo} (cópia)")); depois()
            },
            Triple(R.drawable.ic_check_line, if (lista.finalizada) "Reabrir" else "Marcar como finalizada") {
                lista.finalizada = !lista.finalizada
                lista.finalizadaEm = if (lista.finalizada) System.currentTimeMillis() else null
                Repositorio.salvar(); depois()
            },
            Triple(R.drawable.ic_delete, "Apagar lista") {
                confirmar(activity, "Apagar lista?", "A lista \"${lista.titulo}\" e os itens dela somem. Não dá pra desfazer.", "Apagar") {
                    Repositorio.removerLista(lista.id)
                    Repositorio.registrar("Você apagou a lista ${lista.titulo}", "saida")
                    depois()
                }
            },
        ), perigo = "Apagar lista")
    }

    /** menu de uma pasta */
    fun menuDaPasta(activity: Activity, pasta: Pasta, depois: () -> Unit) {
        menu(activity, pasta.nome, listOf(
            Triple(R.drawable.ic_sliders, "Editar nome e cor") { dialogoPasta(activity, pasta, depois) },
            Triple(R.drawable.ic_share, "Compartilhar a pasta inteira") {
                compartilhar(activity, pasta.nome, Repositorio.listasDaPasta(pasta.id), pasta, depois)
            },
            Triple(R.drawable.ic_delete, "Apagar pasta") {
                confirmar(activity, "Apagar pasta?", "As listas de dentro não somem: só ficam sem pasta.", "Apagar") {
                    Repositorio.removerPasta(pasta.id); depois()
                }
            },
        ), perigo = "Apagar pasta")
    }

    /** criar (pasta == null) ou editar uma pasta: nome e cor */
    fun dialogoPasta(activity: Activity, pasta: Pasta?, depois: () -> Unit) {
        val f = SheetListaBinding.inflate(activity.layoutInflater)
        f.titulo.text = if (pasta == null) "Nova pasta" else "Editar pasta"
        f.subtitulo.text = "Pastas juntam listas parecidas, como \"Mercado do mês\"."
        f.caixaNome.hint = "Nome da pasta"
        f.caixaOrcamento.visivel(false)
        f.rotuloEscolha.text = "Cor"
        f.confirmar.text = if (pasta == null) "Criar pasta" else "Salvar"
        pasta?.let { f.campoNome.setText(it.nome) }
        val chips = CorPasta.entries.associateWith { c ->
            chip(f.escolhas, c.rotulo, c == (pasta?.cor ?: CorPasta.VERDE)).apply {
                setChipIconResource(R.drawable.bg_avatar)
                chipIconTint = ColorStateList.valueOf(cor(activity, c.corRes))
                isChipIconVisible = true
            }
        }
        f.escolhas.isSelectionRequired = true
        val j = janela(activity, f.root)
        f.confirmar.setOnClickListener {
            val nome = f.campoNome.text.toString().trim()
            if (nome.isBlank()) { f.caixaNome.error = "Dê um nome pra pasta"; return@setOnClickListener }
            val p = pasta ?: Pasta()
            p.nome = nome
            p.cor = chips.entries.find { it.value.isChecked }?.key ?: CorPasta.VERDE
            Repositorio.salvarPasta(p)
            j.dismiss(); depois()
        }
    }

    /**
     * compartilhar com pessoas: guarda quem participa e manda um código
     * que a outra pessoa cola no app dela (Listas > Importar).
     */
    fun compartilhar(activity: Activity, titulo: String, listas: List<ListaDeCompras>, pasta: Pasta?, depois: () -> Unit) {
        val f = SheetCompartilharBinding.inflate(activity.layoutInflater)
        f.titulo.text = "Compartilhar"
        f.subtitulo.text = if (pasta != null) "Pasta \"$titulo\" com ${listas.size} lista(s)" else "Lista \"$titulo\""
        val pessoas = (pasta?.pessoas ?: listas.firstOrNull()?.pessoas ?: mutableListOf()).toMutableList()

        fun mostrarPessoas() {
            f.pessoas.removeAllViews()
            val eu = Repositorio.usuario?.nome ?: "Você"
            (listOf("$eu (você)") + pessoas).forEachIndexed { i, nome ->
                val l = linha(f.pessoas)
                l.caixaData.visivel(false)
                l.caixaIcone.visivel(true)
                l.caixaIcone.setBackgroundResource(R.drawable.bg_avatar)
                l.caixaIcone.backgroundTintList = ColorStateList.valueOf(cor(activity, listOf(R.color.primaria, R.color.pasta_vermelha, R.color.pasta_salvia, R.color.pasta_amarela)[i % 4]))
                l.icone.visivel(false)
                l.titulo.text = nome
                l.subtitulo.text = if (i == 0) "dona da lista" else "pode ver e editar a cópia que receber"
                if (i > 0) {
                    l.valor.text = "remover"
                    l.valor.setTextColor(cor(activity, R.color.promo))
                    l.valor.textSize = 13f
                    l.root.setOnClickListener { pessoas.removeAt(i - 1); mostrarPessoas() }
                }
            }
        }
        mostrarPessoas()
        f.adicionar.setOnClickListener {
            val nome = f.campoPessoa.text.toString().trim()
            if (nome.isBlank()) return@setOnClickListener
            pessoas.add(nome); f.campoPessoa.setText(""); mostrarPessoas()
        }
        val j = janela(activity, f.root)
        f.enviar.setOnClickListener {
            // guarda quem participa
            if (pasta != null) pasta.pessoas = pessoas
            listas.forEach { it.pessoas = pessoas.toMutableList() }
            Repositorio.salvar()
            if (pessoas.isNotEmpty()) Repositorio.registrar("Você compartilhou $titulo com ${pessoas.joinToString()}", "mais")
            val texto = Compartilhar.mensagem(titulo, Compartilhar.codigo(listas, pasta))
            activity.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Enviar para..."))
            j.dismiss(); depois()
        }
    }

    /** colar o código que alguém mandou e trazer a lista pra cá */
    fun importar(activity: Activity, depois: () -> Unit) {
        val f = SheetListaBinding.inflate(activity.layoutInflater)
        f.titulo.text = "Importar lista"
        f.subtitulo.text = "Cole aqui a mensagem que recebeu no WhatsApp ou e-mail."
        f.caixaNome.hint = "Código (começa com SPESA:)"
        f.campoNome.maxLines = 4
        f.caixaOrcamento.visivel(false)
        f.rotuloEscolha.visivel(false)
        f.confirmar.text = "Importar"
        // se o código já estiver copiado, cola sozinho
        val area = activity.getSystemService(android.content.ClipboardManager::class.java)
        area.primaryClip?.getItemAt(0)?.text?.toString()?.let { if (it.contains("SPESA:")) f.campoNome.setText(it) }
        val j = janela(activity, f.root)
        f.confirmar.setOnClickListener {
            val n = Compartilhar.importar(f.campoNome.text.toString())
            if (n < 0) { f.caixaNome.error = "Código inválido"; return@setOnClickListener }
            android.widget.Toast.makeText(activity, "$n lista(s) importada(s)!", android.widget.Toast.LENGTH_SHORT).show()
            j.dismiss(); depois()
        }
    }

    /** Janela com um campo para digitar um valor em reais. Deixar vazio ou 0 apaga (sem meta/teto). */
    fun pedirValor(activity: Activity, titulo: String, atual: Double, aoSalvar: (Double) -> Unit) {
        val campo = android.widget.EditText(activity).apply {
            hint = "0,00 (vazio = sem limite)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789,.")
            if (atual > 0) setText(Formato.numero(atual))
        }
        // um espacinho em volta do campo
        val caixa = android.widget.FrameLayout(activity).apply {
            val p = (20 * activity.resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0)
            addView(campo)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
            .setTitle(titulo)
            .setView(caixa)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton("Salvar") { _, _ -> aoSalvar(Formato.lerReais(campo.text.toString())) }
            .show()
    }
}
