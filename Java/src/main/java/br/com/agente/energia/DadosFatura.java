package br.com.agente.energia;

import br.com.agente.energia.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Raiz da fatura — agrega todos os blocos extraídos do PDF.
 *
 * Cobre dois layouts da Neoenergia Cosern:
 *   - Residencial B1 (monofásico, convencional)
 *   - Industrial A4 Horo-sazonal Verde (trifásico, GD, múltiplos postos)
 */
public class DadosFatura {

    private Distribuidora            distribuidora       = new Distribuidora();
    private IdentificacaoCliente     cliente             = new IdentificacaoCliente();
    private DadosTarifarios          tarifario           = new DadosTarifarios();
    private DadosNfe                 nfe                 = new DadosNfe();
    private Leituras                 leituras            = new Leituras();
    private ResumoPagamento          resumoPagamento     = new ResumoPagamento();
    private List<ItemFatura>         itensFatura         = new ArrayList<>();
    private DadosTributos            tributos            = new DadosTributos();
    private List<DadosMedidor>       medidores           = new ArrayList<>();
    private List<HistoricoMes>       historicoConsumo    = new ArrayList<>();
    private Map<String, Double>      grandezasContratadas = new LinkedHashMap<>();
    private SaldoGd                  saldoGd             = null;
    private List<DemonstrativoLinha> demonstrativo        = new ArrayList<>();
    private Double                   fatorCargaPonta      = null;
    private Double                   fatorCargaForaPonta  = null;

    public Distribuidora getDistribuidora()                    { return distribuidora; }
    public IdentificacaoCliente getCliente()                   { return cliente; }
    public DadosTarifarios getTarifario()                      { return tarifario; }
    public DadosNfe getNfe()                                   { return nfe; }
    public Leituras getLeituras()                              { return leituras; }
    public ResumoPagamento getResumoPagamento()                { return resumoPagamento; }
    public List<ItemFatura> getItensFatura()                   { return itensFatura; }
    public DadosTributos getTributos()                         { return tributos; }
    public List<DadosMedidor> getMedidores()                   { return medidores; }
    public List<HistoricoMes> getHistoricoConsumo()            { return historicoConsumo; }
    public Map<String, Double> getGrandezasContratadas()       { return grandezasContratadas; }
    public SaldoGd getSaldoGd()                                { return saldoGd; }
    public void setSaldoGd(SaldoGd v)                         { this.saldoGd = v; }
    public List<DemonstrativoLinha> getDemonstrativo()         { return Collections.unmodifiableList(demonstrativo); }
    public Double getFatorCargaPonta()                         { return fatorCargaPonta; }
    public Double getFatorCargaForaPonta()                     { return fatorCargaForaPonta; }
    public void setFatorCargaPonta(Double v)                   { this.fatorCargaPonta = v; }
    public void setFatorCargaForaPonta(Double v)               { this.fatorCargaForaPonta = v; }

    public void addItem(ItemFatura item)                       { itensFatura.add(item); }
    public void addMedidor(DadosMedidor m)                     { medidores.add(m); }
    public void addHistorico(HistoricoMes h)                   { historicoConsumo.add(h); }
    public void addGrandezaContratada(String nome, Double val) { grandezasContratadas.put(nome, val); }
    public void addDemonstrativo(DemonstrativoLinha dl)        { demonstrativo.add(dl); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sep(sb, "1. DISTRIBUIDORA");
        field(sb, "Nome",           distribuidora.getNome());
        field(sb, "Endereço",       distribuidora.getEndereco());
        field(sb, "CNPJ",           distribuidora.getCnpj());
        field(sb, "Insc. Estadual", distribuidora.getInscricaoEstadual());

        sep(sb, "2. IDENTIFICAÇÃO DO CLIENTE");
        field(sb, "Nome",           cliente.getNome());
        field(sb, "CNPJ/CPF",       cliente.getCnpj());
        field(sb, "Cód. Cliente",   cliente.getCodigoCliente());
        field(sb, "Nº Instalação",  cliente.getNumeroInstalacao());
        field(sb, "Logradouro",     cliente.getLogradouro());
        field(sb, "Bairro",         cliente.getBairro());
        field(sb, "CEP",            cliente.getCep());
        field(sb, "Cidade/UF",      join(" ", cliente.getCidade(), cliente.getUf()));

        sep(sb, "3. DADOS TARIFÁRIOS");
        field(sb, "Classificação",        tarifario.getClassificacao());
        field(sb, "Subclasse",            tarifario.getSubclasse());
        field(sb, "Tipo Fornecimento",    tarifario.getTipoFornecimento());
        field(sb, "Bandeira",             tarifario.getBandeira());
        field(sb, "Acréscimo Bandeira R$",tarifario.getValorAcrescimoBandeira());

        sep(sb, "4. NOTA FISCAL ELETRÔNICA (NF-e)");
        field(sb, "Nº NF",           nfe.getNumero());
        field(sb, "Série",           nfe.getSerie());
        field(sb, "Emissão",         nfe.getDataEmissao());
        field(sb, "Chave de Acesso", nfe.getChaveAcesso());
        field(sb, "Protocolo",       nfe.getProtocolo());
        field(sb, "Data Protocolo",  nfe.getDataProtocolo());

        sep(sb, "5. LEITURAS");
        field(sb, "Data Anterior",   leituras.getDataAnterior());
        field(sb, "Data Atual",      leituras.getDataAtual());
        field(sb, "Nº Dias",         leituras.getNumeroDias());
        field(sb, "Próxima Leitura", leituras.getDataProxima());

        sep(sb, "6. RESUMO DE PAGAMENTO");
        field(sb, "Referência",   resumoPagamento.getReferencia());
        field(sb, "Vencimento",   resumoPagamento.getDataVencimento());
        field(sb, "Total a Pagar",resumoPagamento.getValorTotal());
        field(sb, "Cód. Barras",  resumoPagamento.getCodigoBarras());

        sep(sb, "7. ITENS DA FATURA");
        if (itensFatura.isEmpty()) {
            sb.append("  (nenhum item)\n");
        } else {
            sb.append(String.format("  %-35s %6s %10s %12s %10s %10s %7s %7s %10s%n",
                    "DESCRIÇÃO", "UNID", "QTDE", "PREÇO+TRIB", "VALOR", "PIS/COF", "BC ICMS", "ALQ%", "ICMS"));
            sb.append("  " + "-".repeat(108) + "\n");
            for (ItemFatura it : itensFatura) {
                sb.append(String.format("  %-35s %6s %10s %12s %10s %10s %7s %7s %10s%n",
                        it.getDescricao(),
                        nvl(it.getUnidade(), ""),
                        fmt(it.getQuantidade()),
                        fmt(it.getPrecoUnitComTrib()),
                        fmt(it.getValor()),
                        fmt(it.getPisCofins()),
                        fmt(it.getBaseCalcIcms()),
                        fmt(it.getAliquotaIcms()),
                        fmt(it.getValorIcms())));
            }
        }

        sep(sb, "8. TRIBUTOS");
        tributoRow(sb, "PIS",    tributos.getPis());
        tributoRow(sb, "COFINS", tributos.getCofins());
        tributoRow(sb, "ICMS",   tributos.getIcms());
        field(sb, "COSIP",  tributos.getCosip());
        field(sb, "Multa",  tributos.getMulta());
        field(sb, "Juros",  tributos.getJuros());
        field(sb, "IPCA",   tributos.getIpca());

        sep(sb, "9. MEDIDOR(ES)");
        if (medidores.isEmpty()) {
            sb.append("  (sem dados)\n");
        } else {
            sb.append(String.format("  %-12s %-16s %-12s %-6s %12s %12s %10s %12s%n",
                    "NÚMERO", "GRANDEZA", "POSTO", "UNID", "LEIT.ANT.", "LEIT.AT.", "CONSTANTE", "CONSUMO"));
            sb.append("  " + "-".repeat(100) + "\n");
            for (DadosMedidor m : medidores) {
                sb.append(String.format("  %-12s %-16s %-12s %-6s %12s %12s %10s %12s%n",
                        nvl(m.getNumero(), ""),
                        nvl(m.getGrandeza(), ""),
                        nvl(m.getPostoHorario(), ""),
                        nvl(m.getUnidade(), ""),
                        fmt(m.getLeituraAnterior()),
                        fmt(m.getLeituraAtual()),
                        fmt(m.getConstante()),
                        fmt(m.getConsumo())));
            }
        }

        sep(sb, "10. HISTÓRICO DE CONSUMO");
        if (historicoConsumo.isEmpty()) {
            sb.append("  (sem dados)\n");
        } else {
            sb.append(String.format("  %-8s %10s %8s%n", "MÊS", "kWh", "DIAS"));
            sb.append("  " + "-".repeat(28) + "\n");
            for (HistoricoMes h : historicoConsumo) {
                sb.append(String.format("  %-8s %10s %8s%n",
                        h.getMes(),
                        h.getConsumoKwh() != null ? h.getConsumoKwh() : "-",
                        h.getNumeroDias()  != null ? h.getNumeroDias()  : "-"));
            }
        }

        if (!grandezasContratadas.isEmpty()) {
            sep(sb, "11. GRANDEZAS CONTRATADAS");
            grandezasContratadas.forEach((k, v) -> field(sb, k, v));
        }

        if (saldoGd != null) {
            sep(sb, "12. SALDO DE GERAÇÃO DISTRIBUÍDA (GD)");
            field(sb, "Saldo de Créditos (kWh)", saldoGd.getSaldoCreditos());
            field(sb, "Energia Alocada no Ciclo (kWh)", saldoGd.getEnergiaAlocada());
        }

        if (!demonstrativo.isEmpty()) {
            sep(sb, "13. DEMONSTRATIVO DE CONSUMO (2ª FOLHA)");
            sb.append(String.format("  %-44s %12s %12s %10s %8s %14s%n",
                    "GRANDEZA", "LEIT.INI", "LEIT.FIM", "CONST.", "AJUSTE", "CONSUMO/DEM."));
            sb.append("  " + "-".repeat(104) + "\n");
            for (DemonstrativoLinha dl : demonstrativo) {
                sb.append(String.format("  %-44s %12s %12s %10s %8s %14s%n",
                        dl.getGrandeza(),
                        fmt(dl.getLeituraInicial()),
                        fmt(dl.getLeituraFinal()),
                        fmt(dl.getConstante()),
                        fmt(dl.getAjuste()),
                        fmt(dl.getConsumoDemanda())));
            }
            if (fatorCargaPonta != null)
                field(sb, "Fator de Carga Na Ponta", fatorCargaPonta);
            if (fatorCargaForaPonta != null)
                field(sb, "Fator de Carga Fora de Ponta", fatorCargaForaPonta);
        }

        sb.append("=".repeat(60)).append("\n");
        return sb.toString();
    }

    private void sep(StringBuilder sb, String titulo) {
        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("  ").append(titulo).append("\n");
        sb.append("=".repeat(60)).append("\n");
    }

    private void field(StringBuilder sb, String label, Object value) {
        if (value != null && !value.toString().isBlank())
            sb.append("  ").append(label).append(": ").append(value).append("\n");
    }

    private void tributoRow(StringBuilder sb, String nome, Tributo t) {
        if (t == null) return;
        sb.append(String.format("  %-8s  base=%-12s  alíq=%-8s  valor=%s%n",
                nome, fmt(t.getBaseCalculo()), fmt(t.getAliquota()), fmt(t.getValor())));
    }

    private String fmt(Double v)   { return v != null ? String.format("%.2f", v) : "-"; }
    private String nvl(String s, String def) { return s != null ? s : def; }
    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (p != null && !p.isBlank()) { if (sb.length() > 0) sb.append(sep); sb.append(p); }
        return sb.toString().isBlank() ? null : sb.toString();
    }
}
