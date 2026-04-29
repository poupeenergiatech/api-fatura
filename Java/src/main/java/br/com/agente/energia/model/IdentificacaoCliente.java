package br.com.agente.energia.model;

public class IdentificacaoCliente {
    private String nome;
    private String cnpj;
    private String codigoCliente;
    private String numeroInstalacao;
    private String logradouro;
    private String bairro;
    private String cep;
    private String cidade;
    private String uf;

    public String getNome()                  { return nome; }
    public void setNome(String v)            { this.nome = v; }

    public String getCnpj()                  { return cnpj; }
    public void setCnpj(String v)            { this.cnpj = v; }

    public String getCodigoCliente()         { return codigoCliente; }
    public void setCodigoCliente(String v)   { this.codigoCliente = v; }

    public String getNumeroInstalacao()      { return numeroInstalacao; }
    public void setNumeroInstalacao(String v){ this.numeroInstalacao = v; }

    public String getLogradouro()            { return logradouro; }
    public void setLogradouro(String v)      { this.logradouro = v; }

    public String getBairro()                { return bairro; }
    public void setBairro(String v)          { this.bairro = v; }

    public String getCep()                   { return cep; }
    public void setCep(String v)             { this.cep = v; }

    public String getCidade()                { return cidade; }
    public void setCidade(String v)          { this.cidade = v; }

    public String getUf()                    { return uf; }
    public void setUf(String v)              { this.uf = v; }
}
