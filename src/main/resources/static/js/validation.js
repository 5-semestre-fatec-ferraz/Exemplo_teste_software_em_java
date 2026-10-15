document.addEventListener('DOMContentLoaded', () => {
    // Validação do formulário de Login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (event) => {
            if (!loginForm.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            loginForm.classList.add('was-validated');
        }, false);
    }

    // Validação do formulário de Cadastro
    const cadastroForm = document.getElementById('cadastroForm');
    if (cadastroForm) {
        const senha = document.getElementById('senha');
        const confirmarSenha = document.getElementById('confirmarSenha');
        const passwordAlert = document.getElementById('passwordAlert');

        cadastroForm.addEventListener('submit', (event) => {
            let valid = true;

            // Valida se as senhas coincidem
            if (senha && confirmarSenha) {
                if (senha.value !== confirmarSenha.value) {
                    if (passwordAlert) {
                        passwordAlert.classList.remove('d-none');
                    }
                    confirmarSenha.classList.add('is-invalid');
                    valid = false;
                } else {
                    if (passwordAlert) {
                        passwordAlert.classList.add('d-none');
                    }
                    confirmarSenha.classList.remove('is-invalid');
                }
            }

            if (!cadastroForm.checkValidity() || !valid) {
                event.preventDefault();
                event.stopPropagation();
            }

            cadastroForm.classList.add('was-validated');
        }, false);
    }
});
