# StockUp — Como retomar o trabalho

> Documento de passagem. Escrito no fim de uma sessão longa (36 rodadas).
> Leia junto com o **`VISAO_PRODUTO.md`** — aquela é a bússola, esta é a bússola do código.

---

## 1. Onde está tudo

| O quê | Onde |
|---|---|
| Projeto | `C:\Users\User\Desktop\PROGRAMAS DAVI\PESSOAIS\StockUp` |
| APK pronto | `StockUp.apk` (na raiz do projeto) |
| Cópias do código | `_backups\stockup-codigo-AAAAMMDD-HHMM.zip` (15 cópias) |
| Script de cópia | `ferramentas\salvar-codigo.ps1` |
| Carta do produto | `VISAO_PRODUTO.md` |
| Testes unitários | `app\src\test\...` — **70 testes, todos verdes** |

**O código não usa git ainda.** A proteção são os `.zip`.

> **Atualizado em 02/11:** o projeto agora **usa git**. Primeiro commit
> `StockUp v1.0 - Teste Alfa`, autor **Davi123-pro**, remote
> `https://github.com/Davi123-pro/StockUp.git`. O `.zip` continua valendo como
> segunda cópia.

### Salvar uma cópia nova
```powershell
powershell.exe -ExecutionPolicy Bypass -File ferramentas\salvar-codigo.ps1
```
> O `-ExecutionPolicy Bypass` é necessário porque o Windows bloqueia script não assinado. Não é sinal de problema.
> Chamar `pwsh` de dentro do `pwsh` **falha** ("não é reconhecido") — use `powershell.exe`.

### Cópia dos DADOS (mudou em 25/10)
O app **só grava cópias dentro dele** (`filesDir/backups/`). A gravação em pasta do
celular e o compartilhamento foram **removidos** a pedido do dono, que vai decidir
outra forma de exportar. As cópias antigas (no cartão) foram apagadas.

### Voltar atrás
1. Abra o `.zip` desejado
2. Copie `app\src` por cima do `app\src` do projeto
3. Compile de novo

---

## 2. Compilar e instalar

O build **não tem internet**. Sempre use `--offline --no-configuration-cache`.
Sem os dois, o Gradle falha (o cache de configuração quebra no `ProcessNavigationXmlTask`).

```powershell
$jdk21 = "C:\Users\User\Desktop\PROGRAMAS DAVI\java-21-openjdk-21.0.4.0.7-1.win.jdk.x86_64\java-21-openjdk-21.0.4.0.7-1.win.jdk.x86_64"
#   ↑ sim, a pasta está repetida duas vezes. É assim mesmo.

$jdk17 = "$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
$env:JAVA_HOME = $jdk21

.\gradlew.bat assembleDebug testDebugUnitTest --offline --no-configuration-cache --console=plain `
  "-Dorg.gradle.java.installations.paths=$jdk17"
```

- O JDK **21** compila; a toolchain **17** é usada pelo Kotlin — por isso o `-D...paths`.
- O Gradle escreve em `C:\Users\User\.gradle`, **fora do projeto** → a ferramenta precisa de permissão elevada (`danger-full-access`). Isso é esperado, não é erro.
- O APK sai em `app\build\outputs\apk\debug\StockUp-debug.apk`. Copie por cima de `StockUp.apk`.

---

## 3. Acessar o celular

| | |
|---|---|
| aparelho | Samsung **SM-M156B** (Galaxy M15), **Android 16** |
| tela | 1080 × 2340 px, 450 dpi |
| tema atual | **claro** (`cmd uimode night` → `Night mode: no`) |
| escala de fonte | **1,0** (padrão) |

`adb`
```
C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

### Ver se está conectado
```powershell
& $adb devices -l
```
Precisa aparecer `device` (não `unauthorized`).

### Instalar
```powershell
& $adb install -r "StockUp.apk"
```

### Abrir o app
```powershell
& $adb shell am force-stop com.example.stockup
& $adb shell am start -n com.example.stockup/.MainActivity
```

### Foto da tela
```powershell
& $adb shell screencap -p /sdcard/s.png
& $adb pull /sdcard/s.png "build\screenshots\nome.png"
& $adb shell rm /sdcard/s.png
```

### "O que está escrito na tela" (para saber onde tocar)
```powershell
& $adb shell uiautomator dump /sdcard/u.xml
& $adb pull /sdcard/u.xml "$env:TEMP\u.xml"
& $adb shell rm /sdcard/u.xml
Get-Content "$env:TEMP\u.xml" -Raw
```

---

## 4. As três regras de automação de tela (aprendidas na dor)

### Regra 1 — nunca chute coordenadas, leia do dump
O `dump` traz `bounds="[x1,y1][x2,y2]"`. O centro é
`((x1+x2)/2, (y1+y2)/2)`.

### Regra 2 — o dump corrompe acentos
`"Selecionar vários"` aparece como `"Selecionar vÃ¡rios"`. **Busque só o pedaço sem acento**
(`"Selecionar"`), ou use `content-desc`.

### Regra 3 — tudo num único comando
Tocar → esperar → capturar → tocar de novo, **na mesma chamada**. Se você devolver o controle
entre os passos, a barra de desfazer (8 s) expira e o toque seguinte cai no lugar errado.

---

## 5. Recuperar os produtos (despensa está com 3)

O Azeite foi apagado num teste. Para voltar aos 4 produtos:

1. Abra o app
2. Aba **Config (canto inferior direito)**
3. **Restaurar**
4. Escolha **`Antes de restaurar — 25/10/2026`**
5. Espere a mensagem *"Pronto! … produtos e … fotos voltaram."*

> O app **sempre** cria uma cópia "Antes de restaurar" antes de restaurar. Por isso restaurar é reversível.

### Também há duplicatas na lista de compras
Dois `Leite Cemil` a R$ 4,50 são de **antes** da correção de soma. Apague um na mão — eles não se juntam sozinhos.

---

## 6. Testes que só funcionam com estes truques

### Notificação (o worker roda 1× por dia e não aceita ser forçado)
O WorkManager **recusa** rodar trabalho periódico adiantado
(`WM-WorkerWrapper: Delaying execution... because it is being executed before schedule`).
Para testar:

1. Em `MainActivity.kt`, troque temporariamente:
   ```kotlin
   PeriodicWorkRequestBuilder<ExpirationWorker>(24, TimeUnit.HOURS)
   → OneTimeWorkRequestBuilder<ExpirationWorker>()
   ```
   e `enqueueUniquePeriodicWork(...KEEP...)` → `enqueueUniqueWork(...REPLACE...)`
2. Compile, instale, abra o app → o aviso sai na hora
3. **Volte para a versão periódica e compile de novo**

### Permissão de notificação
Já está concedida. Se precisar de novo:
```powershell
& $adb shell pm grant com.example.stockup android.permission.POST_NOTIFICATIONS
```

### Fonte grande (teste de acessibilidade)
```powershell
& $adb shell settings put system font_scale 1.5     # ou 2.0
# … testar …
& $adb shell settings put system font_scale 1.0     # SEMPRE voltar
```

### Tour guiado
**Configurações → "Ver o tour da tela inicial"** (8 passos).
A flag fica em `seen_home_tour`; pelo `adb` não deu para zerar (`sed` brigou com o PowerShell).
> O cartão do tour tem "Próximo" em posições diferentes a cada passo: leia os
> `bounds` do passo ATUAL antes de tocar, senão você pula o passo que queria ver.

### Perguntas do dia (não perecível) — como testar rápido
Não precisa esperar as 9:00: em **Configurações → Perguntas do dia → Primeira
pergunta**, escolha um horário ~2 minutos à frente. O app reagenda na hora.
Para conferir o agendamento sem olhar a tela:
```powershell
& $adb shell "dumpsys jobscheduler | grep -A10 PerguntaDoDiaWorker"   # veja 'Minimum latency'
```

> **Forçar o worker não funciona**: `cmd jobscheduler run -f com.example.stockup 11`
> devolve "Could not find job" porque os jobs do WorkManager vivem no namespace
> `androidx.work.systemjobscheduler`, e esse namespace não é aceito pelo comando.
> O caminho que funciona é mudar o horário no app.
> Também **não** dá para disparar o `PerguntaAcaoReceiver` por `am broadcast`:
> ele é `exported="false"`.

---

## 7. O que já está pronto

| # | Item | Estado |
|---|---|---|
| 1 | Animações nos avisos/cards | ✅ |
| 2 | Ícone da notificação (silhueta da caixa) | ✅ verificado no aparelho |
| 3 | Botões duplicados → Fluxo Invertido | ✅ |
| 4 | Aviso "detalhes pendentes" que nunca saía | ✅ + 5 testes |
| 5 | Tela de avisos redesenhada + tom sem culpa | ✅ |
| 6 | Permissão de notificação depois do tutorial | ✅ |
| 7 | "Editar item" em Compras | ✅ |
| 8 | Central de Ajuda reescrita (13 perguntas) | ✅ |
| 9 | Promessa de despensa compartilhada removida | ✅ |
| 10 | Lógica de notificação + "lido" persistente | ✅ |
| 11 | Micro-animações nos botões | ✅ |
| — | Tour guiado em 8 passos, sobre a tela real | ✅ |
| — | Produto de exemplo que aparece e some no tour | ✅ |
| — | Lista de compras soma quantidade (não duplica) | ✅ |
| — | Barra de desfazer redesenhada | ✅ |
| — | Tela vazia corrigida | ✅ |
| 12 | **Pulso + ✓ no botão "Mover Rápido"** (só então move) | ✅ verificado no aparelho |
| 13 | **Pulso → ✓ → fechar no "Concluir"** da personalização | ✅ verificado (1 e 2 itens) |
| 14 | **Números e barras dos Relatórios subindo do zero** | ✅ verificado (10× e 1×) |
| 15 | Score: número mantido, **frases sem culpa**, % explicado em palavras | ✅ verificado |
| 16 | Tour passo 6 sem "sem julgamento" | ✅ verificado no aparelho |
| 17 | **Bug do restore** (cópia de segurança sobrescrevia a fonte) | ✅ corrigido e provado |
| 18 | **Backup só dentro do app** (sem pasta/compartilhar) | ✅ verificado |
| 19 | **Botão Feedback** (e-mail com assunto e destinatário) | ✅ verificado no aparelho |
| 20 | **Perguntas do dia** (9:00 e 21:00) sobre não perecível | ⚠️ ver "8.4" |
| 21 | **Estoque mínimo removido** (cadastro, detalhes, alertas) | ✅ |

### Fluxo Invertido (o coração do produto)
- O card tem botão **"Acabou"** → um toque, sai da lista, registrado como usado
- A pergunta "usou ou jogou fora?" saiu do caminho e virou correção na barra
- A notificação tem **"Acabei"** e **"Joguei fora"** — resolve sem abrir o app
- Tudo é reversível com **"Desfazer"**

---

## 8. O que FALTA (esta é a fila)

### 8.1 — As três animações — FEITAS (25/10)
1. **"Mover Rápido"**: pulso → ✓ → **só então** move. Componente `BotaoConcluir`.
   O pulso é de **4%** (com 7% o botão encostava na borda da tela).
2. **"Concluir"** da personalização: pulso → ✓ → só então fecha.
   O aviso de campo obrigatório mudou para o rodapé, junto do botão.
3. **Relatórios**: número e barra sobem do zero com a **mesma** variável
   (`Animatable`), e as barras do histórico mensal sobem com 70 ms de atraso entre si.

### 8.2 — Score de Aproveitamento — DECIDIDO (25/10)
O dono decidiu **manter o número** e trocar as frases. Feito:
- Título virou **"Quanto você aproveitou"** (era "Score de Aproveitamento").
- O % vem sempre com a frase que o explica: *"De cada 100 itens que saíram da
  despensa, N foram aproveitados."*
- Frases de culpa trocadas; a de baixo aproveitamento agora diz *"A culpa foi minha,
  que avisei tarde"*.
- O tour parou de prometer "sem julgamento" no passo 6.

**Ainda em aberto:** a **setinha de tendência** (↑ ↓ →) compara com os 30 dias
anteriores — e a Carta diz *"nem com você mesmo do mês passado"*. Falta o dono
decidir se sai.

### 8.5 — Banco de dados: versão 5 (02/11)
O dono liberou as migrations **só** para a estrutura do motor de consumo.
- Tabelas novas: `consumo_observacoes`, `estoque_atualizacoes`, `avisos_consumo`.
- Migração **4 → 5 é de acréscimo**: só `CREATE TABLE`/`CREATE INDEX`, nada é
  apagado. `fallbackToDestructiveMigration()` **saiu** — com ele, subir de versão
  apagava a despensa.
- Verificado no aparelho: instalou por cima da versão 4 e os 3 produtos
  continuaram lá; as três tabelas existem no arquivo do banco.
- O antigo `ConsumoStore`/`consumo.json` foi removido; quem manda é o
  `ConsumoRepository` (suspend).

### 8.6 — Hora do aviso: o app escolhe (02/11)
Não existe mais "hora do aviso" em Configurações. O app conta em que horas a
pessoa abre a despensa (`usos_por_hora` nas preferências) e usa a hora mais
frequente — **nunca entre 00:00 e 05:00**. Sem uso registrado, 9:00.
Lógica pura em `domain/EscolhaDeHorario.kt`, com 7 testes.

### 8.7 — Relatórios (02/11)
- Filtros: **Este mês · Últimos 3 meses · Todo o período**.
- Cards: **Consumidos / Perdidos**, com "N alimentos consumidos/perdidos".
- Frase de baixo aproveitamento: *"Alguns alimentos acabaram se perdendo. Vamos
  tentar aproveitar melhor da próxima vez?"* (a antiga falava em culpa).
- Gráfico: 200dp de altura (era 160), barras de 150dp, **toque numa barra mostra
  os valores do mês** e uma legenda explica as cores.

### 8.4 — Avisos inteligentes de consumo — FEITO E VERIFICADO (02/11)
Substituiu as "perguntas do dia" (removidas: `PerguntasDoDia`, `PerguntaDoDiaWorker`,
`PerguntaAcaoReceiver` e o card de Configurações).

**Como funciona:**
- `domain/ConsumptionEngine.kt` — média ponderada das últimas 5 observações (as
  recentes pesam mais), previsão de dias restantes e padrão por categoria como
  ponto de partida (`carnes 3 · laticínios 7 · grão 30 · limpeza 60`), dividido pelo
  número de moradores. **Sem IA**: regra e conta.
- `data/consumo/ConsumoStore.kt` — histórico em `filesDir/consumo.json` (o dono
  proibiu migração; o banco ficou intocado). Guarda observações, última atualização
  de estoque e a agenda de avisos (último, ignorados, próximo, última resposta).
- `worker/AvisoDeConsumoWorker.kt` — roda 1×/dia na hora escolhida, avisa **só** de
  produto sem validade com previsão ≤ 1 dia, **um por dia**, e nunca no mesmo dia do
  aviso de validade (a Carta permite um aviso por dia).
- `worker/AvisoConsumoReceiver.kt` — "Ainda tenho" (empurra 2 dias), "Está acabando"
  (põe na lista) e "Já acabou" (registra consumo, sai da despensa, entra na lista).
  As duas últimas têm "Desfazer", que também apaga a observação aprendida.
- Configurações → **"Avisos no celular"**: liga/desliga tudo + "quantas pessoas
  moram aqui" (1 a 10). O campo de moradores fica **sempre visível**, mesmo com os
  avisos desligados, e o **tour tem um passo (o 9º)** apontando para ele.
- Registrar consumo: `HomeViewModel` (botão Acabou), `AcaoRapidaReceiver` (aviso de
  validade) e o próprio aviso de consumo. Desperdício **não** entra na média.

**Verificado no aparelho (02/11):**
- a observação nasceu certa: `arroz|un · 1 un · 0,5 dia`;
- o aviso saiu sozinho no horário e com o texto certo: *"Arroz está acabando? Pelo
  seu consumo (1x), deve acabar hoje. Ainda tem?"*;
- os três botões aparecem e "Ainda tenho" responde *"Combinado: Arroz ainda está aí
  — Volto a perguntar em 2 dias"*, com `proximoAviso` gravado;
- o app voltou ao estado limpo: 3 produtos na despensa, `consumo.json` apagado
  (as estimativas de teste saíram; o histórico de relatórios ficou).

**O que NÃO cabe na notificação:** o Android mostra no máximo 3 botões. Das cinco
respostas pedidas, entram "Ainda tenho", "Está acabando" e "Já acabou". "Atualizar
quantidade" é tocar no aviso e usar o +/− do produto; "Responder mais tarde" é não
responder — o aviso volta no intervalo calculado, que **dobra** a cada silêncio
(1 → 2 → 4 → 8 → 14 dias).

### 8.8 — Antes do GitHub (02/11)
- **Tour e introdução escondidos:** são ferramenta de teste, não recurso de quem
  usa. Ficam atrás do **modo de teste** — 5 toques na linha "StockUp v1.0", no fim
  de Configurações. Com o modo ligado aparecem os cards "(teste)" e o tour também
  pode rodar sozinho no primeiro uso; desligado, nada aparece.
- **Voltar de "Onde guardo as coisas" voltava para a Despensa.** A aba selecionada
  morava dentro da `MainContainerScreen` (com `remember`), então saía da composição
  ao abrir a tela de locais. Agora vive em `Navigation.kt` (`rememberSaveable`).
- **Card "Em breve — Despensa Compartilhada" removido** de `LocationsScreen`: era
  promessa de nuvem/QR/permissões (contra a Carta) e trazia o botão "Entendi".
- **Código morto removido:** o diálogo inalcançável `showHelpDialog` (texto velho de
  estoque mínimo) e as funções `HorarioDaLinha`, `DialogoDeHorario` e
  `horarioEscrito`, que ficaram sem uso quando a hora do aviso deixou de ser
  configurável.
- **APK entregue em** `/sdcard/Download/StockUp.apk` (build de 02/11, 11:30).

### 8.10 — Ajustes finais (02/11, 2ª rodada)
- **Tutorial emenda no tour:** ao terminar a introdução, a tela principal entra no
  tour na hora (`pedirTourDeFora` em `Navigation.kt`), **independente do modo de
  teste** — antes só começava se o modo já estivesse ligado.
- **Passo 8 não destacava o card:** o tour publica o passo atual em
  `LocalPassoDoTour`; a tela de Configurações rola até o card "Avisos no celular"
  quando esse passo chega (250 ms de respiro + `animateScrollTo`).
- **FAQ ganhou a pergunta 16:** "Como apagar vários produtos de uma vez?" (o botão
  de lista no topo da Despensa).
- **Busca do FAQ reescrita:** normaliza acento/caixa, ignora palavras vazias,
  expande sinônimos (`apagar ↔ excluir ↔ deletar`, `vence ↔ validade`…), pontua
  título (3) e corpo (1) e ordena por relevância. Sem resultado aparece
  **"Não achei essa dúvida"**, com até 3 sugestões clicáveis e o botão
  **"Mandar minha dúvida"** — abre o e-mail com assunto pronto
  (`utils/EmailDeFeedback.kt`, o mesmo do botão Feedback).

**Verificado no aparelho:** "deletar" acha "Como apagar vários produtos";
"guardarchave" mostra o card de "não achei" com o botão de envio.
**Pendente de confirmação visual:** a rolagem automática do passo 8 (implementada e
compilada; não consegui percorrer o tour até lá nesta sessão porque o modo de teste
alternava a cada 5 toques na versão).

### 8.11 — Busca da Despensa (verificada em 02/11)
A verificação no aparelho encontrou **dois defeitos reais** e os corrigiu:
- **acento:** "classico" não achava "Clássico" (usava `contains(ignoreCase = true)`);
- **espaço sobrando:** "leite " (espaço no fim) zerava o resultado.

A comparação agora passa por `domain/BuscaDeProduto.kt`: tira acento, caixa e
espaços das duas pontas e também procura no **local** e na **categoria**
("armario" acha o azeite; "bebidas" acha o que for da categoria). 10 testes novos.

**Verificado no aparelho:** `leite`, `LEITE`, `classico`, ` LEITE ` e `armario`
acham; `zzz` mostra "Nenhum resultado"; limpar a busca devolve a lista inteira.

### 8.12 — Teclado da busca e pergunta nova (02/11)
- A tecla **"pesquisar"** do teclado fechava nada: a ação estava declarada
  (`ImeAction.Search`) mas o `onSearch` era vazio. Agora esconde o teclado e limpa
  o foco (a busca continua aplicada e o texto fica no campo; tocar na barra abre
  o teclado de novo).
- FAQ ganhou a pergunta **17**: "Como vejo os detalhes do produto na minha
  despensa?" (tocar no cartão abre a ficha; ali ficam +/−, lápis e lixeira).

**Verificado no aparelho:** com o teclado aberto e "azeite" digitado, tocar na lupa
do teclado fecha o teclado e mantém "1 Total" na lista. A busca "detalhes" no FAQ
encontra a pergunta nova.

## 8.9 — Planos futuros (ideias guardadas, NÃO feitas)


| # | Ideia | Por quê |
|---|---|---|
| 1 | **Trocar a foto do produto** no Detalhes e ao adicionar | hoje a foto só entra no cadastro; no dia a dia dá vontade de trocar (produto sem foto, foto ruim, embalagem nova). O detalhe já registra a foto, falta o botão de trocar/remover |
| 2 | Editar quantidade na tela de Detalhes alimentar o motor de consumo | hoje o +/− do detalhe não passa pelo `ConsumoRepository`; só o "Acabou" e o aviso alimentam a média |
| 3 | Aviso de validade com "abri hoje" | a especificação pede considerar a data de abertura para perecíveis |
| 4 | Exportar/importar cópia para fora do app | a cópia hoje mora só dentro do app; o dono vai decidir o formato |

## 8.3 — Outras pendências conhecidas
| | |
|---|---|
| **`+` cobre o card** com fonte grande (testado em 1,5×) | conflito real de toque |
| **Home fixa 3 blocos** no topo (filtros + "Onde está" + resumo) | organização — dono ainda pensando |
| **Excluir pelo Detalhes** não tem "Desfazer" | caminho irreversível |
| **`allowBackup="false"`** | formatar o celular — ou desinstalar — apaga tudo, inclusive a cópia que agora mora só dentro do app |
| **`showHelpDialog`** morto em `SettingsScreen` (~linha 620) | texto velho de jargão, inalcançável |
| **`MainScreenTest.kt`** não compila | referencia `MainScreen`, que não existe; bloqueia `androidTest` |
| **Chaves de API vivas** em `.env` (OpenRouter) e `openroute/.claude/settings.local.json` (Anthropic) | `.gitignore` atualizado em 02/11 cobre `.env`, `*.env`, `**/.claude/settings.local.json`, `/openroute/` e `.venv/`. **Ainda não existe repositório git**, então nada foi versionado — mas as duas chaves devem ser **revogadas/trocadas** pelo dono, porque já circularam em texto |
| **Widget** | adiado. O **Glance NÃO está no cache offline** — só dá para fazer o widget clássico (`AppWidgetProvider`) |
| **iOS** | o app inteiro é Android-only. Widget não é portátil em nenhuma estratégia |
| **Migrations / `exportSchema`** | o dono pediu para não tocar |

---

## 9. Coisas que eu aprendi sobre este dono

Vale mais que o código:

- Ele **cobra verificação no aparelho** — e está certo. Print vale mais que promessa.
- Ele **prefere honestidade a entrega falsa.** Dizer "não fiz" funciona melhor do que entregar meia-boca.
- Ele **detecta detalhe visual** que eu não vi (o destaque deslocado do tour). Mostre screenshots.
- Ele **pondera antes de decidir** (Score, Home, logo). Não force; pergunte e siga.
- **Nunca afirme que mexeu no banco de dados** sem ele aprovar. Ele pediu explicitamente para não tocar em migrations.
- O público final são os **pais dele**. A régua é:
  > *"Meus pais usaram e eu não precisei explicar nada."*

---

## 10. Se algo der muito errado

1. Não entre em pânico: os dados da despensa estão numa cópia em `filesDir/backups/` (dentro do app)
2. Volte o código por um dos `.zip` de `_backups/`
3. Compile e instale — o banco **não** é apagado por reinstalar (`adb install -r`)
4. Só um `pm clear` ou "Limpar dados" apaga o banco. **Não faça isso** sem antes exportar uma cópia pelo app.
