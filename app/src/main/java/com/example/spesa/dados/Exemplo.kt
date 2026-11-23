package com.example.spesa.dados

import com.example.spesa.model.Aviso
import com.example.spesa.model.Categoria
import com.example.spesa.model.CategoriaGasto
import com.example.spesa.model.ItemLista
import com.example.spesa.model.Lancamento
import com.example.spesa.model.Lembrete
import com.example.spesa.model.ListaDeCompras
import com.example.spesa.model.Mercado
import com.example.spesa.model.PrecoNoMercado
import com.example.spesa.model.Produto
import com.example.spesa.model.Repeticao
import com.example.spesa.model.TipoLembrete
import com.example.spesa.util.Datas

/**
 * dados de exemplo pra mostrar o app funcionando (Ajustes > Preencher com exemplo).
 * só acrescenta coisas: não apaga nada do que já existe.
 */
object Exemplo {
    private var id = System.currentTimeMillis()
    private fun novoId() = ++id

    // dia de hoje mais/menos alguns dias, numa hora certa
    private fun dia(dias: Int, hora: Int = 12): Long {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = Datas.somarDias(Datas.inicioDoDia(System.currentTimeMillis()), dias)
        c.set(java.util.Calendar.HOUR_OF_DAY, hora)
        return c.timeInMillis
    }

    fun preencher() {
        val r = Repositorio

        // mercados
        val atacadao = Mercado(novoId(), "Atacadão Central", "Av. Principal, 1200")
        val bomPreco = Mercado(novoId(), "Super Bom Preço", "Rua das Flores, 45")
        r.mercados.addAll(listOf(atacadao, bomPreco))

        // produtos da despensa com preço nos dois mercados (pro comparativo)
        fun produto(nome: String, cat: Categoria, a: Double, b: Double, promoA: Boolean = false, fav: Boolean = true): Produto {
            val p = Produto(novoId(), nome, categoria = cat, favorito = fav, precos = mutableListOf(
                PrecoNoMercado(atacadao.id, a, promoA, "corredor ${(2..9).random()}"),
                PrecoNoMercado(bomPreco.id, b),
            ))
            r.produtos.add(p)
            return p
        }
        val arroz = produto("Arroz tipo 1 · 5 kg", Categoria.ALIMENTOS, 28.90, 32.90)
        val feijao = produto("Feijão carioca · 1 kg", Categoria.ALIMENTOS, 7.49, 8.99, promoA = true)
        val leite = produto("Leite integral · 1 L", Categoria.BEBIDAS, 4.99, 5.58)
        val banana = produto("Banana prata · kg", Categoria.HORTIFRUTI, 5.90, 6.50)
        val tomate = produto("Tomate · kg", Categoria.HORTIFRUTI, 8.50, 7.90, promoA = true)
        val cafe = produto("Café torrado · 500 g", Categoria.ALIMENTOS, 18.90, 21.50)
        val sabao = produto("Sabão em pó · 1,6 kg", Categoria.LIMPEZA, 16.40, 18.90)
        val deterg = produto("Detergente · 500 ml", Categoria.LIMPEZA, 2.79, 2.99, fav = false)
        val picanha = produto("Picanha · kg", Categoria.ACOUGUE, 69.90, 74.90, fav = false)
        val carvao = produto("Carvão · 5 kg", Categoria.OUTROS, 24.90, 22.90, fav = false)
        val refri = produto("Refrigerante · 2 L", Categoria.BEBIDAS, 8.99, 9.49, fav = false)
        val shampoo = produto("Shampoo", Categoria.HIGIENE, 14.90, 13.50, fav = false)

        fun item(p: Produto, qtd: Int, noCarrinho: Boolean = false) = ItemLista(
            id = novoId(), produtoId = p.id, nome = p.nome, categoria = p.categoria, quantidade = qtd,
            precoUnitario = p.precos[0].preco, promocao = p.precos[0].promocao, noCarrinho = noCarrinho, mercadoId = atacadao.id,
        )

        // pastas criadas na primeira abertura (se o usuário apagou, cria de novo)
        fun pasta(nome: String, cor: com.example.spesa.model.CorPasta) =
            r.pastas.find { it.nome == nome }?.id ?: com.example.spesa.model.Pasta(novoId(), nome, cor).also { r.pastas.add(it) }.id
        val pMercado = pasta("Mercado do mês", com.example.spesa.model.CorPasta.VERDE)
        val pFestas = pasta("Festas e churrascos", com.example.spesa.model.CorPasta.VERMELHA)
        val pCasa = pasta("Casa e limpeza", com.example.spesa.model.CorPasta.AMARELA)
        val pFarmacia = pasta("Farmácia e higiene", com.example.spesa.model.CorPasta.SALVIA)

        // listas ativas
        val feira = ListaDeCompras(novoId(), "Feira Semanal", dia(0, 9), orcamento = 150.0, pastaId = pMercado, itens = mutableListOf(
            item(arroz, 1, true), item(feijao, 2, true), item(leite, 6, true), item(banana, 2), item(tomate, 1), item(cafe, 1), item(sabao, 1),
        ))
        val churrasco = ListaDeCompras(novoId(), "Churrasco de sábado", dia(-1, 18), orcamento = 300.0, pastaId = pFestas,
            pessoas = mutableListOf("Rafa", "Bia"), itens = mutableListOf(item(picanha, 2), item(carvao, 1), item(refri, 4)))
        val limpeza = ListaDeCompras(novoId(), "Limpeza da casa", dia(-2, 10), pastaId = pCasa,
            itens = mutableListOf(item(sabao, 1), item(deterg, 4)))
        val farmacia = ListaDeCompras(novoId(), "Farmácia", dia(-3, 10), pastaId = pFarmacia, itens = mutableListOf(item(shampoo, 1)))

        // listas finalizadas (aparecem no Histórico)
        val antiga1 = ListaDeCompras(novoId(), "Compras da quinzena", dia(-8), finalizada = true, pastaId = pMercado,
            itens = mutableListOf(item(arroz, 2, true), item(cafe, 2, true), item(leite, 12, true)),
            finalizadaEm = dia(-8, 17), mercadoCompraId = atacadao.id, totalPago = 155.68)
        val antiga2 = ListaDeCompras(novoId(), "Compras de outubro", dia(-30), finalizada = true, pastaId = pMercado,
            itens = mutableListOf(item(arroz, 1, true), item(feijao, 3, true), item(banana, 2, true)),
            finalizadaEm = dia(-30, 16), mercadoCompraId = bomPreco.id, totalPago = 98.30)
        listOf(farmacia, limpeza, churrasco, feira).forEach { r.listas.add(0, it) }
        r.listas.addAll(listOf(antiga1, antiga2))

        // finanças: este mês e o mês passado
        fun lanc(entrada: Boolean, valor: Double, desc: String, cat: CategoriaGasto, dias: Int, rep: Repeticao = Repeticao.NAO) =
            r.lancamentos.add(Lancamento(novoId(), entrada, valor, desc, cat, dia(dias), rep))
        val hojeNoMes = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val inicioMes = -(hojeNoMes - 1)        // quantos dias atrás foi o dia 1
        lanc(true, 4250.0, "Salário", CategoriaGasto.RENDA, inicioMes + 4, Repeticao.MENSAL)
        lanc(false, 1200.0, "Aluguel", CategoriaGasto.MORADIA, inicioMes + 4, Repeticao.MENSAL)
        lanc(false, 228.0, "Conta de luz", CategoriaGasto.CONTAS, minOf(0, inicioMes + 9))
        lanc(false, 155.68, "Compras da quinzena · Atacadão Central", CategoriaGasto.MERCADO, -8)
        lanc(false, 24.90, "Corrida de aplicativo", CategoriaGasto.TRANSPORTE, 0)
        lanc(false, 62.0, "Cinema", CategoriaGasto.LAZER, -3)
        lanc(false, 45.0, "Gasolina", CategoriaGasto.TRANSPORTE, -2)
        lanc(true, 350.0, "Freela de design", CategoriaGasto.RENDA, -5)
        lanc(true, 4250.0, "Salário", CategoriaGasto.RENDA, inicioMes - 26, Repeticao.MENSAL)
        lanc(false, 1200.0, "Aluguel", CategoriaGasto.MORADIA, inicioMes - 26, Repeticao.MENSAL)
        lanc(false, 98.30, "Compras de outubro · Super Bom Preço", CategoriaGasto.MERCADO, -30)
        r.lancamentos.sortByDescending { it.data }

        // agenda: compra ligada à lista, contas a pagar e uma tarefa
        r.lembretes.addAll(listOf(
            Lembrete(novoId(), TipoLembrete.COMPRAR, "Feira semanal", dia(1, 9), listaId = feira.id, aviso = Aviso.UMA_HORA),
            Lembrete(novoId(), TipoLembrete.COMPRAR, "Churrasco", dia(4, 9), listaId = churrasco.id),
            Lembrete(novoId(), TipoLembrete.PAGAR, "Internet", dia(2, 10), valor = 99.90, repeticao = Repeticao.MENSAL),
            Lembrete(novoId(), TipoLembrete.PAGAR, "Cartão de crédito", dia(9, 10), valor = 640.0, repeticao = Repeticao.MENSAL),
            Lembrete(novoId(), TipoLembrete.FAZER, "Buscar gelo e carvão", dia(4, 17)),
        ))
        r.lembretes.sortBy { it.data }

        r.salvar()
        r.registrar("Você compartilhou Churrasco de sábado com Rafa, Bia", "mais")
        r.registrar("Você marcou Leite integral em Feira Semanal", "check")
    }
}
