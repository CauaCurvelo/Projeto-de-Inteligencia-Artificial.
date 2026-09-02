# Projeto Sentinela 🗡️ - Inteligência Artificial

Projeto acadêmico focado em arquitetura de jogos desenvolvido para a disciplina de Projeto de Inteligência Artificial.

## Implementações Realizadas

### Aula 02 - Prática de Gameloop
Implementação do padrão estrutural **Game Loop** em Java e JavaFX, separando o ciclo do jogo em 3 camadas com independência de taxa de quadros (`deltaTime`):
1. **Input**: Leitura do teclado (W, A, S, D).
2. **Update**: Cálculos físicos e lógica de movimento.
3. **Render**: O desenho gráfico isolado na tela.

### Aula 03 - Criação de Tilemap e Colisões
Evolução do mapa do jogo implementando uma estrutura completa de **Tile Map** com representação lógica:
- Mapa estruturado em uma grade de tiles (Matriz de navegabilidade).
- Sistema de tipos de terrenos (Grama, Lama, Parede e Água) criados a partir do zero e posicionados.
- Novo sistema de colisão utilizando Bounding Boxes (AABB) em vez de limites fixos da tela, verificando dinamicamente se a próxima célula matriz é transitável (`walkable = true`).
- **Desafio Adicional Concluído**: O campo `movementCost` foi implementado para refletir o custo de navegação diferente dependendo do terreno (Grama = 1, Lama = 2), preparando a estrutura para o futuro algoritmo A*.

## Como jogar
Com o Java 17+ e o Maven instalados, basta executar na raiz do projeto:

```bash
mvn clean javafx:run
```
