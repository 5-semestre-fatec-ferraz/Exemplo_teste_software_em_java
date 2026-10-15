# Relatório de Testes de Penetração Automatizados com BDD (Gherkin + Playwright + JUnit 5)

### 1. Matriz de Cobertura de Ataques e Cenários Implementados

| Vetor de Ataque / Pentest | Classificação | Cenário BDD Implementado | Status na Aplicação Atual & Valor Pedagógico |
| :--- | :--- | :--- | :--- |
| **Login Legítimo & Cookie Ativo** | RF014 / Funcional | `Autenticação legítima com credenciais válidas e verificação de sessão ativa` | **Aprovado** (Valida acesso e cookie `JSESSIONID`). |
| **Logout & Invalidação** | [CWE-613](https://cwe.mitre.org/data/definitions/613.html) | `Encerramento de sessão (Logout) e invalidação de credencial ativa` | **Aprovado** (`session.invalidate()`). |
| **Redirecionamento Pretendido** | Usabilidade / Auth | `Redirecionamento após login para a URL pretendida original` | **Aprovado** (Redirecionamento interceptado para `/MRYnZpAsC9sp`). |
| **Enumeração de Usuários** | [CWE-204](https://cwe.mitre.org/data/definitions/204.html) | Resposta idêntica para senha incorreta e e-mail inexistente | **Aprovado** (Mensagem unificada: *"E-mail ou senha inválidos."*). |
| **Timing Attacks (Temporização)** | [CWE-208](https://cwe.mitre.org/data/definitions/208.html) | `Prevenção de Timing Attack via equalização criptográfica (Dummy Hash)` | **Falha Pedagógica** (Evidencia a necessidade de executar *dummy hash* quando o usuário não existe). |
| **Força Bruta (Brute Force)** | [CWE-307](https://cwe.mitre.org/data/definitions/307.html) | Bloqueio temporário da conta após 5 tentativas consecutivas | **Falha Pedagógica** (Evidencia a falta de bloqueio de conta ou Rate Limiting). |
| **Credential Stuffing / Spraying** | [CWE-307](https://cwe.mitre.org/data/definitions/307.html) | Limitação de taxa de requisições por IP (HTTP 429) em rajada | **Falha Pedagógica** (Evidencia a falta de rate limiting por IP com Bucket4j/Filter). |
| **SQL Injection (E-mail & Senha)** | [CWE-89](https://cwe.mitre.org/data/definitions/89.html) | Tautologia (`' OR '1'='1`) e Comentário SQL (`admin' --`) | **Aprovado** (Prepared Statements e DataMapper bloqueiam o bypass). |
| **XSS Refletido no Login** | [CWE-79](https://cwe.mitre.org/data/definitions/79.html) | Injeção de tags `<script>` e handlers de evento | **Aprovado** (Thymeleaf e sanitização impedem execução de scripts). |
| **Hash Algorithmic DoS** | [CWE-400](https://cwe.mitre.org/data/definitions/400.html) | Rejeição antecipada de senhas com mais de 128 caracteres | **Falha Pedagógica** (Evidencia que a CPU do servidor calcula hash em strings arbitrariamente longas). |
| **Fixação de Sessão (Session Fixation)** | [CWE-384](https://cwe.mitre.org/data/definitions/384.html) | Renovação obrigatória do `JSESSIONID` pós-login | **Falha Pedagógica** (Evidencia que o mesmo Session ID anônimo é mantido pós-autenticação; requer `request.changeSessionId()`). |
| **Flags de Cookies de Sessão** | [CWE-1004](https://cwe.mitre.org/data/definitions/1004.html) / [CWE-614](https://cwe.mitre.org/data/definitions/614.html) | Validação das flags `HttpOnly` e `SameSite=Lax/Strict` | **Falha Pedagógica** (Orienta a configuração de cookies seguros no Servlet Container). |
| **Open Redirect / Phishing** | [CWE-601](https://cwe.mitre.org/data/definitions/601.html) | Bloqueio de URLs externas maliciosas no parâmetro `?redirect=` | **Falha Pedagógica** (Evidencia que redirecionamentos arbitrários precisam ser restritos ao domínio local). |
| **Contas Inativas / Suspensas** | OWASP ASVS V2.1 | Bloqueio de login para contas com `status = false` | **Falha Pedagógica** (Evidencia que [LoginController](file:///var/www/html/ProjetoIntegrador.GWJ.JAVA.Spring.Boot.dinamico/src/main/java/com/gwj/controller/LoginController.java#L122-L142) não valida o status do usuário). |
| **CSRF no Formulário de Login** | [CWE-352](https://cwe.mitre.org/data/definitions/352.html) | Rejeição (HTTP 403) de submissão POST direta sem token | **Falha Pedagógica** (Evidencia a ausência de proteção CSRF no endpoint de login). |
| **HTTP Verb Tampering** | [CWE-650](https://cwe.mitre.org/data/definitions/650.html) | Bloqueio de credenciais submetidas via método GET na URL | **Aprovado** (GET apenas renderiza o formulário, sem autenticar). |
| **Cache de Credenciais** | [CWE-525](https://cwe.mitre.org/data/definitions/525.html) | Cabeçalho `Cache-Control: no-store, no-cache` na resposta | **Falha Pedagógica** (Orienta a inclusão de cabeçalhos de segurança contra proxy cache). |
| **Validação de Campos Vazios e Espaços** | [CWE-20](https://cwe.mitre.org/data/definitions/20.html) | Submissão com campos vazios ou apenas espaços em branco | **Aprovado** (Validação client-side e server-side barram o envio). |

---

### 2. Implementação dos Steps no Playwright

Todos os passos foram implementados em [LoginSteps.java](file:///var/www/html/ProjetoIntegrador.GWJ.JAVA.Spring.Boot.dinamico/src/test/java/com/fatec/login/steps/LoginSteps.java) usando Playwright e asserções explicativas com os códigos CWE e recomendações de código para os alunos.

Por exemplo, a asserção de Session Fixation acusa:
```text
org.opentest4j.AssertionFailedError: FALHA DE SESSION FIXATION (CWE-384 / OWASP ASVS V3.2): 
O JSESSIONID '4A5E00FC821A30173045C1542A735013' permaneceu idêntico após a autenticação bem-sucedida! 
A aplicação deve renovar a sessão com request.changeSessionId().
```

---

### 3. Como Executar os Testes

- **Executar todos os testes:**
  ```bash
  mvn test -Dtest=CucumberTestRunner
  ```

- **Executar apenas testes funcionais:**
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@funcional"
  ```

- **Executar testes de Injeção e Enumeração:**
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@sql_injection or @anti_enumeracao or @xss"
  ```

- **Executar testes de Força Bruta e DoS:**
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@brute_force or @anti_dos"
  ```

- **Executar testes de Gestão de Sessão (Fixação e Cookies):**
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@session_fixation or @cookie_security"
  ```

- **Visualizar o relatório HTML detalhado:**
  Abra no navegador o arquivo:
  `target/cucumber-reports/cucumber.html`

O documento [tutorial-teste-Gherkin.md](file:///var/www/html/ProjetoIntegrador.GWJ.JAVA.Spring.Boot.dinamico/tutorial-teste-Gherkin.md) também foi atualizado com a matriz completa de ataques, explicações detalhadas de cada CWE e o guia de correção para fins acadêmicos.