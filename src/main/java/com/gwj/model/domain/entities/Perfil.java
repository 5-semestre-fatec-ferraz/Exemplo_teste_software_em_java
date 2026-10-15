package com.gwj.model.domain.entities;

import java.util.List;
import com.gwj.model.domain.IEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "tab_perfil") // Mapeia para a tabela 'perfis' do MySQL
public class Perfil implements IEntity {

    // Atributos privados em camelCase
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremento do MySQL
    private Long id;
    private String nome;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tab_perfil_permissao", joinColumns = @JoinColumn(name = "perfil_id"), inverseJoinColumns = @JoinColumn(name = "permissao_id"))
    private List<Permissao> permissoes;

    // Construtor padrão (obrigatório para frameworks)
    public Perfil() {
    }

    // Construtor completo
    public Perfil(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<Permissao> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(List<Permissao> permissoes) {
        this.permissoes = permissoes;
    }

    @Override
    public String toString() {
        return nome;
    }
}
