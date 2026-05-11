package br.com.agente.energia.model;

public class DadosTributos {
    private Tributo pis;
    private Tributo cofins;
    private Tributo icms;
    private Double  cosip;    // Contribuição de Iluminação Pública
    private Double  multa;
    private Double  juros;
    private Double  ipca;
    private Double  compDic;  // Compensação por violação de DIC (valor negativo = crédito ao consumidor)

    public Tributo getPis()              { return pis; }
    public void setPis(Tributo v)        { this.pis = v; }

    public Tributo getCofins()           { return cofins; }
    public void setCofins(Tributo v)     { this.cofins = v; }

    public Tributo getIcms()             { return icms; }
    public void setIcms(Tributo v)       { this.icms = v; }

    public Double getCosip()             { return cosip; }
    public void setCosip(Double v)       { this.cosip = v; }

    public Double getMulta()             { return multa; }
    public void setMulta(Double v)       { this.multa = v; }

    public Double getJuros()             { return juros; }
    public void setJuros(Double v)       { this.juros = v; }

    public Double getIpca()              { return ipca; }
    public void setIpca(Double v)        { this.ipca = v; }

    public Double getCompDic()           { return compDic; }
    public void setCompDic(Double v)     { this.compDic = v; }
}
