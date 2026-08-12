# Prática de Gameloop - Sentinela 🗡️

Projeto acadêmico focado em arquitetura de jogos e na implementação do padrão estrutural **Game Loop**.

## Como funciona
Desenvolvido em **Java e JavaFX**, o código separa estritamente o ciclo do jogo em 3 camadas, com independência de taxa de quadros (`deltaTime`):
1. **Input**: Leitura do teclado (W, A, S, D).
2. **Update**: Cálculos físicos e lógica (colisões e movimento).
3. **Render**: O desenho gráfico isolado na tela.

## Como jogar
Com o Java 17+ e o Maven instalados, basta executar:

```bash
mvn clean javafx:run
```
