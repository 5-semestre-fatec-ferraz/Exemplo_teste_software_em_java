package com.gwj.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gwj.model.domain.entities.Usuario;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        
        // Se a sessão não existir ou não tiver o usuário logado, barra o acesso
        if (session == null || session.getAttribute("usuarioLogado") == null) {
            response.sendRedirect(request.getContextPath() + "/MRYnZpAsC9sp/login");
            return false;
        }
        
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        // Bloqueia clientes (Perfil 4) de acessarem qualquer rota do painel administrativo
        if (usuarioLogado.getPerfil() != null && usuarioLogado.getPerfil().getId() == 4L) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }

        return true;
    }
}
