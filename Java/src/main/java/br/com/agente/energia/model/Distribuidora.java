package br.com.agente.energia.model;

public class Distribuidora {
    private String nome;
    private String endereco;
    private String cnpj;
    private String inscricaoEstadual;

    public String getNome()                   { return nome; }
    public void setNome(String v)             { this.nome = v; }

    public String getEndereco()               { return endereco; }
    public void setEndereco(String v)         { this.endereco = v; }

    public String getCnpj()                   { return cnpj; }
    public void setCnpj(String v)             { this.cnpj = v; }

    public String getInscricaoEstadual()      { return inscricaoEstadual; }
    public void setInscricaoEstadual(String v){ this.inscricaoEstadual = v; }
}
