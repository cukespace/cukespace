# language: pt

Funcionalidade: Fazer login no sistema

  Cenario: Usuário e senha corretos
    Dado O sistema possui o usuario 'admin' cadastrado
    Quando O usuario preenche o login como 'admin' e a senha O botao de login é clicado
    Entao O usuário é redirecionado para a página de pesquisa de usuarios
