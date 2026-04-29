package br.com.agente.energia.model;

public class DadosMedidor {
    private String numero;
    private String grandeza;       // ex: "Energia Ativa", "Demanda Ativa"
    private String postoHorario;   // ex: "Único", "Ponta", "Fora Ponta"
    private String unidade;        // ex: "kWh", "kW", "kVARh"
    private Double leituraAnterior;
    private Double leituraAtual;
    private Double constante;
    private Double consumo;

    public String getNumero()                { return numero; }
    public void setNumero(String v)          { this.numero = v; }

    public String getGrandeza()              { return grandeza; }
    public void setGrandeza(String v)        { this.grandeza = v; }

    public String getPostoHorario()          { return postoHorario; }
    public void setPostoHorario(String v)    { this.postoHorario = v; }

    public String getUnidade()               { return unidade; }
    public void setUnidade(String v)         { this.unidade = v; }

    public Double getLeituraAnterior()       { return leituraAnterior; }
    public void setLeituraAnterior(Double v) { this.leituraAnterior = v; }

    public Double getLeituraAtual()          { return leituraAtual; }
    public void setLeituraAtual(Double v)    { this.leituraAtual = v; }

    public Double getConstante()             { return constante; }
    public void setConstante(Double v)       { this.constante = v; }

    public Double getConsumo()               { return consumo; }
    public void setConsumo(Double v)         { this.consumo = v; }
}
