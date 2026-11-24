# PetAR

Na aba **Câmera**, toque em **Ativar localização** e conceda acesso à posição precisa. O painel mostra o endereço atual e a pontuação da partida. Se o serviço de endereços estiver indisponível, a tela identifica o endereço aproximado de um dos pontos cadastrados quando o GPS indicar que o jogador está próximo.

## Pontuação

- Ao chegar a até 100 metros de um ponto turístico, o jogo sorteia um modelo 3D. Cada arquivo `.glb` descoberto por GPS vale **100 pontos** uma vez por partida. É possível voltar a ter um encontro depois de sair do raio de 125 metros.
- A escolha manual de modelos na câmera deixa o placar igual. Na aba **Mapa**, cada pet capturado com petisco vale **100 pontos** no mesmo placar.
- Se um pet fugir depois de três tentativas, o **Game Over** encerra a pontuação. **Jogar novamente** zera o placar e libera novas descobertas.

## Pontos de encontro em Ponta Grossa

| Ponto | Latitude | Longitude |
| --- | ---: | ---: |
| Praça Marechal Floriano Peixoto, junto à Catedral Sant’Ana | -25.097832 | -50.158968 |
| Praça Barão de Guaraúna, junto à Igreja dos Polacos | -25.094880 | -50.162660 |
| Centro de Visitantes do Parque Estadual de Vila Velha | -25.246332 | -50.021038 |
| Mansão Vila Hilda | -25.091750 | -50.162030 |
| Cine-Teatro Ópera | -25.096472 | -50.160139 |

Para testar no emulador, ative a localização no jogo e simule a posição da Mansão Vila Hilda com `adb emu geo fix -50.162030 -25.091750`. O comando recebe a longitude antes da latitude.

