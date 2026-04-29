package br.com.agente.energia;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
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

    /** Auto-detecta PDF ou imagem pelos magic bytes e processa adequadamente. */
    public DadosFatura lerArquivo(String caminho) throws IOException {
        return ehPdf(caminho) ? lerFatura(caminho) : lerImagem(caminho);
    }

    public DadosFatura lerFatura(String caminhoPdf) throws IOException {
        String texto = extrairTextoOcr(caminhoPdf);
        DadosFatura dados = leitor.lerTexto(texto);

        if (dados.getResumoPagamento().getCodigoBarras() == null) {
            String codigo = new LeitorCodigoBarrasImagem().lerCodigoBarras(caminhoPdf);
            if (codigo != null) dados.getResumoPagamento().setCodigoBarras(codigo);
        }

        return dados;
    }

    public DadosFatura lerImagem(String caminhoImagem) throws IOException {
        BufferedImage imagem = ImageIO.read(new File(caminhoImagem));
        if (imagem == null)
            throw new IllegalArgumentException("Formato de imagem não suportado. Use JPEG, PNG ou TIFF.");

        Tesseract tesseract = criarTesseract();
        String texto;
        try {
            texto = tesseract.doOCR(imagem);
        } catch (TesseractException e) {
            throw new IOException("Falha no OCR da imagem: " + e.getMessage(), e);
        }

        DadosFatura dados = leitor.lerTexto(texto);

        if (dados.getResumoPagamento().getCodigoBarras() == null) {
            String codigo = new LeitorCodigoBarrasImagem().lerCodigoBarras(imagem);
            if (codigo != null) dados.getResumoPagamento().setCodigoBarras(codigo);
        }

        return dados;
    }

    private boolean ehPdf(String caminho) throws IOException {
        try (FileInputStream fis = new FileInputStream(caminho)) {
            byte[] h = fis.readNBytes(4);
            return h.length == 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
        }
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
