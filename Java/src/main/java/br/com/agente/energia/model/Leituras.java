package br.com.agente.energia.model;

public class Leituras {
    private String dataAnterior;
    private String dataAtual;
    private String dataProxima;
    private Integer numeroDias;

    public String getDataAnterior()        { return dataAnterior; }
    public void setDataAnterior(String v)  { this.dataAnterior = v; }

    public String getDataAtual()           { return dataAtual; }
    public void setDataAtual(String v)     { this.dataAtual = v; }

    public String getDataProxima()         { return dataProxima; }
    public void setDataProxima(String v)   { this.dataProxima = v; }

    public Integer getNumeroDias()         { return numeroDias; }
    public void setNumeroDias(Integer v)   { this.numeroDias = v; }
}
