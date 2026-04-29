# Leitor de Faturas de Energia — API Java

Extrator de dados estruturados de faturas de energia elétrica em PDF, compatível com qualquer distribuidora brasileira que siga o padrão NF-e da ANEEL (RN 1000).

Expõe uma API HTTP REST para integração com outros sistemas.

## Funcionalidades

- Extração de texto via **Apache PDFBox 3**
- Leitura de código de barras via imagem com **ZXing** (fallback quando não há linha digitável no texto)
- **API REST** embutida (sem dependência de servidor externo)
- Suporte a faturas dos grupos **B** (residencial/comercial) e **A** (industrial/GD)

## Dados Extraídos

| Bloco | Campos |
|---|---|
| Distribuidora | Nome, CNPJ, IE, Endereço |
| Cliente | Nome, CPF/CNPJ, Código, Instalação, Endereço |
| Tarifário | Classificação, Tipo de Fornecimento, Bandeira, Acréscimo |
| NF-e | Número, Série, Emissão, Chave de Acesso, Protocolo |
| Leituras | Datas anterior/atual/próxima, Nº de Dias |
| Resumo de Pagamento | Referência, Vencimento, Total, Código de Barras |
| Itens da Fatura | Descrição, Unidade, Qtde, Preço, Valor, PIS/COFINS, ICMS |
| Tributos | PIS, COFINS, ICMS (base/alíquota/valor), COSIP, Multa, Juros |
| Medidores | Número, Grandeza, Posto Horário, Leituras, Constante, Consumo |
| Histórico | Consumo mensal dos últimos meses |
| GD | Saldo de créditos, Energia alocada no ciclo |

## Estrutura

```
pdf-reader/
└── Java/
    ├── pom.xml
    ├── run.sh
    └── src/main/java/br/com/agente/energia/
        ├── Main.java
        ├── ApiServer.java
        ├── LeitorFatura.java
        ├── LeitorCodigoBarrasImagem.java
        ├── DadosFatura.java
        ├── FaturaJson.java
        └── model/
```

## Dependências

- Java 17+
- Apache PDFBox 3.0.2
- ZXing 3.5.3 (core + javase)

## Como Usar

### API REST

```bash
cd Java
./run.sh --server 8080
```

**Enviar uma fatura:**
```bash
curl -X POST http://localhost:8080/api/fatura \
  -H "Content-Type: application/pdf" \
  --data-binary @fatura.pdf
```

**Health check:**
```bash
curl http://localhost:8080/api/health
```

### CLI

```bash
# Processa e exporta JSON ao lado do PDF
./run.sh fatura.pdf

# Salva em caminho específico
./run.sh fatura.pdf saida.json

# Dump do texto extraído (debug)
./run.sh fatura.pdf --dump
```

## Exemplo de Resposta

```json
{
  "distribuidora": {
    "nome": "COMPANHIA ENERGÉTICA DO RIO GRANDE DO NORTE",
    "cnpj": "08.324.196/0001-81"
  },
  "cliente": {
    "nome": "GIECIO VANDRE CORTES",
    "numero_instalacao": "554340"
  },
  "resumo_pagamento": {
    "referencia": "10/2025",
    "data_vencimento": "13/10/2025",
    "valor_total": 2452.42,
    "codigo_barras": "34191.09271 76119.132934 85833.390009 3 12330000245242"
  }
}
```
