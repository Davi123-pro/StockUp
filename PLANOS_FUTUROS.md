# StockUp

> **StockUp — Gestão de Despensa e Compras Inteligente para Android**
> *Transforme o caos da despensa em controle total e reduza desperdícios.*

---

## 🎯 Missão

Eliminar o desperdício de alimentos no lar por meio de um controle inteligente
de validade, compras e consumo — ajudando as pessoas a usá-los antes que
vencam, a comprar apenas o necessário e a enxergar, com dados, onde o dinheiro
e os alimentos estão sendo perdidos.

---

## 💡 O que é o StockUp?

StockUp é um aplicativo Android de **gestão de despensa e lista de compras**
construído com Jetpack Compose e Material 3. Em vez de decorar ou fotografar
a despensa, o usuário cadastra produtos com foto, validade, preço e local;
o app lembra automaticamente quando algo vence ou falta, sugere o que usar
primeiro e mostra relatórios de quanto foi consumido vs. desperdiçado no mês.

### Principais funcionalidades

| Área | Funcionalidade | Benefício direto |
|------|----------------|------------------|
| **Cadastro** | Produto com foto, barcode (scan auto preenche via OpenFoodFacts), data de vencimento, categoria, localização, preço, estoque mínimo | Cadastro rápido pós-compra |
| **Validade** | Toggle perecível / não perecível, quick-chips (+7d, +15d, +30d), notificações push via WorkManager | Nada mais vence esquecido |
| **Compras** | Lista rápida + detalhada, “mover rápido para a despensa” (1 toque no caixa) | Usa na fila do mercado |
| **Notificações** | Alertas contextuais (vencido hoje, vence amanhã, estoque baixo) com dicas de consumo por categoria | Ação correta no momento certo |
| **Relatórios** | Score de aproveitamento (30 dias), gráficos de consumo vs. desperdício, breakdown por categoria | Enxerga padrões e pega no bolso |
| **Onboarding** | Tutorial de boas-vindas explicando os 3 pilares | Usuário ativa do primeiro dia |
| **Performance** | Otimização de imagens (EXIF Samsung, downsample 1080p), *largeHeap*, FileProvider seguro | Funciona em aparelhos de 4 GB RAM |

---

## 👥 Público-alvo

| Segmento | Por que usa o StockUp |
|----------|----------------------|
| **Mães/responsáveis** | Nada mais esquece alimento vencido na geladeira; controlam compras da família via lista compartilhada |
| **Casais** | Nunca mais compram o dobro do que precisam — a lista sincroniza entre os dois |
| **Quem gosta de organização / minimalismo** | Sabem exatamente o que têm, onde está e quando vai acabar |
| **Quem se preocupa com sustentabilidade** | Dados reais de quanto está desperdiçando — motiva reducing waste |
| **Diaristas / pessoas moradoras de apartamento pequeno** | Aproveitamento máximo do espaço e dos alimentos |

---

## 💰 Problema & Solução

> **Problema:** 40% dos alimentos comprados em casa vão para o lixo.
> A maioria não sabe o que tem, quando vence ou quanto está gastando com
> itens que não consegue consumir.

> **Solução StockUp:** Um app que tudo cataloga, avisa no momento certo
> e mostra, com números, o impacto real — transformando “não lembro mais”
> em “ei, isso vence hoje, vou usar”.

---

## 🚀 Ideias e Roadmap de Funcionalidades Futuras

### 1. Leitor de Nota Fiscal por QR Code (NFC-e) 🧾 ⭐ (Alta Prioridade)
* **Objetivo:** Acabar com o trabalho manual de cadastrar dezenas de itens após uma grande compra de supermercado.
* **Como funciona:** O usuário aponta a câmera para o QR Code da Nota Fiscal de Consumidor Eletrônica (NFC-e). O app consulta os dados públicos da SEFAZ estadual e extrai automaticamente:
  * Nome de cada produto
  * Quantidade e unidade (kg, un, l)
  * Preço unitário e valor total pago
* **Impacto:** Importação em lote de uma compra inteira em menos de 5 segundos.
* **Complexidade:** Alta (requer parser HTML/JSON das SEFAZ estaduais).

---

### 2. Sugestão Inteligente de Receitas com Itens Próximos do Vencimento 🍳
* **Objetivo:** Transformar o alerta de validade em uma ação prática e saborosa, evitando o desperdício real.
* **Como funciona:** O app identifica os ingredientes da despensa que vencem nos próximos 2 a 4 dias e sugere receitas possíveis:
  * Exemplo: *"Você tem leite, ovos e farinha perto de vencer. Que tal fazer panquecas hoje?"*
  * Filtro por itens que o usuário já tem em casa (evitando precisar comprar mais).
* **Impacto:** Aumenta o engajamento diário e a percepção de utilidade do app.
* **Complexidade:** Média (pode usar banco local de receitas ou mini-integração com IA).

---

### 3. Histórico de Preços e Comparador de Supermercados 🏷️
* **Objetivo:** Ajudar a economizar dinheiro identificando qual mercado é mais barato.
* **Como funciona:** O StockUp já salva os preços informados nos cadastros e compras. Com isso, podemos construir:
  * Gráfico de variação de preço de cada produto ao longo dos meses.
  * Comparativo: *"Mercado Extra (R$ 4,89) vs Mercado Pão de Açúcar (R$ 5,49)"*.
  * Alerta de oportunidade: *"Preço 15% abaixo da sua média usual!"*.
* **Impacto:** O app passa a ser também uma ferramenta de economia financeira e inteligência de compras.
* **Complexidade:** Média.

---

### 4. Widget de Tela Inicial para Android (Glance) 📱
* **Objetivo:** Visualização instantânea sem obrigar a abrir o aplicativo todos os dias.
* **Como funciona:** Um widget na tela inicial do smartphone mostrando:
  * Contador de itens vencendo em breve (ex: `3 itens em risco ⚠️`).
  * Botões de atalho: `[+ Novo Produto]` e `[📷 Bipar Código]`.
* **Impacto:** Garante que o usuário nunca esqueça alimentos vencendo no armário.
* **Complexidade:** Média (Jetpack Glance).

---

### 5. Sincronização Familiar em Tempo Real na Nuvem (Supabase) 👨‍👩‍👧‍👦
* **Objetivo:** Compartilhar a despensa e a lista de compras entre todos os moradores da casa.
* **Como funciona:**
  * Arquitetura *offline-first* (Room local continua funcionando sem internet).
  * Código de grupo familiar simples (ex: `FAM-8392`).
  * Sincronização em tempo real: quando uma pessoa marca o item como comprado no supermercado, a lista da outra em casa atualiza na hora.
* **Impacto:** Elimina compras duplicadas pela família.
* **Complexidade:** Alta (Room + Supabase PostgreSQL + Auth).

---

### 6. Exportação de Relatórios Financeiros (PDF e Excel/CSV) 📊
* **Objetivo:** Facilitar o controle financeiro pessoal e prestação de contas da casa.
* **Como funciona:** Um botão na aba de Relatórios para gerar documento formatado:
  * Total gasto no mês por categoria (Carnes, Laticínios, Limpeza, etc.).
  * Total economizado consumindo a tempo vs valor perdido em desperdício.
  * Compartilhamento direto no WhatsApp ou e-mail.
* **Impacto:** Atrai usuários que fazem controle rigoroso de orçamento doméstico.
* **Complexidade:** Baixa / Média.

-------------------------------------------------------------------

## ✅ O que já foi Concluído e Entregue

1. **Leitor de Código de Barras Híbrido + Auto-Memorização**
   - Integração nativa com Google ML Kit Barcode Scanning.
   - Consulta resiliente em bases abertas (Open Food Facts BR/Mundial e Open Beauty Facts).
   - Memorização automática no banco Room ao salvar produtos personalizados.
   - Proteção de fotos tiradas localmente contra substituição indevida por buscas online.

2. **Apresentação e Onboarding Pedagógico**
   - Tela de Boas-Vindas (`WelcomeScreen`) destacando os 3 pilares do StockUp.
   - Tutorial de 4 passos práticos com demonstrações visuais e `statusBarsPadding` para One UI / entalhes de tela.
   - Atalho nas Configurações para rever o tutorial e acessar a **Central de Ajuda**.

3. **Design System & Controle Unificado de Validade**
   - Redesign do card de validade com seletores pill (`[🥗 Perecível]` vs `[♾️ Não Perecível]`).
   - Atalhos de vencimento rápido (`+7d`, `+15d`, `+30d`).
   - Alerta de estoque mínimo inclusivo (opção `0` para compras unitárias sem falsos alarmes).

4. **Fluxo Rápido de Supermercado**
   - Botão **"⚡ Mover Rápido para a Despensa (Pendente)"** na Lista de Compras para uso instantâneo no caixa do mercado.
   - Tag visual `⚠️ Detalhes pendentes` nos cards da despensa para revisão posterior em casa.

5. **Estabilidade em Dispositivos de 4GB RAM**
   - Configuração de `largeHeap="true"` no AndroidManifest.
   - Armazenamento em arquivos temporários seguros (`FileProvider`) eliminando falhas na câmera nativa Samsung.

-----------------------------------------------------------------------

*Última atualização: 10/10/2026*
