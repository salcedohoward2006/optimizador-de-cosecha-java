import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class OptimizadorCosecha extends JFrame {
    private static final int MAX_TAREAS = 100;
    private final Modelo modelo = new Modelo();

    // Componentes de formulario
    private final JTextField txtNombre = new JTextField(15);
    private final JTextField txtDuracion = new JTextField(8);
    private final JTextField txtRendimiento = new JTextField(8);
    private final JTextField txtPredecesoras = new JTextField(15);
    private final JTextField txtPlazoMaximo = new JTextField("50.0", 8);

    // visualizacion
    private final JTextArea txtSalidaTexto = new JTextArea();
    private final DefaultTableModel modeloTabla;
    private final JTable tablaResultados;
    private final JLabel lblEstadoPlazo = new JLabel("Ingrese tareas y calcule el proyecto.");

    public OptimizadorCosecha() {
        super("Optimizador Inteligente de Cosecha - PERT/CPM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(new EmptyBorder(12, 12, 12, 12));

        // Panel Superior Formulario
        JPanel panelNorte = new JPanel(new BorderLayout(8, 8));
        panelNorte.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                " 📋 Panel de Control y Registro de Actividades ",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 12),
                new Color(50, 50, 50)
        ));
        panelNorte.setBackground(new Color(248, 249, 250));

        JPanel panelFormulario = new JPanel(new GridLayout(3, 4, 10, 10));
        panelFormulario.setBorder(new EmptyBorder(8, 8, 8, 8));
        panelFormulario.setOpaque(false);

        panelFormulario.add(crearLabelEtiqueta("Nombre de tarea:"));
        panelFormulario.add(txtNombre);
        panelFormulario.add(crearLabelEtiqueta("Duracion (horas):"));
        panelFormulario.add(txtDuracion);

        panelFormulario.add(crearLabelEtiqueta("Rendimiento (kg):"));
        panelFormulario.add(txtRendimiento);
        panelFormulario.add(crearLabelEtiqueta("Predecesoras (IDs):"));
        panelFormulario.add(txtPredecesoras);

        panelFormulario.add(crearLabelEtiqueta("Plazo Maximo (h):"));
        panelFormulario.add(txtPlazoMaximo);
        panelFormulario.add(new JLabel(""));
        panelFormulario.add(new JLabel(""));

        panelNorte.add(panelFormulario, BorderLayout.CENTER);

        //  botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        panelBotones.setOpaque(false);

        JButton btnAgregar = crearBotonSolido("➕ Agregar Tarea", new Color(40, 167, 69));
        JButton btnCalcular = crearBotonSolido("⚡ Calcular PERT/CPM", new Color(0, 123, 255));
        JButton btnCamion = crearBotonSolido("🚛 Siguiente Camión", new Color(253, 126, 20));
        JButton btnLimpiar = crearBotonSolido("🧹 Limpiar Todo", new Color(108, 117, 125));

        panelBotones.add(btnAgregar);
        panelBotones.add(btnCalcular);
        panelBotones.add(btnCamion);
        panelBotones.add(btnLimpiar);
        panelNorte.add(panelBotones, BorderLayout.SOUTH);

        add(panelNorte, BorderLayout.NORTH);

        // Pestañas Centrales
        JTabbedPane pestañas = new JTabbedPane();
        pestañas.setFont(new Font("Arial", Font.BOLD, 12));

        String[] columnas = {"ID", "Tarea", "Duracion (h)", "ES", "EF", "LS", "LF", "Holgura", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setRowHeight(24);
        tablaResultados.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tablaResultados.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollTabla = new JScrollPane(tablaResultados);
        pestañas.addTab("📊 Matriz de Resultados y Holguras", scrollTabla);

        txtSalidaTexto.setEditable(false);
        txtSalidaTexto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtSalidaTexto.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane scrollConsola = new JScrollPane(txtSalidaTexto);
        pestañas.addTab("📜 Reporte Textual y Grafo BFS", scrollConsola);

        add(pestañas, BorderLayout.CENTER);

        // Panel Inferior: Alerta de Estado del Plazo
        lblEstadoPlazo.setFont(new Font("Arial", Font.BOLD, 13));
        lblEstadoPlazo.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel panelEstado = new JPanel(new BorderLayout());
        panelEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 0, 0, 0),
                BorderFactory.createEtchedBorder()
        ));
        panelEstado.add(lblEstadoPlazo, BorderLayout.CENTER);
        add(panelEstado, BorderLayout.SOUTH);

        // Eventos
        btnAgregar.addActionListener(e -> agregarTarea());
        btnCalcular.addActionListener(e -> calcular());
        btnCamion.addActionListener(e -> {
            String camion = modelo.siguienteCamion();
            JOptionPane.showMessageDialog(this, "Turno asignado de transporte: " + camion, "Logistica Camiones", JOptionPane.INFORMATION_MESSAGE);
        });
        btnLimpiar.addActionListener(e -> limpiarTodo());

        mostrarAyudaInicial();
    }

    private JLabel crearLabelEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(new Color(60, 60, 60));
        return lbl;
    }

    private JButton crearBotonSolido(String texto, Color colorBase) {
        JButton boton = new JButton(texto);
        boton.setBackground(colorBase);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setFont(new Font("Arial", Font.BOLD, 12));
        boton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(colorBase.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(colorBase);
            }
        });
        return boton;
    }

    private void mostrarAyudaInicial() {
        txtSalidaTexto.setText("SISTEMA OPTIMIZADOR DE COSECHA PERT/CPM\n"
                + "=================================================\n"
                + "1. Ingrese el nombre de la tarea, su duracion en horas y el rendimiento en kg.\n"
                + "2. Si depende de tareas anteriores, escriba sus IDs separados por coma (ej. 1,2).\n"
                + "3. Defina un 'Plazo Maximo' en la parte superior para evaluar incumplimientos de entrega.\n"
                + "4. Haga clic en 'Calcular PERT/CPM' para procesar el grafo y las holguras.\n\n");
        lblEstadoPlazo.setText("Estado: Listo para recibir actividades agricolas.");
        lblEstadoPlazo.setForeground(new Color(0, 100, 0));
    }

    private void agregarTarea() {
        try {
            String n = txtNombre.getText().trim();
            if (n.isEmpty()) throw new IllegalArgumentException("Escriba el nombre de la tarea.");

            double d = Double.parseDouble(txtDuracion.getText().trim());
            double r = Double.parseDouble(txtRendimiento.getText().trim());
            if (d <= 0) throw new IllegalArgumentException("La duracion debe ser mayor que cero.");
            if (r < 0) throw new IllegalArgumentException("El rendimiento no puede ser negativo.");

            int id = modelo.agregarTarea(n, d, r, txtPredecesoras.getText().trim());
            txtSalidaTexto.append("✅ Tarea agregada con exito. ID = " + id + " | " + n + "\n");
            
            txtNombre.setText("");
            txtDuracion.setText("");
            txtRendimiento.setText("");
            txtPredecesoras.setText("");
            txtNombre.requestFocus();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Duracion y rendimiento deben ser valores numericos validos.", "Error de formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void calcular() {
        try {
            double plazoMaximo = Double.parseDouble(txtPlazoMaximo.getText().trim());
            if (plazoMaximo <= 0) throw new IllegalArgumentException("El plazo maximo debe ser mayor a cero.");

            Modelo.ResultadoAnalisis res = modelo.calcularPERTCPM();
            
            modeloTabla.setRowCount(0);
            for (int i = 0; i < res.cantidad; i++) {
                modeloTabla.addRow(new Object[]{
                    (i + 1),
                    res.tareas[i].nombre,
                    String.format(Locale.US, "%.2f", res.matriz[i][0]),
                    String.format(Locale.US, "%.2f", res.inicioTemprano[i]),
                    String.format(Locale.US, "%.2f", res.finTemprano[i]),
                    String.format(Locale.US, "%.2f", res.inicioTardio[i]),
                    String.format(Locale.US, "%.2f", res.finTardio[i]),
                    String.format(Locale.US, "%.2f", res.holgura[i]),
                    res.holgura[i] == 0 ? "CRITICA" : "No critica"
                });
            }

            txtSalidaTexto.setText(res.reporteTexto);

            if (res.duracionProyecto > plazoMaximo) {
                lblEstadoPlazo.setText(String.format(Locale.US, "❌ ALERTA: Imposible cumplir plazos. Duracion (%.2f h) supera el limite (%.2f h).", res.duracionProyecto, plazoMaximo));
                lblEstadoPlazo.setForeground(Color.RED);
            } else {
                lblEstadoPlazo.setText(String.format(Locale.US, "✔ ÉXITO: Proyecto viable. Duracion total (%.2f h) dentro del plazo maximo (%.2f h).", res.duracionProyecto, plazoMaximo));
                lblEstadoPlazo.setForeground(new Color(34, 139, 34));
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El plazo maximo de proyecto debe ser un numero valido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error en el calculo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limpiarTodo() {
        modelo.reiniciar();
        modeloTabla.setRowCount(0);
        mostrarAyudaInicial();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OptimizadorCosecha().setVisible(true));
    }

    //  MODELO LOGICO

    static class Tarea {
        int id;
        String nombre;
        double duracion, rendimiento;
        ListaSimple sucesores = new ListaSimple();
        ListaDoble predecesores = new ListaDoble();
        int gradoEntrada;

        public Tarea() { super(); }

        Tarea(int id, String nombre, double duracion, double rendimiento) {
            super();
            this.id = id; this.nombre = nombre; this.duracion = duracion; this.rendimiento = rendimiento;
        }
    }

    static class NodoSimple { 
        int valor; 
        NodoSimple siguiente; 
        public NodoSimple() { super(); }
        NodoSimple(int v) { super(); valor = v; } 
    }

    static class ListaSimple {
        NodoSimple cabeza;
        public ListaSimple() { super(); }

        void agregar(int v) {
            NodoSimple n = new NodoSimple(v);
            if (cabeza == null) { cabeza = n; return; }
            NodoSimple act = cabeza;
            while (act.siguiente != null) act = act.siguiente;
            act.siguiente = n;
        }
        boolean contiene(int v) {
            for (NodoSimple a = cabeza; a != null; a = a.siguiente) if (a.valor == v) return true;
            return false;
        }
        int[] aArreglo() {
            int n = 0; for (NodoSimple a = cabeza; a != null; a = a.siguiente) n++;
            int[] d = new int[n]; int i = 0;
            for (NodoSimple a = cabeza; a != null; a = a.siguiente) d[i++] = a.valor;
            return d;
        }
    }

    static class NodoDoble { 
        int valor; 
        NodoDoble ant, sig; 
        public NodoDoble() { super(); }
        NodoDoble(int v) { super(); valor = v; } 
    }

    static class ListaDoble {
        NodoDoble cabeza, cola;
        public ListaDoble() { super(); }

        void agregar(int v) {
            NodoDoble n = new NodoDoble(v);
            if (cabeza == null) cabeza = cola = n;
            else { cola.sig = n; n.ant = cola; cola = n; }
        }
        int[] aArreglo() {
            int n = 0; for (NodoDoble a = cabeza; a != null; a = a.sig) n++;
            int[] d = new int[n]; int i = 0;
            for (NodoDoble a = cabeza; a != null; a = a.sig) d[i++] = a.valor;
            return d;
        }
    }

    static class NodoCamion { 
        String nombre; 
        NodoCamion sig; 
        public NodoCamion() { super(); }
        NodoCamion(String n) { super(); nombre = n; } 
    }

    static class ListaCircularCamiones {
        NodoCamion ultimo;
        NodoCamion turnoActual;
        public ListaCircularCamiones() { super(); }

        ListaCircularCamiones(String[] camiones) {
            super();
            for (String c : camiones) agregar(c);
            turnoActual = (ultimo == null) ? null : ultimo.sig;
        }
        void agregar(String nombre) {
            NodoCamion n = new NodoCamion(nombre);
            if (ultimo == null) { ultimo = n; n.sig = n; }
            else { n.sig = ultimo.sig; ultimo.sig = n; ultimo = n; }
        }
        String siguienteTurno() {
            if (turnoActual == null) return "Sin camiones";
            String n = turnoActual.nombre;
            turnoActual = turnoActual.sig;
            return n;
        }
    }

    static class Pila {
        NodoPila cima;
        public Pila() { super(); }

        void push(int v) { NodoPila n = new NodoPila(v); n.sig = cima; cima = n; }
        boolean estaVacia() { return cima == null; }
        int pop() {
            if (estaVacia()) throw new IllegalStateException("Pila vacia");
            int v = cima.valor; cima = cima.sig; return v;
        }
    }
    static class NodoPila { 
        int valor; 
        NodoPila sig; 
        public NodoPila() { super(); }
        NodoPila(int v) { super(); valor = v; } 
    }

    static class Cola {
        NodoCola frente, fin;
        public Cola() { super(); }

        boolean estaVacia() { return frente == null; }
        void encolar(int v) {
            NodoCola n = new NodoCola(v);
            if (fin == null) frente = fin = n;
            else { fin.sig = n; fin = n; }
        }
        int desencolar() {
            if (estaVacia()) throw new IllegalStateException("Cola vacia");
            int v = frente.valor; frente = frente.sig;
            if (frente == null) fin = null;
            return v;
        }
    }
    static class NodoCola { 
        int valor; 
        NodoCola sig; 
        public NodoCola() { super(); }
        NodoCola(int v) { super(); valor = v; } 
    }

    static class Modelo {
        Tarea[] tareas = new Tarea[MAX_TAREAS];
        int cantidad = 0;
        double[][] matriz = new double[MAX_TAREAS][2];
        ListaCircularCamiones camiones = new ListaCircularCamiones(new String[]{"Camion 1 (Carga Ligera)", "Camion 2 (Granel)", "Camion 3 (Refrigerado)"});

        public Modelo() { super(); }

        void reiniciar() {
            cantidad = 0;
            tareas = new Tarea[MAX_TAREAS];
            matriz = new double[MAX_TAREAS][2];
        }

        int agregarTarea(String nombre, double duracion, double rendimiento, String predsStr) {
            if (cantidad >= MAX_TAREAS) throw new IllegalArgumentException("Capacidad maxima alcanzada.");
            int nuevoId = cantidad;
            int[] preds = parsearPredecesoras(predsStr, nuevoId);
            Tarea nueva = new Tarea(nuevoId, nombre, duracion, rendimiento);

            for (int p : preds) {
                if (p == nuevoId) throw new IllegalArgumentException("Una tarea no puede depender de si misma.");
                if (tareas[p].sucesores.contiene(nuevoId)) throw new IllegalArgumentException("Dependencia duplicada.");
            }

            tareas[nuevoId] = nueva;
            matriz[nuevoId][0] = duracion;
            matriz[nuevoId][1] = rendimiento;

            for (int p : preds) {
                tareas[p].sucesores.agregar(nuevoId);
                nueva.predecesores.agregar(p);
                nueva.gradoEntrada++;
            }
            cantidad++;
            return nuevoId + 1;
        }

        private int[] parsearPredecesoras(String texto, int nuevoId) {
            if (texto == null || texto.isBlank()) return new int[0];
            String[] partes = texto.split(",");
            int[] temp = new int[partes.length];
            int n = 0;
            for (String p : partes) {
                int id;
                try { id = Integer.parseInt(p.trim()); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("IDs de predecesoras deben ser numericos."); }
                if (id < 1 || id > nuevoId) throw new IllegalArgumentException("La predecesora " + id + " no es valida.");
                int idx = id - 1;
                for (int i = 0; i < n; i++) if (temp[i] == idx) throw new IllegalArgumentException("ID predecesor repetido.");
                temp[n++] = idx;
            }
            int[] res = new int[n];
            System.arraycopy(temp, 0, res, 0, n);
            return res;
        }

        String siguienteCamion() { return camiones.siguienteTurno(); }

        static class ResultadoAnalisis {
            int cantidad;
            Tarea[] tareas;
            double[][] matriz;
            double[] inicioTemprano, finTemprano, inicioTardio, finTardio, holgura;
            double duracionProyecto;
            String reporteTexto;
            public ResultadoAnalisis() { super(); }
        }

        ResultadoAnalisis calcularPERTCPM() {
            if (cantidad == 0) throw new IllegalStateException("Debe registrar al menos una tarea.");

            int[] grados = new int[cantidad];
            Cola cola = new Cola();
            Cola bfs = new Cola();
            boolean[] visitado = new boolean[cantidad];
            Pila orden = new Pila();
            StringBuilder bfsRecorrido = new StringBuilder();

            for (int i = 0; i < cantidad; i++) {
                grados[i] = tareas[i].gradoEntrada;
                if (grados[i] == 0) {
                    cola.encolar(i);
                    bfs.encolar(i);
                    visitado[i] = true;
                }
            }

            while (!bfs.estaVacia()) {
                int actual = bfs.desencolar();
                if (bfsRecorrido.length() > 0) bfsRecorrido.append(" -> ");
                bfsRecorrido.append(tareas[actual].nombre);
                for (int sig : tareas[actual].sucesores.aArreglo()) {
                    if (!visitado[sig]) { visitado[sig] = true; bfs.encolar(sig); }
                }
            }

            int procesadas = 0;
            int[] ordenLineal = new int[cantidad];
            while (!cola.estaVacia()) {
                int actual = cola.desencolar();
                ordenLineal[procesadas++] = actual;
                orden.push(actual);
                for (int sig : tareas[actual].sucesores.aArreglo()) {
                    grados[sig]--;
                    if (grados[sig] == 0) cola.encolar(sig);
                }
            }

            if (procesadas != cantidad) throw new IllegalStateException("Error: Existe un ciclo cerrado en las dependencias.");

            double[] es = new double[cantidad];
            double[] ef = new double[cantidad];
            for (int k = 0; k < procesadas; k++) {
                int i = ordenLineal[k];
                double maxPred = 0;
                for (int p : tareas[i].predecesores.aArreglo()) maxPred = Math.max(maxPred, ef[p]);
                es[i] = maxPred;
                ef[i] = es[i] + matriz[i][0];
            }

            double duracionProyecto = 0;
            for (int i = 0; i < cantidad; i++) duracionProyecto = Math.max(duracionProyecto, ef[i]);

            double[] ls = new double[cantidad];
            double[] lf = new double[cantidad];
            double[] holguras = new double[cantidad];

            while (!orden.estaVacia()) {
                int i = orden.pop();
                int[] sucs = tareas[i].sucesores.aArreglo();
                double minSuc = Double.POSITIVE_INFINITY;
                for (int s : sucs) minSuc = Math.min(minSuc, ls[s]);

                lf[i] = sucs.length == 0 ? duracionProyecto : minSuc;
                ls[i] = lf[i] - matriz[i][0];
                holguras[i] = ls[i] - es[i];
                if (Math.abs(holguras[i]) < 1e-6) holguras[i] = 0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== INFORME TECNICO DE COSECHA PERT/CPM ===\n");
            sb.append(String.format(Locale.US, "Duracion minima requerida: %.2f horas\n", duracionProyecto));
            sb.append("Recorrido Grafo (BFS): ").append(bfsRecorrido.toString()).append("\n\n");
            sb.append("DETALLE DE TAREAS CRITICAS:\n");
            for (int i = 0; i < cantidad; i++) {
                if (holguras[i] == 0) {
                    sb.append(String.format(Locale.US, " - [%d] %s (Duracion: %.2f h)\n", (i + 1), tareas[i].nombre, matriz[i][0]));
                }
            }
            double totalRendimiento = 0;
            for (int i = 0; i < cantidad; i++) totalRendimiento += matriz[i][1];
            sb.append(String.format(Locale.US, "\nVolumen total cosechado estimado: %.2f kg\n", totalRendimiento));

            ResultadoAnalisis r = new ResultadoAnalisis();
            r.cantidad = cantidad;
            r.tareas = tareas;
            r.matriz = matriz;
            r.inicioTemprano = es;
            r.finTemprano = ef;
            r.inicioTardio = ls;
            r.finTardio = lf;
            r.holgura = holguras;
            r.duracionProyecto = duracionProyecto;
            r.reporteTexto = sb.toString();
            return r;
        }
    }
}