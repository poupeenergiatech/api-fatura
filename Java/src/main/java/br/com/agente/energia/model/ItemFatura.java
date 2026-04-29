package br.com.agente.energia.model;

public class ItemFatura {
    private String descricao;
    private String unidade;
    private Double quantidade;
    private Double precoUnitComTrib;  // Preço unitário com tributo (R$)
    private Double valor;             // Valor do item (R$)
    private Double pisCofins;         // PIS/COFINS sobre o item (R$)
    private Double baseCalcIcms;      // Base de cálculo do ICMS (R$)
    private Double aliquotaIcms;      // Alíquota ICMS (%)
    private Double valorIcms;         // Valor ICMS sobre o item (R$)
    private Double tarifaUnit;        // Tarifa unitária sem tributo (R$)

    public ItemFatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao()              { return descricao; }
    public void setDescricao(String v)        { this.descricao = v; }

    public String getUnidade()                { return unidade; }
    public void setUnidade(String v)          { this.unidade = v; }

    public Double getQuantidade()             { return quantidade; }
    public void setQuantidade(Double v)       { this.quantidade = v; }

    public Double getPrecoUnitComTrib()       { return precoUnitComTrib; }
    public void setPrecoUnitComTrib(Double v) { this.precoUnitComTrib = v; }

    public Double getValor()                  { return valor; }
    public void setValor(Double v)            { this.valor = v; }

    public Double getPisCofins()              { return pisCofins; }
    public void setPisCofins(Double v)        { this.pisCofins = v; }

    public Double getBaseCalcIcms()           { return baseCalcIcms; }
    public void setBaseCalcIcms(Double v)     { this.baseCalcIcms = v; }

    public Double getAliquotaIcms()           { return aliquotaIcms; }
    public void setAliquotaIcms(Double v)     { this.aliquotaIcms = v; }

    public Double getValorIcms()              { return valorIcms; }
    public void setValorIcms(Double v)        { this.valorIcms = v; }

    public Double getTarifaUnit()             { return tarifaUnit; }
    public void setTarifaUnit(Double v)       { this.tarifaUnit = v; }
}
