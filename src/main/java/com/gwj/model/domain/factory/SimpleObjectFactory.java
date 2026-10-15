package com.gwj.model.domain.factory;

import com.gwj.AppConfig;
import com.gwj.model.domain.IEntity;

public abstract class SimpleObjectFactory {

    /**
     * Instancia qualquer classe pelo nome, desde que implemente IEntity.
     */
    public static IEntity create(String fullClassName) {
        try {
            // 1. Carrega a classe dinamicamente
            Class<?> clazz = Class.forName(AppConfig.ENTITIES_PATH  + "." +  fullClassName);

            // 2. Valida se a classe é compatível com IEntity
            if (IEntity.class.isAssignableFrom(clazz)) {
                
                // 3. Cria a instância e faz o cast para a interface base
                return (IEntity) clazz.getDeclaredConstructor().newInstance();
            } else {
                throw new RuntimeException("A classe " + fullClassName + " não é uma IEntity válida.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Classe não encontrada: " + fullClassName);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao instanciar objeto: " + fullClassName, e);
        }
    }
}

