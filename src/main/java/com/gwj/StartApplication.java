package com.gwj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.gwj.model.domain.factory.SchemaValidator;

@SpringBootApplication
public class StartApplication {

	public static void main(String[] args) {
		// Carrega as variáveis do arquivo .env para o sistema
		AppConfig.loadEnv();

		// Roda a verificação automática de classes de entidade da fatia de login
		SchemaValidator.validateAllEntities();

		// Assegura índice único para e-mail
		try {
			SchemaValidator.ensureUniqueEmailIndex();
		} catch (Exception e) {
			System.out.println("ℹ️ Conexão de banco durante boot: " + e.getMessage());
		}

		// Inicia o servidor Tomcat embutido
        SpringApplication.run(StartApplication.class, args);
        System.out.println("=== Aplicação de Login iniciada com sucesso! ===");
    }
}
