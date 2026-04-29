package br.com.agente.energia;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

        BufferedImage imagemProcessada = preprocessar(imagem);

        Tesseract tesseract = criarTesseract();
        String texto;
        try {
            texto = tesseract.doOCR(imagemProcessada);
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

    // Converte para escala de cinza e escala para no mínimo 2000px de largura.
    // Imagens coloridas e de baixa resolução degradam muito a precisão do Tesseract.
    private BufferedImage preprocessar(BufferedImage original) {
        BufferedImage cinza = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = cinza.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        if (cinza.getWidth() >= 2000) return cinza;

        double fator = 2000.0 / cinza.getWidth();
        int novaLargura = (int) (cinza.getWidth() * fator);
        int novaAltura  = (int) (cinza.getHeight() * fator);
        BufferedImage escalada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = escalada.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(cinza, 0, 0, novaLargura, novaAltura, null);
        g2.dispose();
        return escalada;
    }

    private Tesseract criarTesseract() {
        Tesseract t = new Tesseract();
        t.setDatapath(TESSDATA);
        t.setLanguage("por+eng");
        t.setPageSegMode(6);  // PSM_SINGLE_BLOCK — extrai mais texto de faturas com layout tabular
        t.setOcrEngineMode(1); // OEM_LSTM_ONLY
        return t;
    }
}
