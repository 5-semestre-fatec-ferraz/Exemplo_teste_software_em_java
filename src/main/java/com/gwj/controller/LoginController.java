package com.gwj.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gwj.model.domain.entities.Perfil;
import com.gwj.model.domain.entities.Usuario;
import com.gwj.service.IService;
import com.gwj.service.ServiceRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "sucesso", required = false) String sucesso,
            HttpSession session,
            Model model) {
        // Se a sessão existir e houver um usuário logado nela, redireciona imediatamente
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
            if (usuario.getPerfil() != null && usuario.getPerfil().getId() == 4L) {
                return "redirect:/";
            }
            return "redirect:/MRYnZpAsC9sp";
        }
        if (sucesso != null && !sucesso.isBlank()) {
            model.addAttribute("sucesso", "Cadastro realizado com sucesso! Faça login para continuar.");
        }
        return "site/auth/login";
    }

    @GetMapping("/cadastro")
    public String showCadastroForm(HttpSession session) {
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            return "redirect:/";
        }
        return "site/auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String processCadastro(
            @RequestParam("nome") String nome,
            @RequestParam(value = "sobrenome", required = false) String sobrenome,
            @RequestParam("email") String email,
            @RequestParam(value = "telefone", required = false) String telefone,
            @RequestParam("senha") String senha,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Validar se o e-mail já existe
            Usuario filtro = new Usuario();
            filtro.setEmail(email);

            IService<Usuario> usuarioService = ServiceRegistry.getService("Usuario");
            List<Usuario> resultados = usuarioService.read(filtro);

            for (Usuario u : resultados) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    model.addAttribute("erro", "Este e-mail já está cadastrado.");
                    model.addAttribute("nome", nome);
                    model.addAttribute("sobrenome", sobrenome);
                    model.addAttribute("email", email);
                    model.addAttribute("telefone", telefone);
                    return "site/auth/cadastro";
                }
            }

            // Criar novo Usuario com perfil de cliente (ID 4)
            Usuario novoUsuario = new Usuario();
            novoUsuario.setNomeUsuario(nome);
            novoUsuario.setEmail(email);
            novoUsuario.setSenha(senha);
            novoUsuario.criptografarSenha();
            novoUsuario.setStatus(true);

            Perfil perfilCliente = new Perfil();
            perfilCliente.setId(4L);
            novoUsuario.setPerfil(perfilCliente);

            usuarioService.create(novoUsuario);

            redirectAttributes.addAttribute("sucesso", "true");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao realizar o cadastro: " + e.getMessage());
            model.addAttribute("nome", nome);
            model.addAttribute("sobrenome", sobrenome);
            model.addAttribute("email", email);
            model.addAttribute("telefone", telefone);
            return "site/auth/cadastro";
        }
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("email") String email,
            @RequestParam("senha") String senha,
            HttpServletRequest request,
            Model model) {

        Usuario filtro = new Usuario();
        filtro.setEmail(email);

        IService<Usuario> service = ServiceRegistry.getService("Usuario");
        List<Usuario> resultados = service.read(filtro);

        // Aplica o hash na senha fornecida pelo formulário
        String senhaCriptografada = "{sha256}" + PasswordUtil.hash(senha);

        for (Usuario usuarioBanco : resultados) {
            boolean senhaValida = false;

            // Verifica se a senha no banco já está no novo formato criptografado
            if (usuarioBanco.getSenha() != null && usuarioBanco.getSenha().startsWith("{sha256}")) {
                senhaValida = usuarioBanco.getSenha().equals(senhaCriptografada);
            } else if (usuarioBanco.getSenha() != null) {
                // Retrocompatibilidade
                senhaValida = usuarioBanco.getSenha().equals(senha);
            }

            if (usuarioBanco.getEmail().equalsIgnoreCase(email) && senhaValida) {
                HttpSession session = request.getSession();
                session.setAttribute("usuarioLogado", usuarioBanco);

                if (usuarioBanco.getPerfil() != null && usuarioBanco.getPerfil().getId() == 4L) {
                    return "redirect:/"; // Clientes vão para a home
                }
                return "redirect:/MRYnZpAsC9sp"; // Administradores vão para o painel principal
            }
        }

        model.addAttribute("erro", "E-mail ou senha inválidos.");
        return "site/auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    // --- MASCARAMENTO E ROTAS DE LOGIN ADMINISTRATIVO ---

    @GetMapping("/MRYnZpAsC9sp/login")
    public String showAdminLoginForm(HttpSession session) {
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
            if (usuario.getPerfil() != null && usuario.getPerfil().getId() == 4L) {
                return "redirect:/";
            }
            return "redirect:/MRYnZpAsC9sp";
        }
        return "admin/auth/login";
    }

    @PostMapping("/MRYnZpAsC9sp/login")
    public String processAdminLogin(
            @RequestParam("email") String email,
            @RequestParam("senha") String senha,
            HttpServletRequest request,
            Model model) {

        Usuario filtro = new Usuario();
        filtro.setEmail(email);

        IService<Usuario> service = ServiceRegistry.getService("Usuario");
        List<Usuario> resultados = service.read(filtro);

        String senhaCriptografada = "{sha256}" + PasswordUtil.hash(senha);

        for (Usuario usuarioBanco : resultados) {
            boolean senhaValida = false;

            if (usuarioBanco.getSenha() != null && usuarioBanco.getSenha().startsWith("{sha256}")) {
                senhaValida = usuarioBanco.getSenha().equals(senhaCriptografada);
            } else if (usuarioBanco.getSenha() != null) {
                senhaValida = usuarioBanco.getSenha().equals(senha);
            }

            if (usuarioBanco.getEmail().equalsIgnoreCase(email) && senhaValida) {
                // Impede clientes (Perfil 4) de logarem no painel administrativo
                if (usuarioBanco.getPerfil() != null && usuarioBanco.getPerfil().getId() == 4L) {
                    model.addAttribute("erro", "Acesso negado: esta área é restrita a administradores.");
                    return "admin/auth/login";
                }

                HttpSession session = request.getSession();
                session.setAttribute("usuarioLogado", usuarioBanco);
                return "redirect:/MRYnZpAsC9sp";
            }
        }

        model.addAttribute("erro", "E-mail ou senha inválidos.");
        return "admin/auth/login";
    }

    @GetMapping("/MRYnZpAsC9sp/logout")
    public String adminLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/MRYnZpAsC9sp/login";
    }
}
