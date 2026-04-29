package br.com.agente.energia;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Alternativa de extração via OCR (Tesseract 5 / Tess4J).
 * Útil para PDFs baseados em imagem ou escaneados onde a extração
 * de texto nativa do PDFBox retorna resultado vazio ou incompleto.
 */
public class LeitorFaturaOcr {

    private static final String TESSDATA = "/usr/share/tesseract-ocr/5/tessdata";
    private static final int DPI = 300;

    private final LeitorFatura leitor = new LeitorFatura();

    public DadosFatura lerFatura(String caminhoPdf) throws IOException {
        String texto = extrairTextoOcr(caminhoPdf);
        DadosFatura dados = leitor.lerTexto(texto);

        // Tenta código de barras via imagem se o OCR não encontrou na linha digitável
        if (dados.getResumoPagamento().getCodigoBarras() == null) {
            String codigoImagem = new LeitorCodigoBarrasImagem().lerCodigoBarras(caminhoPdf);
            if (codigoImagem != null) {
                dados.getResumoPagamento().setCodigoBarras(codigoImagem);
            }
        }

        return dados;
    }

    private String extrairTextoOcr(String caminhoPdf) throws IOException {
        Tesseract tesseract = criarTesseract();
        StringBuilder sb = new StringBuilder();

        try (PDDocument doc = Loader.loadPDF(new File(caminhoPdf))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int pagina = 0; pagina < doc.getNumberOfPages(); pagina++) {
                BufferedImage imagem = renderer.renderImageWithDPI(pagina, DPI);
                try {
                    sb.append(tesseract.doOCR(imagem)).append("\n");
                } catch (TesseractException e) {
                    // página sem texto reconhecível, continua
                }
            }
        }

        return sb.toString();
    }

    private Tesseract criarTesseract() {
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA);
        t.setLanguage("por+eng");
        t.setPageSegMode(3);  // PSM_AUTO
        t.setOcrEngineMode(1); // OEM_LSTM_ONLY
        return t;
    }
}
