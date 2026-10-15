package com.gwj.model.domain.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.gwj.model.domain.IEntity;
import com.gwj.controller.PasswordUtil;

@Entity
public class Usuario implements IEntity{

	// Atributos primitivos:
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long          id  = 0L; // Inicializa variável de índice
	@ManyToOne
	@JoinColumn(name = "perfil_id")
	protected Perfil        perfil;
	protected String        nomeUsuario;
	protected String        email;
	protected Boolean       status = true;
	protected String        senha;
	protected String        token;
	protected String        ip;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Avisa ao módulo json utilizar este formato.
	protected LocalDateTime dataCadastro;
	
	// Métodos
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Perfil getPerfil() {
		return perfil;
	}
	public void setPerfil(Perfil perfil) {
		this.perfil = perfil;
	}
	public String getNomeUsuario() {
		return nomeUsuario;
	}
	public void setNomeUsuario(String nomeUsuario) {
		this.nomeUsuario = nomeUsuario;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Boolean getStatus() {
		return status;
	}
	public void setStatus(Boolean status) {
		this.status = status;
	}
	public String getSenha() {
		return senha;
	}
	public void setSenha(String senha) {
		this.senha = senha;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}
	public void setDataCadastro(LocalDateTime dataCadastro) {
		this.dataCadastro = dataCadastro;
	}
	
	/**
	 * Verifica se o usuário possui uma permissão específica.
	 * 
	 * @param nomePermissao O nome da permissão (ex: "AGENDAR_HORARIO")
	 * @return true se o perfil do usuário contiver a permissão, false caso contrário.
	 */
	public boolean hasPermissao(String nomePermissao) {
		if (this.perfil != null && this.perfil.getPermissoes() != null) {
			for (Permissao p : this.perfil.getPermissoes()) {
				if (p.getNome() != null && p.getNome().equalsIgnoreCase(nomePermissao)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Criptografa a senha do usuário com SHA-256.
	 * Se a senha estiver em branco, ela é anulada para evitar sobrescrever a senha existente com vazio durante uma edição (Update).
	 */
	public void criptografarSenha() {
		if (this.senha != null) {
			if (this.senha.isBlank()) {
				this.senha = null; 
			} else if (!this.senha.startsWith("{sha256}")) {
				this.senha = "{sha256}" + PasswordUtil.hash(this.senha);
			}
		}
	}
}