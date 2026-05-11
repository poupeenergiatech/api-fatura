package br.com.agente.energia;

import br.com.agente.energia.model.*;
import br.com.agente.energia.model.DemonstrativoLinha;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FaturaJson {

    public static String toJson(DadosFatura d) {
        Sb sb = new Sb();
        sb.open();
        bloco(sb, "distribuidora",  d.getDistribuidora());           sb.comma();
        bloco(sb, "cliente",        d.getCliente());                  sb.comma();
        bloco(sb, "tarifario",      d.getTarifario());                sb.comma();
        bloco(sb, "nfe",            d.getNfe());                      sb.comma();
        bloco(sb, "leituras",       d.getLeituras());                 sb.comma();
        bloco(sb, "resumo_pagamento", d.getResumoPagamento());        sb.comma();
        itensJson(sb, d.getItensFatura());                            sb.comma();
        tributosJson(sb, d.getTributos());                            sb.comma();
        medidoresJson(sb, d.getMedidores());                          sb.comma();
        historicoJson(sb, d.getHistoricoConsumo());
        if (!d.getGrandezasContratadas().isEmpty()) {
            sb.comma();
            grandezasJson(sb, d.getGrandezasContratadas());
        }
        if (d.getSaldoGd() != null) {
            sb.comma();
            saldoGdJson(sb, d.getSaldoGd());
        }
        if (!d.getDemonstrativo().isEmpty()) {
            sb.comma();
            demonstrativoJson(sb, d.getDemonstrativo(), d.getFatorCargaPonta(), d.getFatorCargaForaPonta());
        }
        sb.close();
        return sb.toString();
    }

    // ── Blocos simples ────────────────────────────────────────────

    private static void bloco(Sb sb, String chave, Distribuidora d) {
        sb.key(chave).append("{\n");
        sb.field("nome", d.getNome());
        sb.field("endereco", d.getEndereco());
        sb.field("cnpj", d.getCnpj());
        sb.field("inscricao_estadual", d.getInscricaoEstadual());
        sb.trimComma().append("  }");
    }

    private static void bloco(Sb sb, String chave, IdentificacaoCliente c) {
        sb.key(chave).append("{\n");
        sb.field("nome", c.getNome());
        sb.field("cnpj", c.getCnpj());
        sb.field("codigo_cliente", c.getCodigoCliente());
        sb.field("numero_instalacao", c.getNumeroInstalacao());
        sb.field("logradouro", c.getLogradouro());
        sb.field("bairro", c.getBairro());
        sb.field("cep", c.getCep());
        sb.field("cidade", c.getCidade());
        sb.field("uf", c.getUf());
        sb.trimComma().append("  }");
    }

    private static void bloco(Sb sb, String chave, DadosTarifarios t) {
        sb.key(chave).append("{\n");
        sb.field("classificacao", t.getClassificacao());
        sb.field("subclasse", t.getSubclasse());
        sb.field("tipo_fornecimento", t.getTipoFornecimento());
        sb.field("bandeira", t.getBandeira());
        sb.field("acrescimo_bandeira", t.getValorAcrescimoBandeira());
        sb.trimComma().append("  }");
    }

    private static void bloco(Sb sb, String chave, DadosNfe n) {
        sb.key(chave).append("{\n");
        sb.field("numero", n.getNumero());
        sb.field("serie", n.getSerie());
        sb.field("data_emissao", n.getDataEmissao());
        sb.field("chave_acesso", n.getChaveAcesso());
        sb.field("protocolo", n.getProtocolo());
        sb.field("data_protocolo", n.getDataProtocolo());
        sb.trimComma().append("  }");
    }

    private static void bloco(Sb sb, String chave, Leituras l) {
        sb.key(chave).append("{\n");
        sb.field("data_anterior", l.getDataAnterior());
        sb.field("data_atual", l.getDataAtual());
        sb.field("numero_dias", l.getNumeroDias());
        sb.field("data_proxima", l.getDataProxima());
        sb.trimComma().append("  }");
    }

    private static void bloco(Sb sb, String chave, ResumoPagamento r) {
        sb.key(chave).append("{\n");
        sb.field("referencia", r.getReferencia());
        sb.field("data_vencimento", r.getDataVencimento());
        sb.field("valor_total", r.getValorTotal());
        sb.field("codigo_barras", r.getCodigoBarras());
        sb.trimComma().append("  }");
    }

    // ── Itens ─────────────────────────────────────────────────────

    private static void itensJson(Sb sb, List<ItemFatura> itens) {
        sb.key("itens_fatura").append("[\n");
        for (int i = 0; i < itens.size(); i++) {
            ItemFatura it = itens.get(i);
            List<String> campos = new ArrayList<>();
            addCampo(campos, "descricao",         it.getDescricao());
            addCampo(campos, "unidade",           it.getUnidade());
            addCampo(campos, "quantidade",        it.getQuantidade());
            addCampo(campos, "preco_unit_c_trib", it.getPrecoUnitComTrib());
            addCampo(campos, "valor",             it.getValor());
            addCampo(campos, "pis_cofins",        it.getPisCofins());
            addCampo(campos, "base_calc_icms",    it.getBaseCalcIcms());
            addCampo(campos, "aliquota_icms",     it.getAliquotaIcms());
            addCampo(campos, "valor_icms",        it.getValorIcms());
            addCampo(campos, "tarifa_unit",       it.getTarifaUnit());
            sb.append("    {\n");
            for (int j = 0; j < campos.size(); j++)
                sb.append("      ").append(campos.get(j)).append(j < campos.size() - 1 ? "," : "").append("\n");
            sb.append("    }").append(i < itens.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]");
    }

    // ── Tributos ──────────────────────────────────────────────────

    private static void tributosJson(Sb sb, DadosTributos t) {
        sb.key("tributos").append("{\n");
        List<String> extras = new ArrayList<>();
        addCampoTrib(extras, "cosip",    t.getCosip());
        addCampoTrib(extras, "multa",    t.getMulta());
        addCampoTrib(extras, "juros",    t.getJuros());
        addCampoTrib(extras, "ipca",     t.getIpca());
        addCampoTrib(extras, "comp_dic", t.getCompDic());
        tributoJson(sb, "pis",    t.getPis());    sb.append(",\n");
        tributoJson(sb, "cofins", t.getCofins()); sb.append(",\n");
        tributoJson(sb, "icms",   t.getIcms());   if (!extras.isEmpty()) sb.append(",");  sb.append("\n");
        for (int i = 0; i < extras.size(); i++)
            sb.append("    ").append(extras.get(i)).append(i < extras.size() - 1 ? "," : "").append("\n");
        sb.append("  }");
    }

    private static void tributoJson(Sb sb, String nome, Tributo t) {
        sb.append("    \"").append(nome).append("\": ");
        if (t == null) { sb.append("null"); return; }
        sb.append("{ \"base_calculo\": ").append(fmt(t.getBaseCalculo()))
          .append(", \"aliquota\": ").append(fmt(t.getAliquota()))
          .append(", \"valor\": ").append(fmt(t.getValor())).append(" }");
    }

    // ── Medidores ─────────────────────────────────────────────────

    private static void medidoresJson(Sb sb, List<DadosMedidor> medidores) {
        sb.key("medidores").append("[\n");
        for (int i = 0; i < medidores.size(); i++) {
            DadosMedidor m = medidores.get(i);
            List<String> campos = new ArrayList<>();
            addCampo(campos, "numero",           m.getNumero());
            addCampo(campos, "grandeza",         m.getGrandeza());
            addCampo(campos, "posto_horario",    m.getPostoHorario());
            addCampo(campos, "unidade",          m.getUnidade());
            addCampo(campos, "leitura_anterior", m.getLeituraAnterior());
            addCampo(campos, "leitura_atual",    m.getLeituraAtual());
            addCampo(campos, "constante",        m.getConstante());
            addCampo(campos, "consumo",          m.getConsumo());
            sb.append("    {\n");
            for (int j = 0; j < campos.size(); j++)
                sb.append("      ").append(campos.get(j)).append(j < campos.size() - 1 ? "," : "").append("\n");
            sb.append("    }").append(i < medidores.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]");
    }

    // ── Histórico ─────────────────────────────────────────────────

    private static void historicoJson(Sb sb, List<HistoricoMes> hist) {
        sb.key("historico_consumo").append("[\n");
        for (int i = 0; i < hist.size(); i++) {
            HistoricoMes h = hist.get(i);
            sb.append("    { \"mes\": \"").append(h.getMes()).append("\"");
            if (h.getConsumoKwh() != null) sb.append(", \"kwh\": ").append(String.valueOf(h.getConsumoKwh()));
            if (h.getNumeroDias()  != null) sb.append(", \"dias\": ").append(String.valueOf(h.getNumeroDias()));
            sb.append(" }").append(i < hist.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]");
    }

    // ── Grandezas Contratadas ─────────────────────────────────────

    private static void grandezasJson(Sb sb, Map<String, Double> gc) {
        sb.key("grandezas_contratadas").append("{\n");
        List<Map.Entry<String, Double>> entries = new ArrayList<>(gc.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> e = entries.get(i);
            sb.append("    \"").append(e.getKey()).append("\": ").append(fmt(e.getValue()));
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }");
    }

    // ── Saldo GD ──────────────────────────────────────────────────

    private static void saldoGdJson(Sb sb, SaldoGd s) {
        sb.key("saldo_gd").append("{\n");
        sb.append("    \"saldo_creditos_kwh\": ").append(fmt(s.getSaldoCreditos())).append(",\n");
        sb.append("    \"energia_alocada_kwh\": ").append(fmt(s.getEnergiaAlocada())).append("\n");
        sb.append("  }");
    }

    // ── Demonstrativo de Consumo (2ª folha) ───────────────────────

    private static void demonstrativoJson(Sb sb, List<DemonstrativoLinha> linhas,
                                          Double fatorPonta, Double fatorForaPonta) {
        sb.key("demonstrativo_consumo").append("{\n");
        sb.append("    \"linhas\": [\n");
        for (int i = 0; i < linhas.size(); i++) {
            DemonstrativoLinha dl = linhas.get(i);
            sb.append("      {\n");
            sb.append("        \"grandeza\": \"").append(dl.getGrandeza().replace("\"", "\\\"")).append("\",\n");
            sb.append("        \"leitura_inicial\": ").append(fmt(dl.getLeituraInicial())).append(",\n");
            sb.append("        \"leitura_final\": ").append(fmt(dl.getLeituraFinal())).append(",\n");
            sb.append("        \"constante\": ").append(fmt(dl.getConstante())).append(",\n");
            sb.append("        \"ajuste\": ").append(fmt(dl.getAjuste())).append(",\n");
            sb.append("        \"consumo_demanda\": ").append(fmt(dl.getConsumoDemanda())).append("\n");
            sb.append("      }").append(i < linhas.size() - 1 ? "," : "").append("\n");
        }
        sb.append("    ]");
        if (fatorPonta != null || fatorForaPonta != null) {
            sb.append(",\n    \"fator_carga\": {\n");
            if (fatorPonta != null)      sb.append("      \"na_ponta\": ").append(fmt(fatorPonta));
            if (fatorPonta != null && fatorForaPonta != null) sb.append(",");
            if (fatorPonta != null)      sb.append("\n");
            if (fatorForaPonta != null)  sb.append("      \"fora_ponta\": ").append(fmt(fatorForaPonta)).append("\n");
            sb.append("    }");
        }
        sb.append("\n  }");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static void addCampo(List<String> list, String k, Object v) {
        if (v == null) return;
        if (v instanceof String) list.add("\"" + k + "\": \"" + ((String) v).replace("\"", "\\\"") + "\"");
        else list.add("\"" + k + "\": " + v);
    }

    private static void addCampoTrib(List<String> list, String k, Double v) {
        if (v != null) list.add("\"" + k + "\": " + fmt(v));
    }

    private static String fmt(Double v) { return v != null ? String.valueOf(v) : "null"; }

    public static void exportar(DadosFatura d, String destino) throws IOException {
        try (PrintWriter w = new PrintWriter(destino, StandardCharsets.UTF_8)) {
            w.print(toJson(d));
        }
        System.out.println("JSON exportado: " + destino);
    }

    private static class Sb {
        private final StringBuilder b = new StringBuilder();
        Sb open()  { b.append("{\n"); return this; }
        Sb close() { b.append("}\n"); return this; }
        Sb comma() { b.append(",\n"); return this; }
        Sb key(String k) { b.append("  \"").append(k).append("\": "); return this; }
        Sb append(String s) { b.append(s); return this; }

        // Remove trailing comma+newline so a null last-field doesn't break JSON
        Sb trimComma() {
            int len = b.length();
            if (len >= 2 && b.charAt(len - 1) == '\n' && b.charAt(len - 2) == ',') {
                b.deleteCharAt(len - 2);
            }
            return this;
        }

        void field(String k, Object v)               { field(k, v, false); }
        void field(String k, Object v, boolean last) {
            if (v == null) return;
            b.append("    \"").append(k).append("\": ");
            if (v instanceof String) b.append('"').append(((String) v).replace("\"", "\\\"")).append('"');
            else b.append(v);
            if (!last) b.append(",");
            b.append("\n");
        }
        @Override public String toString() { return b.toString(); }
    }
}
