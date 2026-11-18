# Spesa — lista de compras inteligente (app Android)

Projeto acadêmico da Anny (iniciante em Kotlin). Código simples e explicável vale mais que arquitetura sofisticada.

## O que é
App Android nativo (Kotlin + Views/XML + Material 3) para listas de supermercado: múltiplas listas, itens com preço/promoção/categoria, soma em tempo real, teto de gastos, despensa (catálogo de produtos com favoritos), mercados cadastrados com "onde encontrar" (preço por mercado) e comparativo de quanto a lista custa em cada mercado.

## Onde roda / como sobe
- Pasta: `~/programacao/spesa` (git local, sem remoto). Backup do estado original em `~/programacao/spesa-backup-2026-10-29`.
- Android Studio abre direto. Pela linha de comando:
  `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home ./gradlew assembleDebug`
- Emulador: AVD `Pixel_6` (API 34). Para testar por adb sem a janela roubar cliques: `emulator -avd Pixel_6 -no-window` e `adb shell settings put secure show_ime_with_hard_keyboard 0`.
- AGP 9.4 / Gradle 9.6, compileSdk 37, minSdk 24. Kotlin vem embutido no AGP (sem plugin kotlin-android).

## Estrutura (`app/src/main/java/com/example/spesa/`)
- `model/`: `ListaDeCompras`, `ItemLista`, `Produto`, `PrecoNoMercado`, `Mercado`, `Categoria`.
- `dados/Repositorio.kt`: único ponto de leitura/gravação. Guarda tudo como JSON (Gson) no SharedPreferences. Chamar `Repositorio.iniciar(context)` antes de usar.
- `ui/`: uma Activity por tela (Splash, Main, Lista, ItemForm, Mercados, Produtos, ProdutoForm, Comparativo). ViewBinding ligado.
- `adaptadores/`: um RecyclerView.Adapter por lista na tela.
- `util/`: `Formato` (R$ e datas), `Visual` (extensões de view).

## Decisões
- Sem login/usuário: tudo local no celular.
- Item digitado numa lista vira produto na despensa automaticamente; item salvo com mercado+preço atualiza o preço do produto naquele mercado.
- Cores: todas em `res/values/colors.xml` (nomes semânticos: `primaria`, `fundo`, `promo`, `orcamento_*`, `cat_*`). Paleta "italiana elegante": verde-garrafa #1F4A3A, creme #F6F4EE, vermelho queimado #B23A33 só em promoção/estouro.
- Marca: cesta com etiqueta pendurada à esquerda (SVGs em `marca/`; vetores em `drawable/logo_simbolo.xml` e `ic_launcher_foreground.xml`). Títulos em Playfair Display (`res/font/playfair_display.ttf`, licença OFL), via estilo `Spesa.Titulo`.
- Campos de dinheiro aceitam vírgula e ponto (`android:digits`).
