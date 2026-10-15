# Tutorial de Testes de Penetração Automatizados com BDD (Gherkin + Playwright + JUnit 5)

Este tutorial orienta os alunos e desenvolvedores a implementar, executar e compreender **Testes de Invasão (Pentest) e Regressão de Segurança** aplicados à interface de autenticação (`/login`) utilizando **BDD (Behavior-Driven Development)** com **Cucumber**, **Playwright** e **JUnit 5 Platform Suite**.

---

## 1. Por que este teste é importante? (Contexto Pedagógico)

O formulário de login é a principal porta de entrada da maioria das aplicações web e o alvo mais visado por invasores. Falhas nessa tela podem comprometer todo o sistema ou vazar informações confidenciais sobre a base de usuários.

Este conjunto de testes automatizados cobre **100% dos tipos de ataques conhecidos contra o login**, simulando cenários reais baseados no **OWASP Web Security Testing Guide (WSTG)** e **OWASP Application Security Verification Standard (ASVS)**:

| Vetor de Pentest | Vulnerabilidade (CWE) | Risco para a Aplicação | Comportamento Seguro Esperado |
| :--- | :--- | :--- | :--- |
| **Enumeração de Usuários** | [CWE-204](https://cwe.mitre.org/data/definitions/204.html) *(Observable Response Discrepancy)* | Mensagens de erro distintas ("Usuário não existe" vs "Senha incorreta") revelam e-mails cadastrados. | Mensagem unificada: `"E-mail ou senha inválidos."` para ambos os casos. |
| **Timing Attack (Temporização)** | [CWE-208](https://cwe.mitre.org/data/definitions/208.html) *(Observable Timing Discrepancy)* | O invasor mede o tempo de resposta: se o usuário existe, o hash consome CPU; se não existe, retorna de imediato. | Equalização via **Dummy Hash** na busca por usuário inexistente. |
| **Força Bruta (Brute Force)** | [CWE-307](https://cwe.mitre.org/data/definitions/307.html) *(Improper Restriction of Excessive Attempts)* | Tentativas contínuas e automatizadas de adivinhar a senha de um usuário específico. | Bloqueio temporário da conta ou Rate Limiting após N tentativas consecutivas inválidas. |
| **Credential Stuffing & Spraying** | [CWE-307](https://cwe.mitre.org/data/definitions/307.html) *(Excessive Authentication Attempts)* | Rajadas de requisições tentando senhas populares contra milhares de contas a partir do mesmo IP. | Limitação de taxa de requisições por IP (HTTP 429 Too Many Requests / CAPTCHA). |
| **SQL Injection (Auth Bypass)** | [CWE-89](https://cwe.mitre.org/data/definitions/89.html) *(SQL Command Neutralization)* | Payloads como `' OR '1'='1` tentam burlar a consulta SQL para logar sem senha ou causar erro interno (500). | Tratamento como credencial inválida, uso de Prepared Statements / DataMapper, retorno amigável sem vazar exceções. |
| **Cross-Site Scripting (Reflected XSS)** | [CWE-79](https://cwe.mitre.org/data/definitions/79.html) *(Improper Neutralization of Input)* | Injeção de scripts maliciosos nos campos de e-mail ou mensagens de erro refletidas na tela. | Sanitização e encoding automático no HTML/DOM, sem execução de caixas de diálogo (`alert`). |
| **Hash Algorithmic DoS** | [CWE-400](https://cwe.mitre.org/data/definitions/400.html) *(Resource Exhaustion)* | Envio de senhas gigantescas (> 128 caracteres) para esgotar a CPU do servidor no cálculo criptográfico de hash. | Validação prévia de tamanho máximo (ex: 128 caracteres) antes de acionar a CPU. |
| **Fixação de Sessão (Session Fixation)** | [CWE-384](https://cwe.mitre.org/data/definitions/384.html) *(Session Fixation)* | O invasor força um Session ID pré-autenticação para sequestrar a conta após o usuário logar. | Renovação obrigatória do `JSESSIONID` pós-login (`request.changeSessionId()`). |
| **Flags de Cookies de Sessão** | [CWE-1004](https://cwe.mitre.org/data/definitions/1004.html) / [CWE-614](https://cwe.mitre.org/data/definitions/614.html) | Ausência de flags `HttpOnly` e `SameSite` permite roubo do cookie via script ou ataque CSRF. | Cookies de autenticação com `HttpOnly=true` e `SameSite=Lax` ou `Strict`. |
| **Open Redirect / Phishing** | [CWE-601](https://cwe.mitre.org/data/definitions/601.html) *(URL Redirection to Untrusted Site)* | O parâmetro `?redirect=` encaminha o usuário pós-login para uma página maliciosa externa de phishing. | Validação estrita para aceitar apenas rotas relativas internas da aplicação local. |
| **Conta Inativa / Desativada** | [OWASP ASVS V2.1](https://owasp.org/www-project-application-security-verification-standard/) | Usuário com conta banida, inativa ou suspensa consegue autenticar-se. | Recusa imediata da autenticação de contas cujo status seja inativo/desativado. |
| **Cross-Site Request Forgery (CSRF)** | [CWE-352](https://cwe.mitre.org/data/definitions/352.html) *(Cross-Site Request Forgery)* | Submissões fraudulentas de autenticação forjadas a partir de sites de terceiros. | Exigência e validação de token anti-CSRF na submissão POST (retornando HTTP 403 se ausente/inválido). |
| **HTTP Verb Tampering** | [CWE-650](https://cwe.mitre.org/data/definitions/650.html) / [WSTG-CONF-06](https://owasp.org/www-project-web-security-testing-guide/) | Envio de credenciais sensíveis via método HTTP GET, expondo senhas na URL, logs e histórico. | Proibição de autenticação via GET; aceitação exclusiva via POST seguro. |
| **Prevenção de Cache Sensível** | [CWE-525](https://cwe.mitre.org/data/definitions/525.html) *(Use of Web Browser Cache)* | Respostas de telas de login ou autenticação armazenadas em cache por proxies ou navegadores. | Cabeçalho `Cache-Control: no-store, no-cache` obrigatório nas respostas. |
| **Bypass de Validação de Entrada** | [CWE-20](https://cwe.mitre.org/data/definitions/20.html) *(Improper Input Validation)* | Envio de campos vazios ou apenas com espaços em branco burlam o formulário no frontend. | Bloqueio na camada do cliente e do servidor, impedindo a progressão do fluxo. |
| **Gestão de Sessão & Logout** | [CWE-613](https://cwe.mitre.org/data/definitions/613.html) *(Insufficient Session Expiration)* | Sessão permanece aberta no servidor após o logout, permitindo reutilização do cookie. | Sessão destruída e usuário redirecionado imediatamente ao formulário de login. |

---

## 2. Arquitetura da Solução

A automação é dividida em três camadas desacopladas:

```
┌────────────────────────────────────────────────────────┐
│  1. Especificação de Cenários (Gherkin - .feature)    │
│     Define em português claro os vetores e critérios   │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  2. Step Definitions & Automação (Playwright Java)     │
│     Controla o navegador Chromium de forma headless    │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  3. Orquestrador & Relatórios (JUnit 5 Suite / Maven)  │
│     Executa no ciclo de build e gera relatório HTML    │
└────────────────────────────────────────────────────────┘
```

---

## 3. Pré-requisitos e Dependências

### 3.1. Versões Mínimas
- **Java**: 21 LTS
- **Maven**: 3.8+
- **Aplicação GWJ rodando** na porta `8089` (`http://localhost:8089`)

### 3.2. Dependências no `pom.xml`

Certifique-se de que o seu `pom.xml` contenha as propriedades e dependências de teste:

```xml
<properties>
    <cucumber.version>7.18.1</cucumber.version>
    <junit.jupiter.version>5.10.3</junit.jupiter.version>
    <playwright.version>1.46.0</playwright.version>
    <assertj.version>3.26.3</assertj.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-bom</artifactId>
            <version>${cucumber.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>${junit.jupiter.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Cucumber Java & JUnit Platform Engine -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 Suite Runner -->
    <dependency>
        <groupId>org.junit.platform</groupId>
        <artifactId>junit-platform-suite</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Playwright Java para automação Web -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>${playwright.version}</version>
        <scope>test</scope>
    </dependency>

    <!-- Fluent Assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>${assertj.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 4. Instalação dos Navegadores do Playwright

Como o Playwright está com escopo `<scope>test</scope>`, é **obrigatório** informar `-Dexec.classpathScope=test` ao chamar o CLI de instalação:

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install chromium"
```

---

## 5. Estrutura de Arquivos do Teste

Organize o projeto com a seguinte estrutura de diretórios:

```text
src/test/
├── java/
│   └── com/
│       └── fatec/
│           └── login/
│               ├── runner/
│               │   └── CucumberTestRunner.java  # Orquestrador JUnit 5 Suite
│               └── steps/
│                   └── LoginSteps.java          # Implementação Playwright dos passos
└── resources/
    ├── features/
    │   └── login.feature                        # Especificação BDD em Gherkin
    └── login.feature                            # Cópia raiz para consultas diretas
```

---

## 6. Como Executar os Testes

### 6.1. Passo 1: Iniciar a Aplicação
Certifique-se de que a aplicação Spring Boot está em execução na porta `8089`:
```bash
./mvnw spring-boot:run
```

### 6.2. Passo 2: Executar a Suíte Completa de Pentest
No terminal, execute o runner com:
```bash
mvn test -Dtest=CucumberTestRunner
```

### 6.3. Executar Cenários Específicos por Categoria de Ataque

Você pode rodar apenas categorias específicas utilizando filtros de tags do Cucumber:

- **Ataques de Força Bruta & Credential Stuffing**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@brute_force or @credential_stuffing"
  ```
- **Ataques de Timing & Enumeração**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@anti_enumeracao or @anti_timing_attack"
  ```
- **Ataques de Injeção (SQL Injection e XSS)**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@sql_injection or @xss"
  ```
- **Ataques de Sessão & Cookies (Session Fixation, HttpOnly, SameSite)**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@session_fixation or @cookie_security"
  ```
- **Ataques de Redirecionamento & Phishing (Open Redirect)**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@open_redirect"
  ```
- **Falsificação de Requisição (CSRF) & Cabeçalhos de Cache**:
  ```bash
  mvn test -Dtest=CucumberTestRunner -Dcucumber.filter.tags="@csrf or @cache_control"
  ```

### 6.4. Execução Visual (Modo Headed / Interface Gráfica)
Para assistir ao Playwright abrindo o Chromium e executando os ataques em tempo real:
```bash
TESTE_HEADLESS=false mvn test -Dtest=CucumberTestRunner
```

---

## 7. Valor Pedagógico das Falhas Reportadas (TDD / Guia de Correção)

Na abordagem pedagógica de **Test-Driven Development (TDD)** e Auditoria de Segurança, os testes são criados **antes** da implementação das defesas. As falhas reportadas explicitam com clareza o que falta construir no backend:

1. **Falha em `@anti_timing_attack` (CWE-208)**:
   - *Diagnóstico*: O backend retorna instantaneamente para e-mails não cadastrados, mas demora para calcular hash em usuários existentes.
   - *Correção*: Executar um cálculo de hash com senha fictícia (*dummy hash*) quando o e-mail não for encontrado.

2. **Falha em `@brute_force` (CWE-307)**:
   - *Diagnóstico*: É possível submeter centenas de tentativas inválidas sem bloqueio.
   - *Correção*: Implementar contador de tentativas inválidas em memória/Redis ou interceptor com `Bucket4j` aplicando HTTP 429.

3. **Falha em `@session_fixation` (CWE-384)**:
   - *Diagnóstico*: O cookie `JSESSIONID` permanece o mesmo antes e depois do login.
   - *Correção*: Chamar `request.changeSessionId()` no momento da autenticação bem-sucedida.

4. **Falha em `@cookie_security` (CWE-1004 / CWE-614)**:
   - *Diagnóstico*: Os cookies de sessão não possuem atributos de segurança estritos.
   - *Correção*: Configurar no `application.properties`: `server.servlet.session.cookie.http-only=true` e `server.servlet.session.cookie.same-site=lax`.

5. **Falha em `@open_redirect` (CWE-601)**:
   - *Diagnóstico*: O parâmetro `?redirect=` aceita domínios externos arbitrários.
   - *Correção*: Validar que a URL de redirecionamento inicia com `/` e não contém barras duplas (`//`) ou esquemas externos (`http:`, `https:`).

---

## 8. Relatório HTML de Execução

Ao término dos testes, o Cucumber gera automaticamente um relatório HTML interativo com o tempo de cada passo e status de aprovação.

Abra o arquivo no seu navegador:
```text
target/cucumber-reports/cucumber.html
```
