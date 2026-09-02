# Projeto Sentinela 🗡️ - Inteligência Artificial

Projeto acadêmico focado em arquitetura de jogos e navegação autônoma desenvolvido para a disciplina de Projeto de Inteligência Artificial.

## Visão Geral do Projeto
O Sentinela é um jogo no formato *Top-down* construído em **Java** utilizando **JavaFX**. O objetivo principal do projeto é aplicar os conceitos estruturais de games (como a implementação do padrão Game Loop) integrados a algoritmos clássicos de IA (como a navegação inteligente com o Algoritmo A*), resultando em um agente autônomo capaz de desviar de obstáculos em um Tilemap.

## Tecnologias
- Java 17+
- JavaFX (Graphics e Controls)
- Apache Maven

## Como Executar e Jogar

Para compilar e jogar localmente, certifique-se de ter o **Java 17+** e o **Maven** instalados e configurados nas variáveis de ambiente em sua máquina.

1. **Na primeira execução (para configurar dependências):**
   - Execute o script `setup.bat` com um duplo-clique. Ele validará a sua instalação do Java/Maven, fará o download das bibliotecas do JavaFX e compilará o jogo pela primeira vez.

2. **Para jogar (nas demais vezes):**
   - Execute o script `start_game.bat`.
   
*(Alternativamente, você pode abrir o terminal na pasta raiz e digitar `mvn clean javafx:run` caso prefira não utilizar os scripts .bat)*.
