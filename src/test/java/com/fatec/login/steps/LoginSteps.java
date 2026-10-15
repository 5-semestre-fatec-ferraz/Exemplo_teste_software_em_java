package com.fatec.login.steps;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginSteps {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    // Métricas para Timing Attacks
    private long tempoRespostaExistenteMs = 0;
    private long tempoRespostaInexistenteMs = 0;

    // Estado para Força Bruta
    private String mensagemUltimaTentativa = "";
    private boolean rateLimitAcionado = false;

    // Estado para detecção de XSS
    private boolean xssAlertDisparado = false;

    // Estado para Session Fixation
    private String preLoginSessionId = "";

    // Estado para CSRF e HTTP Verbs
    private int statusCsrfResponse = 0;
    private Map<String, String> ultimosHeadersResposta = new HashMap<>();

    @Before
    public void setup() {
        playwright = Playwright.create();
        boolean headless = !"false".equalsIgnoreCase(System.getenv("TESTE_HEADLESS"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        context = browser.newContext();
        page = context.newPage();

        // Monitora diálogos (alert, prompt, confirm) para comprovar execução de XSS
        page.onDialog(dialog -> {
            xssAlertDisparado = true;
            dialog.accept();
        });
    }

    @After
    public void teardown() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    private String getBaseUrl() {
        String baseUrl = System.getenv("TEST_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8089/login";
        }
        return baseUrl;
    }

    private String getBaseOrigin() {
        String baseUrl = getBaseUrl();
        if (baseUrl.contains("/login")) {
            return baseUrl.substring(0, baseUrl.indexOf("/login"));
        }
        return baseUrl;
    }

    private Locator getEmailInput() {
        return page.locator("input[name='email']").first();
    }

    private Locator getPasswordInput() {
        return page.locator("input[name='senha']").first();
    }

    private Locator getSubmitButton() {
        return page.locator("button[type='submit']").first();
    }

    private Locator getLoginForm() {
        return page.locator("#loginForm, form[action*='login']").first();
    }

    private com.microsoft.playwright.options.Cookie getSessionCookie() {
        return context.cookies().stream()
                .filter(c -> "JSESSIONID".equalsIgnoreCase(c.name) || "session_id".equalsIgnoreCase(c.name))
                .findFirst()
                .orElse(null);
    }

    private String getSessionCookieValue() {
        com.microsoft.playwright.options.Cookie c = getSessionCookie();
        return c != null ? c.value : "";
    }

    // =========================================================================
    // SEÇÃO 1: FLUXO LEGÍTIMO E GESTÃO DE SESSÃO BÁSICA
    // =========================================================================

    @Dado("que o usuário está na página de login")
    public void queOUsuarioEstaNaPaginaDeLogin() {
        page.navigate(getBaseUrl());
        assertThat(getEmailInput()).isVisible();
        assertThat(getPasswordInput()).isVisible();
    }

    @Quando("o usuário preenche o e-mail com {string}")
    public void oUsuarioPreencheOEmailCom(String email) {
        getEmailInput().fill(email);
        if (getEmailInput().inputValue().isEmpty() && !email.isEmpty()) {
            getEmailInput().evaluate("(el, val) => { el.value = val; el.dispatchEvent(new Event('input', {bubbles: true})); el.dispatchEvent(new Event('change', {bubbles: true})); }", email);
        }
    }

    @E("o usuário preenche a senha com {string}")
    public void oUsuarioPreencheASenhaCom(String senha) {
        getPasswordInput().fill(senha);
        if (getPasswordInput().inputValue().isEmpty() && !senha.isEmpty()) {
            getPasswordInput().evaluate("(el, val) => { el.value = val; el.dispatchEvent(new Event('input', {bubbles: true})); el.dispatchEvent(new Event('change', {bubbles: true})); }", senha);
        }
    }

    @Quando("o usuário deixa os campos de e-mail e senha vazios")
    public void oUsuarioDeixaOsCamposDeEmailESenhaVazios() {
        getEmailInput().fill("");
        getPasswordInput().fill("");
    }

    @Quando("o usuário preenche o e-mail com {string} e a senha com {string}")
    public void oUsuarioPreencheOEmailComEASenhaCom(String email, String senha) {
        getEmailInput().fill(email);
        getPasswordInput().fill(senha);
    }

    @E("clica no botão de entrar")
    public void clicaNoBotaoDeEntrar() {
        getSubmitButton().click();
        page.waitForLoadState();
    }

    @Então("o usuário deve ser autenticado com sucesso e redirecionado para a área interna")
    public void oUsuarioDeveSerAutenticadoComSucessoERedirecionadoParaAAreaInterna() {
        page.waitForCondition(() -> !page.url().contains("/login"), new Page.WaitForConditionOptions().setTimeout(10000));
        Assertions.assertFalse(page.url().contains("/login"), "O usuário deveria ter sido redirecionado para fora do /login");
    }

    @E("deve possuir um cookie de sessão ativo")
    public void devePossuirUmCookieDeSessaoAtivo() {
        com.microsoft.playwright.options.Cookie cookie = getSessionCookie();
        Assertions.assertNotNull(cookie, "O navegador deveria possuir um cookie de sessão ativo (JSESSIONID) após autenticação");
        Assertions.assertFalse(cookie.value.isBlank(), "O valor do cookie de sessão não pode ser vazio");
    }

    @Quando("o usuário clica no botão de sair")
    public void oUsuarioClicaNoBotaoDeSair() {
        Locator logoutLink = page.locator("a[href*='logout'], [data-testid='button-logout']").first();
        if (logoutLink.count() > 0 && logoutLink.isVisible()) {
            logoutLink.click();
        } else {
            page.navigate(getBaseOrigin() + "/logout");
        }
    }

    @Então("a sessão deve ser encerrada e o formulário de login reexibido")
    public void aSessaoDeveSerEncerradaEOFormularioDeLoginReexibido() {
        page.waitForCondition(() -> page.url().contains("/login"), new Page.WaitForConditionOptions().setTimeout(10000));
        Assertions.assertTrue(page.url().contains("/login"), "A URL deve ser /login após o encerramento da sessão");
        assertThat(getLoginForm()).isVisible();
    }

    @Dado("que o usuário tentou acessar a página restrita {string}")
    public void queOUsuarioTentouAcessarAPaginaRestrita(String paginaRestrita) {
        page.navigate(getBaseOrigin() + paginaRestrita);
        page.waitForCondition(() -> page.url().contains("/login"), new Page.WaitForConditionOptions().setTimeout(10000));
        assertThat(getEmailInput()).isVisible();
    }

    @Então("a resposta deve indicar o redirecionamento para a página {string}")
    public void aRespostaDeveIndicarORedirecionamentoParaAPagina(String urlEsperada) {
        page.waitForCondition(() -> page.url().contains(urlEsperada), new Page.WaitForConditionOptions().setTimeout(10000));
        Assertions.assertTrue(page.url().contains(urlEsperada),
                "A URL final deveria ser ou conter: " + urlEsperada + ", mas foi: " + page.url());
    }

    // =========================================================================
    // SEÇÃO 2: ANTI-ENUMERAÇÃO E TIMING ATTACKS
    // =========================================================================

    @Então("o sistema deve exibir a mensagem de erro {string}")
    public void oSistemaDeveExibirAMensagemDeErro(String mensagem) {
        Locator feedback = page.locator("[data-testid='feedback-message'], .custom-alert-danger, .alert-danger").first();
        assertThat(feedback).isVisible();
        assertThat(feedback).containsText(mensagem);
    }

    @E("o usuário deve permanecer na página de login")
    @E("não deve permitir acesso ao sistema")
    public void oUsuarioDevePermanecerNaPaginaDeLogin() {
        Assertions.assertTrue(page.url().contains("/login"), "O usuário deveria permanecer na rota de login");
        assertThat(getLoginForm()).isVisible();
    }

    @Quando("o usuário submete login com e-mail existente {string} e senha incorreta medindo o tempo de resposta")
    public void oUsuarioSubmeteLoginComEmailExistenteESenhaIncorretaMedindoOTempoDeResposta(String email) {
        page.navigate(getBaseUrl());
        getEmailInput().fill(email);
        getPasswordInput().fill("SenhaIncorreta@DummyTiming123");
        long inicio = System.currentTimeMillis();
        getSubmitButton().click();
        page.waitForLoadState();
        tempoRespostaExistenteMs = System.currentTimeMillis() - inicio;
    }

    @E("o usuário submete login com e-mail inexistente {string} e senha incorreta medindo o tempo de resposta")
    public void oUsuarioSubmeteLoginComEmailInexistenteESenhaIncorretaMedindoOTempoDeResposta(String email) {
        page.navigate(getBaseUrl());
        getEmailInput().fill(email);
        getPasswordInput().fill("SenhaIncorreta@DummyTiming123");
        long inicio = System.currentTimeMillis();
        getSubmitButton().click();
        page.waitForLoadState();
        tempoRespostaInexistenteMs = System.currentTimeMillis() - inicio;
    }

    @Então("a diferença de tempo de resposta entre e-mail existente e inexistente deve ser desprezível demonstrando equalização via dummy hash")
    public void aDiferencaDeTempoDeRespostaEntreEmailExistenteEInexistenteDeveSerDesprezivel() {
        long delta = Math.abs(tempoRespostaExistenteMs - tempoRespostaInexistenteMs);
        Assertions.assertTrue(delta < 150,
                String.format("FALHA DE TIMING ATTACK (CWE-208 / OWASP ASVS V2.10): Discrepância de tempo detectada (%d ms). " +
                        "Tempo usuário existente: %d ms, Tempo usuário inexistente: %d ms. " +
                        "O sistema deve executar um cálculo de dummy hash quando o e-mail não existir para impedir que atacantes meçam a existência de contas.",
                        delta, tempoRespostaExistenteMs, tempoRespostaInexistenteMs));
    }

    // =========================================================================
    // SEÇÃO 3: FORÇA BRUTA E CREDENTIAL STUFFING
    // =========================================================================

    @Quando("o usuário realiza {string} tentativas consecutivas de login com senha incorreta para {string}")
    public void oUsuarioRealizaTentativasConsecutivasDeLoginComSenhaIncorretaPara(String qtdStr, String email) {
        int tentativas = Integer.parseInt(qtdStr);
        for (int i = 1; i <= tentativas; i++) {
            page.navigate(getBaseUrl());
            getEmailInput().fill(email);
            getPasswordInput().fill("SenhaInvalidaTentativa#" + i);
            getSubmitButton().click();
            page.waitForLoadState();
        }
        Locator feedback = page.locator("[data-testid='feedback-message'], .custom-alert-danger, .alert-danger").first();
        if (feedback.count() > 0) {
            mensagemUltimaTentativa = feedback.innerText();
        }
    }

    @Então("o sistema deve bloquear novas tentativas temporariamente por excesso de tentativas")
    public void oSistemaDeveBloquearNovasTentativasTemporariamentePorExcessoDeTentativas() {
        boolean bloqueado = mensagemUltimaTentativa.toLowerCase().contains("bloqueada")
                || mensagemUltimaTentativa.toLowerCase().contains("excesso")
                || mensagemUltimaTentativa.toLowerCase().contains("tentativas")
                || mensagemUltimaTentativa.toLowerCase().contains("temporariamente")
                || getSubmitButton().isDisabled();
        Assertions.assertTrue(bloqueado,
                "FALHA ANTI-BRUTE FORCE (CWE-307 / OWASP ASVS V2.2): O sistema permitiu 5 tentativas consecutivas incorretas sem aplicar bloqueio temporário ou mecanismo de Rate Limiting.");
    }

    @Quando("o cliente realiza rajada de requisições de login com senhas variadas para diferentes contas")
    public void oClienteRealizaRajadaDeRequisicoesDeLoginComSenhasVariadasParaDiferentesContas() {
        APIRequestContext requestContext = playwright.request().newContext();
        for (int i = 1; i <= 15; i++) {
            APIResponse resp = requestContext.post(getBaseUrl(), RequestOptions.create()
                    .setForm(FormData.create()
                            .set("email", "alvo" + i + "@teste.com")
                            .set("senha", "SenhaSpraying#" + i)));
            if (resp.status() == 429) {
                rateLimitAcionado = true;
                break;
            }
        }
        requestContext.dispose();
    }

    @Então("o sistema deve acionar limitação de taxa por IP retornando status de sobrecarga ou bloqueio")
    public void oSistemaDeveAcionarLimitacaoDeTaxaPorIpRetornandoStatusDeSobrecargaOuBloqueio() {
        Assertions.assertTrue(rateLimitAcionado,
                "FALHA CREDENTIAL STUFFING (CWE-307 / OWASP ASVS V2.2): Nenhuma política de Rate Limiting por IP (HTTP 429 Too Many Requests) foi acionada durante rajada de autenticações.");
    }

    // =========================================================================
    // SEÇÃO 4: INJEÇÕES E XSS
    // =========================================================================

    @Então("o sistema não deve executar scripts maliciosos e deve sanitizar a saída no DOM")
    public void oSistemaNaoDeveExecutarScriptsMaliciososEDeveSanitizarASaidaNoDom() {
        Assertions.assertFalse(xssAlertDisparado,
                "FALHA DE XSS REFLETIDO (CWE-79 / OWASP Top 10 A03): Caixa de diálogo Javascript executada a partir do input de login!");
        Locator unescapedScript = page.locator("[data-testid='feedback-message'] script, .cadastro-container script");
        Assertions.assertEquals(0, unescapedScript.count(),
                "FALHA DE XSS: Tag <script> não escapada foi encontrada no DOM após submissão de payload.");
    }

    // =========================================================================
    // SEÇÃO 5: HASH DOS (RESOURCE EXHAUSTION)
    // =========================================================================

    @Quando("o usuário tenta submeter uma senha com mais de {string} caracteres")
    public void oUsuarioTentaSubmeterUmaSenhaComMaisDeCaracteres(String maxChars) {
        int limite = Integer.parseInt(maxChars);
        String senhaLonga = "A".repeat(limite + 100);
        getEmailInput().fill("teste@teste.com");
        getPasswordInput().fill(senhaLonga);
        getSubmitButton().click();
        page.waitForLoadState();
    }

    @Então("o sistema deve recusar o processamento antecipadamente sem sobrecarregar a CPU")
    public void oSistemaDeveRecusarOProcessamentoAntecipadamenteSemSobrecarregarACpu() {
        Assertions.assertTrue(page.url().contains("/login"), "O sistema não deveria autenticar com senha de teste longa");
    }

    @E("deve exibir uma mensagem indicando que a senha excede o limite permitido")
    public void deveExibirUmaMensagemIndicandoQueASenhaExcedeOLimitePermitido() {
        Locator feedback = page.locator("[data-testid='feedback-message'], .custom-alert-danger, .alert-danger, :invalid").first();
        boolean temAviso = feedback.count() > 0 && (
                feedback.innerText().toLowerCase().contains("limite") ||
                feedback.innerText().toLowerCase().contains("128") ||
                feedback.innerText().toLowerCase().contains("máximo")
        );
        Assertions.assertTrue(temAviso,
                "FALHA DE HASH DOS (CWE-400 / OWASP ASVS V2.1): A aplicação não impôs limite de 128 caracteres na senha antes de calcular o hash criptográfico, vulnerável a esgotamento de CPU.");
    }

    // =========================================================================
    // SEÇÃO 6: GESTÃO AVANÇADA DE SESSÃO E COOKIES
    // =========================================================================

    @Dado("que o usuário acessa a página de login e obtém um identificador de sessão anônimo")
    public void queOUsuarioAcessaAPaginaDeLoginEObtemUmIdentificadorDeSessaoAnonimo() {
        page.navigate(getBaseUrl());
        preLoginSessionId = getSessionCookieValue();
    }

    @Quando("o usuário autentica-se com sucesso com credenciais válidas {string} e {string}")
    public void oUsuarioAutenticaseComSucessoComCredenciaisValidasE(String email, String senha) {
        getEmailInput().fill(email);
        getPasswordInput().fill(senha);
        getSubmitButton().click();
        page.waitForLoadState();
        page.waitForCondition(() -> !page.url().contains("/login"), new Page.WaitForConditionOptions().setTimeout(10000));
    }

    @Então("o identificador de sessão deve ser renovado para um novo ID diferente do anterior")
    public void oIdentificadorDeSessaoDeveSerRenovadoParaUmNovoIdDiferenteDoAnterior() {
        String postLoginSessionId = getSessionCookieValue();
        Assertions.assertFalse(postLoginSessionId.isBlank(), "Deve existir um identificador de sessão pós-login");
        Assertions.assertNotEquals(preLoginSessionId, postLoginSessionId,
                "FALHA DE SESSION FIXATION (CWE-384 / OWASP ASVS V3.2): O JSESSIONID '" + preLoginSessionId +
                        "' permaneceu idêntico após a autenticação bem-sucedida! A aplicação deve renovar a sessão com request.changeSessionId().");
    }

    @Então("o cookie de sessão da aplicação deve possuir a flag HttpOnly habilitada")
    public void oCookieDeSessaoDaAplicacaoDevePossuirAFlagHttpOnlyHabilitada() {
        com.microsoft.playwright.options.Cookie sessionCookie = getSessionCookie();
        Assertions.assertNotNull(sessionCookie, "Cookie de sessão (JSESSIONID) não encontrado");
        Assertions.assertTrue(sessionCookie.httpOnly,
                "FALHA DE SEGURANÇA EM COOKIE (CWE-1004 / OWASP ASVS V3.4): O cookie de sessão não possui a flag HttpOnly ativa, expondo o identificador de sessão a roubo via scripts maliciosos.");
    }

    @E("o cookie de sessão deve conter o atributo SameSite com valor Lax ou Strict")
    public void oCookieDeSessaoDeveConterOAtributoSameSiteComValorLaxOuStrict() {
        com.microsoft.playwright.options.Cookie sessionCookie = getSessionCookie();
        Assertions.assertNotNull(sessionCookie, "Cookie de sessão não encontrado");
        String sameSite = sessionCookie.sameSite != null ? sessionCookie.sameSite.name() : "";
        boolean validSameSite = "Lax".equalsIgnoreCase(sameSite) || "Strict".equalsIgnoreCase(sameSite);
        Assertions.assertTrue(validSameSite,
                "FALHA DE SEGURANÇA EM COOKIE (CWE-614 / OWASP ASVS V3.4): O atributo SameSite do cookie de sessão deve ser configurado como 'Lax' ou 'Strict' para defesa contra CSRF.");
    }

    // =========================================================================
    // SEÇÃO 7: OPEN REDIRECT
    // =========================================================================

    @Dado("que o usuário tenta acessar a página de login com parâmetro redirect malicioso {string}")
    public void queOUsuarioTentaAcessarAPaginaDeLoginComParametroRedirectMalicioso(String redirectUrl) {
        page.navigate(getBaseUrl() + "?redirect=" + redirectUrl);
        assertThat(getEmailInput()).isVisible();
    }

    @Então("o sistema deve recusar o redirecionamento externo e manter o usuário no domínio da aplicação")
    public void oSistemaDeveRecusarORedirecionamentoExternoEManterOUsuarioNoDominioDaAplicacao() {
        Assertions.assertFalse(page.url().contains("site-falso-phishing.com"),
                "FALHA DE OPEN REDIRECT (CWE-601 / OWASP ASVS V5.1): O sistema realizou redirecionamento arbitrário para URL externa maliciosa informada no parâmetro redirect.");
        Assertions.assertTrue(page.url().startsWith(getBaseOrigin()),
                "O redirecionamento pós-login deveria permanecer estritamente no domínio da aplicação (" + getBaseOrigin() + "), mas foi: " + page.url());
    }

    // =========================================================================
    // SEÇÃO 8: CONTA INATIVA
    // =========================================================================

    @Dado("que existe um usuário com conta inativa ou desativada no sistema")
    public void queExisteUmUsuarioComContaInativaOuDesativadaNoSistema() {
        // Cenário pedagógico de conta desativada
    }

    @Quando("o usuário submete o formulário de login com a conta inativa")
    public void oUsuarioSubmeteOFormularioDeLoginComAContaInativa() {
        page.navigate(getBaseUrl());
        getEmailInput().fill("inativo@alphaengine.com.br");
        getPasswordInput().fill("Senha@123");
        getSubmitButton().click();
        page.waitForLoadState();
    }

    @Então("o sistema deve recusar a autenticação de contas inativas")
    public void oSistemaDeveRecusarAAutenticacaoDeContasInativas() {
        Assertions.assertTrue(page.url().contains("/login"),
                "FALHA DE CONTA INATIVA (OWASP ASVS V2.1): O sistema não deve permitir autenticação de contas inativas ou suspensas.");
    }

    @E("não deve permitir o acesso ao painel do sistema")
    public void naoDevePermitirOAcessoAoPainelDoSistema() {
        Assertions.assertTrue(page.url().contains("/login"), "O usuário permaneceu indevidamente fora do /login");
        assertThat(getLoginForm()).isVisible();
    }

    // =========================================================================
    // SEÇÃO 9: CSRF, HTTP VERBS E CABEÇALHOS DE CACHE
    // =========================================================================

    @Dado("que uma requisição de autenticação POST é forjada sem o token anti-CSRF válido")
    public void queUmaRequisicaoDeAutenticacaoPostEForjadaSemOTokenAntiCsrfValido() {
        // Setup de requisição POST direta sem token CSRF
    }

    @Quando("a requisição é enviada ao endpoint de login")
    public void aRequisicaoEEnviadaAoEndpointDeLogin() {
        APIRequestContext requestContext = playwright.request().newContext();
        APIResponse response = requestContext.post(getBaseUrl(), RequestOptions.create()
                .setHeader("Origin", "http://site-atacante-malicioso.com")
                .setForm(FormData.create()
                        .set("email", "teste@teste.com")
                        .set("senha", "123456")));
        statusCsrfResponse = response.status();
        requestContext.dispose();
    }

    @Então("o sistema deve rejeitar a requisição com código de status 403 Forbidden ou mensagem de token inválido")
    public void oSistemaDeveRejeitarARequisicaoComCodigoDeStatus403ForbiddenOuMensagemDeTokenInvalido() {
        Assertions.assertEquals(403, statusCsrfResponse,
                "FALHA DE CSRF (CWE-352 / OWASP Top 10 A01): Submissão POST forjada sem token anti-CSRF deveria ter sido rejeitada com status 403 Forbidden.");
    }

    @Quando("uma tentativa de login é enviada utilizando o método HTTP GET com as credenciais na URL")
    public void umaTentativaDeLoginEEnviadaUtilizandoOMetodoHttpGetComAsCredenciaisNaUrl() {
        page.navigate(getBaseUrl() + "?email=teste@teste.com&senha=123456");
    }

    @Então("o sistema deve recusar a autenticação e não expor as credenciais na URL")
    public void oSistemaDeveRecusarAAutenticacaoENaoExporAsCredenciaisNaUrl() {
        Assertions.assertTrue(page.url().contains("/login"), "O método GET não deve conceder acesso autenticado");
        assertThat(getLoginForm()).isVisible();
    }

    @Quando("o usuário acessa ou submete a página de autenticação")
    public void oUsuarioAcessaOuSubmeteAPaginaDeAutenticacao() {
        APIRequestContext requestContext = playwright.request().newContext();
        APIResponse response = requestContext.get(getBaseUrl());
        ultimosHeadersResposta = response.headers();
        requestContext.dispose();
    }

    @Então("a resposta HTTP deve conter os cabeçalhos de segurança Cache-Control no-store ou no-cache impedindo armazenamento intermediário")
    public void aRespostaHttpDeveConterOsCabecalhosDeSegurancaCacheControlNoStore() {
        String cacheControl = ultimosHeadersResposta.getOrDefault("cache-control", "").toLowerCase();
        boolean seguro = cacheControl.contains("no-store") || cacheControl.contains("no-cache");
        Assertions.assertTrue(seguro,
                "FALHA DE CACHE-CONTROL (CWE-525 / OWASP WSTG-ATHN-06): A resposta da tela de login deve conter 'Cache-Control: no-store, no-cache' para mitigar o cache de credenciais em proxies ou navegadores compartilhados.");
    }

    // =========================================================================
    // SEÇÃO 10: BYPASS DE VALIDAÇÃO
    // =========================================================================

    @Então("o formulário deve impedir o envio com validação de campos obrigatórios")
    public void oFormularioDeveImpedirOEnvioComValidacaoDeCamposObrigatorios() {
        Assertions.assertTrue(page.url().contains("/login"), "O envio não deveria avançar para outra rota");
        assertThat(getLoginForm()).isVisible();
    }
}