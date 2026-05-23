package batalhanaval.server;

import batalhanaval.model.*;
import batalhanaval.protocol.Protocolo;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Servidor da Batalha Naval.
 *
 * Uso: java -cp BatalhaNaval-server.jar batalhanaval.server.Servidor [porta]
 *
 * - Aguarda dois clientes
 * - Árbitro de todas as regras
 * - Guarda/carrega estado do jogo
 * - Usa uma thread por cliente para comunicação não bloqueante
 */
public class Servidor {

    public static final int PORTA_PADRAO = 12345;

    /**
     * Ponto de entrada CLI — usa Scanner para perguntar qual jogo carregar.
     * Quando lançado pelo Launcher (GUI), usa iniciarComEstado() diretamente.
     */
    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║    BATALHA NAVAL - SERVIDOR        ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Porta: " + porta);

        EstadoJogo estado = null;

        String[] guardados = EstadoJogo.listarGuardados();
        if (guardados.length > 0) {
            System.out.println("\nJogos guardados disponíveis:");
            for (int i = 0; i < guardados.length; i++)
                System.out.println("  " + (i + 1) + ") " + guardados[i]);
            System.out.print("Carregar jogo? (número ou Enter para novo): ");
            try {
                Scanner sc = new Scanner(System.in);
                String linha = sc.nextLine().trim();
                if (!linha.isEmpty()) {
                    try {
                        int idx = Integer.parseInt(linha) - 1;
                        estado = EstadoJogo.carregar(guardados[idx]);
                        System.out.println("Jogo carregado: " + guardados[idx]);
                    } catch (Exception e) {
                        System.out.println("Erro ao carregar. Novo jogo.");
                    }
                }
            } catch (Exception e) {
                System.out.println("(stdin indisponível — novo jogo)");
            }
        }

        if (estado == null) {
            estado = new EstadoJogo();
            System.out.println("Novo jogo criado.");
        }

        iniciarComEstado(estado, porta);
    }

    /**
     * Inicia o servidor com um estado já decidido (novo ou carregado).
     * Usado pelo Launcher GUI para evitar dependência de Scanner/System.in.
     */
    public static void iniciarComEstado(EstadoJogo estado, int porta) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("\nAguardando jogadores em porta " + porta + "...\n");
            GestorJogo gestor = new GestorJogo(estado, serverSocket);
            gestor.iniciar();
        }
    }
}
