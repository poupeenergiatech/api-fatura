package br.com.agente.energia.model;

public class HistoricoMes {
    private String mes;        // ex: "ABR26"
    private Integer consumoKwh;
    private Integer numeroDias;

    public HistoricoMes(String mes, Integer consumoKwh, Integer numeroDias) {
        this.mes        = mes;
        this.consumoKwh = consumoKwh;
        this.numeroDias = numeroDias;
    }

    public String getMes()               { return mes; }
    public Integer getConsumoKwh()       { return consumoKwh; }
    public Integer getNumeroDias()       { return numeroDias; }
}
