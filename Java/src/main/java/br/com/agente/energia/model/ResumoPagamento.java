package br.com.agente.energia.model;

public class ResumoPagamento {
    private String referencia;      // MM/AAAA
    private String dataVencimento;
    private Double valorTotal;
    private String codigoBarras;

    public String getReferencia()           { return referencia; }
    public void setReferencia(String v)     { this.referencia = v; }

    public String getDataVencimento()       { return dataVencimento; }
    public void setDataVencimento(String v) { this.dataVencimento = v; }

    public Double getValorTotal()           { return valorTotal; }
    public void setValorTotal(Double v)     { this.valorTotal = v; }

    public String getCodigoBarras()         { return codigoBarras; }
    public void setCodigoBarras(String v)   { this.codigoBarras = v; }
}
