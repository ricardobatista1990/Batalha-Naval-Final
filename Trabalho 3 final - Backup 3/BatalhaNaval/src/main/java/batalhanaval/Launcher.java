package batalhanaval;

import batalhanaval.client.ClienteGUI;
import batalhanaval.model.EstadoJogo;
import batalhanaval.server.Servidor;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Launcher gráfico da Batalha Naval.
 * Ponto de entrada único — escolhe se quer ser Servidor ou Cliente.
 */
public class Launcher extends JFrame {

    private static final Color BG       = new Color(10, 28, 58);
    private static final Color ACCENT   = new Color(0, 155, 215);
    private static final Color TEXT     = new Color(215, 230, 255);
    private static final Color TEXT_DIM = new Color(130, 165, 205);
    private static final Color BORDER_C = new Color(35, 75, 135);
    private static final Color SUCCESS  = new Color(55, 195, 95);
    private static final Color DANGER   = new Color(215, 55, 55);
    private static final Color WARN     = new Color(225, 175, 45);

    /** Verdadeiro se já há um servidor a correr nesta instância do Launcher. */
    private static volatile boolean servidorActivo = false;

    public static void main(String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "servidor", "server" -> {
                    try { Servidor.main(new String[0]); }
                    catch (IOException e) { e.printStackTrace(); }
                    return;
                }
                case "cliente", "client" -> {
                    SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
                    return;
                }
            }
        }
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }

    public Launcher() {
        super("Batalha Naval — Launcher");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBackground(BG);
        painel.setBorder(new EmptyBorder(40, 60, 40, 60));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 0, 8, 0);
        c.gridx = 0; c.gridy = 0;

        JLabel titulo = new JLabel("⚓  BATALHA NAVAL", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        titulo.setForeground(ACCENT);
        painel.add(titulo, c);

        c.gridy++;
        JLabel sub = new JLabel("Jogo Multijogador via Rede", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_DIM);
        painel.add(sub, c);

        c.gridy++; c.insets = new Insets(14, 0, 14, 0);
        painel.add(separador(), c);
        c.insets = new Insets(7, 0, 7, 0);

        // Servidor — novo jogo
        c.gridy++;
        JButton btnNovo = criarBotao("🖥  Servidor — Novo Jogo", new Color(0, 100, 160));
        btnNovo.addActionListener(e -> arrancarServidor(new EstadoJogo(), false));
        painel.add(btnNovo, c);

        // Servidor — carregar jogo
        c.gridy++;
        JButton btnCarregar = criarBotao("📂  Servidor — Carregar Jogo Guardado", new Color(0, 75, 120));
        btnCarregar.addActionListener(e -> {
            EstadoJogo estado = escolherJogoGuardado(this);
            if (estado != null) arrancarServidor(estado, true);
        });
        actualizarBotaoCarregar(btnCarregar);
        painel.add(btnCarregar, c);

        c.gridy++; c.insets = new Insets(14, 0, 14, 0);
        painel.add(separador(), c);
        c.insets = new Insets(7, 0, 7, 0);

        // Cliente
        c.gridy++;
        JButton btnCliente = criarBotao("🎮  Entrar como Jogador", new Color(0, 130, 80));
        btnCliente.addActionListener(e -> SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true)));
        painel.add(btnCliente, c);

        // Dois jogadores
        c.gridy++;
        JButton btnDois = criarBotao("👥  Dois Jogadores (mesmo PC)", new Color(100, 60, 150));
        btnDois.addActionListener(e -> {
            if (servidorActivo || portaEmUso(Servidor.PORTA_PADRAO)) {
                // Servidor já a correr — abrir apenas os dois clientes
                SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
                SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
            } else {
                // Arrancar servidor e depois os dois clientes
                arrancarServidor(new EstadoJogo(), false);
                new Timer(900, ev -> {
                    SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
                    SwingUtilities.invokeLater(() -> new ClienteGUI().setVisible(true));
                }) {{ setRepeats(false); start(); }};
            }
        });
        painel.add(btnDois, c);

        c.gridy++; c.insets = new Insets(18, 0, 0, 0);
        JLabel nota = new JLabel(
            "<html><center><small>Para jogar em rede: um PC inicia o Servidor,<br>" +
            "os jogadores entram com o IP do servidor.</small></center></html>",
            SwingConstants.CENTER);
        nota.setForeground(new Color(100, 130, 170));
        nota.setFont(new Font("SansSerif", Font.PLAIN, 11));
        painel.add(nota, c);

        setContentPane(painel);
        pack();
        setLocationRelativeTo(null);
    }

    // ── Arranque do servidor com janela de estado ─────────────────────────────

    private void arrancarServidor(EstadoJogo estado, boolean retomado) {
        // Janela de estado do servidor (fica visível enquanto o servidor corre)
        JDialog dlg = new JDialog(this, "Servidor — Batalha Naval", false);
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlg.setResizable(false);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);

        JLabel icone = new JLabel("🖥", SwingConstants.CENTER);
        icone.setFont(new Font("SansSerif", Font.PLAIN, 36));
        p.add(icone, g);

        g.gridy++;
        JLabel titulo = new JLabel(retomado ? "Jogo Retomado" : "Servidor a Correr", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        titulo.setForeground(retomado ? WARN : SUCCESS);
        p.add(titulo, g);

        g.gridy++;
        JLabel porta = new JLabel("Porta: " + Servidor.PORTA_PADRAO, SwingConstants.CENTER);
        porta.setForeground(TEXT_DIM);
        porta.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.add(porta, g);

        g.gridy++;
        JLabel estadoLbl = new JLabel("⏳  À espera dos jogadores...", SwingConstants.CENTER);
        estadoLbl.setForeground(TEXT);
        estadoLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.add(estadoLbl, g);

        g.gridy++; g.insets = new Insets(16, 0, 6, 0);
        JButton btnFechar = criarBotao("✕  Fechar Servidor", DANGER);
        btnFechar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnFechar.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(dlg,
                "Fechar o servidor?\nSe o jogo estiver em curso, o estado será guardado automaticamente.",
                "Fechar Servidor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION) {
                System.exit(0); // o shutdown hook do GestorJogo guarda o estado
            }
        });
        p.add(btnFechar, g);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);

        // Lançar servidor em thread
        Thread t = new Thread(() -> {
            try {
                servidorActivo = true;
                SwingUtilities.invokeLater(() ->
                    estadoLbl.setText("✅  Servidor activo na porta " + Servidor.PORTA_PADRAO));
                Servidor.iniciarComEstado(estado, Servidor.PORTA_PADRAO);
                servidorActivo = false;
                SwingUtilities.invokeLater(() -> {
                    estadoLbl.setText("Servidor terminou.");
                    estadoLbl.setForeground(TEXT_DIM);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    estadoLbl.setText("Erro: " + ex.getMessage());
                    estadoLbl.setForeground(DANGER);
                    JOptionPane.showMessageDialog(dlg,
                        "Erro ao iniciar servidor:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "Servidor");
        t.setDaemon(false);
        t.start();
    }

    // ── Selecção de jogo guardado ─────────────────────────────────────────────

    private static EstadoJogo escolherJogoGuardado(Component parent) {
        String[] guardados = EstadoJogo.listarGuardados();
        if (guardados.length == 0) {
            JOptionPane.showMessageDialog(parent,
                "Não existem jogos guardados.",
                "Sem jogos guardados", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        String[] nomesCurtos = new String[guardados.length];
        for (int i = 0; i < guardados.length; i++)
            nomesCurtos[i] = guardados[i].replace("saves/", "").replace("saves\\", "");

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(20, 52, 98));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lbl = new JLabel("Seleciona o jogo a retomar:");
        lbl.setForeground(new Color(215, 230, 255));
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        panel.add(lbl, BorderLayout.NORTH);

        JList<String> lista = new JList<>(nomesCurtos);
        lista.setBackground(new Color(10, 28, 58));
        lista.setForeground(new Color(215, 230, 255));
        lista.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lista.setSelectionBackground(new Color(0, 155, 215));
        lista.setSelectionForeground(Color.WHITE);
        lista.setSelectedIndex(guardados.length - 1);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(35, 75, 135)));
        scroll.setPreferredSize(new Dimension(340, Math.min(guardados.length * 22 + 10, 160)));
        panel.add(scroll, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(parent, panel,
            "Carregar Jogo Guardado", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION || lista.getSelectedIndex() < 0) return null;

        try {
            EstadoJogo estado = EstadoJogo.carregar(guardados[lista.getSelectedIndex()]);
            System.out.println("[Launcher] Jogo carregado: " + guardados[lista.getSelectedIndex()]);
            return estado;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                "Erro ao carregar o jogo:\n" + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static void actualizarBotaoCarregar(JButton btn) {
        String[] guardados = EstadoJogo.listarGuardados();
        btn.setEnabled(guardados.length > 0);
        if (guardados.length == 0) {
            btn.setBackground(new Color(40, 60, 80));
            btn.setToolTipText("Sem jogos guardados disponíveis");
        } else {
            btn.setToolTipText(guardados.length + " jogo(s) guardado(s) disponível(eis)");
        }
    }

    // ── UI factory ────────────────────────────────────────────────────────────

    /** Verifica se já há algo a ouvir na porta (outro servidor activo). */
    private static boolean portaEmUso(int porta) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", porta), 300);
            return true; // ligou = porta ocupada
        } catch (Exception e) {
            return false; // não ligou = porta livre
        }
    }


    private JSeparator separador() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER_C);
        return s;
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(cor);
        btn.setForeground(TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(310, 46));
        btn.addMouseListener(new MouseAdapter() {
            final Color original = cor;
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(original.brighter());
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(original);
            }
        });
        return btn;
    }
}
