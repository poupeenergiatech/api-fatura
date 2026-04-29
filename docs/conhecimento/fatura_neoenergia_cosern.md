# Conhecimento: Fatura de Energia — Neoenergia Cosern

## Sobre a Distribuidora

- **Nome:** Neoenergia Cosern
- **Estado:** Rio Grande do Norte (RN)
- **Grupo:** Neoenergia (controlada pela Iberdrola)
- **Área de concessão:** Estado do RN

## Estrutura Típica da Fatura

### Seção de Identificação
- Número da instalação (UC — Unidade Consumidora)
- Número do cliente
- Nome e endereço do titular
- Mês/Ano de referência

### Seção de Leituras e Consumo
- Data e valor da leitura anterior
- Data e valor da leitura atual
- Consumo do período em kWh
- Fator de multiplicação do medidor (quando aplicável)

### Seção de Detalhamento de Valores
- Energia elétrica (kWh × tarifa)
- TUSD — Tarifa de Uso do Sistema de Distribuição
- TUST — Tarifa de Uso do Sistema de Transmissão (quando aplicável)
- Sub-total antes de impostos

### Tributos
- **ICMS:** Imposto sobre Circulação de Mercadorias e Serviços (estadual, ~25% no RN)
- **PIS:** Programa de Integração Social
- **COFINS:** Contribuição para o Financiamento da Seguridade Social
- **COSIP / CIP:** Contribuição de Iluminação Pública (municipal)

### Seção de Pagamento
- Valor total a pagar
- Data de vencimento
- Código de barras / Pix

## Classes Tarifárias Comuns
- **Residencial:** tarifa B1
- **Residencial Baixa Renda:** tarifa B1-baixa renda (subclasses A, B, C, D)
- **Comercial:** tarifa B3
- **Industrial:** tarifa A (alta tensão) ou B2
- **Rural:** tarifa B2-rural

## Modalidades Tarifárias
- **Convencional (Monômio):** uma única tarifa para toda energia consumida
- **Branca:** três postos tarifários (ponta, intermediário, fora ponta)
- **Verde / Azul:** para grandes consumidores (alta tensão)

## Histórico de Consumo
A fatura da Cosern geralmente inclui gráfico ou tabela com os últimos 13 meses
de consumo — dado importante para análise de tendências pelo agente.

## Tarifas Vigentes (referência — sujeitas a revisão anual)
As tarifas são reguladas pela ANEEL e revisadas anualmente.
Consultar: https://www.neoenergia.com/cosern (área do cliente)

## Particularidades do Layout PDF (descobertas com fatura real)

- A fatura é uma **NF-e** (DANFE de Energia Elétrica Eletrônica)
- O cabeçalho da página 1 tem texto **embaralhado** pelo PDF — usar página 2 para nome e endereço
- PIS e COFINS ficam **embutidos** nas linhas de Consumo-TUSD e Consumo-TE (não em linha própria)
- COSIP e ICMS aparecem na **mesma linha**: `Ilum. Púb. Municipal 34,94 ICMS 308,74 20,00 61,74`
- O número do cliente fica no **rodapé**, na linha anterior ao cabeçalho "CÓDIGO DO CLIENTE"
- As leituras do medidor vêm em tabela: `num_medidor Energia Ativa Único leit_ant leit_atual constante consumo`
- Totais de referência, valor e vencimento: o cabeçalho está numa linha e os dados na **próxima**
- O número de instalação (UC) está embaralhado na extração de texto — requer extração por coordenadas (bbox)

## Campos para Treinamento do Agente

| Campo                  | Relevância para o Agente           |
|------------------------|------------------------------------|
| Consumo kWh            | Base para análise de eficiência    |
| Histórico 13 meses     | Detecção de anomalias e sazonalidade |
| Classe tarifária       | Determina se há oportunidade de migração |
| ICMS                   | Verificar alíquota correta aplicada |
| COSIP                  | Verificar conformidade com lei municipal |
| Bandeira tarifária     | Explica variações mensais no valor |
