package com.gwj.service;

import com.gwj.model.domain.entities.Usuario;
import com.gwj.model.repository.UsuarioRepository;

public class UsuarioService extends GenericService<Usuario> {
    
    public UsuarioService() {
        super(new UsuarioRepository());
    }

    public UsuarioService(UsuarioRepository repository) {
        super(repository);
    }

    @Override
    public Usuario create(Usuario entity) {
        entity.criptografarSenha();
        return super.create(entity);
    }

    @Override
    public Long update(Usuario entity) {
        if (entity.getSenha() != null && !entity.getSenha().isBlank()) {
            entity.criptografarSenha();
        }
        return super.update(entity);
    }
}
