package com.gwj.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gwj.model.domain.entities.Usuario;
import com.gwj.model.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UsuarioServiceTest {

    private UsuarioRepository repositoryMock;
    private UsuarioService service;

    @BeforeEach
    public void setup() {
        repositoryMock = mock(UsuarioRepository.class);
        service = new UsuarioService(repositoryMock);
    }

    @Test
    public void testCreateShouldEncryptPassword() {
        Usuario u = new Usuario();
        u.setNomeUsuario("usuario_teste");
        u.setEmail("teste@dominio.com");
        u.setSenha("12345");

        when(repositoryMock.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario salvo = service.create(u);

        assertNotNull(salvo.getSenha());
        assertTrue(salvo.getSenha().startsWith("{sha256}"));
        verify(repositoryMock, times(1)).save(u);
    }

    @Test
    public void testUpdateShouldEncryptNewPassword() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNomeUsuario("usuario_teste");
        u.setEmail("teste@dominio.com");
        u.setSenha("nova_senha");

        when(repositoryMock.update(any(Usuario.class))).thenReturn(1L);

        Long updatedId = service.update(u);

        assertEquals(1L, updatedId);
        assertNotNull(u.getSenha());
        assertTrue(u.getSenha().startsWith("{sha256}"));
        verify(repositoryMock, times(1)).update(u);
    }

    @Test
    public void testUpdateWithNullPasswordShouldNotEncrypt() {
        Usuario u = new Usuario();
        u.setId(2L);
        u.setNomeUsuario("usuario_sem_senha");
        u.setEmail("teste2@dominio.com");
        u.setSenha(null);

        when(repositoryMock.update(any(Usuario.class))).thenReturn(2L);

        Long updatedId = service.update(u);

        assertEquals(2L, updatedId);
        assertNull(u.getSenha());
        verify(repositoryMock, times(1)).update(u);
    }
}
