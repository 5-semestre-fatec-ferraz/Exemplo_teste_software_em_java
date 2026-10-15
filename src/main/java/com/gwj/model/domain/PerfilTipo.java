package com.gwj.model.domain;

public enum PerfilTipo {
    ADMINISTRADOR(1, "Administrador"),
    RECEPCIONISTA(2, "Recepcionista"),
    BARBEIRO(3, "Barbeiro"),
    CLIENTE(4, "Cliente");

    private final int id;
    private final String descricao;

    PerfilTipo(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }
}
