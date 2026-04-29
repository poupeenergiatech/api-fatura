package br.com.agente.energia.model;

public class DadosNfe {
    private String numero;
    private String serie;
    private String dataEmissao;
    private String chaveAcesso;
    private String protocolo;
    private String dataProtocolo;

    public String getNumero()              { return numero; }
    public void setNumero(String v)        { this.numero = v; }

    public String getSerie()               { return serie; }
    public void setSerie(String v)         { this.serie = v; }

    public String getDataEmissao()         { return dataEmissao; }
    public void setDataEmissao(String v)   { this.dataEmissao = v; }

    public String getChaveAcesso()         { return chaveAcesso; }
    public void setChaveAcesso(String v)   { this.chaveAcesso = v; }

    public String getProtocolo()           { return protocolo; }
    public void setProtocolo(String v)     { this.protocolo = v; }

    public String getDataProtocolo()       { return dataProtocolo; }
    public void setDataProtocolo(String v) { this.dataProtocolo = v; }
}
