-- ==============================================================================
-- Schema SQL e Seed da Fatia Vertical de Login
-- Banco: tgos_barbearia (ou gwj2)
-- ==============================================================================

-- 1. Criação da Tabela de Perfis
CREATE TABLE IF NOT EXISTS `tab_perfil` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `nome` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Criação da Tabela de Permissões
CREATE TABLE IF NOT EXISTS `tab_permissao` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `nome` VARCHAR(100) NOT NULL UNIQUE,
  `descricao` VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Criação da Tabela de Associação Perfil x Permissão
CREATE TABLE IF NOT EXISTS `tab_perfil_permissao` (
  `perfil_id` BIGINT NOT NULL,
  `permissao_id` BIGINT NOT NULL,
  PRIMARY KEY (`perfil_id`, `permissao_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Criação da Tabela de Usuários (Central para autenticação e pentest)
CREATE TABLE IF NOT EXISTS `tab_usuario` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `perfil_id` BIGINT DEFAULT NULL,
  `nome_usuario` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(100) NOT NULL,
  `senha` VARCHAR(255) NOT NULL,
  `status` TINYINT(1) DEFAULT 1,
  `token` VARCHAR(255) DEFAULT NULL,
  `ip` VARCHAR(45) DEFAULT NULL,
  `data_cadastro` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `email_unique` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Seed de Perfis
INSERT IGNORE INTO `tab_perfil` (`id`, `nome`) VALUES
(1, 'Administrador'),
(2, 'Recepcionista'),
(3, 'Barbeiro'),
(4, 'Cliente');

-- 6. Seed de Usuários para Testes de Penetração e Funcionais
-- Senha de teste padrão: 123456 (armazenada com suporte a hash SHA-256 e legado)
INSERT IGNORE INTO `tab_usuario` (`id`, `perfil_id`, `nome_usuario`, `email`, `senha`, `status`) VALUES
(1, 1, 'Admin', 'admin@teste.com', '{sha256}jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', 1),
(6, 1, 'Teste', 'teste@teste.com', '123456', 1),
(7, 4, 'Cliente Teste', 'cliente@teste.com', '{sha256}jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', 1),
(8, 4, 'Inativo Teste', 'inativo@alphaengine.com.br', '{sha256}jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=', 0);
