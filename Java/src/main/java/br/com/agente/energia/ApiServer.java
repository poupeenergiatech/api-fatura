package br.com.agente.energia;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class ApiServer {

    @FunctionalInterface
    interface FaturaProcessor {
        DadosFatura processar(String caminho) throws IOException;
    }

    private final int porta;
    private final LeitorFatura leitor = new LeitorFatura();
    private final LeitorFaturaOcr leitorOcr = new LeitorFaturaOcr();

    public ApiServer(int porta) {
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/api/fatura",     ex -> processarRequisicao(ex, leitor::lerFatura));
        server.createContext("/api/fatura/ocr", ex -> processarRequisicao(ex, leitorOcr::lerArquivo));
        server.createContext("/api/health",     this::handleHealth);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.printf("API iniciada → http://localhost:%d%n", porta);
        System.out.println("  POST /api/fatura       (extração de texto nativa — PDFBox)");
        System.out.println("  POST /api/fatura/ocr   (extração via OCR — Tesseract 5; aceita PDF ou imagem JPEG/PNG/TIFF)");
        System.out.println("  GET  /api/health");
    }

    private void processarRequisicao(HttpExchange ex, FaturaProcessor proc) throws IOException {
        addCors(ex);

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            responder(ex, 405, "{\"erro\":\"Método não permitido. Use POST.\"}");
            return;
        }

        Path tmp = Files.createTempFile("fatura_", ".bin");
        try {
            byte[] corpo;
            try (InputStream in = ex.getRequestBody()) {
                corpo = in.readAllBytes();
            }

            if (corpo.length == 0) {
                responder(ex, 400, "{\"erro\":\"Corpo vazio. Envie o arquivo no corpo da requisição ou via multipart.\"}");
                return;
            }

            // Detecta multipart/form-data e extrai o conteúdo do arquivo
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && contentType.contains("multipart/form-data")) {
                corpo = extrairMultipart(corpo, contentType);
                if (corpo == null) {
                    responder(ex, 400, "{\"erro\":\"Não foi possível extrair o arquivo do multipart. Use campo 'arquivo'.\"}");
                    return;
                }
            }

            Files.write(tmp, corpo);

            DadosFatura dados = proc.processar(tmp.toString());
            responder(ex, 200, FaturaJson.toJson(dados));

        } catch (IllegalArgumentException e) {
            responder(ex, 400, json("erro", e.getMessage()));
        } catch (Exception e) {
            responder(ex, 500, json("erro", e.getMessage()));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        addCors(ex);
        responder(ex, 200, "{\"status\":\"ok\"}");
    }

    private void responder(HttpExchange ex, int status, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String json(String chave, String valor) {
        return "{\"" + chave + "\":\"" + (valor != null ? valor.replace("\"", "'") : "erro interno") + "\"}";
    }

    // Extrai o conteúdo binário do primeiro campo de arquivo em um multipart/form-data.
    private byte[] extrairMultipart(byte[] corpo, String contentType) {
        try {
            String boundary = null;
            for (String part : contentType.split(";")) {
                part = part.strip();
                if (part.startsWith("boundary=")) {
                    boundary = part.substring("boundary=".length()).strip().replace("\"", "");
                    break;
                }
            }
            if (boundary == null) return null;

            byte[] delim = ("--" + boundary).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] crlf  = "\r\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            byte[] hdrEnd = "\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

            int pos = indexOf(corpo, delim, 0);
            while (pos >= 0) {
                pos += delim.length;
                if (pos + 2 <= corpo.length && corpo[pos] == '-' && corpo[pos + 1] == '-') break;
                if (pos + crlf.length <= corpo.length) pos += crlf.length; // pula CRLF após boundary
                int bodyStart = indexOf(corpo, hdrEnd, pos);
                if (bodyStart < 0) break;
                bodyStart += hdrEnd.length;
                int nextBound = indexOf(corpo, delim, bodyStart);
                if (nextBound < 0) break;
                // Remove CRLF antes do próximo boundary
                int bodyEnd = nextBound - crlf.length;
                if (bodyEnd > bodyStart) {
                    byte[] resultado = new byte[bodyEnd - bodyStart];
                    System.arraycopy(corpo, bodyStart, resultado, 0, resultado.length);
                    return resultado;
                }
                pos = nextBound;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int indexOf(byte[] data, byte[] pattern, int from) {
        outer: for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
