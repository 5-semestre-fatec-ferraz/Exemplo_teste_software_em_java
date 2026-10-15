package com.gwj.model.domain.factory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import com.gwj.AppConfig;
import com.gwj.model.domain.IEntity;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

public class SchemaValidator {

    @SuppressWarnings("unchecked")
    public static void validateAllEntities() {
        try {
            String path = AppConfig.ENTITIES_PATH.replaceAll("\\.$", "");
            List<Class<?>> classes = getClassesFromPackage(path);

            for (Class<?> clazz : classes) {
                // 1. Ignora Interfaces, Classes Abstratas e Enums
                if (clazz.isInterface() ||
                    Modifier.isAbstract(clazz.getModifiers()) ||
                    clazz.isEnum()) {
                    continue;
                }

                // 2. Obrigatoriedade da IEntity
                if (!IEntity.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException(
                        "\n[ERRO DE ARQUITETURA]: A classe '" + clazz.getSimpleName() + 
                        "' está no pacote de entidades mas NÃO implementa 'IEntity'.\n" +
                        "Toda entidade de domínio deve seguir este contrato para o DAO funcionar."
                    );
                }

                // 3. Valida os relacionamentos e coleções
                validateNamingConvention((Class<? extends IEntity>) clazz);
            }
            System.out.println("✅ Todas as entidades em '" + path + "' implementam IEntity e seguem os padrões.");
        } catch (Exception e) {
            System.err.println("❌ Falha na validação de entidades: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void validateNamingConvention(Class<? extends IEntity> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                boolean hasOneToMany = field.isAnnotationPresent(OneToMany.class);
                boolean hasManyToMany = field.isAnnotationPresent(ManyToMany.class);

                if (!hasOneToMany && !hasManyToMany) {
                    throw new RuntimeException(
                        "\n[ERRO DE MAPEAMENTO]: Na classe '" + clazz.getSimpleName() + 
                        "', a coleção '" + field.getName() + "' precisa da anotação @OneToMany ou @ManyToMany.");
                }

                if (!(field.getGenericType() instanceof ParameterizedType)) {
                    throw new RuntimeException(
                        "\n[ERRO DE GENERICO]: A coleção '" + field.getName() + "' na classe '" + 
                        clazz.getSimpleName() + "' deve especificar o tipo. Ex: List<Permissao>.");
                }
            }
        }
    }

    private static List<Class<?>> getClassesFromPackage(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
        List<Class<?>> classes = new ArrayList<>();
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(java.net.URLDecoder.decode(resource.getFile(), "UTF-8"));
            
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".class")) {
                            String fileName = file.getName().replace(".class", "");
                            String className = packageName + "." + fileName;
                            try {
                                classes.add(Class.forName(className));
                            } catch (ClassNotFoundException e) {
                                System.err.println("Não foi possível carregar a classe: " + className);
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    public static void ensureUniqueEmailIndex() {
        try (java.sql.Connection conn = com.gwj.model.dataAccessObject.ConnectionDB.getInstance().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            try {
                if (AppConfig.isPostgreSql()) {
                    stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS email_unique ON tab_usuario (email)");
                } else {
                    stmt.execute("ALTER TABLE `tab_usuario` ADD UNIQUE KEY `email_unique` (`email`)");
                }
                System.out.println("✅ Índice UNIQUE no campo 'email' criado com sucesso na tabela tab_usuario.");
            } catch (java.sql.SQLException e) {
                if (e.getErrorCode() == 1061 || "42000".equals(e.getSQLState()) || "42P07".equals(e.getSQLState())) {
                    System.out.println("ℹ️ O índice UNIQUE no campo 'email' já existe.");
                } else {
                    System.err.println("Aviso ao assegurar índice de e-mail UNIQUE: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso na conexão durante validação de índice único: " + e.getMessage());
        }
    }
}
