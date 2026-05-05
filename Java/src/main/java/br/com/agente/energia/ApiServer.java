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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiServer {

    @FunctionalInterface
    interface FaturaProcessor {
        DadosFatura processar(String caminho, String senha) throws IOException;
    }

    private static final String SENHA_PADRAO = "59832";

    private final int porta;
    private final LeitorFatura leitor = new LeitorFatura();

    public ApiServer(int porta) {
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/api/fatura", ex -> processarRequisicao(ex, leitor::lerFatura));
        server.createContext("/api/health", this::handleHealth);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.printf("API iniciada → http://localhost:%d%n", porta);
        System.out.println("  POST /api/fatura   multipart/form-data: arquivo (PDF) + senha (opcional)");
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

        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        byte[] bodyBytes;
        try (InputStream in = ex.getRequestBody()) {
            bodyBytes = in.readAllBytes();
        }

        if (bodyBytes.length == 0) {
            responder(ex, 400, "{\"erro\":\"Corpo vazio. Envie o PDF via multipart/form-data.\"}");
            return;
        }

        byte[] pdfBytes;
        String senha = SENHA_PADRAO;

        if (contentType != null && contentType.contains("multipart/form-data")) {
            String boundary = extrairBoundary(contentType);
            if (boundary == null) {
                responder(ex, 400, "{\"erro\":\"Boundary ausente no Content-Type multipart.\"}");
                return;
            }
            Map<String, byte[]> partes = parseMultipart(bodyBytes, boundary);
            pdfBytes = partes.get("arquivo");
            if (partes.containsKey("senha")) {
                String s = new String(partes.get("senha"), StandardCharsets.UTF_8).strip();
                if (!s.isEmpty()) senha = s;
            }
            if (pdfBytes == null || pdfBytes.length == 0) {
                responder(ex, 400, "{\"erro\":\"Campo 'arquivo' ausente ou vazio.\"}");
                return;
            }
        } else {
            // compatibilidade: body com bytes brutos do PDF (usa senha padrão)
            pdfBytes = bodyBytes;
        }

        Path tmp = Files.createTempFile("fatura_", ".pdf");
        try {
            Files.write(tmp, pdfBytes);
            final String senhaFinal = senha;
            DadosFatura dados = proc.processar(tmp.toString(), senhaFinal);
            responder(ex, 200, FaturaJson.toJson(dados));
        } catch (IllegalArgumentException e) {
            responder(ex, 400, json("erro", e.getMessage()));
        } catch (Exception e) {
            responder(ex, 500, json("erro", e.getMessage()));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private String extrairBoundary(String contentType) {
        for (String parte : contentType.split(";")) {
            parte = parte.strip();
            if (parte.startsWith("boundary=")) {
                return parte.substring("boundary=".length()).replace("\"", "");
            }
        }
        return null;
    }

    private Map<String, byte[]> parseMultipart(byte[] body, String boundary) {
        Map<String, byte[]> resultado = new LinkedHashMap<>();
        byte[] startBound = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] sep        = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] CRLFCRLF   = {'\r', '\n', '\r', '\n'};

        int pos = indexOf(body, startBound, 0, body.length);
        if (pos < 0) return resultado;
        pos += startBound.length;

        while (pos < body.length) {
            if (pos + 1 < body.length && body[pos] == '-' && body[pos + 1] == '-') break;
            if (pos + 1 < body.length && body[pos] == '\r' && body[pos + 1] == '\n') pos += 2;

            int proximoBound = indexOf(body, sep, pos, body.length);
            if (proximoBound < 0) proximoBound = body.length;

            int fimHeaders = indexOf(body, CRLFCRLF, pos, proximoBound);
            if (fimHeaders < 0) { pos = proximoBound + sep.length; continue; }

            String headers = new String(body, pos, fimHeaders - pos, StandardCharsets.UTF_8);
            int inicioConteudo = fimHeaders + 4;

            String nome = extrairNomeCampo(headers);
            if (nome != null) {
                resultado.put(nome, Arrays.copyOfRange(body, inicioConteudo, proximoBound));
            }

            pos = proximoBound + sep.length;
        }
        return resultado;
    }

    private String extrairNomeCampo(String headers) {
        Matcher m = Pattern.compile("name=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(headers);
        return m.find() ? m.group(1) : null;
    }

    private int indexOf(byte[] source, byte[] target, int from, int to) {
        outer:
        for (int i = from; i <= to - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) continue outer;
            }
            return i;
        }
        return -1;
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
