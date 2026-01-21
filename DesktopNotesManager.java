import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/* =========================
   Main Application
   ========================= */
public class DesktopNotesManager extends JFrame {

    private final NotesTableModel tableModel;
    private final JTable table;
    private final JTextArea editor;
    private final JLabel statusLabel;

    private final File saveFile = new File("notes.dat");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DesktopNotesManager().setVisible(true);
        });
    }

    public DesktopNotesManager() {
        super("Desktop Notes Manager");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        tableModel = new NotesTableModel();
        loadNotes();

        table = new JTable(tableModel);
        editor = new JTextArea();
        statusLabel = new JLabel("Ready");

        buildUI();
        startAutoSave();
    }

    /* =========================
       UI Construction
       ========================= */
    private void buildUI() {
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.NORTH);
        add(createMainPane(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                editor.setText(tableModel.getNote(row).content);
            }
        });

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    tableModel.updateContent(row, editor.getText());
                    status("Edited note");
                }
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem newNote = new JMenuItem("New Note");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem exit = new JMenuItem("Exit");

        newNote.addActionListener(e -> addNote());
        save.addActionListener(e -> saveNotes());
        exit.addActionListener(e -> System.exit(0));

        file.add(newNote);
        file.add(save);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "Desktop Notes Manager\nSwing example app",
                "About",
                JOptionPane.INFORMATION_MESSAGE)
        );

        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    private JToolBar createToolBar() {
        JToolBar tb = new JToolBar();
        JButton add = new JButton("Add");
        JButton del = new JButton("Delete");

        add.addActionListener(e -> addNote());
        del.addActionListener(e -> deleteNote());

        tb.add(add);
        tb.add(del);
        return tb;
    }

    private JSplitPane createMainPane() {
        JScrollPane tablePane = new JScrollPane(table);
        JScrollPane editorPane = new JScrollPane(editor);

        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            tablePane,
            editorPane
        );
        split.setDividerLocation(350);
        return split;
    }

    private JPanel createStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(4, 8, 4, 8));
        p.add(statusLabel, BorderLayout.WEST);
        return p;
    }

    /* =========================
       Actions
       ========================= */
    private void addNote() {
        tableModel.addNote(new Note("New Note", ""));
        table.setRowSelectionInterval(
            tableModel.getRowCount() - 1,
            tableModel.getRowCount() - 1
        );
        status("Note added");
    }

    private void deleteNote() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            tableModel.remove(row);
            editor.setText("");
            status("Note deleted");
        }
    }

    private void status(String msg) {
        statusLabel.setText(msg + " • " + LocalDateTime.now());
    }

    /* =========================
       Persistence
       ========================= */
    private void saveNotes() {
        try (ObjectOutputStream oos =
                 new ObjectOutputStream(new FileOutputStream(saveFile))) {
            oos.writeObject(tableModel.notes);
            status("Saved");
        } catch (IOException ex) {
            ex.printStackTrace();
            status("Save failed");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadNotes() {
        if (!saveFile.exists()) return;
        try (ObjectInputStream ois =
                 new ObjectInputStream(new FileInputStream(saveFile))) {
            tableModel.notes = (List<Note>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAutoSave() {
        new Timer(10000, e -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    saveNotes();
                    return null;
                }
            }.execute();
        }).start();
    }
}

/* =========================
   Data Model
   ========================= */
class Note implements Serializable {
    String title;
    String content;

    Note(String t, String c) {
        title = t;
        content = c;
    }
}

class NotesTableModel extends AbstractTableModel {

    List<Note> notes = new ArrayList<>();

    @Override
    public int getRowCount() {
        return notes.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int col) {
        return "Title";
    }

    @Override
    public Object getValueAt(int row, int col) {
        return notes.get(row).title;
    }

    public Note getNote(int row) {
        return notes.get(row);
    }

    public void addNote(Note n) {
        notes.add(n);
        fireTableRowsInserted(notes.size() - 1, notes.size() - 1);
    }

    public void remove(int row) {
        notes.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void updateContent(int row, String text) {
        notes.get(row).content = text;
    }
}

