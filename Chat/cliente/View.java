import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

public class View extends JFrame {
    private static final Logger logger = Logger.getLogger(View.class.getName());
    private static final long serialVersionUID = 1L;

    // Componentes
    private JTextArea areaChat;
    private JTextField campoEntrada;
    private JButton btnEnviar, btnConectar, btnDesconectar, btnComandoList, btnComandoPing, btnLimpiar;

    // Paneles y etiquetas
    private JLabel lblEstado, lblUsuario, lblConectados;
    private JPanel panelEntrada, panelBotones, panelInfo, panelChat;

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
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ========== PANEL DE INFORMACIÓN ==========
        panelInfo = new JPanel(new GridLayout(1, 3, 10, 0));
        panelInfo.setBorder(BorderFactory.createTitledBorder("Estado"));

        lblEstado = new JLabel("Estado: Desconectado");
        lblEstado.setFont(new Font("Arial", Font.BOLD, 12));
        lblUsuario = new JLabel("Usuario: -");
        lblUsuario.setFont(new Font("Arial", Font.BOLD, 12));
        lblConectados = new JLabel("Conectados: 0");
        lblConectados.setFont(new Font("Arial", Font.BOLD, 12));

        panelInfo.add(lblEstado);
        panelInfo.add(lblUsuario);
        panelInfo.add(lblConectados);

        // ========== PANEL DE CHAT ==========
        panelChat = new JPanel(new BorderLayout(5, 5));
        panelChat.setBorder(BorderFactory.createTitledBorder("Chat"));
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Courier New", Font.PLAIN, 12));
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);

        JScrollPane scrollChat = new JScrollPane(areaChat);
        panelChat.add(scrollChat, BorderLayout.CENTER);

        // ========== PANEL DE ENTRADA ==========
        panelEntrada = new JPanel(new BorderLayout(5, 5));
        panelEntrada.setBorder(BorderFactory.createTitledBorder("Mensaje"));

        campoEntrada = new JTextField();
        btnEnviar = new JButton("Enviar");
        // Estilo sólido para botón Enviar
        estilizarBoton(btnEnviar, new Color(70, 130, 180), Color.WHITE);
        btnEnviar.setEnabled(false);

        panelEntrada.add(campoEntrada, BorderLayout.CENTER);
        panelEntrada.add(btnEnviar, BorderLayout.EAST);

        // ========== PANEL DE BOTONES DE CONTROL ==========
        panelBotones = new JPanel(new GridLayout(1, 5, 5, 0));
        panelBotones.setBorder(BorderFactory.createTitledBorder("Controles"));

        btnConectar = new JButton("Conectar");
        estilizarBoton(btnConectar, new Color(46, 204, 113), Color.WHITE); // Verde sólido

        btnDesconectar = new JButton("Desconectar");
        estilizarBoton(btnDesconectar, new Color(231, 76, 60), Color.WHITE); // Rojo sólido
        btnDesconectar.setEnabled(false);

        btnComandoList = new JButton("/list");
        estilizarBoton(btnComandoList, new Color(52, 73, 94), Color.WHITE); // Gris azulado
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

        // ========== ENSAMBLAJE FINAL ==========
        panelPrincipal.add(panelInfo, BorderLayout.NORTH);
        panelPrincipal.add(panelChat, BorderLayout.CENTER);

        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.add(panelEntrada, BorderLayout.NORTH);
        panelInferior.add(panelBotones, BorderLayout.SOUTH);

        panelPrincipal.add(panelInferior, BorderLayout.SOUTH);
        setContentPane(panelPrincipal);
    }

    /**
     * Aplica un diseño de color sólido y plano a un botón.
     */
    private void estilizarBoton(JButton boton, Color fondo, Color texto) {
        boton.setBackground(fondo);
        boton.setForeground(texto);
        boton.setFocusPainted(false); // Quita el recuadro punteado al hacer clic
        boton.setBorderPainted(false); // Quita el borde 3D por defecto
        boton.setContentAreaFilled(true); // Asegura que el color se vea
        boton.setOpaque(true); // Necesario para que el color sea sólido en Mac/Windows
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setFont(new Font("Arial", Font.BOLD, 12));
    }

    private void configurarEventos() {
        campoEntrada.addActionListener(e -> btnEnviar.doClick());
        btnLimpiar.addActionListener(e -> areaChat.setText(""));
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

    /**
     * Muestra un mensaje en el área de chat
     *
     * @param mensaje El mensaje a mostrar
     */
    public void mostrarMensaje(String mensaje) {
        areaChat.append(mensaje + "\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    /**
     * Muestra un diálogo de error
     *
     * @param titulo Título del diálogo
     * @param mensaje Mensaje de error
     */
    public void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Solicita los datos del servidor (host y puerto)
     *
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
     *
     * @return El nombre de usuario ingresado o null si se cancela
     */
    public String solicitarNombreUsuario() {
        String nombre = JOptionPane.showInputDialog(this, "Ingrese su nombre de usuario:", "");
        return nombre;
    }

    /**
     * Establece el nombre del usuario en la interfaz
     *
     * @param usuario El nombre del usuario
     */
    public void establecerUsuario(String usuario) {
        lblUsuario.setText("Usuario: " + usuario);
    }

    /**
     * Obtiene el texto del campo de entrada
     *
     * @return El texto ingresado
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

    // --- GETTERS DE BOTONES ---
    public JButton obtenerBtnEnviar() { return btnEnviar; }
    public JButton obtenerBtnConectar() { return btnConectar; }
    public JButton obtenerBtnDesconectar() { return btnDesconectar; }
    public JButton obtenerBtnList() { return btnComandoList; }
    public JButton obtenerBtnPing() { return btnComandoPing; }
    public JTextField obtenerCampoEntrada() { return campoEntrada; }
}