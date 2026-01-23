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
    private JButton btnToggleMenu, btnToggleFeedback;

    // Paneles y etiquetas
    private JLabel lblEstado, lblUsuario, lblConectados;
    private JPanel panelEntrada, panelBotones, panelInfo, panelChat, panelMenu;
    private JPanel panelIzquierdo, panelDerecho;
    private JPanel panelBotonMenu, panelBotonFeedback; // Paneles flotantes para botones
    private JPanel areaFeedbackPanel; // Panel para mostrar mensajes de feedback
    private JPanel panelPrincipal; // Panel principal para cambio de tema
    private JButton btnSol; // Botón para cambiar tema
    private boolean menuVisible = false;
    private boolean feedbackVisible = false;
    private boolean isDarkMode = false; // Estado del tema
    private Timer animacionTimer;
    private int anchoMenu = 140;
    private int anchoFeedback = 280;
    private int anchoActual = 0;
    private int anchoFeedbackActual = 0;

    // Paleta de Colores para Modo Oscuro
    private final Color DARK_BG = new Color(24, 24, 24);          // Fondo principal (menos puro)
    private final Color DARK_ACCENT = new Color(33, 33, 33);      // Paneles secundarios
    private final Color DARK_CHAT_BG = new Color(18, 18, 18);     // Área de chat (más profunda)
    private final Color DARK_TEXT = new Color(225, 225, 225);     // Texto principal
    private final Color DARK_TEXT_MUTED = new Color(150, 150, 150); // Texto secundario
    private final Color DARK_BORDER = new Color(45, 45, 45);      // Bordes sutiles
    private final Color DARK_BLUE_BUBBLE = new Color(10, 100, 200); // Azul más suave para el ojo

    // Paleta de Colores para Modo Claro
    private final Color LIGHT_BG = Color.WHITE;
    private final Color LIGHT_ACCENT = new Color(245, 245, 245);
    private final Color LIGHT_TEXT = Color.BLACK;
    private final Color LIGHT_BORDER = Color.LIGHT_GRAY;

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
        panelPrincipal = new JPanel(new BorderLayout(0, 10));
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
        btnSol = new JButton(createSunIcon(Color.ORANGE));
        btnSol.setPreferredSize(new Dimension(25, 25));
        btnSol.setContentAreaFilled(false);
        btnSol.setBorderPainted(false);
        btnSol.setFocusPainted(false);
        btnSol.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSol.setToolTipText("Cambiar a Tema Claro");

        // Añadir acción para cambiar tema
        btnSol.addActionListener(e -> toggleTheme());

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

        // ========== PANEL DERECHO (FEEDBACK) ==========
        panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(240, 240, 240));
        panelDerecho.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(200, 200, 200)));

        // --- CABECERA DEL PANEL DE FEEDBACK ---
        JPanel panelCabeceraFeedback = new JPanel(new BorderLayout());
        panelCabeceraFeedback.setOpaque(false);
        panelCabeceraFeedback.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panelCabeceraFeedback.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel lblFeedback = new JLabel("FEEDBACK");
        lblFeedback.setFont(new Font("Arial", Font.BOLD, 12));
        lblFeedback.setForeground(new Color(50, 50, 50));

        // Botón de Ocultar/Mostrar Feedback
        btnToggleFeedback = new JButton("◀");
        btnToggleFeedback.setPreferredSize(new Dimension(25, 25));
        btnToggleFeedback.setFont(new Font("Arial", Font.BOLD, 16));
        btnToggleFeedback.setBackground(new Color(70, 130, 180));
        btnToggleFeedback.setForeground(Color.WHITE);
        btnToggleFeedback.setFocusPainted(false);
        btnToggleFeedback.setBorderPainted(false);
        btnToggleFeedback.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleFeedback.setToolTipText("Mostrar feedback");
        btnToggleFeedback.setBorder(BorderFactory.createEmptyBorder());

        // Efecto hover
        btnToggleFeedback.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnToggleFeedback.setBackground(new Color(90, 150, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnToggleFeedback.setBackground(new Color(70, 130, 180));
            }
        });

        btnToggleFeedback.addActionListener(e -> toggleFeedback());

        // Añadir al panel de cabecera
        panelCabeceraFeedback.add(lblFeedback, BorderLayout.WEST);
        panelCabeceraFeedback.add(btnToggleFeedback, BorderLayout.EAST);

        // --- ÁREA DE MENSAJES DE FEEDBACK ---
        areaFeedbackPanel = new JPanel();
        areaFeedbackPanel.setLayout(new BoxLayout(areaFeedbackPanel, BoxLayout.Y_AXIS));
        areaFeedbackPanel.setBackground(new Color(255, 255, 255)); // Blanco
        areaFeedbackPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        areaFeedbackPanel.add(Box.createVerticalGlue());

        // Scroll para el área de feedback
        JScrollPane scrollFeedback = new JScrollPane(areaFeedbackPanel);
        scrollFeedback.setBackground(new Color(255, 255, 255));
        scrollFeedback.getViewport().setBackground(new Color(255, 255, 255));
        scrollFeedback.setBorder(BorderFactory.createEmptyBorder());

        panelDerecho.add(panelCabeceraFeedback, BorderLayout.NORTH);
        panelDerecho.add(scrollFeedback, BorderLayout.CENTER);
        panelDerecho.setPreferredSize(new Dimension(0, 0));
        panelDerecho.setVisible(true); // Visible pero con ancho 0

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
        panelConMenu.add(panelDerecho, BorderLayout.EAST);

        panelPrincipal.add(panelConMenu, BorderLayout.CENTER);

        // Añadir el botón flotante encima del contenido
        panelPrincipal.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                posicionarBotonFlotante();
            }
        });

        // Crear un Glass Pane para los botones flotantes
        JPanel glassPane = new JPanel(null);
        glassPane.setOpaque(false);
        glassPane.add(btnToggleMenu);

        // Agregar botón flotante para feedback
        JButton btnToggleFeedbackFlotante = new JButton("◀");
        btnToggleFeedbackFlotante.setPreferredSize(new Dimension(30, 30));
        btnToggleFeedbackFlotante.setFont(new Font("Arial", Font.BOLD, 16));
        btnToggleFeedbackFlotante.setBackground(new Color(70, 130, 180));
        btnToggleFeedbackFlotante.setForeground(Color.WHITE);
        btnToggleFeedbackFlotante.setFocusPainted(false);
        btnToggleFeedbackFlotante.setBorderPainted(false);
        btnToggleFeedbackFlotante.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleFeedbackFlotante.setToolTipText("Mostrar feedback");
        btnToggleFeedbackFlotante.setBorder(BorderFactory.createEmptyBorder());

        // Efecto hover para botón flotante feedback
        btnToggleFeedbackFlotante.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnToggleFeedbackFlotante.setBackground(new Color(90, 150, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnToggleFeedbackFlotante.setBackground(new Color(70, 130, 180));
            }
        });

        btnToggleFeedbackFlotante.addActionListener(e -> {
            toggleFeedback();
            // Actualizar el texto del botón flotante
            if (feedbackVisible) {
                btnToggleFeedbackFlotante.setText("▶");
                btnToggleFeedbackFlotante.setToolTipText("Ocultar feedback");
            } else {
                btnToggleFeedbackFlotante.setText("◀");
                btnToggleFeedbackFlotante.setToolTipText("Mostrar feedback");
            }
        });

        glassPane.add(btnToggleFeedbackFlotante);
        setGlassPane(glassPane);
        getGlassPane().setVisible(true);

        setContentPane(panelPrincipal);

        // Posicionar ambos botones después de que todo esté listo
        SwingUtilities.invokeLater(() -> posicionarBotonesFlotantes(btnToggleFeedbackFlotante));
    }

    private void posicionarBotonFlotante() {
        int margen = 15;
        int x = anchoActual + margen;
        int y = 15;
        btnToggleMenu.setBounds(x, y, 45, 45);
    }

    private void posicionarBotonesFlotantes(JButton btnToggleFeedbackFlotante) {
        int margen = 15;
        int x = anchoActual + margen;
        int y = 15;
        btnToggleMenu.setBounds(x, y, 45, 45);

        // Posicionar botón de feedback en esquina superior derecha
        int xDerecha = getWidth() - anchoFeedbackActual - 60;
        btnToggleFeedbackFlotante.setBounds(xDerecha, y, 45, 45);
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

    private void toggleFeedback() {
        feedbackVisible = !feedbackVisible;

        if (animacionTimer != null && animacionTimer.isRunning()) {
            animacionTimer.stop();
        }

        int destino = feedbackVisible ? anchoFeedback : 0;
        int paso = 20;

        animacionTimer = new Timer(15, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (feedbackVisible) {
                    anchoFeedbackActual = Math.min(anchoFeedbackActual + paso, destino);
                } else {
                    anchoFeedbackActual = Math.max(anchoFeedbackActual - paso, destino);
                }

                panelDerecho.setPreferredSize(new Dimension(anchoFeedbackActual, 0));
                panelDerecho.revalidate();

                if (anchoFeedbackActual == destino) {
                    animacionTimer.stop();
                    if (feedbackVisible) {
                        btnToggleFeedback.setText("▶");
                        btnToggleFeedback.setToolTipText("Ocultar feedback");
                    } else {
                        btnToggleFeedback.setText("◀");
                        btnToggleFeedback.setToolTipText("Mostrar feedback");
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
        areaChatPanel.add(Box.createVerticalGlue());
        areaChatPanel.revalidate();
        areaChatPanel.repaint();
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
    }

    /**
     * Aplica el tema claro a toda la aplicación
     */
    private void applyLightTheme() {
        Color bgPrimary = LIGHT_BG;
        Color bgSecondary = LIGHT_ACCENT;

        // 1. Paneles Principales
        panelPrincipal.setBackground(bgPrimary);
        panelChat.setBackground(bgPrimary);
        panelInfo.setBackground(bgPrimary);
        panelEntrada.setBackground(bgPrimary);
        panelBotones.setBackground(bgPrimary);

        // 2. Paneles Laterales (Menu y Feedback)
        panelIzquierdo.setBackground(new Color(45, 45, 48)); // El menú se mantiene oscuro por diseño
        panelDerecho.setBackground(bgSecondary);
        panelDerecho.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, LIGHT_BORDER));
        areaFeedbackPanel.setBackground(Color.WHITE);

        // 3. Área de Chat y Scroll
        areaChatPanel.setBackground(bgSecondary);
        JScrollPane scrollChat = (JScrollPane) areaChatPanel.getParent().getParent();
        if (scrollChat != null) {
            scrollChat.getViewport().setBackground(bgSecondary);
            scrollChat.getVerticalScrollBar().setBackground(bgPrimary);
            scrollChat.getHorizontalScrollBar().setBackground(bgPrimary);
        }

        // 4. Componentes de Entrada
        campoEntrada.setBackground(Color.WHITE);
        campoEntrada.setForeground(LIGHT_TEXT);
        campoEntrada.setCaretColor(LIGHT_TEXT);
        campoEntrada.setBorder(BorderFactory.createLineBorder(LIGHT_BORDER));

        // 5. Etiquetas
        lblEstado.setForeground(Color.RED);
        lblUsuario.setForeground(LIGHT_TEXT);
        lblConectados.setForeground(LIGHT_TEXT);

        // Cambiar icono a sol (tema claro)
        btnSol.setIcon(createSunIcon(Color.ORANGE));
        btnSol.setToolTipText("Cambiar a Tema Oscuro");

        // Refrescar toda la interfaz
        refreshUI();
    }

    /**
     * Aplica el tema oscuro a toda la aplicación con soporte para bocadillos dinámicos
     */
    private void applyDarkTheme() {
        // 1. Paneles Principales y Contenedores
        panelPrincipal.setBackground(DARK_BG);
        panelChat.setBackground(DARK_CHAT_BG);
        panelInfo.setBackground(DARK_BG);
        panelEntrada.setBackground(DARK_BG);
        panelBotones.setBackground(DARK_BG);

        // IMPORTANTE: Actualizar el fondo del panel que contiene los mensajes
        areaChatPanel.setBackground(DARK_CHAT_BG);

        // 2. Paneles Laterales
        panelIzquierdo.setBackground(DARK_ACCENT);
        panelMenu.setBackground(DARK_ACCENT);
        panelDerecho.setBackground(DARK_ACCENT);
        panelDerecho.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DARK_BORDER));
        areaFeedbackPanel.setBackground(DARK_ACCENT);

        // 3. ScrollBars - CRÍTICO para evitar parches blancos
        JScrollPane scrollChat = (JScrollPane) areaChatPanel.getParent().getParent();
        if (scrollChat != null) {
            scrollChat.setBackground(DARK_CHAT_BG);
            scrollChat.getViewport().setBackground(DARK_CHAT_BG);
            scrollChat.setBorder(null);
            scrollChat.getVerticalScrollBar().setBackground(DARK_ACCENT);
            scrollChat.getHorizontalScrollBar().setBackground(DARK_ACCENT);
        }

        // 4. Componentes de Entrada
        campoEntrada.setBackground(new Color(45, 45, 45));
        campoEntrada.setForeground(DARK_TEXT);
        campoEntrada.setCaretColor(Color.WHITE);
        campoEntrada.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DARK_BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 5. Etiquetas de estado
        lblEstado.setForeground(new Color(129, 199, 132)); // Verde pastel
        lblUsuario.setForeground(DARK_TEXT);
        lblConectados.setForeground(DARK_TEXT_MUTED);

        // CRÍTICO: Asegurar que las etiquetas no tengan fondos opacos
        lblEstado.setOpaque(false);
        lblUsuario.setOpaque(false);
        lblConectados.setOpaque(false);

        // 6. Actualizar todos los paneles hijos recursivamente para modo oscuro
        updateComponentsInDarkTheme(this);

        // 7. Actualizar Bocadillos y sus Contenedores - CRÍTICO para consistencia
        for (Component c : areaChatPanel.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(DARK_CHAT_BG); // Quita el fondo blanco del contenedor del mensaje
                for (Component sub : ((JPanel) c).getComponents()) {
                    if (sub instanceof BocadilloChat) {
                        ((BocadilloChat) sub).actualizarTema(true);
                    }
                }
            }
        }

        // 8. Actualizar Icono del botón de tema
        btnSol.setIcon(createMoonIcon(new Color(241, 196, 15)));
        btnSol.setToolTipText("Cambiar a Tema Claro");

        // 9. Refrescar toda la interfaz
        refreshUI();
    }

    /**
     * Actualiza recursivamente todos los componentes para modo oscuro
     */
    private void updateComponentsInDarkTheme(Component component) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            // No cambiar panelMenu (que es oscuro por diseño)
            if (panel != panelMenu && panel != panelIzquierdo && panel != panelDerecho) {
                // Verificar si el panel tiene un fondo blanco o gris claro
                if (panel.isOpaque() && (panel.getBackground().equals(Color.WHITE) ||
                    panel.getBackground().equals(new Color(245, 245, 245)) ||
                    panel.getBackground().equals(new Color(240, 240, 240)))) {
                    panel.setBackground(DARK_BG);
                }
            }

            // Recurrir en los componentes hijos
            for (Component child : panel.getComponents()) {
                updateComponentsInDarkTheme(child);
            }
        } else if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            label.setOpaque(false); // Las etiquetas no deben ser opacas
        }
    }

    /**
     * Actualiza y redibuja toda la interfaz de usuario
     * Asegura que todos los componentes hijos se adapten al nuevo esquema de color
     */
    private void refreshUI() {
        SwingUtilities.updateComponentTreeUI(this);
        panelPrincipal.revalidate();
        panelPrincipal.repaint();
    }

    /**
     * Genera un icono de luna mediante dibujo vectorial (Graphics2D)
     */
    private Icon createMoonIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
            public int getIconWidth() {
                return 20;
            }

            @Override
            public int getIconHeight() {
                return 20;
            }
        };
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
            public int getIconWidth() {
                return 20;
            }

            @Override
            public int getIconHeight() {
                return 20;
            }
        };
    }

    // --- GETTERS DE BOTONES ---
    public JButton obtenerBtnEnviar() { return btnEnviar; }
    public JButton obtenerBtnConectar() { return btnConectar; }
    public JButton obtenerBtnDesconectar() { return btnDesconectar; }
    public JButton obtenerBtnList() { return btnComandoList; }
    public JButton obtenerBtnPing() { return btnComandoPing; }
    public JTextField obtenerCampoEntrada() { return campoEntrada; }

    /**
     * Solicita datos del servidor al usuario
     * @return Array con [host, puerto] o null si se cancela
     */
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

    /**
     * Solicita el nombre de usuario
     * @return El nombre de usuario ingresado
     */
    public String solicitarNombreUsuario() {
        return JOptionPane.showInputDialog(this, "Ingrese su nombre de usuario:", "");
    }

    /**
     * Muestra un diálogo de error
     * @param titulo Título del diálogo
     * @param mensaje Mensaje de error
     */
    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Establece el estado de conexión
     * @param conectado true si conectado, false si desconectado
     */
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

    /**
     * Establece el nombre del usuario conectado
     * @param usuario Nombre del usuario
     */
    public void establecerUsuario(String usuario) {
        lblUsuario.setText("Usuario: " + usuario);
    }

    /**
     * Obtiene el texto ingresado en el campo de entrada
     * @return El texto del campo
     */
    public String obtenerTextoEntrada() {
        return campoEntrada.getText();
    }

    /**
     * Limpia el campo de entrada
     */
    public void limpiarEntrada() {
        campoEntrada.setText("");
    }

    /**
     * Muestra un mensaje de sistema en el chat
     * @param mensaje El mensaje a mostrar
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
     * Muestra un mensaje de feedback del servidor en la pestaña derecha
     * @param mensaje El mensaje de feedback a mostrar
     */
    public void mostrarFeedback(String mensaje) {
        // Crear un nuevo panel de mensaje de sistema
        MensajeSistemaPanel mensajePanel = new MensajeSistemaPanel(mensaje);
        mensajePanel.setBackground(new Color(240, 240, 240)); // Fondo gris claro

        // Añadir con un espaciado vertical
        areaFeedbackPanel.add(mensajePanel);
        areaFeedbackPanel.add(Box.createVerticalStrut(10));

        // Refrescar el área de feedback
        areaFeedbackPanel.revalidate();
        areaFeedbackPanel.repaint();

        // Auto-scroll al final
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) areaFeedbackPanel.getParent().getParent();
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
     * Clase para renderizar un bocadillo de chat personalizado
     */
    public static class BocadilloChat extends JPanel {
        private String mensaje;
        private boolean esDerecha;
        private Color colorFondo;
        private Color colorTexto;
        private JLabel labelMensaje;

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
            setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 12));

            // Usar JLabel con HTML para soportar enlaces clicables
            labelMensaje = new JLabel();
            labelMensaje.setForeground(colorTexto);
            labelMensaje.setFont(new Font("Arial", Font.PLAIN, 13));
            labelMensaje.setOpaque(false);

            // Formatear el mensaje con enlaces si contiene URLs
            String mensajeHTML;
            if (UtilURLs.contieneURLs(mensaje)) {
                mensajeHTML = "<html><p style='width: 150px; margin: 0;'>" +
                        UtilURLs.formatearConEnlaces(mensaje) +
                        "</p></html>";
            } else {
                mensajeHTML = "<html><p style='width: 150px; margin: 0;'>" + mensaje + "</p></html>";
            }

            labelMensaje.setText(mensajeHTML);

            // Agregar MouseListener para detectar clicks en URLs
            labelMensaje.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    procesarClickEnURL(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    // Cambiar cursor cuando está sobre un enlace
                    String htmlText = labelMensaje.getText();
                    if (htmlText != null && htmlText.contains("<a href")) {
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });

            add(labelMensaje, BorderLayout.CENTER);
        }

        /**
         * Procesa los clicks en URLs dentro del mensaje
         */
        private void procesarClickEnURL(MouseEvent e) {
            try {
                // Buscar URLs en el mensaje original
                java.util.List<String> urls = UtilURLs.extraerURLs(mensaje);

                // Abrir la primera URL encontrada
                if (!urls.isEmpty()) {
                    UtilURLs.abrirURL(urls.get(0));
                }

            } catch (Exception ex) {
                System.err.println("Error al procesar click: " + ex.getMessage());
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int ancho = getWidth();
            int alto = getHeight();
            int radio = 18;
            int tamCola = 10;

            // Crear camino para el bocadillo
            RoundRectangle2D.Float formaBase;
            Polygon cola = new Polygon();

            g2d.setColor(colorFondo);

            if (esDerecha) {
                // Bocadillo a la derecha con cola a la derecha
                formaBase = new RoundRectangle2D.Float(0, 0, ancho - tamCola, alto, radio, radio);

                // Dibujar el cuerpo redondeado
                g2d.fill(formaBase);

                // Dibujar la cola (derecha abajo)
                int[] xPoints = {ancho - tamCola - 2, ancho, ancho - tamCola + 2};
                int[] yPoints = {alto - 12, alto + 3, alto - 8};
                cola = new Polygon(xPoints, yPoints, 3);
                g2d.fillPolygon(cola);
            } else {
                // Bocadillo a la izquierda con cola a la izquierda
                formaBase = new RoundRectangle2D.Float(tamCola, 0, ancho - tamCola, alto, radio, radio);

                // Dibujar el cuerpo redondeado
                g2d.fill(formaBase);

                // Dibujar la cola (izquierda abajo)
                int[] xPoints = {tamCola + 2, 0, tamCola - 2};
                int[] yPoints = {alto - 12, alto + 3, alto - 8};
                cola = new Polygon(xPoints, yPoints, 3);
                g2d.fillPolygon(cola);
            }

            super.paintComponent(g);
        }

        /**
         * Método para actualizar el tema de un bocadillo existente
         * Se debe llamar a este método para forzar la actualización del color en bocadillos ya creados
         */
        public void actualizarTema() {
            if (esDerecha) {
                this.colorFondo = new Color(0, 132, 255); // Azul
                this.colorTexto = Color.WHITE;
            } else {
                this.colorFondo = new Color(233, 233, 235); // Gris claro
                this.colorTexto = Color.BLACK;
            }

            labelMensaje.setForeground(colorTexto);
            labelMensaje.repaint();
        }

        /**
         * Método para actualizar el tema de un bocadillo existente con soporte para modo oscuro
         * @param esOscuro true para modo oscuro, false para modo claro
         */
        public void actualizarTema(boolean esOscuro) {
            if (esOscuro) {
                // Colores optimizados para modo oscuro (Material Design)
                if (esDerecha) {
                    this.colorFondo = new Color(10, 100, 200); // Azul más suave para el ojo
                    this.colorTexto = Color.WHITE;
                } else {
                    this.colorFondo = new Color(50, 50, 50); // Gris oscuro más suave
                    this.colorTexto = new Color(220, 220, 220); // Texto claro legible
                }
            } else {
                // Colores para modo claro
                if (esDerecha) {
                    this.colorFondo = new Color(0, 132, 255); // Azul
                    this.colorTexto = Color.WHITE;
                } else {
                    this.colorFondo = new Color(233, 233, 235); // Gris claro
                    this.colorTexto = Color.BLACK;
                }
            }

            labelMensaje.setForeground(colorTexto);
            repaint();
        }
    }
}
