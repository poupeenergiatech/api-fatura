package br.com.agente.energia.model;

/** Saldo de Geração Distribuída — presente em faturas de usinas solares/GD. */
public class SaldoGd {
    private Double saldoCreditos;     // kWh acumulados
    private Double energiaAlocada;    // kWh alocados no ciclo

    public Double getSaldoCreditos()          { return saldoCreditos; }
    public void setSaldoCreditos(Double v)    { this.saldoCreditos = v; }

    public Double getEnergiaAlocada()         { return energiaAlocada; }
    public void setEnergiaAlocada(Double v)   { this.energiaAlocada = v; }
}
