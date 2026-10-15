# Projeto Didático: Fatia Vertical de Login e Testes Automatizados (BDD + Unitários)

Este projeto foi extraído como um **exemplo didático isolado (fatia vertical)** a partir do sistema completo, contendo **exclusivamente os componentes necessários para o fluxo de autenticação (Login/Logout), controle de sessão e testes automatizados**.

Todas as outras funcionalidades de domínio (produtos, agendamentos, serviços, carrinho, pedidos e wizard de setup) foram intencionalmente descartadas para manter o projeto enxuto, didático e de fácil compreensão para os alunos.

---

## 1. O que é a "Fatia Vertical" de Login?

Em arquitetura de software, uma **fatia vertical (Vertical Slice)** contempla todas as camadas necessárias para entregar uma funcionalidade ponta a ponta:

```
┌────────────────────────────────────────────────────────┐
│  1. Camada de Apresentação (View / Web)               │
│     - Thymeleaf: /login, /cadastro, /MRYnZpAsC9sp/login│
│     - Templates: site/auth/login.html, admin/auth/     │
│     - JavaScript: validation.js                        │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  2. Camada de Controle & Segurança (Controller)        │
│     - LoginController (processamento de login/logout)  │
│     - HomeController (rotas pós-login / e painel adm)  │
│     - AdminInterceptor (proteção de rotas restritas)   │
│     - WebConfig (registro de interceptores)            │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  3. Camada de Serviço & Negócio (Service)              │
│     - UsuarioService (regras e criptografia de senha)  │
│     - PasswordUtil (hashing SHA-256)                   │
│     - ServiceRegistry (localizador de serviços)        │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  4. Camada de Acesso a Dados & Transação (Persistence) │
│     - UsuarioRepository & GenericRepository            │
│     - UnitOfWork (controle de transação e conexões)    │
│     - DataMapper & QueryBuilder (mapeamento SQL)       │
│     - ConnectionDB / ConnectionPostGreSQL (JDBC)       │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│  5. Camada de Domínio (Domain / Entities)              │
│     - Usuario, Perfil, Permissao, IEntity              │
└────────────────────────────────────────────────────────┘
```

---

## 2. Estrutura de Arquivos da Fatia Vertical

```text
login-java/
├── .env                              # Variáveis de ambiente locais (banco de dados)
├── .env.example                      # Modelo de configuração para os alunos
├── pom.xml                           # Dependências Maven (Spring Boot, Cucumber, Playwright, JUnit 5)
├── mvnw / mvnw.cmd / .mvn/           # Maven Wrapper para execução sem instalar Maven
├── schema-login.sql                  # Script DDL e dados de teste (seed)
├── tutorial-teste-Gherkin.md         # Guia passo a passo de BDD e Pentest OWASP
├── resultado.md                      # Relatório dos cenários de teste e valor pedagógico
├── src/
│   ├── main/
│   │   ├── java/com/gwj/
│   │   │   ├── StartApplication.java         # Ponto de entrada do Spring Boot
│   │   │   ├── AppConfig.java                # Leitor de configurações (.env / properties)
│   │   │   ├── controller/
│   │   │   │   ├── LoginController.java      # Rotas /login, /logout, /MRYnZpAsC9sp/login
│   │   │   │   ├── HomeController.java       # Redirecionamentos pós-autenticação
│   │   │   │   ├── AdminInterceptor.java     # Bloqueio de rotas não autorizadas
│   │   │   │   ├── PasswordUtil.java         # Hashing SHA-256
│   │   │   │   └── WebConfig.java            # Configuração Web MVC
│   │   │   ├── model/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── IEntity.java          # Contrato base de entidades
│   │   │   │   │   ├── PerfilTipo.java       # Enum de tipos de perfil
│   │   │   │   │   ├── entities/
│   │   │   │   │   │   ├── Usuario.java      # Entidade principal de usuário
│   │   │   │   │   │   ├── Perfil.java       # Entidade de perfil de acesso
│   │   │   │   │   │   └── Permissao.java    # Entidade de permissões
│   │   │   │   │   └── factory/
│   │   │   │   │       ├── SchemaValidator.java
│   │   │   │   │       └── SimpleObjectFactory.java
│   │   │   │   ├── dataAccessObject/
│   │   │   │   │   ├── ConnectionDB.java     # Gerenciador JDBC MariaDB/MySQL
│   │   │   │   │   ├── ConnectionPostGreSQL.java
│   │   │   │   │   ├── DataMapper.java       # Mapeador Objeto-Relacional leve
│   │   │   │   │   └── QueryBuilder.java     # Construtor dinâmico de queries
│   │   │   │   └── repository/
│   │   │   │       ├── IRepository.java
│   │   │   │       ├── GenericRepository.java
│   │   │   │       └── UsuarioRepository.java
│   │   │   └── service/
│   │   │       ├── IService.java
│   │   │       ├── GenericService.java
│   │   │       ├── UsuarioService.java       # Serviço com criptografia de senha
│   │   │       ├── ServiceRegistry.java
│   │   │       └── transaction/
│   │   │           └── UnitOfWork.java       # Controle transacional
│   │   └── resources/
│   │       ├── application.properties        # Propriedades padrão Spring Boot
│   │       ├── static/
│   │       │   ├── css/                      # Estilos visuais do formulário de login
│   │       │   └── js/validation.js          # Validação client-side HTML5
│   │       └── templates/
│   │           ├── site/auth/login.html      # Tela de login do cliente
│   │           ├── site/auth/cadastro.html   # Tela de cadastro de usuário
│   │           ├── site/pages/home.html      # Página inicial após login do cliente
│   │           ├── admin/auth/login.html     # Tela de login administrativo restrito
│   │           └── admin/dashboard/index.html# Dashboard administrativo
│   └── test/
│       ├── java/
│       │   ├── com/fatec/login/
│       │   │   ├── runner/
│       │   │   │   └── CucumberTestRunner.java # Orquestrador JUnit 5 Suite
│       │   │   └── steps/
│       │   │       └── LoginSteps.java         # Implementação Playwright dos cenários BDD
│       │   └── com/gwj/service/
│       │       └── UsuarioServiceTest.java     # Testes unitários com Mockito
│       └── resources/
│           ├── features/login.feature          # Especificação BDD Gherkin (19 cenários)
│           └── login.feature                   # Cópia raiz para consultas rápidas
```

---

## 3. Como Preparar o Ambiente

### 3.1. Pré-requisitos
- **Java 21 LTS** instalado (`java -version`).
- Servidor **MariaDB** ou **MySQL** em execução.

### 3.2. Configurar o Banco de Dados
Execute o script [`schema-login.sql`](file:///var/www/html/teste-software/login-java/schema-login.sql) para criar as tabelas e usuários de teste:
```bash
mariadb -u root -p < schema-login.sql
```

Os seguintes usuários de teste já estarão disponíveis:
| E-mail | Senha | Perfil | Status |
| :--- | :--- | :--- | :--- |
| `teste@teste.com` | `123456` | Administrador (1) | Ativo |
| `cliente@teste.com` | `123456` | Cliente (4) | Ativo |
| `inativo@alphaengine.com.br` | `Senha@123` | Cliente (4) | Inativo (0) |

### 3.3. Configurar Credenciais Locais
Copie o arquivo de exemplo caso ainda não tenha o `.env`:
```bash
cp .env.example .env
```
Edite as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` caso suas credenciais locais sejam diferentes.

---

## 4. Como Executar

### 4.1. Executar os Testes Unitários (Mockito)
Não requer inicialização do servidor nem banco de dados ativo:
```bash
./mvnw test -Dtest=UsuarioServiceTest
```

### 4.2. Iniciar a Aplicação Spring Boot
```bash
./mvnw spring-boot:run
```
Acesse a aplicação no navegador em:
- Tela de login: `http://localhost:8089/login`
- Login administrativo: `http://localhost:8089/MRYnZpAsC9sp/login`

### 4.3. Executar os Testes de Penetração e BDD (Playwright + Cucumber)
Com a aplicação em execução na porta `8089`, abra outro terminal e rode:
```bash
./mvnw test -Dtest=CucumberTestRunner
```

Para consultar detalhes sobre cada um dos 19 cenários de ataque OWASP/ASVS testados, consulte [`tutorial-teste-Gherkin.md`](file:///var/www/html/teste-software/login-java/tutorial-teste-Gherkin.md) e [`resultado.md`](file:///var/www/html/teste-software/login-java/resultado.md).
