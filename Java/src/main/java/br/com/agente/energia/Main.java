package br.com.agente.energia;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        if (args[0].equals("--server")) {
            int porta = args.length >= 2 ? Integer.parseInt(args[1]) : 8080;
            try {
                new ApiServer(porta).iniciar();
            } catch (IOException e) {
                System.err.println("Erro ao iniciar servidor: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        String caminhoPdf = args[0];

        if (args.length >= 2 && args[1].equals("--dump")) {
            try {
                org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.Loader.loadPDF(new java.io.File(caminhoPdf));
                String text = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
                doc.close();
                String[] lines = text.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    if (!lines[i].strip().isEmpty())
                        System.out.printf("%4d: %s%n", i, lines[i]);
                }
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        LeitorFatura leitor = new LeitorFatura();
        try {
            DadosFatura dados = leitor.lerFatura(caminhoPdf);
            System.out.println(dados);
            String destino = args.length >= 2 ? args[1]
                    : caminhoPdf.replaceAll("(?i)\\.pdf$", "_resultado.json");
            FaturaJson.exportar(dados, destino);
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Erro ao processar fatura: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  Servidor:  java -jar leitor-fatura-energia.jar --server [porta]");
        System.out.println("  CLI:       java -jar leitor-fatura-energia.jar <fatura.pdf> [saida.json]");
        System.out.println("  Dump:      java -jar leitor-fatura-energia.jar <fatura.pdf> --dump");
    }
}
