# Deploy no EasyPanel

## Pré-requisito

Repositório do projeto conectado ao EasyPanel via GitHub.

---

## 1. Criar o serviço

No painel do EasyPanel:

1. Abra o projeto desejado
2. Clique em **+ Create Service → App**
3. Dê o nome `leitor-fatura-energia` (ou outro de sua preferência)

---

## 2. Configurar a fonte

Na aba **Source**:

| Campo | Valor |
|---|---|
| Provider | GitHub |
| Repository | `seu-usuario/pdf-reader` |
| Branch | `main` |
| Build Method | **Dockerfile** |
| Dockerfile path | `Dockerfile` |

---

## 3. Configurar a porta

Na aba **Domains**:

- Clique em **Add Domain** para expor externamente, ou
- Use apenas **Port** `8080` para acesso interno

Na aba **General → Ports**:

| Campo | Valor |
|---|---|
| Port | `8080` |

---

## 4. Variáveis de ambiente

Na aba **Environment**, adicione se quiser sobrescrever a porta padrão:

| Chave | Valor padrão | Descrição |
|---|---|---|
| `PORT` | `8080` | Porta em que a API escuta |

Sem nenhuma configuração a API já sobe na 8080.

---

## 5. Health Check

Na aba **General → Health Check**:

| Campo | Valor |
|---|---|
| Path | `/api/health` |
| Port | `8080` |
| Interval | `30` s |

---

## 6. Deploy

Clique em **Deploy**. O EasyPanel irá:

1. Clonar o repositório
2. Executar o build multi-estágio do Dockerfile (Maven compila → JRE executa)
3. Subir o container na porta configurada

---

## Rotas disponíveis

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/health` | Verifica se a API está no ar |
| `POST` | `/api/fatura` | Envia PDF no corpo (bytes brutos) e retorna JSON estruturado |

### Exemplo de chamada

```bash
curl -X POST https://sua-url.easypanel.host/api/fatura \
  --data-binary @fatura.pdf \
  -H "Content-Type: application/octet-stream"
```

---

## Tamanho estimado da imagem

| Camada | Tamanho |
|---|---|
| JRE Alpine (runtime) | ~90 MB |
| JAR (app + dependências) | ~30 MB |
| **Total** | **~120 MB** |
