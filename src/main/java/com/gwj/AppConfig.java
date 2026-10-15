package com.gwj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class AppConfig {
    private static final Properties props = new Properties();

    // Configurações dinâmicas com suporte a recarregamento em tempo de execução
    public static volatile String DB_URL          = "jdbc:mariadb://localhost:3306/gwj2";
    public static volatile String DB_USER         = "desenvolvedor";
    public static volatile String DB_PASS         = "";
    public static volatile String DB_DRIVER       = "org.mariadb.jdbc.Driver";
    public static volatile String TABLE_PREFIX    = "tab_";
    public static volatile String ENTITIES_PATH   = "com.gwj.model.domain.entities";
    
    public static final int    LIMITE_USUARIOS = 100;
    public static final double VERSAO          = 1.0;

    static {
        reload();
    }

    /**
     * Identifica se a aplicação está configurada para utilizar PostgreSQL.
     */
    public static boolean isPostgreSql() {
        return (DB_URL != null && DB_URL.contains("postgresql")) ||
               (DB_DRIVER != null && DB_DRIVER.toLowerCase().contains("postgresql"));
    }

    /**
     * Recarrega o .env e atualiza todas as configurações estáticas.
     */
    public static synchronized void reload() {
        loadEnv();
        try (var is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.clear();
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("Aviso: application.properties não encontrado, usando padrões.");
        }

        DB_URL        = resolveValue("spring.datasource.url", "DB_URL", "jdbc:mariadb://localhost:3306/gwj2");
        DB_USER       = resolveValue("spring.datasource.username", "DB_USERNAME", "desenvolvedor");
        DB_PASS       = resolveValue("spring.datasource.password", "DB_PASSWORD", "");
        DB_DRIVER     = resolveValue("spring.datasource.driver-class-name", "DB_DRIVER", "org.mariadb.jdbc.Driver");
        TABLE_PREFIX  = resolveValue("app.database.prefix", "APP_DB_PREFIX", "tab_");
        ENTITIES_PATH = resolveValue("app.entities.path", "APP_ENTITIES_PATH", "com.gwj.model.domain.entities");
    }

    /**
     * Carrega as variáveis do arquivo .env para System Properties do Java.
     */
    public static void loadEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File(System.getProperty("user.dir"), ".env");
        }
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    System.setProperty(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível carregar o arquivo .env: " + e.getMessage());
        }
    }

    private static String resolveValue(String propKey, String envKey, String fallback) {
        // 1. Variável de ambiente do SO (prioridade alta para contêineres/nuvem)
        String val = System.getenv(envKey);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = System.getenv(propKey.replace('.', '_').toUpperCase());
        if (val != null && !val.isBlank()) {
            return val;
        }

        // 2. System Property (carregada do .env ou passada via -D)
        val = System.getProperty(envKey);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = System.getProperty(propKey);
        if (val != null && !val.isBlank()) {
            return val;
        }

        // 3. application.properties (podendo conter placeholder ${VAR:default})
        val = props.getProperty(propKey);
        if (val != null && !val.isBlank()) {
            val = resolvePlaceholder(val);
            if (!val.isBlank()) {
                return val;
            }
        }

        return fallback;
    }

    private static String resolvePlaceholder(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.startsWith("${") && val.endsWith("}")) {
            String inner = val.substring(2, val.length() - 1);
            int colonIdx = inner.indexOf(':');
            String varName = colonIdx != -1 ? inner.substring(0, colonIdx).trim() : inner.trim();
            String defaultVal = colonIdx != -1 ? inner.substring(colonIdx + 1).trim() : "";

            String envVal = System.getenv(varName);
            if (envVal != null && !envVal.isBlank()) return envVal;

            String sysVal = System.getProperty(varName);
            if (sysVal != null && !sysVal.isBlank()) return sysVal;

            return defaultVal;
        }
        return val;
    }

    // Construtor privado para evitar instanciação
    private AppConfig() {
        throw new UnsupportedOperationException("Esta é uma classe de constantes e não pode ser instanciada");
    }
}
