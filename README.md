# Sistema de Gerenciamento de Cantina

Projeto acadêmico desenvolvido como trabalho de conclusão de curso no SENAI.

O sistema simula o funcionamento de uma cantina, permitindo o gerenciamento de produtos, estoque, pedidos, pagamentos e retirada de pedidos, com diferentes níveis de acesso para clientes, funcionários e gerentes.

> **Nota sobre o front-end:** o foco deste projeto é o desenvolvimento **Back-end** (regras de negócio, persistência de dados, segurança e autenticação/autorização). A interface visual foi gerada com apoio de IA apenas para permitir a execução e demonstração dos fluxos do sistema, e não representa o objetivo principal do projeto.

## Problema

Em uma cantina com grande fluxo de pessoas, o controle manual de pedidos pode gerar filas, erros no atendimento e dificuldade para acompanhar estoque, pagamentos e retiradas.

A proposta do projeto foi criar uma aplicação capaz de centralizar essas operações em um único sistema.

## Solução

O sistema permite que clientes consultem o cardápio, criem pedidos e acompanhem o andamento da compra. Funcionários podem acompanhar os pedidos, confirmar pagamentos, iniciar o preparo, marcar pedidos como prontos e confirmar a retirada. Gerentes possuem acesso a funcionalidades administrativas, como cadastro de produtos, controle de estoque, registro de perdas e gerenciamento de usuários.

O sistema utiliza autenticação e autorização com Spring Security, separando as permissões entre `CLIENTE`, `FUNCIONARIO` e `GERENTE`.

## Funcionalidades

### Cliente
- Cadastro e login
- Visualização do cardápio
- Filtro de produtos por nome e categoria
- Criação de pedidos
- Adição, alteração e remoção de itens
- Acompanhamento dos próprios pedidos
- Escolha da forma de pagamento
- Recebimento de código de retirada após aprovação do pagamento

### Funcionário
- Visualização dos pedidos, com filtros por tipo e status
- Visualização dos detalhes do pedido
- Confirmação de pagamentos
- Início do preparo
- Alteração do pedido para pronto
- Confirmação da retirada
- Busca de pedido pelo código de retirada

### Gerente
- Painel administrativo
- Cadastro e edição de produtos
- Ativação e desativação de produtos
- Controle de estoque (entrada, perda e ajuste manual)
- Visualização do histórico de movimentações
- Cadastro de funcionários e gerentes
- Ativação e desativação de usuários
- Acesso às funcionalidades de funcionário

## Fluxo do pedido

```
AGUARDANDO_PAGAMENTO → PAGO → EM_PREPARO → PRONTO → RETIRADO
```

O pedido também pode ser cancelado enquanto estiver aguardando pagamento. As regras de transição são controladas na camada de serviço para impedir alterações de status fora da ordem esperada.

## Pagamentos

O sistema possui um fluxo de pagamento simulado (sem integração com instituições financeiras, apenas para fins acadêmicos).

- **Reservas feitas pelo aplicativo:** pagamento apenas via **Pix**
- **Pedidos presenciais:** cartão de crédito, cartão de débito ou dinheiro

Após a aprovação do pagamento, o pedido passa para o status `PAGO`, o estoque dos produtos é atualizado e um código de retirada é gerado.

## Controle de estoque

Cada produto possui estoque atual, estoque mínimo, preço de custo, preço de venda, categoria e status (ativo/inativo).

O sistema registra todas as movimentações de estoque (produto, tipo, motivo, quantidade, saldo anterior, saldo atual, data/hora e observação). As movimentações podem ocorrer por compra, venda, perda, ajuste ou cancelamento de pedido.

## Perfis de acesso

| Perfil | Responsabilidade |
|---|---|
| **CLIENTE** | Criar e acompanhar os próprios pedidos |
| **FUNCIONARIO** | Atendimento, pagamentos, preparo e retirada dos pedidos |
| **GERENTE** | Funcionalidades administrativas + tudo que o funcionário acessa |

## Segurança

- Autenticação e autorização por papel com **Spring Security**
- Senhas armazenadas com hash **BCrypt**, nunca em texto puro
- Controle de propriedade nos pedidos (um cliente só acessa os próprios pedidos, mesmo autenticado)
- Credenciais de banco de dados configuradas via **variável de ambiente**, nunca commitadas no código

## Testes

O projeto possui testes unitários das regras de negócio das camadas de serviço (`PedidoService` e `PagamentoService`), utilizando **JUnit 5** e **Mockito**. Os testes cobrem cenários como transições de status inválidas, validação de estoque, regras de forma de pagamento por tipo de pedido e o fluxo de estorno.

## Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Thymeleaf
- Bean Validation
- MySQL
- JUnit 5 / Mockito
- HTML e CSS
- Maven

## Estrutura do projeto

```
src/main/java/com/senai/cantina/cantina
├── config
├── controller
├── exception
├── model
├── repository
└── service
```

A aplicação foi organizada em camadas para separar responsabilidades:

- **controller**: requisições e navegação
- **service**: regras de negócio
- **repository**: comunicação com o banco de dados
- **model**: entidades e relacionamentos do sistema
- **config**: configurações de segurança e da aplicação
- **exception**: tratamento de exceções

## Como executar o projeto

### Pré-requisitos
- Java 21
- MySQL

### Configuração do banco de dados

O projeto utiliza MySQL. Por segurança, a senha do banco é lida a partir de uma variável de ambiente, não fica no código.

Defina a variável de ambiente `DB_PASSWORD` com a senha do seu MySQL antes de rodar o projeto. Exemplo (Linux/macOS):

```bash
export DB_PASSWORD=sua_senha_aqui
```

No Windows (PowerShell):

```powershell
$env:DB_PASSWORD="sua_senha_aqui"
```

Ou, se estiver usando uma IDE (IntelliJ, Eclipse), configure a variável em **Run Configurations → Environment Variables**.

Exemplo de configuração em `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cantina?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Sao_Paulo
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
```

O parâmetro `createDatabaseIfNotExist=true` permite a criação do banco caso ele ainda não exista.

### Executando

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Usuário gerente de demonstração

O projeto cria automaticamente um usuário gerente para facilitar os testes da aplicação (essa conta existe apenas para demonstração acadêmica):

```
E-mail: gerente@appetite.com
Senha: admin123
```

## Sobre o projeto

Este projeto foi desenvolvido como trabalho final de curso no SENAI, com o objetivo principal de aplicar na prática conceitos de desenvolvimento **Back-end com Java e Spring Boot**: autenticação, autorização, persistência de dados, relacionamentos entre entidades, validações, regras de negócio e testes automatizados.