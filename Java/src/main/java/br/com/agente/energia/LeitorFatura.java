package br.com.agente.energia;

import br.com.agente.energia.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrator genérico de faturas de energia elétrica emitidas sob a RN 1000 ANEEL.
 *
 * Cobre qualquer distribuidora brasileira que siga o padrão NF-e de energia,
 * incluindo os layouts mais comuns:
 *  - Grupo B (B1/B2/B3): monofásico/trifásico convencional
 *  - Grupo A (A4/A3/A2/A1): horo-sazonal, com demanda, GD, múltiplos postos
 */
public class LeitorFatura {

    // Unidades de medida reconhecidas
    private static final String UNIDADES = "kWh|kW|kVAr|kVARh|kVArh|MWh|MVAr";

    // Item completo: DESCRICAO UNIDADE QTDE PRECO VALOR PIS BASE ALQ ICMS TARIFA (10 colunas)
    private static final Pattern P_ITEM_COMPLETO = Pattern.compile(
            "^(.+?)\\s+(" + UNIDADES + ")\\s+([\\d\\.,]+-?)\\s+([\\d\\.,]+)\\s+([\\d\\.,]+-?)" +
            "\\s+([\\d\\.,]+-?)\\s+([\\d\\.,]+-?)\\s+([\\d,]+)\\s+([\\d\\.,]+-?)\\s+([\\d\\.,]+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Medidor: NUM GRANDEZA POSTO LEIT_ANT LEIT_AT CONST CONSUMO
    // Aceita variações de posto horário: Único, Ponta, Fora Ponta, F.Ponta, Fora de Ponta
    private static final Pattern P_MEDIDOR = Pattern.compile(
            "^(\\d{6,})\\s+((?:Energia|Demanda|Potência)\\s+(?:Ativa|Reativa|Elétrica))" +
            "\\s+(Único|Única|Ponta|Fora\\s*(?:de\\s*)?Ponta|F\\.?\\s*Ponta)\\s+" +
            "([\\d\\.,]+)\\s+([\\d\\.,]+)\\s+([\\d\\.,]+)\\s+([\\d\\.,]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern P_DATA    = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern P_MES_ANO = Pattern.compile("\\d{2}/\\d{4}");
    private static final Pattern P_CEP     = Pattern.compile("\\d{5}[\\-\\s]\\d{3}");

    public DadosFatura lerTexto(String texto) {
        DadosFatura dados = new DadosFatura();
        processar(texto, dados);
        return dados;
    }

    public DadosFatura lerFatura(String caminhoPdf) throws IOException {
        return lerFatura(caminhoPdf, "59832");
    }

    public DadosFatura lerFatura(String caminhoPdf, String senha) throws IOException {
        File arquivo = new File(caminhoPdf);
        if (!arquivo.exists()) throw new IllegalArgumentException("Arquivo não encontrado: " + caminhoPdf);
        DadosFatura dados = new DadosFatura();
        try (PDDocument doc = carregarPdf(arquivo, senha)) {
            processar(new PDFTextStripper().getText(doc), dados);
        }
        if (dados.getResumoPagamento().getCodigoBarras() == null) {
            String codigoImagem = new LeitorCodigoBarrasImagem().lerCodigoBarras(caminhoPdf);
            if (codigoImagem != null) {
                dados.getResumoPagamento().setCodigoBarras(codigoImagem);
            }
        }
        return dados;
    }

    private PDDocument carregarPdf(File arquivo, String senha) throws IOException {
        try {
            return Loader.loadPDF(arquivo);
        } catch (Exception e) {
            return Loader.loadPDF(arquivo, senha);
        }
    }

    private void processar(String texto, DadosFatura d) {
        String[] ls = texto.split("\\r?\\n");

        for (int i = 0; i < ls.length; i++) {
            String l  = ls[i].strip();
            String lu = l.toUpperCase();
            String prox  = i + 1 < ls.length ? ls[i + 1].strip() : "";
            String prox2 = i + 2 < ls.length ? ls[i + 2].strip() : "";
            String prox3 = i + 3 < ls.length ? ls[i + 3].strip() : "";

            // ═══════════════════════════════════════════════════════
            // BLOCO 1 — DISTRIBUIDORA
            // Estratégia genérica: ao encontrar a linha CNPJ + IE,
            // busca o nome nos N linhas anteriores e o endereço (CEP) antes ou depois.
            // ═══════════════════════════════════════════════════════
            if (lu.startsWith("CNPJ") &&
                (lu.contains("INSCRIÇÃO ESTADUAL") || lu.contains("INSCRICAO ESTADUAL") ||
                 lu.contains("INSCRI") && lu.contains("ESTADUAL") ||
                 lu.contains(" IE:") || lu.contains(" IE ") || lu.contains("\tIE")) &&
                d.getDistribuidora().getCnpj() == null) {

                Matcher m = Pattern.compile("CNPJ[\\s:]+([\\d./\\-]+)", Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find()) d.getDistribuidora().setCnpj(m.group(1));

                m = Pattern.compile("(?:INSCRI[CÇ][AÃ]O ESTADUAL|\\bIE\\b)[\\s:|]+([\\d\\-\\.]+)",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find()) d.getDistribuidora().setInscricaoEstadual(m.group(1));

                // Busca nome e endereço nas linhas anteriores
                for (int j = i - 1; j >= Math.max(0, i - 8); j--) {
                    String prev = ls[j].strip();
                    if (prev.isEmpty()) continue;
                    String prevU = prev.toUpperCase();
                    if (P_CEP.matcher(prev).find() && d.getDistribuidora().getEndereco() == null) {
                        d.getDistribuidora().setEndereco(prev);
                    } else if (d.getDistribuidora().getNome() == null &&
                               prev.length() > 5 &&
                               !prevU.startsWith("CNPJ") &&
                               !prevU.contains("NOTA FISCAL") &&
                               !prevU.contains("INSCRIÇÃO") &&
                               !prevU.matches(".*\\d{5}[\\-\\s]\\d{3}.*")) {
                        d.getDistribuidora().setNome(prev);
                    }
                    if (d.getDistribuidora().getNome() != null && d.getDistribuidora().getEndereco() != null)
                        break;
                }

                // Se ainda não encontrou endereço, busca nas linhas seguintes
                if (d.getDistribuidora().getEndereco() == null) {
                    for (int j = i + 1; j < Math.min(ls.length, i + 6); j++) {
                        String next = ls[j].strip();
                        if (!next.isEmpty() && P_CEP.matcher(next).find()) {
                            d.getDistribuidora().setEndereco(next);
                            break;
                        }
                    }
                }
            }

            // IE em linha separada (quando CNPJ e IE estão em linhas diferentes)
            if ((lu.startsWith("INSCRIÇÃO ESTADUAL") || lu.startsWith("INSCRICAO ESTADUAL") ||
                 lu.startsWith("IE:")) &&
                d.getDistribuidora().getCnpj() != null && d.getDistribuidora().getInscricaoEstadual() == null) {
                Matcher m = Pattern.compile("(?:INSCRI[CÇ][AÃ]O ESTADUAL|IE)[\\s:|]+([\\d\\-\\.]+)",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find()) d.getDistribuidora().setInscricaoEstadual(m.group(1));
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 2 — IDENTIFICAÇÃO DO CLIENTE
            // ═══════════════════════════════════════════════════════
            if ((lu.equals("NOME DO CLIENTE:") || lu.equals("NOME DO CLIENTE") ||
                 lu.equals("CONSUMIDOR:") || lu.equals("CONSUMIDOR") ||
                 lu.equals("CLIENTE:") || lu.equals("CLIENTE") ||
                 lu.equals("TITULAR:") || lu.equals("TITULAR")) &&
                d.getCliente().getNome() == null) {
                if (!prox.isEmpty() && !prox.toUpperCase().contains("CÓDIGO") && prox.length() < 80)
                    d.getCliente().setNome(prox);
            }

            // CPF ou CNPJ do cliente
            if ((lu.startsWith("CNPJ:") || lu.startsWith("CPF:") ||
                 lu.startsWith("CPF/CNPJ:") || lu.startsWith("CPF / CNPJ:")) &&
                d.getCliente().getCnpj() == null) {
                Matcher m = Pattern.compile("(?:CPF/CNPJ|CPF|CNPJ)[:\\s]+([\\d./\\-*]+)",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find()) d.getCliente().setCnpj(m.group(1));
            }

            if ((lu.contains("CÓDIGO DA INSTALAÇÃO") || lu.contains("CODIGO DA INSTALACAO") ||
                 lu.contains("NÚMERO DA INSTALAÇÃO") || lu.contains("Nº INSTALAÇÃO") ||
                 lu.contains("CÓD. INSTALAÇÃO")) &&
                d.getCliente().getNumeroInstalacao() == null) {
                if (prox.matches("\\d+")) d.getCliente().setNumeroInstalacao(prox);
            }

            if ((lu.contains("CÓDIGO DO CLIENTE") || lu.contains("CODIGO DO CLIENTE") ||
                 lu.contains("CÓD. CLIENTE") || lu.contains("CÓDIGO CLIENTE")) &&
                d.getCliente().getCodigoCliente() == null) {
                if (prox.matches("\\d{6,}")) d.getCliente().setCodigoCliente(prox);
            }

            if ((lu.equals("ENDEREÇO:") || lu.equals("ENDERECO:") ||
                 lu.equals("ENDEREÇO DA UNIDADE:") || lu.equals("LOGRADOURO:")) &&
                d.getCliente().getLogradouro() == null) {
                d.getCliente().setLogradouro(prox);
                for (int j = i + 2; j < Math.min(i + 6, ls.length); j++) {
                    Matcher m = Pattern.compile("(\\d{5}[\\-\\s]\\d{3})\\s+(.+?)\\s+(\\w{2})$").matcher(ls[j].strip());
                    if (m.find()) {
                        d.getCliente().setBairro(ls[i + 2].strip());
                        d.getCliente().setCep(m.group(1).replace(" ", "-"));
                        d.getCliente().setCidade(m.group(2).strip());
                        d.getCliente().setUf(m.group(3));
                        break;
                    }
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 3 — DADOS TARIFÁRIOS
            // ═══════════════════════════════════════════════════════
            if (lu.contains("CLASSIFICA") && d.getTarifario().getClassificacao() == null) {
                Matcher m = Pattern.compile(
                        "(?i)CLASSIFICA[ÇC][ÃA]O[:\\s]+([^\\t]+?)(?:\\s{2,}TIPO DE FORNECIMENTO[:\\s]+(.+))?$").matcher(l);
                if (m.find()) {
                    d.getTarifario().setClassificacao(m.group(1).strip());
                    if (m.group(2) != null) {
                        d.getTarifario().setTipoFornecimento(m.group(2).strip());
                    } else {
                        Matcher m2 = Pattern.compile("TIPO DE FORNECIMENTO[:\\s]+(.+)$", Pattern.CASE_INSENSITIVE).matcher(prox);
                        if (m2.find()) d.getTarifario().setTipoFornecimento(m2.group(1).strip());
                        Matcher ms = Pattern.compile("^([A-ZÁÉÍÓÚÂÊÔÃÕÜ\\s]+?)\\s+--?\\s+", Pattern.CASE_INSENSITIVE).matcher(prox);
                        if (ms.find()) d.getTarifario().setSubclasse(ms.group(1).strip());
                    }
                }
            }
            if (lu.contains("BANDEIRA") || lu.contains("BAND.") || lu.contains("BD.")) {
                // Aceita "Vermelha" puro, "Vermelha 1/2", "Vermelha Patamar 1/2"
                Pattern pBand = Pattern.compile(
                        "\\b(Verde|Amarela|Vermelha(?:\\s+(?:Patamar\\s+)?[12])?)\\b",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = pBand.matcher(l);
                if (m.find() && d.getTarifario().getBandeira() == null) {
                    // Normaliza para o nome principal da bandeira (sem patamar)
                    String b = m.group(1).replaceAll("(?i)\\s+(?:Patamar\\s+)?[12]$", "").strip();
                    d.getTarifario().setBandeira(b);
                }
                if (d.getTarifario().getBandeira() == null && !prox.isEmpty()) {
                    m = pBand.matcher(prox);
                    if (m.find()) {
                        String b = m.group(1).replaceAll("(?i)\\s+(?:Patamar\\s+)?[12]$", "").strip();
                        d.getTarifario().setBandeira(b);
                    }
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 4 — NF-e
            // ═══════════════════════════════════════════════════════
            if (lu.contains("NOTA FISCAL") && lu.contains("SÉRIE")) {
                Matcher m = Pattern.compile(
                        "NOTA FISCAL N[°º]\\s+(\\d+).*SÉRIE\\s+(\\d+).*DATA DE EMISS[ÃA]O[:\\s]+(\\d{2}/\\d{2}/\\d{4})",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find() && d.getNfe().getNumero() == null) {
                    d.getNfe().setNumero(m.group(1));
                    d.getNfe().setSerie(m.group(2));
                    d.getNfe().setDataEmissao(m.group(3));
                }
            }
            if (lu.equals("CHAVE DE ACESSO:") || lu.equals("CHAVE DE ACESSO")) {
                if (!prox.isEmpty() && d.getNfe().getChaveAcesso() == null)
                    d.getNfe().setChaveAcesso(prox);
            }
            if (lu.contains("PROTOCOLO DE AUTORIZA") && d.getNfe().getProtocolo() == null) {
                Matcher m = Pattern.compile("(?i)PROTOCOLO.*?:\\s*(\\d+)\\s*-\\s*(\\d{2}/\\d{2}/\\d{4})").matcher(l);
                if (m.find()) { d.getNfe().setProtocolo(m.group(1)); d.getNfe().setDataProtocolo(m.group(2)); }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 5 — LEITURAS
            // ═══════════════════════════════════════════════════════
            if (lu.contains("LEITURA ANTERIOR") && lu.contains("LEITURA ATUAL") && d.getLeituras().getDataAnterior() == null) {
                Matcher m = Pattern.compile(
                        "LEITURA ANTERIOR\\s+(\\d{2}/\\d{2}/\\d{4})\\s+LEITURA ATUAL\\s+(\\d{2}/\\d{2}/\\d{4})" +
                        "(?:\\s+N[°º]\\s+DE DIAS\\s+(\\d+))?(?:\\s+PR[ÓO]XIMA LEITURA\\s+(\\d{2}/\\d{2}/\\d{4}))?",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.find()) {
                    d.getLeituras().setDataAnterior(m.group(1));
                    d.getLeituras().setDataAtual(m.group(2));
                    if (m.group(3) != null) d.getLeituras().setNumeroDias(Integer.parseInt(m.group(3)));
                    if (m.group(4) != null) d.getLeituras().setDataProxima(m.group(4));
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 6 — RESUMO DE PAGAMENTO
            // Aceita variações de rótulo entre distribuidoras
            // ═══════════════════════════════════════════════════════
            if (d.getResumoPagamento().getReferencia() == null &&
                (lu.equals("REF:MÊS/ANO") || lu.equals("REF:MES/ANO") ||
                 lu.equals("MÊS/ANO") || lu.equals("MES/ANO") ||
                 lu.contains("REFERÊNCIA") || lu.contains("REFERENCIA") ||
                 lu.contains("PERÍODO") || lu.contains("COMPETÊNCIA") ||
                 lu.equals("MÊS DE REFERÊNCIA") || lu.equals("PERIODO"))) {
                Matcher m = P_MES_ANO.matcher(l);
                if (m.find()) {
                    d.getResumoPagamento().setReferencia(m.group());
                } else if (P_MES_ANO.matcher(prox).matches()) {
                    d.getResumoPagamento().setReferencia(prox);
                }
            }
            if (d.getResumoPagamento().getDataVencimento() == null &&
                (lu.equals("VENCIMENTO") || lu.contains("DATA DE VENCIMENTO") ||
                 lu.startsWith("VCTO") || lu.startsWith("VENCTO"))) {
                Matcher m = P_DATA.matcher(l);
                if (m.find()) {
                    d.getResumoPagamento().setDataVencimento(m.group());
                } else if (P_DATA.matcher(prox).matches()) {
                    d.getResumoPagamento().setDataVencimento(prox);
                }
            }
            if (d.getResumoPagamento().getValorTotal() == null &&
                (lu.contains("TOTAL A PAGAR") || lu.contains("VALOR A PAGAR") ||
                 lu.contains("VALOR TOTAL") || lu.equals("TOTAL"))) {
                Double v = lastNumber(l);
                if (v == null || v == 0.0) v = parseValor(prox);
                if (v != null && v > 0) d.getResumoPagamento().setValorTotal(v);
            }
            // Linha digitável do boleto: AAAAA.BBBBB CCCCC.CCCCCC DDDDD.DDDDDD K EEEEEEEEEEEEEE
            if (l.matches("\\d{5}\\.\\d+\\s+\\d{5}\\.\\d+\\s+\\d{5}\\.\\d+\\s+\\d\\s+\\d+") ||
                // Código de barras numérico sem pontos (44 dígitos em 4 campos)
                l.matches("\\d{12}\\s+\\d{12}\\s+\\d{12}\\s+\\d{12}")) {
                d.getResumoPagamento().setCodigoBarras(l.replaceAll("\\s+", " "));
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 7 — ITENS DA FATURA
            // ═══════════════════════════════════════════════════════
            {
                Matcher m = P_ITEM_COMPLETO.matcher(l);
                if (m.matches()) {
                    ItemFatura it = new ItemFatura(m.group(1).strip());
                    it.setUnidade(m.group(2));
                    it.setQuantidade(parseValorGd(m.group(3)));
                    it.setPrecoUnitComTrib(parseValor(m.group(4)));
                    it.setValor(parseValorGd(m.group(5)));
                    it.setPisCofins(parseValorGd(m.group(6)));
                    it.setBaseCalcIcms(parseValorGd(m.group(7)));
                    it.setAliquotaIcms(parseValor(m.group(8)));
                    it.setValorIcms(parseValorGd(m.group(9)));
                    it.setTarifaUnit(parseValor(m.group(10)));
                    d.addItem(it);
                    // Identifica acréscimo de bandeira no formato completo (com unidade)
                    String descU = m.group(1).toUpperCase();
                    if (isBandeiraDesc(descU)) {
                        Double ac = d.getTarifario().getValorAcrescimoBandeira();
                        d.getTarifario().setValorAcrescimoBandeira((ac != null ? ac : 0.0) + (it.getValor() != null ? it.getValor() : 0.0));
                    }
                }
            }
            // Acréscimo de Bandeira sem unidade de medida:
            //  - Formato 5 colunas: DESC  VALOR  PIS  BASE_ICMS  ALQ  ICMS
            //    ex: "Acrés. Band.VERMELHA    23,17    1,16    23,17  20,00    4,63"
            //        "Acrés.Bd.VERMELHA-P2   170,44    8,61   170,44  20,00   34,08"
            //  - Formato 1 coluna: DESC  VALOR
            //    ex: "Acréscimo Bandeira Amarela    5,90"
            if (isBandeiraDesc(lu) &&
                !Pattern.compile("\\b(" + UNIDADES + ")\\b", Pattern.CASE_INSENSITIVE).matcher(l).find()) {
                ItemFatura it = new ItemFatura(l.split("  +")[0].strip());
                // Tenta 5 colunas: valor  pis  base  aliq  icms (valores podem ser negativos com trailing -)
                Matcher m5 = Pattern.compile(
                        "^.+?\\s{2,}([\\d\\.]+,[\\d]+-?)\\s+([\\d\\.]+,[\\d]+-?)\\s+([\\d\\.]+,[\\d]+-?)\\s+([\\d,]+)\\s+([\\d\\.]+,[\\d]+-?)\\s*$"
                ).matcher(l);
                if (m5.matches()) {
                    it.setValor(parseValorGd(m5.group(1)));
                    it.setPisCofins(parseValorGd(m5.group(2)));
                    it.setBaseCalcIcms(parseValorGd(m5.group(3)));
                    it.setAliquotaIcms(parseValor(m5.group(4)));
                    it.setValorIcms(parseValorGd(m5.group(5)));
                } else {
                    it.setValor(parseValorGd(lastNumberRaw(l)));
                }
                if (it.getValor() != null) {
                    d.addItem(it);
                    Double ac = d.getTarifario().getValorAcrescimoBandeira();
                    d.getTarifario().setValorAcrescimoBandeira((ac != null ? ac : 0.0) + it.getValor());
                }
            }
            // COSIP / Ilum. Pública
            // Padrão mais comum: "Ilum. Púb. Municipal" ou "Iluminação Pública Municipal"
            if (lu.contains("ILUM") && lu.contains("MUNICIPAL")) {
                Double v = lastNumber(l);
                ItemFatura it = new ItemFatura("Ilum. Púb. Municipal");
                it.setValor(v);
                d.addItem(it);
                if (v != null) d.getTributos().setCosip(v);
            }
            // Distribuidoras que usam "COSIP" ou "CIP" como rótulo do item (com valor na mesma linha)
            // Guarda: não re-detectar se já capturado, e exige valor numérico no próprio item
            if (d.getTributos().getCosip() == null &&
                (lu.equals("CIP") || lu.startsWith("COSIP ") || lu.equals("COSIP")) ) {
                Double v = lastNumber(l);
                if (v != null && v > 0) {
                    ItemFatura it = new ItemFatura("Ilum. Púb. Municipal");
                    it.setValor(v);
                    d.addItem(it);
                    d.getTributos().setCosip(v);
                }
            }
            // Parcelamento
            if (l.matches("(?i)Parc\\d+/\\d+.*")) {
                ItemFatura it = new ItemFatura(l.replaceAll("\\s{2,}.*", "").strip());
                it.setValor(lastNumber(l));
                d.addItem(it);
            }
            // Encargos: Multa, Juros, IPCA
            if (lu.startsWith("MULTA-NF") || lu.startsWith("MULTA NF")) {
                ItemFatura it = new ItemFatura(l.replaceAll("\\s{2,}.*", "").strip());
                it.setValor(lastNumber(l));
                d.addItem(it);
                d.getTributos().setMulta(lastNumber(l));
            }
            if (lu.startsWith("JUROS-NF") || lu.startsWith("JUROS NF")) {
                ItemFatura it = new ItemFatura(l.replaceAll("\\s{2,}.*", "").strip());
                it.setValor(lastNumber(l));
                d.addItem(it);
                d.getTributos().setJuros(lastNumber(l));
            }
            if (lu.startsWith("IPCA-NF") || lu.startsWith("IPCA NF")) {
                ItemFatura it = new ItemFatura(l.replaceAll("\\s{2,}.*", "").strip());
                it.setValor(lastNumber(l));
                d.addItem(it);
                d.getTributos().setIpca(lastNumber(l));
            }
            // Compensação DIC/FIC/DMIC — crédito por violação de continuidade (valor negativo)
            if (lu.startsWith("COMP.DIC") || lu.startsWith("COMP. DIC") ||
                lu.startsWith("COMPENSAÇÃO DIC") || lu.startsWith("COMPENSACAO DIC") ||
                lu.startsWith("COMP. FIC") || lu.startsWith("COMP.FIC") ||
                lu.startsWith("COMP. DMIC") || lu.startsWith("COMP.DMIC")) {
                String raw = lastNumberRaw(l);
                if (raw != null) {
                    ItemFatura it = new ItemFatura(l.replaceAll("\\s{2,}.*", "").strip());
                    it.setValor(parseValorGd(raw));
                    d.addItem(it);
                    Double acum = d.getTributos().getCompDic();
                    d.getTributos().setCompDic((acum != null ? acum : 0.0) + it.getValor());
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 8 — TRIBUTOS
            // ═══════════════════════════════════════════════════════
            if (lu.matches("\\s*PIS\\s+[\\d\\.,]+.*"))    d.getTributos().setPis(parseTributo(l));
            if (lu.matches("\\s*COFINS\\s+[\\d\\.,]+.*")) d.getTributos().setCofins(parseTributo(l));
            if (lu.matches("\\s*ICMS\\s+[\\d\\.,]+.*"))   d.getTributos().setIcms(parseTributo(l));

            // ═══════════════════════════════════════════════════════
            // BLOCO 9 — MEDIDOR(ES)
            // ═══════════════════════════════════════════════════════
            {
                Matcher m = P_MEDIDOR.matcher(l);
                if (m.find()) {
                    DadosMedidor med = new DadosMedidor();
                    med.setNumero(m.group(1));
                    med.setGrandeza(m.group(2));
                    // Normaliza posto horário
                    String posto = m.group(3).toUpperCase().replaceAll("\\s+", " ").strip();
                    if (posto.contains("FORA") || posto.startsWith("F.") || posto.equals("FP"))
                        posto = "Fora Ponta";
                    else if (posto.equals("ÚNICO") || posto.equals("ÚNICA"))
                        posto = "Único";
                    else if (posto.equals("PONTA") || posto.equals("P"))
                        posto = "Ponta";
                    med.setPostoHorario(posto);
                    med.setUnidade(m.group(2).toUpperCase().contains("DEMANDA") ? "kW" : "kWh");
                    med.setLeituraAnterior(parseValor(m.group(4)));
                    med.setLeituraAtual(parseValor(m.group(5)));
                    med.setConstante(parseValor(m.group(6)));
                    med.setConsumo(parseValor(m.group(7)));
                    d.addMedidor(med);
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 10 — HISTÓRICO DE CONSUMO (Grupo B)
            // Formato: "ABR26 313 29" ou "NOV25" (sem dados)
            // ═══════════════════════════════════════════════════════
            {
                Matcher m = Pattern.compile(
                        "^((?:JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)\\d{2})(?:\\s+(\\d+)\\s+(\\d+))?$",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                if (m.matches()) {
                    Integer kwh  = m.group(2) != null ? Integer.parseInt(m.group(2)) : null;
                    Integer dias = m.group(3) != null ? Integer.parseInt(m.group(3)) : null;
                    d.addHistorico(new HistoricoMes(m.group(1).toUpperCase(), kwh, dias));
                }
            }

            // BLOCO 10b — HISTÓRICO EM GRÁFICO (Grupo A / GD)
            // Linha de meses + linha de anos
            if (d.getHistoricoConsumo().isEmpty()) {
                Matcher mMeses = Pattern.compile(
                        "^((?:JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)" +
                        "(?:\\s+(?:JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)){2,})$",
                        Pattern.CASE_INSENSITIVE).matcher(l);
                Matcher mAnos = Pattern.compile("^\\d{2}(?:\\s+\\d{2})+$").matcher(prox);
                if (mMeses.matches() && mAnos.matches()) {
                    String[] meses = l.trim().split("\\s+");
                    String[] anos  = prox.trim().split("\\s+");
                    for (int j = 0; j < Math.min(meses.length, anos.length); j++)
                        d.addHistorico(new HistoricoMes(meses[j].toUpperCase() + anos[j], null, null));
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 11 — GRANDEZAS CONTRATADAS (Grupo A)
            // ═══════════════════════════════════════════════════════
            if (lu.startsWith("DEMANDA CONTRATADA")) {
                Double v = lastNumber(l);
                if (v != null) d.addGrandezaContratada(l.replaceAll("\\s{2,}.*", "").strip(), v);
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 12 — SALDO GD (Geração Distribuída)
            // ═══════════════════════════════════════════════════════
            if (lu.contains("SALDO ATUALIZADO DE CRÉDITOS") || lu.contains("SALDO ATUALIZADO DE CREDITOS")) {
                Double v = lastNumber(l);
                if (v != null) {
                    if (d.getSaldoGd() == null) d.setSaldoGd(new SaldoGd());
                    d.getSaldoGd().setSaldoCreditos(v);
                }
            }
            if (lu.contains("ENERGIA ALOCADA NO CICLO")) {
                Double v = lastNumber(l);
                if (v != null) {
                    if (d.getSaldoGd() == null) d.setSaldoGd(new SaldoGd());
                    d.getSaldoGd().setEnergiaAlocada(v);
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 13 — DEMONSTRATIVO DE CONSUMO (2ª folha, Grupo A)
            // 5 valores: leit.ini  leit.fim  constante(≥4 dec)  ajuste  consumo
            // ═══════════════════════════════════════════════════════
            {
                Matcher m = Pattern.compile(
                        "^(.+?)\\s{2,}([\\d\\.]+,[\\d]+)\\s+([\\d\\.]+,[\\d]+)\\s+([\\d\\.]+,[\\d]{4,6})\\s+([\\d\\.]+,[\\d]+)\\s+([\\d\\.]+,[\\d]+)\\s*$"
                ).matcher(l);
                if (m.matches()) {
                    d.addDemonstrativo(new DemonstrativoLinha(
                            m.group(1).strip(),
                            parseValor(m.group(2)),
                            parseValor(m.group(3)),
                            parseValor(m.group(4)),
                            parseValor(m.group(5)),
                            parseValor(m.group(6))));
                }
            }

            // ═══════════════════════════════════════════════════════
            // BLOCO 14 — FATOR DE CARGA (2ª folha)
            // ═══════════════════════════════════════════════════════
            if (lu.contains("NA PONTA") && lu.contains("FORA DE PONTA") && d.getFatorCargaPonta() == null) {
                Matcher m = Pattern.compile(
                        "(?i)na ponta[:\\s]+(\\d+[,.]\\d+)\\s+fora de ponta[:\\s]+(\\d+[,.]\\d+)").matcher(l);
                if (m.find()) {
                    d.setFatorCargaPonta(parseValor(m.group(1).replace(".", ",")));
                    d.setFatorCargaForaPonta(parseValor(m.group(2).replace(".", ",")));
                }
            }
        }
    }

    /** Verifica se uma descrição (já em maiúsculas) é de acréscimo de bandeira tarifária. */
    private boolean isBandeiraDesc(String lu) {
        boolean temAcres = lu.contains("ACRÉS") || lu.contains("ACRES") ||
                           lu.contains("ACRÉSCIMO") || lu.contains("ACRESCIMO");
        boolean temBand  = lu.contains("VERMELH") || lu.contains("AMAREL") ||
                           lu.contains("BANDEIRA") || lu.contains("BAND.") || lu.contains("BD.");
        return temAcres && temBand;
    }

    private Tributo parseTributo(String linha) {
        Matcher m = Pattern.compile("([\\d\\.]+,[\\d]{2,4})\\s+([\\d\\.]+,[\\d]{2})\\s+([\\d\\.]+,[\\d]{2})").matcher(linha);
        if (m.find()) return new Tributo(parseValor(m.group(1)), parseValor(m.group(2)), parseValor(m.group(3)));
        return null;
    }

    /** Retorna o texto bruto do último número da linha, incluindo sinal negativo trailing (ex: "41,93-"). */
    private String lastNumberRaw(String linha) {
        Matcher m = Pattern.compile("[\\d\\.]+,[\\d]{2,4}-?").matcher(linha);
        String last = null;
        while (m.find()) last = m.group();
        return last;
    }

    private Double lastNumber(String linha) {
        Matcher m = Pattern.compile("[\\d\\.]+,[\\d]{2,4}|\\d+(?:\\.\\d+)?").matcher(linha);
        String last = null;
        while (m.find()) last = m.group();
        return last != null ? parseValor(last) : null;
    }

    private Double parseValorGd(String texto) {
        if (texto == null) return null;
        boolean neg = texto.endsWith("-");
        Double v = parseValor(texto.replace("-", ""));
        return (v != null && neg) ? -v : v;
    }

    private Double parseValor(String texto) {
        if (texto == null) return null;
        try { return Double.parseDouble(texto.strip().replace(".", "").replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }
}
