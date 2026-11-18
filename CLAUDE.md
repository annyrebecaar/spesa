# Spesa — lista de compras inteligente (app Android)

Projeto acadêmico da Anny (iniciante em Kotlin). Código simples e explicável vale mais que arquitetura sofisticada.

## O que é
App Android nativo (Kotlin + Views/XML + Material 3) para listas de supermercado: múltiplas listas, itens com preço/promoção/categoria, soma em tempo real, teto de gastos, despensa (catálogo de produtos com favoritos), mercados cadastrados com "onde encontrar" (preço por mercado) e comparativo de quanto a lista custa em cada mercado.

## Onde roda / como sobe
- Pasta: `~/Documents/programacao/spesa` (git local, sem remoto). Backup do estado original em `~/Documents/programacao/spesa-backup-2026-10-29`.
- A pasta Documentos sincroniza com o iCloud e criava cópias "arquivo 2.xml" no build. Por isso o build vai para `~/.spesa-build/app` (definido em `app/build.gradle.kts`); o APK fica em `~/.spesa-build/app/outputs/apk/debug/`.
- Android Studio abre direto. Pela linha de comando:
  `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home ./gradlew assembleDebug`
- Emulador: AVD `Pixel_6` (API 34). Para testar por adb sem a janela roubar cliques: `emulator -avd Pixel_6 -no-window` e `adb shell settings put secure show_ime_with_hard_keyboard 0`.
- AGP 9.4 / Gradle 9.6, compileSdk 37, minSdk 24. Kotlin vem embutido no AGP (sem plugin kotlin-android).

## Estrutura (`app/src/main/java/com/example/spesa/`)
- `model/`: `ListaDeCompras`, `ItemLista`, `Produto`, `PrecoNoMercado`, `Mercado`, `Categoria`.
- `dados/Repositorio.kt`: único ponto de leitura/gravação. Guarda tudo como JSON (Gson) no SharedPreferences. Chamar `Repositorio.iniciar(context)` antes de usar.
- `ui/`: Splash → Login/Cadastro (conta local, senha em SHA-256) → Main com menu de 5 abas (`ui/abas/`: Início, Listas, Finanças, Agenda, Ajustes, cada uma um Fragment). Outras telas: Pasta, Lista (com janela Finalizar compra), ItemForm, LancamentoForm, LembreteForm, Mercados, Produtos, ProdutoForm, Comparativo. ViewBinding ligado.
- `util/Telas.kt`: linhas e diálogos repetidos (linha de lista, pasta, chips, nova lista/pasta). `util/Datas.kt`: contas de data. `util/Lembretes.kt`: notificações via AlarmManager.
- Redesign de 18/11/2026 segue o protótipo `~/Downloads/Spesa App.html` (18 telas). Fontes: Nunito (texto) e Fraunces (títulos); títulos precisam de `android:fontFamily` e `app:fontFamily` no próprio TextView, o textAppearance sozinho não pega.
- Conta de teste no emulador: marina@teste.com / spesa-teste-01.
- `adaptadores/`: um RecyclerView.Adapter por lista na tela.
- `util/`: `Formato` (R$ e datas), `Visual` (extensões de view).

## Decisões
- Sem login/usuário: tudo local no celular.
- Item digitado numa lista vira produto na despensa automaticamente; item salvo com mercado+preço atualiza o preço do produto naquele mercado.
- Cores: todas em `res/values/colors.xml` (nomes semânticos: `primaria`, `fundo`, `promo`, `orcamento_*`, `cat_*`). Paleta "italiana elegante": verde-garrafa #1F4A3A, creme #F6F4EE, vermelho queimado #B23A33 só em promoção/estouro.
- Pastas (4 criadas na primeira abertura), lançamentos, lembretes e atividades também ficam no Repositorio. Finalizar compra pode lançar o valor como saída em Finanças.
- Marca: cesta com etiqueta pendurada à esquerda (SVGs em `marca/`; vetores em `drawable/logo_simbolo.xml` e `ic_launcher_foreground.xml`). Paleta e fontes do protótipo (verde #1F4D3B, fundo #F5F6F3; Fraunces + Nunito, licença OFL).
- Campos de dinheiro aceitam vírgula e ponto (`android:digits`).
