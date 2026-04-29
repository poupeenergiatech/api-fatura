package br.com.agente.energia.model;

public class DadosTarifarios {
    private String classificacao;         // ex: "B1 RESIDENCIAL"
    private String subclasse;             // ex: "RESIDENCIAL"
    private String tipoFornecimento;      // ex: "Conv. Monômia - Monofásico"
    private String bandeira;              // Verde | Amarela | Vermelha 1 | Vermelha 2
    private Double valorAcrescimoBandeira; // R$ cobrado quando bandeira != Verde

    public String getClassificacao()                      { return classificacao; }
    public void setClassificacao(String v)                { this.classificacao = v; }

    public String getSubclasse()                          { return subclasse; }
    public void setSubclasse(String v)                    { this.subclasse = v; }

    public String getTipoFornecimento()                   { return tipoFornecimento; }
    public void setTipoFornecimento(String v)             { this.tipoFornecimento = v; }

    public String getBandeira()                           { return bandeira; }
    public void setBandeira(String v)                     { this.bandeira = v; }

    public Double getValorAcrescimoBandeira()              { return valorAcrescimoBandeira; }
    public void setValorAcrescimoBandeira(Double v)       { this.valorAcrescimoBandeira = v; }
}
