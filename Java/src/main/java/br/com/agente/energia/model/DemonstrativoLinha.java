package br.com.agente.energia.model;

public class DemonstrativoLinha {
    private String  grandeza;
    private Double  leituraInicial;
    private Double  leituraFinal;
    private Double  constante;
    private Double  ajuste;
    private Double  consumoDemanda;

    public DemonstrativoLinha(String grandeza, Double leituraInicial, Double leituraFinal,
                              Double constante, Double ajuste, Double consumoDemanda) {
        this.grandeza       = grandeza;
        this.leituraInicial = leituraInicial;
        this.leituraFinal   = leituraFinal;
        this.constante      = constante;
        this.ajuste         = ajuste;
        this.consumoDemanda = consumoDemanda;
    }

    public String  getGrandeza()       { return grandeza; }
    public Double  getLeituraInicial() { return leituraInicial; }
    public Double  getLeituraFinal()   { return leituraFinal; }
    public Double  getConstante()      { return constante; }
    public Double  getAjuste()         { return ajuste; }
    public Double  getConsumoDemanda() { return consumoDemanda; }
}
