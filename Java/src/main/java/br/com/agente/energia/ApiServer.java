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
        server.createContext("/api/fatura/ocr", ex -> processarRequisicao(ex, leitorOcr::lerFatura));
        server.createContext("/api/health",     this::handleHealth);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.printf("API iniciada → http://localhost:%d%n", porta);
        System.out.println("  POST /api/fatura       (extração de texto nativa — PDFBox)");
        System.out.println("  POST /api/fatura/ocr   (extração via OCR — Tesseract 5)");
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

        Path tmp = Files.createTempFile("fatura_", ".pdf");
        try {
            try (InputStream in = ex.getRequestBody()) {
                Files.write(tmp, in.readAllBytes());
            }

            if (Files.size(tmp) == 0) {
                responder(ex, 400, "{\"erro\":\"Corpo vazio. Envie o PDF em bytes no corpo da requisição.\"}");
                return;
            }

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
}
