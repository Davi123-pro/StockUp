# 📋 Erros e Problemas Identificados - StockUp (Fase Beta)

## Prioridade ALTA (Corrigir Agora)

### 1. DateUtils.kt - Tratamento inconsistente de tempo
- **Arquivo**: `app/src/main/java/com/example/stockup/utils/DateUtils.kt`
- **Problema**: Duas funções (`daysUntilExpiration()` e `daysFromNow()`) lidam com horários de formas diferentes:
  - `daysUntilExpiration()`: Zera ambos "agora" e "expiração" para **meia-noite (00:00:00.000)**
  - `daysFromNow()`: Define o horário final para **23:59:59.999**
- **Impacto**: Produtos podem ser classificados incorretamente. Um produto vencendo às 15h pode aparecer como "1 dia restante" comparado contra a meia-noite, mesmo sendo o mesmo dia.
- **Exemplo**: Se produto vence hoje às 15h:
  - `daysUntilExpiration` retorna 0 (comparação meia-noite vs meia-noite = "hoje")
  - Pode causar classificação errada de urgência

### 2. MainContainerScreen.kt - Parâmetro incorreto passado para HomeScreen
- **Arquivo**: `app/src/main/java/com/example/stockup/ui/main/MainContainerScreen.kt` (linha 138)
- **Problema**: O parâmetro `onEditProduct` está sendo passado como `onProductClick` para o HomeScreen:
  ```kotlin
  onProductClick = onEditProduct,  // ERRO: deveria ser onProductClick ou parâmetro adequado
  ```
- **Impacto**: Quando um usuário clica em um produto na tela inicial, a função de edição é acionada em vez da função de clique normal. Isso quebra a navegação esperada - clicar em um produto não abre o detalhe do produto.
- **Fluxo afetado**: MainContainerScreen → HomeScreen → ProductCard onClick

## Prioridade MÉDIA (Corrigir Brevemente)

### 3. DateUtils.kt - Funções redundantes de formatação de data
- **Arquivo**: `app/src/main/java/com/example/stockup/utils/DateUtils.kt`
- **Problema**: A função `expirationText()` usa `daysUntilExpiration()` internamente, que já zera os horários para meia-noite. Isso pode causar inconsistências ao exibir textos como "Vence hoje!" vs "Vence em 0 dias".
- **Impacto**: Experiência do usuário confusa com mensagens de tempo aparentemente contraditórias.

### 4. ExpirationWorker.kt - Ícone genérico de notificação
- **Arquivo**: `app/src/main/java/com/example/stockup/worker/ExpirationWorker.kt` (linha 74)
- **Problema**: Usa `android.R.drawable.ic_dialog_info` em vez de ícone brand do app
- **Impacto**: Problema de UI/branding, reduz polimento do aplicativo
- **Solução**: Usar o ícone do app (ic_stockup_logo ou similar do drawable)

### 5. StockUpDatabase.kt - Migração destrutiva em produção
- **Arquivo**: `app/src/main/java/com/example/stockup/data/db/StockUpDatabase.kt` (linha 31)
- **Problema**: `fallbackToDestructiveMigration()` apaga TODOS os dados em mudanças de schema
- **Impacto**: Perda de dados durante desenvolvimento se schema mudar
- **Nota**: Comentário indica "em dev" - aceitável para fase beta, mas documentar risco

### 6. HomeViewModel.kt - Comportamento inesperado do filtro "Geral"
- **Arquivo**: `app/src/main/java/com/example/stockup/ui/home/HomeViewModel.kt` (linhas 42-53)
- **Problema**: Ao selecionar "Geral" no filtro de localização, apenas produtos com `location = "Geral"` são mostrados, não "todos os produtos"
- **Impacto**: Usuário pode esperar que "Geral" signifique "todos os locais", mas funciona como filtro específico
- **Suggestion**: Considerar renomear ou ajustar a lógica/UIX

### 7. HomeScreen.kt - Estado swipe-to-dismiss pode travar
- **Arquivo**: `app/src/main/java/com/example/stockup/ui/home/HomeScreen.kt` (linhas 397-412)
- **Problema**: O `LaunchedEffect(productToDelete)` e o gerenciamento de estado podem ter casos de borda onde o swipe fica "preso"
- **Impacto**: Funcionalidade de deslizar para excluir pode inconsistente ou travar exigindo reinicialização do app

## Prioridade BAIXA (Pode Aguardar - Fase Beta)

### 8. Notification channel - Configuração básica funcional
- **Arquivo**: `app/src/main/java/com/example/stockup/worker/ExpirationWorker.kt` (linhas 88-99)
- **Status**: Canal criado com nome "Notificações de Validade" - funcional, pode ser polido depois

### 9. UI polish variado
- **Status**: Vários pequenos ajustes de interface across screens
- **Impacto**: Estético, não bloqueia funcionalidades

### 10. Diretório openroute
- **Status**: Contém apenas `.claude/settings.local.json`
- **Impacto**: Minimal - propósito pouco claro, pode ser investigado separadamente

---

## 📌 Resumo de Ações

### Corrigir Agora (2 bugs críticos):
1. **DateUtils.kt** - Harmonizar tratamento de tempo (usar consistência meia-noite em ambas funções)
2. **MainContainerScreen.kt** - Corrigir passagem incorreta de parâmetro `onProductClick` vs `onEditProduct`

### Corrigir Brevemente (4 melhorias):
3. DateUtils.kt - Revisar função `expirationText()` consistência
4. ExpirationWorker.kt - Substituir ícone genérico pelo brand do app
5. StockUpDatabase.kt - Considerar migração não destrutiva ou documentar melhor
6. HomeViewModel.kt - Ajustar filtro/UX de localização "Geral"

### Pode Aguardar (Beta):
7-10. Melhorias estéticas e configurações básicas

---

**Data da análise**: 2026-10-13  
**Fase do projeto**: Beta/Teste  
**Observação**: Arquivo criado para revisão e correção gradual. Bugs críticos (1 e 2) bloqueiam funcionalidades esperadas e devem ser prioridade.
