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
O **Sentinela: Instinto Selvagem** é um simulador de sobrevivência ecológica em perspectiva *Top-Down 2.5D*, construído em Java 17 com renderização procedural em JavaFX e síntese sonora em tempo real (`javax.sound.sampled`), sem dependência de sprites ou arquivos de áudio externos.

Ao contrário de abordagens clássicas em que o jogador comanda um avatar caçador, a proposta deste projeto inverte os papéis: o usuário assume o controle de uma raposa tentando sobreviver às pressões impostas por um ecossistema vivo com **14 espécies** — entre **presas ágeis** (coelhos, cervos, sapos e pássaros), **rivais** (lobos, javalis e texugos), **ameaça humana** (caçadores com arco e armadilhas de urso + cães farejadores) e **4 chefes territoriais** (Urso Pardo Alfa, Urso Polar Alfa, Pantera Negra e Crocodilo do Rio).

O projeto serve como bancada prática para aplicação e validação em tempo real de **algoritmos fundamentais de Inteligência Artificial**, combinando:
1. **Navegação Ótima:** Algoritmo de busca informada **A\* em 8 direções (heurística Octile)** com bloqueio de corte de quinas, suavização **String-Pulling** e ponderação de custos por terreno ($g(n)$).
2. **Locomoção Orgânica (Steering Behaviors):** Vetores de velocidade com inércia, força de separação entre agentes e deslizamento lateral em obstáculos (*wall-sliding*).
3. **Tomada de Decisão (FSM):** Máquinas de Estados Finitos com **21 estados comportamentais** (`AIState`), incluindo patrulha, suspeita progressiva, rastreamento olfativo, emboscada, fuga de presas e disputas territoriais entre predadores e chefes.
4. **Percepção Multissensorial Simulada:** Visão setorial em cone com traçado de raios (*Raycasting*) e oclusão por grama alta/árvores, acústica com ondas de ruído ao correr e rastreamento olfativo através de pegadas com decaimento temporal.
5. **Mundo Procedural Infinito:** Geração contínua em tempo de execução via ruído fractal multi-oitavas (*fBm*) em chunks dinâmicos com **5 biomas** (Clareira Verde, Mata Fechada, Pântano Sombrio, Serra Granítica e Tundra Nevada).

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
                      │  21 Estados (Patrol ➔ Chase ➔ Combat)  │
                      └──────────────────┬─────────────────────┘
                                         │
                                         ▼
                      ┌────────────────────────────────────────┐
                      │         NAVEGAÇÃO E PATHFINDING        │
                      │    A* 8 Direções (Octile) + Steering   │
                      │  String-Pulling + Custos por Terreno   │
                      └────────────────────────────────────────┘
```

### 2.1 Algoritmo A* em 8 Direções com Heurística Octile e String-Pulling
O planejamento de rotas é operado pela classe `Pathfinder.java`. A função de avaliação é formulada como:
$$f(n) = g(n) + h(n)$$
Onde:
- $g(n)$ é o custo real acumulado da origem até o nó $n$, ponderado pela física do terreno (ex.: lama e água têm custo elevado; movimento diagonal multiplica por $\sqrt{2} \approx 1{,}414$).
- $h(n)$ é a **Distância Octile** (admissível e consistente para movimentação em 8 direções sem corte de quinas), seguida de suavização **String-Pulling** (`smoothPath`) para eliminar trajetórias em zigue-zague e entregar rotas fluidas aos *Steering Behaviors*.

### 2.2 Custos Ponderados de Terreno e Influência no Comportamento da IA
A IA toma decisões ecológicas baseadas nos custos e efeitos dos terrenos (`TileType`):
- **Trilha de Terra (`DIRT_PATH` - Custo 1):** Superfície de menor resistência onde caçadores e patrulhas convergem naturalmente.
- **Grama Pura (`GRASS` - Custo 1):** Piso padrão de deslocamento.
- **Grama Alta (`TALL_GRASS` - Custo 1):** Reduz drasticamente o cone de visão dos inimigos sobre a raposa (fator de camuflagem).
- **Lamaçal (`MUD` - Custo 3):** Penaliza a velocidade e eleva o custo no A\*, fazendo agentes desviarem de atoleiros (exceto variantes adaptadas ao brejo).
- **Água / Rio (`WATER` - Custo 4) e Gelo (`ICE`):** Nadar reduz a velocidade e **corta o rastro de cheiro** seguido pelos cães farejadores.
- **Árvores e Paredões (`TREE` / `WALL` - Obstáculo):** Bloqueiam passagem e a linha de visão do *Raycasting*.

### 2.3 Máquina de Estados Finitos (21 Estados em `AIState`)
Cada criatura é governada por estados comportamentais especializados:
- **Percepção & Alerta:** `PATROL`, `SUSPICIOUS` (0%–100%), `ALERT`, `SNIFFING_TRACK` (Cão farejando pegadas).
- **Caça & Combate:** `CHASE`, `AIMING` / `KITING` (Caçador com arco e recuo tático), `MELEE_ATTACK`, `CHARGING` (Investida de javali), `STALKING` (Espreita da pantera), `TERRITORIAL_ATTACK`, `FIGHTING_ENEMY` (Disputas entre feras e chefes), `ROARING` (Fúria de chefe), `HUNTING_PREY`.
- **Ecossistema & Sobrevivência:** `SEARCHING_FOOD`, `EATING`, `DRINKING`, `SATIATED`, `RESTING`, `RETREATING` e `TRAPPED` (Preso em armadilha de urso).

---

## 🎮 3. Mecânicas de Jogo e Dinâmica de Simulação

- **Saúde & Vida Baixa:** Corações visuais discretos com invulnerabilidade temporária ao sofrer dano. Com apenas **1 coração**, a velocidade é reduzida e uma aura carmesim pulsa no personagem e nas bordas da tela.
- **Fome Metabólica & Stamina:** Correr (*Sprint*) drena stamina e ataques primários consomem **15%** de energia (entrando em exaustão ao zerar). Se a nutrição cair para **≤ 25%**, o sprint é bloqueado até a raposa se alimentar.
- **Combate, Presas & Tocas:** A raposa pode morder (`Espaço`, `J` ou `Clique Esquerdo`), caçar presas silvestres ágeis (coelhos, cervos, sapos e pássaros), coletar frutas regionais e se esconder/recuperar em **tocas**. Caçadores arremessam **armadilhas de urso** no chão que prendem qualquer criatura desatenta.
- **Clima Dinâmico & Ciclo Solar/Lunar:** Alternância entre **Céu Limpo**, **Chuva**, **Neblina** e **Tempestade com Raios**, além de transição contínua de dia, pôr do sol e noite com halo focal ao redor da raposa.
- **Bestiário, Conquistas & Áudio Procedural:** Diário de campo animado das 14 espécies, histórico em CSV, conquistas até a platina *Espírito da Floresta* e síntese sonora multicanal ao vivo.

---

## 💻 4. Compilação e Execução

### Pré-requisitos
- **Java Development Kit (JDK) 17 ou superior** instalado e configurado (`JAVA_HOME`).
- **Apache Maven 3.8+** instalado.

### Executando pelo Terminal / Maven
```bash
git clone https://github.com/CauaCurvelo/Projeto-de-Inteligencia-Artificial..git
cd Projeto-de-Inteligencia-Artificial.
mvn clean javafx:run
```
Ou execute diretamente o pacote standalone em `dist/InstintoSelvagem.exe` (Windows).

---

## ⌨️ 5. Controles do Usuário

| Comando | Ação |
| :--- | :--- |
| **W, A, S, D** ou **Setas** | Movimentação da raposa em 8 direções |
| **Shift (Segurar)** | Corrida rápida (*Sprint*) — consome stamina e gera ruído acústico |
| **Espaço / J / Botão Esquerdo** | Ataque de mordida (custo de 15% de stamina) |
| **ESC** | Menu de Pausa (Configurações, Bestiário e Conquistas) |
| **+ / -** | Ajustar Zoom da câmera |
| **Tecla [F11]** | Alternar Modo Tela Cheia (Fullscreen) |
| **Tecla [R]** (no Game Over) | Reiniciar a partida e gerar novo mundo procedural |

> **Modo Demonstração / Inspeção de IA (`H` ou `F3`):** Atalho exclusivo para apresentação acadêmica que exibe em tempo real os cones de visão (*Raycasting*), rotas A\* em 8 direções, vetores de *Steering*, tags FSM, cadeia olfativa, sensores de presas e sonar acústico.
