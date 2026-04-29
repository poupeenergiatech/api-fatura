package br.com.agente.energia.model;

public class Tributo {
    private Double baseCalculo;
    private Double aliquota;
    private Double valor;

    public Tributo() {}

    public Tributo(Double baseCalculo, Double aliquota, Double valor) {
        this.baseCalculo = baseCalculo;
        this.aliquota    = aliquota;
        this.valor       = valor;
    }

    public Double getBaseCalculo()          { return baseCalculo; }
    public void setBaseCalculo(Double v)    { this.baseCalculo = v; }

    public Double getAliquota()             { return aliquota; }
    public void setAliquota(Double v)       { this.aliquota = v; }

    public Double getValor()                { return valor; }
    public void setValor(Double v)          { this.valor = v; }
}
