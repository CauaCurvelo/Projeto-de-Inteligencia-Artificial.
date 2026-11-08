# 🦊 Sentinela: Instinto Selvagem (*Wild Instinct*)
### Projeto Final de Inteligência Artificial — Engenharia e Simulação de Agentes Autônomos

[![Java](https://img.shields.io/badge/Java-17%20LTS-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Status](https://img.shields.io/badge/Status-Concluído-success?style=for-the-badge)](#)

---

## 📌 Identificação Acadêmica
- **Instituição:** UNEX — Centro Universitário de Excelência (Vitória da Conquista)
- **Curso:** Bacharelado em Sistemas de Informação
- **Componente Curricular:** Projeto de Inteligência Artificial
- **Docente Responsável:** Prof. Marcus Vinicius Monteiro de Almeida Tavares
- **Discentes:**
  - Artur Maia Coqueiro
  - Cauã Curvelo Ribeiro Meira
  - Marco Antonio Meira de Lima
  - Pedro Gabriel Soares Tigres
- **Ano Letivo:** 2026

---

## 📖 1. Visão Geral e Proposta do Projeto
O **Sentinela: Instinto Selvagem** é um simulador de sobrevivência ecológica em perspectiva *Top-Down 2D*, construído em Java puro com aceleração gráfica via JavaFX nativo. 

Ao contrário de abordagens clássicas em que o jogador comanda um avatar caçador, a proposta deste projeto inverte os papéis: o usuário assume o controle de um espécime silvestre nativo (raposa) tentando sobreviver às pressões impostas por uma cadeia de **agentes inteligentes concorrentes e hostis** (caçadores humanos armados, cães farejadores de matilha, animais rivais competidores e superpredadores alfa).

O projeto serve como bancada prática para aplicação e validação em tempo real de **algoritmos fundamentais de Inteligência Artificial**, combinando:
1. **Navegação Ótima:** Algoritmo de busca informada A* (*A-Star Search*) com heurística de Manhattan admissível e ponderação de custos por terreno ($g(n)$).
2. **Tomada de Decisão:** Máquinas de Estados Finitos estritas (*Finite State Machines - FSM*) com comportamentos táticos de patrulha, suspeita progressiva, combate territorial e ataque entre predadores.
3. **Percepção Multissensorial Simulada:** Visão setorial em cone com traçado de raios (*Raycasting*) para verificação de oclusão por obstáculos, acústica com propagação de ondas de ruído em tempo real e rastreamento químico de feromônios através de cadeia de pegadas com decaimento temporal.
4. **Mundo Procedural Infinito:** Geração contínua em tempo de execução via ruído fractal multi-oitavas (*fBm - Fractal Brownian Motion*) estruturado em chunks matriciais dinâmicos.

---

## 🧠 2. Arquitetura da Inteligência Artificial

```
                      ┌────────────────────────────────────────┐
                      │          PERCEPÇÃO SENSORIAL           │
                      │  Visão (Raycast) | Som | Cheiro/Rastro │
                      └──────────────────┬─────────────────────┘
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │    MÁQUINA DE ESTADOS FINITOS (FSM)    │
                      │  Patrol ➔ Suspicious ➔ Chase ➔ Combat  │
                      └──────────────────┬─────────────────────┘
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │         NAVEGAÇÃO E PATHFINDING        │
                      │       Algoritmo A* (Heurística)        │
                      │     Custos Ponderados por Terreno      │
                      └────────────────────────────────────────┘
```

### 2.1 Algoritmo A* com Heurística de Manhattan
O planejamento de rotas é operado pela classe `Pathfinder.java`. A função de avaliação é formulada como:
$$f(n) = g(n) + h(n)$$
Onde:
- $g(n)$ é o custo real acumulado da origem até o nó $n$. Cada tile do mapa possui um custo específico de travessia baseado na física do terreno.
- $h(n)$ é a função heurística estimada de $n$ até o destino, calculada pela **Distância de Manhattan**:
  $$h(n) = |col_{atual} - col_{alvo}| + |row_{atual} - row_{alvo}|$$

*Justificativa Formal:* Por estarmos lidando com um grid ortogonal (4 direções primárias), a heurística de Manhattan é **matematicamente admissível** ($\forall n, h(n) \leq h^*(n)$) e **consistente** ($h(n) \leq c(n, p) + h(p)$), garantindo que a primeira rota expandida seja a rota ótima e reduzindo a expansão de nós na `PriorityQueue` a patamares computacionalmente desprezíveis (execução contínua a 60 FPS).

### 2.2 Custos Ponderados de Terreno e Influência no Comportamento da IA
A IA não enxerga apenas distâncias euclidianas; ela toma decisões ecológicas baseadas nos custos de transição dos terrenos (`TileType`):
- **Trilha de Terra (`DIRT_PATH` - Custo 1):** Superfície de menor resistência. Caçadores e predadores convergem suas patrulhas naturais para as trilhas.
- **Grama Pura (`GRASS` - Custo 1):** Piso padrão de deslocamento.
- **Grama Alta (`TALL_GRASS` - Custo 1):** Não freia a locomoção, mas reduz o cone de visão de caçadores sobre a raposa em **75%** (fator de camuflagem).
- **Lamaçal (`MUD` - Custo 3):** O A* calcula que percorrer 3 blocos de grama limpa é preferível a atravessar 1 bloco de lama. O agente desvia ativamente de pântanos.
- **Água / Rio (`WATER` - Custo 4):** Nadar penaliza a velocidade em 60%. Caçadores e cães evitam entrar na água, buscando ativamente as **pontes naturais de terra firme** para interceptar a raposa.
- **Árvores e Paredões (`TREE` / `WALL` - Custo 999 / Obstáculo):** Bloqueiam completamente a passagem e a linha de visão do *Raycasting*.

### 2.3 Máquina de Estados Finitos (FSM)
Cada criatura é governada por estados mutuamente exclusivos:
- `PATROL`: Patrulha autônoma com escolha estocástica de nós válidos.
- `SUSPICIOUS`: Gatilho disparado por estímulo acústico distante ou vislumbre no cone periférico. Uma barra de suspeita sobe de 0% a 100%.
- `ALERT`: Estado de prontidão e varredura ativa após perder contato temporário com o alvo.
- `CHASE`: Perseguição contínua com recálculo adaptativo de caminho A*.
- `AIMING`: Estacionamento para traçado de mira e disparo de projétil à distância (Caçador).
- `SNIFFING_TRACK`: Rastreamento encadeado de pegadas olfativas nó a nó (Cão de Caça).
- `SEARCHING_FOOD` & `EATING`: Coleta e consumo oportunista de recursos antes do jogador (Rivais).
- `TERRITORIAL_ATTACK`: Engajamento violento em defesa de comida ou espaço territorial.
- `FIGHTING_ENEMY`: Combate inter-espécies (Superpredadores predando caçadores e outros animais).

---

## 🎮 3. Mecânicas de Jogo e Dinâmica de Simulação

- **Sistema de Saúde Estilo Zelda:** Corações visuais discretos. O jogador sofre perda de coração ao ser atingido, recebendo um *burst* de adrenalina (invulnerabilidade temporária com *damage flash*).
- **Sistema de Fome Metabólica:** Dreno contínuo acelerado ao correr (*Sprint*). Atingir 0% acarreta morte por inanição.
- **Combate Físico & Meat Drops:** A raposa pode desferir mordidas e patadas frontais (`Espaço`, `J` ou `Clique Esquerdo`). Ao zerar a vida de um inimigo, ele tomba e derruba carne assada com osso no cenário. Coletar a carne recupera **+1 Coração de Vida**, **+50 de Fome** e concede bônus de pontuação.
- **Clima Dinâmico e Impacto Sensorial:** Alternância cíclica entre **Céu Limpo**, **Chuva** (dissolve pegadas e abafa o som) e **Neblina** (reduz os cones de visão em 45%).
- **Persistência de Recordes (`highscore.dat`):** Serialização binária em disco de maior pontuação e tempo recorde de sobrevivência.

---

## 💻 4. Compilação e Execução

### Pré-requisitos
- **Java Development Kit (JDK) 17 ou superior** instalado e configurado nas variáveis de ambiente (`JAVA_HOME`).
- **Apache Maven 3.8+** instalado.

### Executando pelo Terminal / Maven
Clone o repositório e navegue até a pasta raiz:
```bash
git clone https://github.com/CauaCurvelo/jogo.git
cd jogo
mvn clean javafx:run
```

---

## ⌨️ 5. Controles do Usuário

| Comando | Ação |
| :--- | :--- |
| **W, A, S, D** ou **Setas** | Movimentação da raposa em 8 direções |
| **Shift (Segurar)** | Corrida rápida (*Sprint*) — gera maior ruído acústico |
| **Espaço / J / Botão Esquerdo** | Ataque de mordida / patada estilo Zelda |
| **Tecla [H]** ou **[F3]** | Alternar Modo de Depuração da IA (Cones de visão e rotas A*) |
| **Tecla [F11]** | Alternar Modo Tela Cheia (Fullscreen) |
| **Tecla [R]** | Reiniciar a partida e gerar novo mundo procedural |
