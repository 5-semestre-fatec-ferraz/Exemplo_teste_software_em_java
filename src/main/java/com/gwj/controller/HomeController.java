package com.gwj.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gwj.model.domain.entities.Usuario;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(HttpSession session, Model model) {
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
            model.addAttribute("usuario", usuario);
        }
        return "site/pages/home";
    }

    @GetMapping({"/MRYnZpAsC9sp", "/MRYnZpAsC9sp/"})
    public String adminDashboard(HttpSession session, Model model) {
        if (session != null && session.getAttribute("usuarioLogado") != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
            model.addAttribute("usuario", usuario);
        }
        return "admin/dashboard/index";
    }
}
