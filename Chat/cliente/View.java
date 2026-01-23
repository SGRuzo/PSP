import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.logging.Logger;

public class View extends JFrame {
    private static final Logger logger = Logger.getLogger(View.class.getName());
    private static final long serialVersionUID = 1L;

    // Componentes
    private JPanel areaChatPanel;
    private JTextField campoEntrada;
    private JButton btnEnviar, btnConectar, btnDesconectar, btnComandoList, btnComandoPing, btnLimpiar;
    private JButton btnToggleMenu;

    // Paneles y etiquetas
    private JLabel lblEstado, lblUsuario, lblConectados;
    private JPanel panelEntrada, panelBotones, panelInfo, panelChat, panelMenu;
    private JPanel panelIzquierdo;
    private JPanel panelBotonMenu; // Panel flotante para el botón
    private boolean menuVisible = false;
    private Timer animacionTimer;
    private int anchoMenu = 140;
    private int anchoActual = 0;

    public View() {
        inicializarVentana();
        crearComponentes();
        configurarEventos();
    }

    private void inicializarVentana() {
        setTitle("Cliente de Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
    }

    private void crearComponentes() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelPrincipal.setBackground(Color.WHITE);

        // ========== PANEL DE INFORMACIÓN ==========
        panelInfo = new JPanel(new GridLayout(1, 3, 10, 0));
        panelInfo.setBackground(Color.WHITE);
        panelInfo.setBorder(BorderFactory.createEmptyBorder(10, 70, 10, 10));

        lblEstado = new JLabel("Estado: Desconectado");
        lblEstado.setFont(new Font("Arial", Font.BOLD, 12));
        lblUsuario = new JLabel("Usuario: -");
        lblUsuario.setFont(new Font("Arial", Font.BOLD, 12));
        lblConectados = new JLabel("Conectados: 0");
        lblConectados.setFont(new Font("Arial", Font.BOLD, 12));

        // Opción 1: Agregar padding al label
        lblEstado.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 50));

        // Opción 2: Usar un layout que respete los espacios
        JPanel estadoPanel = new JPanel(new BorderLayout());
        estadoPanel.setBackground(Color.WHITE);
        estadoPanel.add(lblEstado, BorderLayout.WEST);
        // estadoPanel.add(menuButton, BorderLayout.EAST); // Descomenta si necesitas un botón en el lado derecho

        // Opción 3: Establecer un ancho mínimo preferido
        lblEstado.setPreferredSize(new Dimension(200, 30));

        panelInfo.add(estadoPanel);
        panelInfo.add(lblUsuario);
        panelInfo.add(lblConectados);

        // ========== PANEL DE CHAT ==========
        panelChat = new JPanel(new BorderLayout(5, 5));
        panelChat.setBackground(Color.WHITE);

        // Crear panel personalizado para los mensajes
        areaChatPanel = new JPanel();
        areaChatPanel.setLayout(new BoxLayout(areaChatPanel, BoxLayout.Y_AXIS));
        areaChatPanel.setBackground(new Color(245, 245, 245)); // Gris muy suave tipo WhatsApp/iMessage
        areaChatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        areaChatPanel.add(Box.createVerticalGlue());

        // Scroll para el panel de chat
        JScrollPane scrollChat = new JScrollPane(areaChatPanel);
        scrollChat.setBackground(new Color(245, 245, 245));
        scrollChat.getViewport().setBackground(new Color(245, 245, 245));
        scrollChat.setBorder(BorderFactory.createEmptyBorder());

        panelChat.add(scrollChat, BorderLayout.CENTER);

        // ========== PANEL DE ENTRADA ==========
        panelEntrada = new JPanel(new BorderLayout(5, 5));
        panelEntrada.setBackground(Color.WHITE);

        campoEntrada = new JTextField();
        btnEnviar = new JButton("Enviar");
        estilizarBoton(btnEnviar, new Color(70, 130, 180), Color.WHITE);
        btnEnviar.setEnabled(false);

        panelEntrada.add(campoEntrada, BorderLayout.CENTER);
        panelEntrada.add(btnEnviar, BorderLayout.EAST);

        // ========== PANEL DE BOTONES DE CONTROL ==========
        panelBotones = new JPanel(new GridLayout(1, 5, 5, 0));
        panelBotones.setBackground(Color.WHITE);

        btnConectar = new JButton("Conectar");
        estilizarBoton(btnConectar, new Color(46, 204, 113), Color.WHITE);

        btnDesconectar = new JButton("Desconectar");
        estilizarBoton(btnDesconectar, new Color(231, 76, 60), Color.WHITE);
        btnDesconectar.setEnabled(false);

        btnComandoList = new JButton("/list");
        estilizarBoton(btnComandoList, new Color(52, 73, 94), Color.WHITE);
        btnComandoList.setEnabled(false);

        btnComandoPing = new JButton("/ping");
        estilizarBoton(btnComandoPing, new Color(52, 73, 94), Color.WHITE);
        btnComandoPing.setEnabled(false);

        btnLimpiar = new JButton("Limpiar");
        estilizarBoton(btnLimpiar, new Color(149, 165, 166), Color.WHITE);

        panelBotones.add(btnConectar);
        panelBotones.add(btnDesconectar);
        panelBotones.add(btnComandoList);
        panelBotones.add(btnComandoPing);
        panelBotones.add(btnLimpiar);

        // ========== PANEL LATERAL (MENU) ==========
        panelMenu = new JPanel();
        panelMenu.setLayout(new BoxLayout(panelMenu, BoxLayout.Y_AXIS));
        panelMenu.setBackground(new Color(45, 45, 48));
        panelMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 2, new Color(30, 30, 32)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
        ));

        // --- NUEVA CABECERA DEL MENÚ (Título + Sol) ---
        JPanel panelCabeceraMenu = new JPanel(new BorderLayout());
        panelCabeceraMenu.setOpaque(false);
        panelCabeceraMenu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panelCabeceraMenu.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel lblTitulo = new JLabel("MENÚ");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setForeground(new Color(200, 200, 200));

        // Botón de Sol (Tema Claro)
        JButton btnSol = new JButton(createSunIcon(Color.ORANGE));
        btnSol.setPreferredSize(new Dimension(25, 25));
        btnSol.setContentAreaFilled(false);
        btnSol.setBorderPainted(false);
        btnSol.setFocusPainted(false);
        btnSol.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSol.setToolTipText("Cambiar a Tema Claro");

        // Añadir al panel de cabecera
        panelCabeceraMenu.add(lblTitulo, BorderLayout.WEST);
        panelCabeceraMenu.add(btnSol, BorderLayout.EAST);

        // Añadir cabecera al menú principal
        panelMenu.add(panelCabeceraMenu);
        panelMenu.add(Box.createVerticalStrut(10));

        // --- RESTO DE BOTONES (Igual que antes) ---
        JButton btnOption1 = new JButton("Opción 1");
        estilizarBotonMenu(btnOption1);
        JButton btnOption2 = new JButton("Opción 2");
        estilizarBotonMenu(btnOption2);
        JButton btnOption3 = new JButton("Configuración");
        estilizarBotonMenu(btnOption3);

        // Envolver botones en paneles con centrado
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel1.setOpaque(false);
        panel1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        panel1.add(btnOption1);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel2.setOpaque(false);
        panel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        panel2.add(btnOption2);

        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel3.setOpaque(false);
        panel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        panel3.add(btnOption3);

        panelMenu.add(panel1);
        panelMenu.add(Box.createVerticalStrut(10));
        panelMenu.add(panel2);
        panelMenu.add(Box.createVerticalStrut(10));
        panelMenu.add(panel3);
        panelMenu.add(Box.createVerticalGlue());

        // Panel izquierdo contenedor
        panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(new Color(45, 45, 48));
        panelIzquierdo.add(panelMenu, BorderLayout.CENTER);
        panelIzquierdo.setPreferredSize(new Dimension(0, 0));
        panelIzquierdo.setVisible(true); // Visible pero con ancho 0

        // ========== BOTÓN DE MENÚ FLOTANTE ==========
        btnToggleMenu = new JButton("☰");
        btnToggleMenu.setPreferredSize(new Dimension(30, 30));
        btnToggleMenu.setFont(new Font("Arial", Font.BOLD, 16));
        btnToggleMenu.setBackground(new Color(70, 130, 180));
        btnToggleMenu.setForeground(Color.WHITE);
        btnToggleMenu.setFocusPainted(false);
        btnToggleMenu.setBorderPainted(false);
        btnToggleMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleMenu.setToolTipText("Mostrar menú");
        btnToggleMenu.setBorder(BorderFactory.createEmptyBorder());

        // Efecto hover
        btnToggleMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnToggleMenu.setBackground(new Color(90, 150, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnToggleMenu.setBackground(new Color(70, 130, 180));
            }
        });

        btnToggleMenu.addActionListener(e -> toggleMenu());

        // Panel contenedor del botón (flotante)
        panelBotonMenu = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelBotonMenu.setBackground(new Color(255, 255, 255, 0)); // Transparente
        panelBotonMenu.add(btnToggleMenu);

        // ========== ENSAMBLAJE FINAL ==========
        JPanel panelContenido = new JPanel(new BorderLayout(0, 0));
        panelContenido.setBackground(Color.WHITE);

        // Panel superior solo con info (botón flotante aparte)
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(Color.WHITE);
        panelSuperior.add(panelInfo, BorderLayout.CENTER);

        panelContenido.add(panelSuperior, BorderLayout.NORTH);
        panelContenido.add(panelChat, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.add(panelEntrada, BorderLayout.NORTH);
        panelInferior.add(panelBotones, BorderLayout.SOUTH);

        panelContenido.add(panelInferior, BorderLayout.SOUTH);

        // Crear un panel con LayeredPane para el botón flotante
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        // Panel principal con menú lateral
        JPanel panelConMenu = new JPanel(new BorderLayout(0, 0));
        panelConMenu.add(panelIzquierdo, BorderLayout.WEST);
        panelConMenu.add(panelContenido, BorderLayout.CENTER);

        panelPrincipal.add(panelConMenu, BorderLayout.CENTER);

        // Añadir el botón flotante encima del contenido
        panelPrincipal.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                posicionarBotonFlotante();
            }
        });

        // Crear un Glass Pane para el botón flotante
        JPanel glassPane = new JPanel(null);
        glassPane.setOpaque(false);
        glassPane.add(btnToggleMenu);
        setGlassPane(glassPane);
        getGlassPane().setVisible(true);

        setContentPane(panelPrincipal);

        // Posicionar el botón después de que todo esté listo
        SwingUtilities.invokeLater(this::posicionarBotonFlotante);
    }

    private void posicionarBotonFlotante() {
        int margen = 15;
        int x = anchoActual + margen;
        int y = 15;
        btnToggleMenu.setBounds(x, y, 45, 45);
    }

    private void toggleMenu() {
        menuVisible = !menuVisible;

        if (animacionTimer != null && animacionTimer.isRunning()) {
            animacionTimer.stop();
        }

        int destino = menuVisible ? anchoMenu : 0;
        int paso = 20;

        animacionTimer = new Timer(15, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (menuVisible) {
                    anchoActual = Math.min(anchoActual + paso, destino);
                } else {
                    anchoActual = Math.max(anchoActual - paso, destino);
                }

                panelIzquierdo.setPreferredSize(new Dimension(anchoActual, 0));
                panelIzquierdo.revalidate();
                posicionarBotonFlotante();

                if (anchoActual == destino) {
                    animacionTimer.stop();
                    if (menuVisible) {
                        btnToggleMenu.setText("✕");
                        btnToggleMenu.setToolTipText("Ocultar menú");
                    } else {
                        btnToggleMenu.setText("☰");
                        btnToggleMenu.setToolTipText("Mostrar menú");
                    }
                }
            }
        });

        animacionTimer.start();
    }

    private void estilizarBoton(JButton boton, Color fondo, Color texto) {
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 5, true));
        boton.setContentAreaFilled(true);
        boton.setOpaque(true);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    private void estilizarBotonMenu(JButton boton) {
        boton.setMaximumSize(new Dimension(130, 45));
        boton.setPreferredSize(new Dimension(130, 45));
        boton.setBackground(new Color(60, 60, 64));
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setContentAreaFilled(true);
        boton.setOpaque(true);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setFont(new Font("Arial", Font.PLAIN, 13));
        boton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Efecto hover
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(new Color(80, 80, 88));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(new Color(60, 60, 64));
            }
        });
    }

    private void configurarEventos() {
        campoEntrada.addActionListener(e -> btnEnviar.doClick());
        btnLimpiar.addActionListener(e -> limpiarChat());
    }

    /**
     * Limpia el área de chat
     */
    private void limpiarChat() {
        areaChatPanel.removeAll();
        areaChatPanel.revalidate();
        areaChatPanel.repaint();
    }

    /**
     * Añade un mensaje de usuario al chat con estilo de bocadillo
     * @param mensaje El texto a mostrar
     * @param esPropio true si lo envías tú (azul, derecha), false si es del otro (gris, izquierda)
     */
    public void mostrarMensaje(String mensaje, boolean esPropio) {
        // Panel contenedor para la alineación (FlowLayout hace el truco)
        JPanel panelAlineacion = new JPanel(new FlowLayout(esPropio ? FlowLayout.RIGHT : FlowLayout.LEFT));
        panelAlineacion.setBackground(areaChatPanel.getBackground());
        panelAlineacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Crear el bocadillo
        BocadilloChat bocadillo = new BocadilloChat(mensaje, esPropio);
        panelAlineacion.add(bocadillo);

        areaChatPanel.add(panelAlineacion);
        areaChatPanel.add(Box.createVerticalStrut(10)); // Espacio entre mensajes

        // Refrescar la interfaz
        areaChatPanel.revalidate();
        areaChatPanel.repaint();

        // Auto-scroll al fondo
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) areaChatPanel.getParent().getParent();
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    /**
     * Añade un mensaje del sistema en un bocadillo
     */
    public void mostrarMensajeSistema(String mensaje) {
        // Crear panel contenedor centrado
        JPanel panelCentrado = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelCentrado.setBackground(new Color(245, 245, 245));
        panelCentrado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        panelCentrado.add(new MensajeSistemaPanel(mensaje));

        areaChatPanel.add(panelCentrado);
        areaChatPanel.add(Box.createVerticalStrut(10));
        areaChatPanel.revalidate();
        areaChatPanel.repaint();

        // Scroll al final
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) areaChatPanel.getParent().getParent();
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    /**
     * Panel personalizado para mostrar un recuadro del sistema
     */
    public static class MensajeSistemaPanel extends JPanel {
        private String mensaje;

        public MensajeSistemaPanel(String mensaje) {
            this.mensaje = mensaje;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 45));
            setMaximumSize(new Dimension(300, 45));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();
            int margenX = 10;
            int margenY = 5;

            // Dibujar fondo del recuadro
            g2d.setColor(new Color(200, 220, 255)); // Azul claro
            g2d.fillRoundRect(margenX, margenY, ancho - 2*margenX, alto - 2*margenY, 10, 10);

            // Dibujar borde del recuadro
            g2d.setColor(new Color(100, 150, 200)); // Azul más oscuro
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(margenX, margenY, ancho - 2*margenX, alto - 2*margenY, 10, 10);

            // Dibujar texto del mensaje centrado
            g2d.setColor(new Color(30, 30, 30));
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            FontMetrics fm = g2d.getFontMetrics();

            // Calcular posición X para centrar horizontalmente
            int anchoTexto = fm.stringWidth(mensaje);
            int xCentrado = (ancho - anchoTexto) / 2;

            // Calcular posición Y para centrar verticalmente
            int y = margenY + ((alto - 2*margenY - fm.getHeight()) / 2) + fm.getAscent();

            g2d.drawString(mensaje, xCentrado, y);
        }
    }

    /**
     * Genera un icono de sol mediante dibujo vectorial (Graphics2D)
     */
    private Icon createSunIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = 20;
                int cx = x + size / 2;
                int cy = y + size / 2;
                int radius = 5;
                
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                // Rayos
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    int x1 = cx + (int) (Math.cos(angle) * (radius + 2));
                    int y1 = cy + (int) (Math.sin(angle) * (radius + 2));
                    int x2 = cx + (int) (Math.cos(angle) * (radius + 5));
                    int y2 = cy + (int) (Math.sin(angle) * (radius + 5));
                    g2d.drawLine(x1, y1, x2, y2);
                }
                // Centro
                g2d.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                g2d.dispose();
            }
            @Override
            public int getIconWidth() { return 20; }
            @Override
            public int getIconHeight() { return 20; }
        };
    }

    // --- MÉTODOS DE ACTUALIZACIÓN DE ESTADO ---
    public void establecerEstado(boolean conectado) {
        if (conectado) {
            lblEstado.setText("Estado: Conectado");
            lblEstado.setForeground(new Color(39, 174, 96));
            campoEntrada.setEnabled(true);
            btnEnviar.setEnabled(true);
            btnConectar.setEnabled(false);
            btnDesconectar.setEnabled(true);
            btnComandoList.setEnabled(true);
            btnComandoPing.setEnabled(true);
        } else {
            lblEstado.setText("Estado: Desconectado");
            lblEstado.setForeground(Color.RED);
            campoEntrada.setEnabled(false);
            btnEnviar.setEnabled(false);
            btnConectar.setEnabled(true);
            btnDesconectar.setEnabled(false);
            btnComandoList.setEnabled(false);
            btnComandoPing.setEnabled(false);
        }
    }


    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    public String[] solicitarDatosServidor() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField fieldHost = new JTextField("localhost", 15);
        JTextField fieldPuerto = new JTextField("5000", 15);
        panel.add(new JLabel("Host:"));
        panel.add(fieldHost);
        panel.add(new JLabel("Puerto:"));
        panel.add(fieldPuerto);
        int resultado = JOptionPane.showConfirmDialog(this, panel, "Datos del Servidor", JOptionPane.OK_CANCEL_OPTION);
        if (resultado == JOptionPane.OK_OPTION) {
            return new String[]{fieldHost.getText(), fieldPuerto.getText()};
        }
        return null;
    }

    public String solicitarNombreUsuario() {
        return JOptionPane.showInputDialog(this, "Ingrese su nombre de usuario:", "");
    }

    public void establecerUsuario(String usuario) {
        lblUsuario.setText("Usuario: " + usuario);
    }

    public String obtenerTextoEntrada() {
        return campoEntrada.getText();
    }

    public void limpiarEntrada() {
        campoEntrada.setText("");
    }

    // --- GETTERS DE BOTONES ---
    public JButton obtenerBtnEnviar() { return btnEnviar; }
    public JButton obtenerBtnConectar() { return btnConectar; }
    public JButton obtenerBtnDesconectar() { return btnDesconectar; }
    public JButton obtenerBtnList() { return btnComandoList; }
    public JButton obtenerBtnPing() { return btnComandoPing; }
    public JTextField obtenerCampoEntrada() { return campoEntrada; }

    /**
     * Clase para renderizar un bocadillo de chat personalizado
     */
    public static class BocadilloChat extends JPanel {
        private String mensaje;
        private boolean esDerecha;
        private Color colorFondo;
        private Color colorTexto;

        public BocadilloChat(String mensaje, boolean esDerecha) {
            this.mensaje = mensaje;
            this.esDerecha = esDerecha;

            // Colores similares a los de la imagen
            if (esDerecha) {
                this.colorFondo = new Color(0, 132, 255); // Azul
                this.colorTexto = Color.WHITE;
            } else {
                this.colorFondo = new Color(233, 233, 235); // Gris claro
                this.colorTexto = Color.BLACK;
            }

            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            // JLabel con soporte para saltos de línea automáticos (HTML)
            JLabel lbl = new JLabel("<html><p style='width: 150px;'>" + mensaje + "</p></html>");
            lbl.setForeground(colorTexto);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            add(lbl, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();
            int radio = 20;
            int tamCola = 10;

            g2d.setColor(colorFondo);

            // Dibujar el cuerpo del bocadillo
            if (esDerecha) {
                // Cuerpo redondeado
                g2d.fillRoundRect(0, 0, ancho - tamCola, alto, radio, radio);
                // Dibujar la cola (derecha abajo)
                int[] xPoints = {ancho - tamCola, ancho, ancho - tamCola};
                int[] yPoints = {alto - 15, alto, alto - 5};
                g2d.fillPolygon(xPoints, yPoints, 3);
            } else {
                // Cuerpo redondeado
                g2d.fillRoundRect(tamCola, 0, ancho - tamCola, alto, radio, radio);
                // Dibujar la cola (izquierda abajo)
                int[] xPoints = {tamCola, 0, tamCola};
                int[] yPoints = {alto - 15, alto, alto - 5};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Panel personalizado para el menú extendido con icono de sol
     */
    public static class ExtendedMenuPanel extends JPanel {
        private JButton sunButton;
        private boolean isDarkMode = false;
        
        public ExtendedMenuPanel() {
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(250, 600));
            
            // Panel superior para el icono del sol
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            topPanel.setOpaque(false);
            
            // Crear botón con icono de sol
            sunButton = new JButton();
            sunButton.setPreferredSize(new Dimension(40, 40));
            sunButton.setContentAreaFilled(false);
            sunButton.setBorderPainted(false);
            sunButton.setFocusPainted(false);
            sunButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            sunButton.setToolTipText("Cambiar tema");
            
            // Añadir acción para cambiar tema
            sunButton.addActionListener(e -> toggleTheme());
            
            topPanel.add(sunButton);
            
            // Panel central para el contenido del menú
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            
            // Añadir opciones del menú (ejemplo)
            addMenuItem(contentPanel, "Nueva conversación");
            addMenuItem(contentPanel, "Historial");
            addMenuItem(contentPanel, "Configuración");
            addMenuItem(contentPanel, "Ayuda");
            
            // Añadir paneles al menú principal
            add(topPanel, BorderLayout.NORTH);
            add(contentPanel, BorderLayout.CENTER);
            
            // Aplicar tema inicial (claro)
            applyLightTheme();
        }
        
        /**
         * Añade un item al menú
         */
        private void addMenuItem(JPanel panel, String text) {
            JButton item = new JButton(text);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.setBorderPainted(false);
            item.setFocusPainted(false);
            item.setHorizontalAlignment(SwingConstants.LEFT);
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            panel.add(item);
            panel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        
        /**
         * Alterna entre tema claro y oscuro
         */
        private void toggleTheme() {
            isDarkMode = !isDarkMode;
            if (isDarkMode) {
                applyDarkTheme();
            } else {
                applyLightTheme();
            }
            repaint();
        }
        
        /**
         * Aplica tema claro
         */
        private void applyLightTheme() {
            setBackground(Color.WHITE);
            sunButton.setIcon(createSunIcon(Color.ORANGE));
            // Actualizar colores de los items del menú
            for (Component comp : getComponents()) {
                if (comp instanceof JPanel) {
                    updatePanelTheme((JPanel) comp, Color.WHITE, Color.BLACK);
                }
            }
        }
        
        /**
         * Aplica tema oscuro
         */
        private void applyDarkTheme() {
            setBackground(new Color(30, 30, 30));
            sunButton.setIcon(createMoonIcon(Color.LIGHT_GRAY));
            // Actualizar colores de los items del menú
            for (Component comp : getComponents()) {
                if (comp instanceof JPanel) {
                    updatePanelTheme((JPanel) comp, new Color(30, 30, 30), Color.WHITE);
                }
            }
        }
        
        /**
         * Actualiza el tema de un panel
         */
        private void updatePanelTheme(JPanel panel, Color bg, Color fg) {
            panel.setBackground(bg);
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JButton) {
                    comp.setBackground(bg);
                    comp.setForeground(fg);
                } else if (comp instanceof JPanel) {
                    updatePanelTheme((JPanel) comp, bg, fg);
                }
            }
        }
        
        /**
         * Crea un icono de sol personalizado
         */
        private Icon createSunIcon(Color color) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int size = 24;
                    int cx = x + size / 2;
                    int cy = y + size / 2;
                    int radius = 6;
                    
                    // Dibujar rayos del sol
                    g2d.setColor(color);
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, 
                                                 BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.PI * 2 * i / 8;
                        int x1 = cx + (int) (Math.cos(angle) * (radius + 2));
                        int y1 = cy + (int) (Math.sin(angle) * (radius + 2));
                        int x2 = cx + (int) (Math.cos(angle) * (radius + 6));
                        int y2 = cy + (int) (Math.sin(angle) * (radius + 6));
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                    
                    // Dibujar círculo central
                    g2d.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                    
                    g2d.dispose();
                }
                
                @Override
                public int getIconWidth() { return 24; }
                
                @Override
                public int getIconHeight() { return 24; }
            };
        }
        
        /**
         * Crea un icono de luna para el modo oscuro
         */
        private Icon createMoonIcon(Color color) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    g2d.setColor(color);
                    
                    // Dibujar luna creciente
                    Ellipse2D moon = new Ellipse2D.Double(x + 4, y + 2, 16, 20);
                    Ellipse2D shadow = new Ellipse2D.Double(x + 8, y + 2, 16, 20);
                    
                    Area moonArea = new Area(moon);
                    moonArea.subtract(new Area(shadow));
                    
                    g2d.fill(moonArea);
                    
                    g2d.dispose();
                }
                
                @Override
                public int getIconWidth() { return 24; }
                
                @Override
                public int getIconHeight() { return 24; }
            };
        }
        
        /**
         * Método de prueba
         */
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Menú con Icono de Sol");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new ExtendedMenuPanel());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        }
    }
}