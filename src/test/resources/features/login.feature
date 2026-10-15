# language: pt

@auth @pentest @seguranca @exaustivo @owasp @asvs
Funcionalidade: Pentest e Validação Exaustiva de Segurança na Autenticação (OWASP & ASVS)
  Como um auditor de segurança, desenvolvedor e educador
  Quero executar testes de intrusão, conformidade e regressão comportamental no fluxo de login
  Para comprovar a resiliência contra 100% dos vetores de ataque conhecidos (força bruta, injeção, temporização, fixação de sessão, CSRF, DoS e enumeração)
  E evidenciar falhas arquiteturais para fins pedagógicos e guia de remediação

  Contexto:
    Dado que o usuário está na página de login

  # ==============================================================================
  # SEÇÃO 1: FLUXO LEGÍTIMO E GESTÃO DE SESSÃO BÁSICA (Reaproveitado do Alpha Engine)
  # ==============================================================================

  @funcional @login_valido @RF014
  Cenário: Autenticação legítima com credenciais válidas e verificação de sessão ativa
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "123456"
    E clica no botão de entrar
    Então o usuário deve ser autenticado com sucesso e redirecionado para a área interna
    E deve possuir um cookie de sessão ativo

  @pentest @sessao @logout @owasp_sess_06 @cwe_613
  Cenário: Encerramento de sessão (Logout) e invalidação de credencial ativa
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "123456"
    E clica no botão de entrar
    Então o usuário deve ser autenticado com sucesso e redirecionado para a área interna
    Quando o usuário clica no botão de sair
    Então a sessão deve ser encerrada e o formulário de login reexibido

  @funcional @redirecionamento @url_pretendida
  Cenário: Redirecionamento após login para a URL pretendida original
    Dado que o usuário tentou acessar a página restrita "/MRYnZpAsC9sp"
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "123456"
    E clica no botão de entrar
    Então a resposta deve indicar o redirecionamento para a página "/MRYnZpAsC9sp"

  # ==============================================================================
  # SEÇÃO 2: ANTI-ENUMERAÇÃO DE CONTAS E DEFESA CONTRA TIMING ATTACKS
  # ==============================================================================

  @pentest @anti_enumeracao @owasp_athn_02 @cwe_204
  Cenário: Pentest - Tentativa de login com senha incorreta mantendo resposta anti-enumeração
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "SenhaIncorreta@999"
    E clica no botão de entrar
    Então o sistema deve exibir a mensagem de erro "E-mail ou senha inválidos."
    E o usuário deve permanecer na página de login

  @pentest @anti_enumeracao @owasp_athn_02 @cwe_204
  Cenário: Pentest - Tentativa de login com e-mail inexistente com resposta uniforme
    Quando o usuário preenche o e-mail com "usuario.inexistente.hack@teste.com"
    E o usuário preenche a senha com "QualquerSenha#123"
    E clica no botão de entrar
    Então o sistema deve exibir a mensagem de erro "E-mail ou senha inválidos."
    E o usuário deve permanecer na página de login

  @pentest @anti_timing_attack @owasp_athn_dummy @cwe_208
  Cenário: Pentest - Prevenção de Timing Attack via equalização criptográfica de tempo (Dummy Hash)
    Quando o usuário submete login com e-mail existente "teste@teste.com" e senha incorreta medindo o tempo de resposta
    E o usuário submete login com e-mail inexistente "inexistente.timing@teste.com" e senha incorreta medindo o tempo de resposta
    Então a diferença de tempo de resposta entre e-mail existente e inexistente deve ser desprezível demonstrando equalização via dummy hash

  # ==============================================================================
  # SEÇÃO 3: FORÇA BRUTA, DICIONÁRIO E CREDENTIAL STUFFING (OWASP ASVS V2.2)
  # ==============================================================================

  @pentest @brute_force @rate_limiting @cwe_307 @owasp_asvs_v2_2
  Cenário: Pentest - Bloqueio temporário ou Rate Limiting após múltiplas tentativas consecutivas inválidas (Anti-Brute Force)
    Quando o usuário realiza "5" tentativas consecutivas de login com senha incorreta para "teste@teste.com"
    Então o sistema deve bloquear novas tentativas temporariamente por excesso de tentativas

  @pentest @credential_stuffing @cwe_307 @owasp_asvs_v2_2
  Cenário: Pentest - Proteção contra Credential Stuffing / Password Spraying em massa
    Quando o cliente realiza rajada de requisições de login com senhas variadas para diferentes contas
    Então o sistema deve acionar limitação de taxa por IP retornando status de sobrecarga ou bloqueio

  # ==============================================================================
  # SEÇÃO 4: INJEÇÃO DE CÓDIGO (SQL INJECTION E REFLECTED XSS) (OWASP TOP 10 A03)
  # ==============================================================================

  @pentest @sql_injection @owasp_inpv_05 @cwe_89
  Cenário: Pentest - Tentativa de Authentication Bypass via SQL Injection no campo de e-mail
    Quando o usuário preenche o e-mail com "' OR '1'='1"
    E o usuário preenche a senha com "qualquer"
    E clica no botão de entrar
    Então o sistema deve exibir a mensagem de erro "E-mail ou senha inválidos."
    E não deve permitir acesso ao sistema

  @pentest @sql_injection @owasp_inpv_05 @cwe_89
  Cenário: Pentest - Tentativa de SQL Injection no campo de senha via comentário SQL
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "123' OR '1'='1"
    E clica no botão de entrar
    Então o sistema deve exibir a mensagem de erro "E-mail ou senha inválidos."
    E não deve permitir acesso ao sistema

  @pentest @xss @owasp_inpv_01 @cwe_79
  Cenário: Pentest - Tentativa de Cross-Site Scripting (XSS Refletido) no formulário de login
    Quando o usuário preenche o e-mail com "<script>alert('xss')</script>"
    E o usuário preenche a senha com "qualquer"
    E clica no botão de entrar
    Então o sistema não deve executar scripts maliciosos e deve sanitizar a saída no DOM

  # ==============================================================================
  # SEÇÃO 5: NEGAÇÃO DE SERVIÇO ALGORÍTMICO (HASH DOS / RESOURCE EXHAUSTION)
  # ==============================================================================

  @pentest @anti_dos @hash_dos @cwe_400 @owasp_asvs_v2_1
  Cenário: Pentest - Rejeição de senha com tamanho excessivo para proteção contra DoS no algoritmo de hash
    Quando o usuário tenta submeter uma senha com mais de "128" caracteres
    Então o sistema deve recusar o processamento antecipadamente sem sobrecarregar a CPU
    E deve exibir uma mensagem indicando que a senha excede o limite permitido

  # ==============================================================================
  # SEÇÃO 6: GESTÃO AVANÇADA DE SESSÃO E COOKIES (SESSION FIXATION & FLAGS)
  # ==============================================================================

  @pentest @session_fixation @owasp_sess_03 @cwe_384
  Cenário: Pentest - Prevenção contra Fixação de Sessão (Session Fixation) com renovação obrigatória do Session ID
    Dado que o usuário acessa a página de login e obtém um identificador de sessão anônimo
    Quando o usuário autentica-se com sucesso com credenciais válidas "teste@teste.com" e "123456"
    Então o identificador de sessão deve ser renovado para um novo ID diferente do anterior

  @pentest @cookie_security @cwe_1004 @cwe_614 @owasp_asvs_v3_4
  Cenário: Pentest - Validação de flags de segurança nos Cookies de Sessão (HttpOnly e SameSite)
    Dado que o usuário autentica-se com sucesso com credenciais válidas "teste@teste.com" e "123456"
    Então o cookie de sessão da aplicação deve possuir a flag HttpOnly habilitada
    E o cookie de sessão deve conter o atributo SameSite com valor Lax ou Strict

  # ==============================================================================
  # SEÇÃO 7: PHISHING E OPEN REDIRECT
  # ==============================================================================

  @pentest @open_redirect @cwe_601 @owasp_asvs_v5_1
  Cenário: Pentest - Proteção contra Open Redirect no parâmetro de redirecionamento pós-login
    Dado que o usuário tenta acessar a página de login com parâmetro redirect malicioso "https://site-falso-phishing.com/roubo"
    Quando o usuário preenche o e-mail com "teste@teste.com"
    E o usuário preenche a senha com "123456"
    E clica no botão de entrar
    Então o sistema deve recusar o redirecionamento externo e manter o usuário no domínio da aplicação

  # ==============================================================================
  # SEÇÃO 8: CONTROLE DE ESTADO DA CONTA (CONTAS INATIVAS)
  # ==============================================================================

  @pentest @conta_inativa @owasp_asvs_v2_1
  Cenário: Pentest - Tentativa de login em conta desativada ou inativa
    Dado que existe um usuário com conta inativa ou desativada no sistema
    Quando o usuário submete o formulário de login com a conta inativa
    Então o sistema deve recusar a autenticação de contas inativas
    E não deve permitir o acesso ao painel do sistema

  # ==============================================================================
  # SEÇÃO 9: FALSIFICAÇÃO DE REQUISIÇÃO, HTTP VERB TAMPERING E CABEÇALHOS DE CACHE
  # ==============================================================================

  @pentest @csrf @owasp_top10_a01 @cwe_352
  Cenário: Pentest - Proteção contra Cross-Site Request Forgery (CSRF) no formulário de autenticação
    Dado que uma requisição de autenticação POST é forjada sem o token anti-CSRF válido
    Quando a requisição é enviada ao endpoint de login
    Então o sistema deve rejeitar a requisição com código de status 403 Forbidden ou mensagem de token inválido

  @pentest @http_verb_tampering @cwe_650 @owasp_wstg_conf_06
  Cenário: Pentest - Bloqueio de submissão de credenciais via método HTTP GET
    Quando uma tentativa de login é enviada utilizando o método HTTP GET com as credenciais na URL
    Então o sistema deve recusar a autenticação e não expor as credenciais na URL

  @pentest @cache_control @cwe_525 @owasp_wstg_athn_06
  Cenário: Pentest - Prevenção de Cache de Credenciais e Informações Sensíveis nas Respostas HTTP
    Quando o usuário acessa ou submete a página de autenticação
    Então a resposta HTTP deve conter os cabeçalhos de segurança Cache-Control no-store ou no-cache impedindo armazenamento intermediário

  # ==============================================================================
  # SEÇÃO 10: BYPASS DE VALIDAÇÃO E ENTRADAS EM BRANCO
  # ==============================================================================

  @pentest @validacao_entrada @cwe_20
  Cenário: Pentest - Submissão de formulário com campos vazios (Bypass de validação)
    Quando o usuário deixa os campos de e-mail e senha vazios
    E clica no botão de entrar
    Então o formulário deve impedir o envio com validação de campos obrigatórios

  @pentest @validacao_entrada @espacos_em_branco @cwe_20
  Cenário: Pentest - Submissão de campos apenas com espaços em branco
    Quando o usuário preenche o e-mail com "   " e a senha com "   "
    E clica no botão de entrar
    Então o formulário deve impedir o envio com validação de campos obrigatórios