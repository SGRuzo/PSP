import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SocialChatInterface extends JFrame {

    private final Color COLOR_FONDO = new Color(243, 244, 246);
    private final Color COLOR_LATERAL = Color.WHITE;
    private final Color COLOR_TEXTO_SECUNDARIO = new Color(120, 120, 120);
    private final Color COLOR_ACCENTO = new Color(100, 108, 255); // Un azul vibrante tipo red social

    // Variable para rastrear el contacto seleccionado
    private String contactoSeleccionado = "Juan Perez";

    public SocialChatInterface() {
        setTitle("SocialConnect - Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout());

        add(crearSidebar(), BorderLayout.WEST);
        add(crearPanelChatPrincipal(), BorderLayout.CENTER);
        add(crearPanelAmigos(), BorderLayout.EAST);
    }

    private JPanel crearSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(COLOR_LATERAL);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Perfil de Usuario (Arriba)
        JLabel logo = new JLabel(" 👋 Hola, Emilia");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setBorder(new EmptyBorder(30, 25, 30, 10));
        sidebar.add(logo);

        String[] menuItems = {"💬 Mensajes", "👥 Amigos", "🏹 Explorar", "🔔 Notificaciones", "⚙ Configuración"};
        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            btn.setForeground(COLOR_TEXTO_SECUNDARIO);
            btn.setBorder(new EmptyBorder(12, 25, 12, 10));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(240, 45));
            sidebar.add(btn);
        }

        return sidebar;
    }

    private JPanel crearPanelChatPrincipal() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Header de Bienvenida
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 5));
        header.setOpaque(false);
        JLabel lblTitulo = new JLabel("Tus Conversaciones", SwingConstants.LEFT);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 32));

        JLabel lblSubtitulo = new JLabel("Tienes 3 mensajes nuevos sin leer.", SwingConstants.LEFT);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSubtitulo.setForeground(COLOR_TEXTO_SECUNDARIO);

        header.add(lblTitulo);
        header.add(lblSubtitulo);
        panel.add(header, BorderLayout.NORTH);

        // Grid de Contactos Destacados (Fila de sugerencias o chats activos)
        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(30, 0, 30, 0));

        grid.add(crearTarjetaContacto("Juan Perez", "En línea", "🟢", new Color(235, 245, 255)));
        grid.add(crearTarjetaContacto("Maria Garcia", "Escribiendo...", "✍️", new Color(255, 247, 230)));
        grid.add(crearTarjetaContacto("Grupo de Viaje", "12 miembros", "✈️", new Color(235, 250, 235)));
        grid.add(crearTarjetaContacto("Carlos Ruiz", "Hace 5 min", "🕒", new Color(250, 240, 255)));

        panel.add(grid, BorderLayout.CENTER);

        // Barra de Mensaje Rápido
        panel.add(crearBarraChat(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearTarjetaContacto(String nombre, String estado, String avatar, Color color) {
        RoundedPanel tarjeta = new RoundedPanel(20, color);
        tarjeta.setLayout(new BorderLayout(15, 0));
        tarjeta.setBorder(new EmptyBorder(20, 20, 20, 20));
        tarjeta.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Evento de clic para "seleccionar" al contacto
        tarjeta.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                contactoSeleccionado = nombre;
                // Mostrar confirmación de selección
                JOptionPane.showMessageDialog(SocialChatInterface.this,
                    "Has seleccionado chat con " + nombre,
                    "Contacto Seleccionado",
                    JOptionPane.INFORMATION_MESSAGE);
                // Aquí podrías actualizar la barra de chat dinámicamente
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                // Efecto visual al pasar el ratón
                tarjeta.setBorder(new EmptyBorder(18, 18, 18, 18));
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                tarjeta.setBorder(new EmptyBorder(20, 20, 20, 20));
                repaint();
            }
        });

        JLabel lblAvatar = new JLabel(avatar);
        lblAvatar.setFont(new Font("Segoe UI", Font.PLAIN, 30));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel lblEstado = new JLabel(estado);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblEstado.setForeground(COLOR_TEXTO_SECUNDARIO);

        info.add(lblNombre);
        info.add(lblEstado);

        tarjeta.add(lblAvatar, BorderLayout.WEST);
        tarjeta.add(info, BorderLayout.CENTER);

        return tarjeta;
    }

    private JPanel crearBarraChat() {
        RoundedPanel contenedor = new RoundedPanel(25, Color.WHITE);
        contenedor.setLayout(new BorderLayout());
        contenedor.setPreferredSize(new Dimension(0, 70));
        contenedor.setBorder(new EmptyBorder(5, 20, 5, 20));

        JTextField input = new JTextField();
        input.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        input.setBorder(null);

        // Placeholder dinámico que actualiza según el contacto seleccionado
        input.setText("Escribe a " + contactoSeleccionado + "...");
        input.setForeground(Color.GRAY);

        JButton btnEnviar = new JButton("Enviar ➔");
        btnEnviar.setEnabled(false); // Desactivado por defecto
        btnEnviar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEnviar.setForeground(Color.GRAY);
        btnEnviar.setBorderPainted(false);
        btnEnviar.setContentAreaFilled(false);
        btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Escuchador para activar el botón solo si hay texto "real" (válido)
        input.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                boolean tieneTexto = !input.getText().trim().isEmpty()
                    && !input.getText().equals("Escribe a " + contactoSeleccionado + "...");
                btnEnviar.setEnabled(tieneTexto);
                btnEnviar.setForeground(tieneTexto ? COLOR_ACCENTO : Color.GRAY);
            }
        });

        // Acción del botón Enviar
        @SuppressWarnings("Convert2Lambda")
        java.awt.event.ActionListener actionListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(@SuppressWarnings("unused") java.awt.event.ActionEvent evt) {
                String mensaje = input.getText().trim();
                if (!mensaje.isEmpty()) {
                    JOptionPane.showMessageDialog(SocialChatInterface.this,
                        "Mensaje enviado a " + contactoSeleccionado + ":\n" + mensaje,
                        "Mensaje Enviado",
                        JOptionPane.INFORMATION_MESSAGE);
                    input.setText("Escribe a " + contactoSeleccionado + "...");
                    input.setForeground(Color.GRAY);
                    btnEnviar.setEnabled(false);
                    btnEnviar.setForeground(Color.GRAY);
                }
            }
        };
        btnEnviar.addActionListener(actionListener);

        contenedor.add(input, BorderLayout.CENTER);
        contenedor.add(btnEnviar, BorderLayout.EAST);

        return contenedor;
    }

    private JPanel crearPanelAmigos() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_FONDO);
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBorder(new EmptyBorder(30, 10, 20, 25));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Sugerencias de Amigos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(titulo);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        String[] amigos = {"Lucas Silva", "Ana Belén", "Kevin Smith", "Laura M."};
        for (String amigo : amigos) {
            RoundedPanel card = new RoundedPanel(15, Color.WHITE);
            card.setLayout(new BorderLayout(10, 0));
            card.setBorder(new EmptyBorder(10, 15, 10, 15));
            card.setMaximumSize(new Dimension(260, 60));

            JLabel lblAmigo = new JLabel(amigo);
            lblAmigo.setFont(new Font("Segoe UI", Font.BOLD, 13));

            JButton btnAdd = new JButton("Añadir");
            btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnAdd.setForeground(COLOR_ACCENTO);
            btnAdd.setBorder(BorderFactory.createLineBorder(COLOR_ACCENTO));
            btnAdd.setContentAreaFilled(false);

            card.add(lblAmigo, BorderLayout.CENTER);
            card.add(btnAdd, BorderLayout.EAST);

            panel.add(card);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        return panel;
    }

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;

        RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            super.paintComponent(g);
        }
    }

    public static void main(@SuppressWarnings("unused") String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SocialChatInterface().setVisible(true));
    }
}

