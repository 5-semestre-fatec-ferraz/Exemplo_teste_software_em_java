package com.gwj.model.repository;

import com.gwj.model.domain.entities.Usuario;

public class UsuarioRepository extends GenericRepository<Usuario> {
    public UsuarioRepository() {
        super(Usuario.class);
    }
}
