package br.com.agente.energia;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renderiza cada página do PDF como imagem e usa ZXing para decodificar
 * o código de barras visualmente. Suporta ITF (boleto bancário FEBRABAN),
 * Code 128, Code 39 e demais formatos reconhecidos pela biblioteca.
 */
public class LeitorCodigoBarrasImagem {

    private static final int DPI = 300;

    private static final List<BarcodeFormat> FORMATOS = Arrays.asList(
            BarcodeFormat.ITF,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.EAN_13,
            BarcodeFormat.PDF_417,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.QR_CODE
    );

    public String lerCodigoBarras(BufferedImage imagem) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, FORMATOS);
        LuminanceSource source = new BufferedImageLuminanceSource(imagem);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return new MultiFormatReader().decode(bitmap, hints).getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    public String lerCodigoBarras(String caminhoPdf) throws IOException {
        File arquivo = new File(caminhoPdf);
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, FORMATOS);

        MultiFormatReader reader = new MultiFormatReader();

        try (PDDocument doc = Loader.loadPDF(arquivo)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int pagina = 0; pagina < doc.getNumberOfPages(); pagina++) {
                String resultado = tentarDecodificar(renderer, pagina, hints, reader);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    private String tentarDecodificar(PDFRenderer renderer, int pagina,
                                     Map<DecodeHintType, Object> hints,
                                     MultiFormatReader reader) throws IOException {
        BufferedImage imagem = renderer.renderImageWithDPI(pagina, DPI);
        LuminanceSource source = new BufferedImageLuminanceSource(imagem);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return reader.decode(bitmap, hints).getText();
        } catch (NotFoundException e) {
            return null;
        }
    }
}
