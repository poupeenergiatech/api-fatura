package br.com.agente.energia;

import br.com.agente.energia.model.ItemFatura;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LeitorFaturaTest {

    private static final LeitorFatura LEITOR = new LeitorFatura();

    /**
     * Simula o texto extraído pelo PDFBox de uma fatura real (330875247289.pdf)
     * que contém o item Comp.DIC com valor negativo (crédito ao consumidor).
     */
    private static final String TEXTO_FATURA_COM_DIC = """
            COMPANHIA ENERGÉTICA DO RIO GRANDE DO NORTE
            RUA MERMOZ, 150, BALDO, NATAL, RIO GRANDE DO NORTE CEP 59025-250
            CNPJ 08.324.196/0001-81 INSCRIÇÃO ESTADUAL 20055199-0
            NOTA FISCAL N° 158431874 - SÉRIE 000 / DATA DE EMISSÃO: 06/05/2026
            chave de acesso:
            2426 0508 3241 9600 0181 6600 0158 4318 7410 6025 5822
            Protocolo de autorização: 3242600007048415 - 06/05/2026 às 12:02:02
            NOME DO CLIENTE:
            ASSOCIACAO POUPE ENERGIA
            CNPJ: 59.832.***/****-** Inscrição Estadual: 20685****
            CÓDIGO DA INSTALAÇÃO
            885544
            ENDEREÇO:
            RUA DO MALHADO 311
            CJ PQ DAS DUNAS
            PAJUCARA/AREA URBANA
            59132-300 NATAL RN
            REF:MÊS/ANO
            03/2026
            TOTAL A PAGAR R$
            228,46
            VENCIMENTO
            21/05/2026
            CLASSIFICAÇÃO: B1 RESIDENCIAL -RESIDENCIAL
            TIPO DE FORNECIMENTO: Conv. Monômia - Monofásico
            LEITURA ANTERIOR 26/02/2026 LEITURA ATUAL 26/03/2026 N° DE DIAS 28 PRÓXIMA LEITURA 27/04/2026
            Consumo-TUSD              kWh     464,00     0,58308173     270,54      15,70     270,54     20,00      54,10     0,43260000
            Consumo-TE                kWh     464,00     0,42004528     194,90      11,31     194,90     20,00      38,98     0,31164000
            Ilum. Púb. Municipal  51,79
            Multa-NF 154568812    4,54
            Juros-NF 154568812    0,52
            IPCA-NF-152715408    0,09
            IPCA-NF-154568812    0,90
            Comp.DIC Mês 01/26    41,93-
            PIS     170,04     1,29     2,19
            COFINS  170,04     5,97    10,15
            ICMS    212,55    20,00    42,51
            2212814221  Energia Ativa  Único  15.486,00  15.950,00  1,00000  464,00
            MAR26 464 28
            FEV26 440 29
            838000000025 284600384075 029502280202 026094878033
            CÓDIGO DO CLIENTE
            7029502280
            """;

    @Test
    void detectaCompDicComoItemNegativo() {
        DadosFatura d = LEITOR.lerTexto(TEXTO_FATURA_COM_DIC);

        Optional<ItemFatura> compDic = d.getItensFatura().stream()
                .filter(it -> it.getDescricao().toUpperCase().startsWith("COMP.DIC"))
                .findFirst();

        assertTrue(compDic.isPresent(), "Item Comp.DIC deve estar presente em itens_fatura");
        assertEquals(-41.93, compDic.get().getValor(), 0.001,
                "Valor do Comp.DIC deve ser negativo (-41,93)");
    }

    @Test
    void armazenaCompDicEmTributos() {
        DadosFatura d = LEITOR.lerTexto(TEXTO_FATURA_COM_DIC);

        assertNotNull(d.getTributos().getCompDic(), "compDic não deve ser null em tributos");
        assertEquals(-41.93, d.getTributos().getCompDic(), 0.001,
                "tributos.compDic deve ser -41,93");
    }

    @Test
    void naoAdicionaCompDicQuandoAusente() {
        String texto = """
                Consumo-TUSD  kWh  100,00  0,50000000  50,00  2,00  50,00  20,00  10,00  0,40000000
                Ilum. Púb. Municipal  10,00
                """;

        DadosFatura d = LEITOR.lerTexto(texto);

        boolean temCompDic = d.getItensFatura().stream()
                .anyMatch(it -> it.getDescricao().toUpperCase().startsWith("COMP.DIC"));

        assertFalse(temCompDic, "Comp.DIC não deve aparecer quando ausente da fatura");
        assertNull(d.getTributos().getCompDic(), "compDic deve ser null quando item não existe");
    }

    @Test
    void jsonContemCompDic() {
        DadosFatura d = LEITOR.lerTexto(TEXTO_FATURA_COM_DIC);
        String json = FaturaJson.toJson(d);

        assertTrue(json.contains("\"comp_dic\""), "JSON deve conter chave comp_dic");
        assertTrue(json.contains("-41.93"), "JSON deve conter valor -41.93");
    }

    @Test
    void itensConvencionaisContinuamFuncionando() {
        DadosFatura d = LEITOR.lerTexto(TEXTO_FATURA_COM_DIC);
        List<ItemFatura> itens = d.getItensFatura();

        Optional<ItemFatura> consumoTusd = itens.stream()
                .filter(it -> "Consumo-TUSD".equals(it.getDescricao()))
                .findFirst();
        assertTrue(consumoTusd.isPresent(), "Consumo-TUSD deve ser detectado");
        assertEquals(270.54, consumoTusd.get().getValor(), 0.001);

        assertTrue(d.getTributos().getCosip() != null && d.getTributos().getCosip() > 0,
                "COSIP deve ser detectado");
        assertTrue(d.getTributos().getMulta() != null && d.getTributos().getMulta() > 0,
                "Multa deve ser detectada");
        assertTrue(d.getTributos().getJuros() != null && d.getTributos().getJuros() > 0,
                "Juros deve ser detectado");
    }
}
