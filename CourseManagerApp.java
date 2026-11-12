/*
 * COS201 — Programming I | Mid-Semester Lab Assessment Project
 * Student Course Management System (Mini-System)
 *
 * Java 8+ (Swing). Run:
 *   javac CourseManagerApp.java
 *   java  CourseManagerApp
 *
 * Save this file as:  CourseManagerApp.java
 */

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CourseManagerApp {

    // -------- theme ------------------------------------------------------ //
    static final Color BG        = new Color(0xF5F6F8);
    static final Color CARD      = Color.WHITE;
    static final Color TEXT      = new Color(0x1F2430);
    static final Color MUTED     = new Color(0x7A8290);
    static final Color BORDER    = new Color(0xE3E6EB);
    static final Color ACCENT    = new Color(0x2F6FED);
    static final Color ACCENT_DK = new Color(0x2559C4);
    static final Color SOFT_BLUE = new Color(0xEAF1FE);
    static final Color STRIPE    = new Color(0xFBFCFD);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    static Font uiFont(int style, int size) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return base.deriveFont(style, (float) size);
    }
}

/* ====================================================================== */
/*  MODEL                                                                 */
/* ====================================================================== */
class Course {

    public static final int MIN_UNITS = 1, MAX_UNITS = 6;
    public static final int MIN_CODE_LEN = 2, MAX_CODE_LEN = 10;

    private final String code;
    private final String title;
    private final int units;

    /** Public constructor used by the UI — normalises and validates input. */
    public Course(String codeRaw, String titleRaw, String unitsRaw) {
        this.code  = normaliseCode(codeRaw);
        this.title = normaliseTitle(titleRaw);
        this.units = parseUnits(unitsRaw);
    }

    /** Internal constructor used by the file loader (already normalised). */
    private Course(String code, String title, int units) {
        this.code = code;
        this.title = title;
        this.units = units;
    }

    public String getCode()  { return code; }
    public String getTitle() { return title; }
    public int    getUnits() { return units; }

    // ---- string processing & validation -------------------------------- //
    public static String normaliseCode(String raw) {
        if (raw == null) throw new IllegalArgumentException("Course code cannot be empty.");
        String code = raw.trim().toUpperCase().replaceAll("\\s+", "");
        if (code.isEmpty())
            throw new IllegalArgumentException("Course code cannot be empty.");
        if (code.length() < MIN_CODE_LEN || code.length() > MAX_CODE_LEN)
            throw new IllegalArgumentException("Course code must be "
                    + MIN_CODE_LEN + "–" + MAX_CODE_LEN + " characters (e.g. COS201).");
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isLetterOrDigit(code.charAt(i)))
                throw new IllegalArgumentException(
                        "Course code may only contain letters and digits.");
        }
        return code;
    }

    public static String normaliseTitle(String raw) {
        if (raw == null) throw new IllegalArgumentException("Course title cannot be empty.");
        String title = raw.trim().replaceAll("\\s+", " ");
        if (title.isEmpty())
            throw new IllegalArgumentException("Course title cannot be empty.");
        if (title.length() < 3)
            throw new IllegalArgumentException("Course title is too short.");
        return title;
    }

    public static int parseUnits(String raw) {
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Units must be a whole number (e.g. 3).");
        int units;
        try {
            units = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Units must be a whole number (e.g. 3).");
        }
        if (units < MIN_UNITS || units > MAX_UNITS)
            throw new IllegalArgumentException("Units must be between "
                    + MIN_UNITS + " and " + MAX_UNITS + ".");
        return units;
    }

    // ---- file (de)serialisation --------------------------------------- //
    public String toLine() {
        return code + "|" + title + "|" + units;
    }

    public static Course fromLine(String line) {
        int firstPipe = line.indexOf('|');
        int lastPipe  = line.lastIndexOf('|');
        if (firstPipe <= 0 || lastPipe <= firstPipe)
            throw new IllegalArgumentException("Malformed line");
        String code  = line.substring(0, firstPipe);
        String title = line.substring(firstPipe + 1, lastPipe);
        String units = line.substring(lastPipe + 1);
        return new Course(normaliseCode(code), normaliseTitle(title), parseUnits(units));
    }

    @Override
    public String toString() {
        return code + " — " + title + " (" + units + "u)";
    }
}

/* ====================================================================== */
/*  CONTROLLER / STORAGE                                                  */
/* ====================================================================== */
class CourseManager {

    private final List<Course> courses = new ArrayList<>();

    public void add(Course c) {
        if (find(c.getCode()) != null)
            throw new IllegalArgumentException(c.getCode() + " has already been added.");
        courses.add(c);
    }

    public List<Course> all()   { return new ArrayList<>(courses); }
    public boolean     isEmpty(){ return courses.isEmpty(); }
    public int         size()   { return courses.size(); }
    public void        clear()  { courses.clear(); }

    // ---- recursion #1: linear search ----------------------------------- //
    public Course find(String code) {
        return searchFrom(code, 0);
    }
    private Course searchFrom(String code, int index) {
        if (index >= courses.size()) return null;                  // base case
        if (courses.get(index).getCode().equals(code))             // base case
            return courses.get(index);
        return searchFrom(code, index + 1);                        // recursive step
    }

    // ---- recursion #2: total credit units ------------------------------ //
    public int totalUnits() {
        return sumFrom(0);
    }
    private int sumFrom(int index) {
        if (index >= courses.size()) return 0;                     // base case
        return courses.get(index).getUnits() + sumFrom(index + 1); // recursive step
    }

    // ---- file I/O ------------------------------------------------------ //
    public int save(File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Course c : courses) {
                writer.write(c.toLine());
                writer.newLine();
            }
        }
        return courses.size();
    }

    /** Merges a saved file into the list. Returns {added, skipped}. */
    public int[] load(File file) throws IOException {
        int added = 0, skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    Course c = Course.fromLine(line);
                    if (find(c.getCode()) == null) {
                        courses.add(c);
                        added++;
                    } else {
                        skipped++;
                    }
                } catch (IllegalArgumentException ex) {
                    skipped++;
                }
            }
        }
        return new int[]{ added, skipped };
    }
}

/* ====================================================================== */
/*  VIEW                                                                  */
/* ====================================================================== */
class MainFrame extends JFrame {

    private final CourseManager manager = new CourseManager();

    private final JTextField codeField  = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextField unitsField = new JTextField();
    private final JTextField searchField = new JTextField();

    private final JLabel countLabel  = new JLabel("0 courses · 0 units");
    private final JLabel statusLabel = new JLabel("Ready — add your first course to get started.");

    private final DefaultTableModel tableModel;
    private final JTable table;

    public MainFrame() {
        setTitle("Course Manager — COS201");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(880, 620));
        setSize(960, 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(CourseManagerApp.BG);

        tableModel = new DefaultTableModel(
                new Object[]{"CODE", "COURSE TITLE", "UNITS"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        configureTable();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CourseManagerApp.BG);
        root.setBorder(new EmptyBorder(24, 28, 16, 28));
        setContentPane(root);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildHeader());
        top.add(Box.createVerticalStrut(18));
        top.add(buildFormCard());
        top.add(Box.createVerticalStrut(14));
        top.add(buildToolbar());

        root.add(top,                BorderLayout.NORTH);
        root.add(buildTableCard(),   BorderLayout.CENTER);
        root.add(buildStatusBar(),   BorderLayout.SOUTH);

        refreshTable();
        SwingUtilities.invokeLater(codeField::requestFocusInWindow);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });
    }

    // ---- header -------------------------------------------------------- //
    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Course Manager");
        title.setFont(CourseManagerApp.uiFont(Font.BOLD, 24));
        title.setForeground(CourseManagerApp.TEXT);

        JLabel sub = new JLabel(
                "COS201 · Programming I  —  Student Course Management System");
        sub.setFont(CourseManagerApp.uiFont(Font.PLAIN, 12));
        sub.setForeground(CourseManagerApp.MUTED);

        title.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(2));
        p.add(sub);
        return p;
    }

    // ---- add-course card ---------------------------------------------- //
    private JPanel buildFormCard() {
        RoundedPanel card = new RoundedPanel(CourseManagerApp.CARD, 14);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0; card.add(fieldLabel("COURSE CODE"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; card.add(fieldLabel("COURSE TITLE"), gbc);
        gbc.gridx = 2; gbc.weightx = 0; card.add(fieldLabel("UNITS"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 0, 12);

        styleTextField(codeField);   codeField.setPreferredSize(new Dimension(130, 34));
        styleTextField(titleField);  titleField.setPreferredSize(new Dimension(340, 34));
        styleTextField(unitsField);  unitsField.setPreferredSize(new Dimension(70, 34));

        gbc.gridx = 0; gbc.weightx = 0; card.add(codeField, gbc);
        gbc.gridx = 1; gbc.weightx = 1; card.add(titleField, gbc);
        gbc.gridx = 2; gbc.weightx = 0; card.add(unitsField, gbc);

        JButton add = new AccentButton("Add Course");
        add.addActionListener(e -> onAdd());
        gbc.gridx = 3; gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 0, 0);
        card.add(add, gbc);

        ActionListener enter = e -> onAdd();
        codeField.addActionListener(enter);
        titleField.addActionListener(enter);
        unitsField.addActionListener(enter);

        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(CourseManagerApp.uiFont(Font.BOLD, 10));
        l.setForeground(CourseManagerApp.MUTED);
        return l;
    }

    private void styleTextField(JTextField field) {
        field.setFont(CourseManagerApp.uiFont(Font.PLAIN, 13));
        field.setForeground(CourseManagerApp.TEXT);
        field.setBackground(CourseManagerApp.CARD);
        field.setCaretColor(CourseManagerApp.TEXT);

        Border normal = BorderFactory.createCompoundBorder(
                new LineBorder(CourseManagerApp.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10));
        Border focus = BorderFactory.createCompoundBorder(
                new LineBorder(CourseManagerApp.ACCENT, 1, true),
                new EmptyBorder(6, 10, 6, 10));

        field.setBorder(normal);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.setBorder(focus);  }
            @Override public void focusLost  (FocusEvent e) { field.setBorder(normal); }
        });
    }

    // ---- toolbar ------------------------------------------------------- //
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        styleTextField(searchField);
        searchField.setPreferredSize(new Dimension(220, 34));
        searchField.addActionListener(e -> onSearch());

        JButton searchBtn = new GhostButton("Search");
        searchBtn.addActionListener(e -> onSearch());

        left.add(searchField);
        left.add(searchBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton totalBtn = new GhostButton("Total Units");   totalBtn.addActionListener(e -> onTotal());
        JButton saveBtn  = new GhostButton("Save to File");  saveBtn.addActionListener(e -> onSave());
        JButton loadBtn  = new GhostButton("Load from File");loadBtn.addActionListener(e -> onLoad());
        JButton clearBtn = new GhostButton("Clear All");     clearBtn.addActionListener(e -> onClear());
        JButton exitBtn  = new GhostButton("Exit");          exitBtn.addActionListener(e -> onExit());
        right.add(totalBtn); right.add(saveBtn); right.add(loadBtn);
        right.add(clearBtn); right.add(exitBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ---- table card ---------------------------------------------------- //
    private JPanel buildTableCard() {
        RoundedPanel card = new RoundedPanel(CourseManagerApp.CARD, 14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 20, 14, 14));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 0, 10, 6));

        JLabel title = new JLabel("My Courses");
        title.setFont(CourseManagerApp.uiFont(Font.BOLD, 14));
        title.setForeground(CourseManagerApp.TEXT);

        countLabel.setFont(CourseManagerApp.uiFont(Font.BOLD, 11));
        countLabel.setForeground(CourseManagerApp.ACCENT_DK);
        countLabel.setOpaque(true);
        countLabel.setBackground(CourseManagerApp.SOFT_BLUE);
        countLabel.setBorder(new EmptyBorder(4, 10, 4, 10));

        head.add(title,      BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CourseManagerApp.CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void configureTable() {
        table.setFont(CourseManagerApp.uiFont(Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(CourseManagerApp.CARD);
        table.setForeground(CourseManagerApp.TEXT);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                l.setBackground(new Color(0xF0F2F5));
                l.setForeground(CourseManagerApp.MUTED);
                l.setFont(CourseManagerApp.uiFont(Font.BOLD, 10));
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                l.setHorizontalAlignment(c == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return l;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(0).setMaxWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(440);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setMaxWidth(110);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                l.setFont(CourseManagerApp.uiFont(c == 2 ? Font.BOLD : Font.PLAIN, 13));
                l.setHorizontalAlignment(c == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (sel) {
                    l.setBackground(CourseManagerApp.SOFT_BLUE);
                    l.setForeground(CourseManagerApp.ACCENT_DK);
                } else {
                    l.setBackground(r % 2 == 0 ? CourseManagerApp.CARD : CourseManagerApp.STRIPE);
                    l.setForeground(CourseManagerApp.TEXT);
                }
                return l;
            }
        });
    }

    // ---- status -------------------------------------------------------- //
    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 2, 0, 2));
        statusLabel.setFont(CourseManagerApp.uiFont(Font.PLAIN, 11));
        statusLabel.setForeground(CourseManagerApp.MUTED);
        p.add(statusLabel, BorderLayout.WEST);
        return p;
    }

    // ---- actions ------------------------------------------------------- //
    private void onAdd() {
        try {
            Course c = new Course(codeField.getText(),
                                  titleField.getText(),
                                  unitsField.getText());
            manager.add(c);
            refreshTable();
            selectRow(c.getCode());

            codeField.setText("");
            titleField.setText("");
            unitsField.setText("");
            codeField.requestFocusInWindow();

            setStatus("Added " + c.getCode() + " — " + c.getTitle()
                    + " (" + c.getUnits() + " units).");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Cannot add course", JOptionPane.ERROR_MESSAGE);
            setStatus("Error: " + ex.getMessage());
        }
    }

    private void onSearch() {
        try {
            String code = Course.normaliseCode(searchField.getText());
            Course c = manager.find(code);                 // recursive search
            if (c == null) {
                JOptionPane.showMessageDialog(this,
                        "No course with code '" + code + "' was found.",
                        "Not found", JOptionPane.WARNING_MESSAGE);
                setStatus("Search: '" + code + "' not found.");
                return;
            }
            selectRow(c.getCode());
            setStatus("Found: " + c.getCode() + " — " + c.getTitle()
                    + " (" + c.getUnits() + " units).");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid course code", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onTotal() {
        List<Course> all = manager.all();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No courses recorded yet.",
                    "Total Units", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Course c : all) {
            sb.append(String.format("%-10s %2d units%n", c.getCode(), c.getUnits()));
        }
        sb.append("------------------------\n");
        int total = manager.totalUnits();                  // recursive sum
        sb.append(all.size()).append(" course(s)  ·  ")
          .append(total).append(" total units");

        JOptionPane.showMessageDialog(this, sb.toString(),
                "Total Units", JOptionPane.INFORMATION_MESSAGE);
        setStatus("Total credit units: " + total
                + " across " + all.size() + " course(s).");
    }

    private void onSave() {
        if (manager.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There is nothing to save yet.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save course list");
        chooser.setSelectedFile(new File("courses.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            setStatus("Save cancelled.");
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }
        try {
            int count = manager.save(file);
            setStatus("Saved " + count + " course(s) to " + file.getName() + ".");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not write the file:\n" + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Save failed.");
        }
    }

    private void onLoad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load course list");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            setStatus("Load cancelled.");
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            int[] res = manager.load(file);
            refreshTable();
            String msg = "Loaded " + res[0] + " course(s) from " + file.getName() + ".";
            if (res[1] > 0)
                msg += "  " + res[1] + " line(s) skipped (duplicate or invalid).";
            setStatus(msg);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read the file:\n" + ex.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Load failed.");
        }
    }

    private void onClear() {
        if (manager.isEmpty()) {
            setStatus("The list is already empty.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove all courses from the list?\nSaved files are not affected.",
                "Clear all", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            manager.clear();
            refreshTable();
            setStatus("All courses removed from the list.");
        }
    }

    private void onExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Exit Course Manager?", "Exit", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    // ---- helpers ------------------------------------------------------- //
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Course c : manager.all()) {
            tableModel.addRow(new Object[]{ c.getCode(), c.getTitle(), c.getUnits() });
        }
        int count = manager.size();
        int units = manager.totalUnits();
        countLabel.setText(count + " course" + (count == 1 ? "" : "s")
                + " · " + units + " units");
    }

    private void selectRow(String code) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (code.equals(tableModel.getValueAt(i, 0))) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }
}

/* ====================================================================== */
/*  CUSTOM COMPONENTS                                                     */
/* ====================================================================== */
class RoundedPanel extends JPanel {
    private final Color bg;
    private final int radius;

    RoundedPanel(Color bg, int radius) {
        this.bg = bg;
        this.radius = radius;
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(CourseManagerApp.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}

class AccentButton extends JButton {
    private final Color bg      = new Color(0x2F6FED);
    private final Color bgHover = new Color(0x2559C4);
    private final Color bgPress = new Color(0x1F4BA8);

    AccentButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(CourseManagerApp.uiFont(Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(9, 20, 9, 20));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = bg;
        if (getModel().isPressed())       c = bgPress;
        else if (getModel().isRollover()) c = bgHover;
        g2.setColor(c);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}

class GhostButton extends JButton {
    GhostButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(CourseManagerApp.TEXT);
        setFont(CourseManagerApp.uiFont(Font.PLAIN, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = Color.WHITE;
        if (getModel().isPressed())       fill = new Color(0xEDEFF3);
        else if (getModel().isRollover()) fill = new Color(0xF3F5F8);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g2.setColor(getModel().isRollover()
                ? new Color(0xCFD4DC)
                : CourseManagerApp.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.dispose();
        super.paintComponent(g);
    }
}