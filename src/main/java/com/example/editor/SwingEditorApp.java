package com.example.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SwingEditorApp extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final UndoManager undoManager = new UndoManager();

    private Path currentFile = null;
    private boolean modified = false;

    public SwingEditorApp() {
        super("Swing Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        initEditor();
        initMenuBar();
        initLayout();
        initListeners();
        updateTitle();
    }

    private void initEditor() {
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        textArea.getDocument().addUndoableEditListener(undoManager);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }

            private void changed() {
                modified = true;
                updateTitle();
            }
        });

        // Ctrl+F binding
        textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "find"
        );

        textArea.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });
    }

    private void initLayout() {
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void initListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        textArea.addCaretListener(e -> updateCaret());
    }

    private void initMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMenu view = new JMenu("View");
        JMenu help = new JMenu("Help");

        file.add(menuItem("New", KeyEvent.VK_N, e -> newFile()));
        file.add(menuItem("Open", KeyEvent.VK_O, e -> openFile()));
        file.add(menuItem("Save", KeyEvent.VK_S, e -> saveFile()));
        file.add(menuItem("Save As", KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK, e -> saveFileAs()));
        file.addSeparator();
        file.add(menuItem("Exit", null, e -> exit()));

        edit.add(menuItem("Undo", KeyEvent.VK_Z, e -> undo()));
        edit.add(menuItem("Redo", KeyEvent.VK_Y, e -> redo()));
        edit.addSeparator();
        edit.add(menuItem("Cut", null, e -> textArea.cut()));
        edit.add(menuItem("Copy", null, e -> textArea.copy()));
        edit.add(menuItem("Paste", null, e -> textArea.paste()));
        edit.addSeparator();
        edit.add(menuItem("Find", KeyEvent.VK_F, e -> showFindDialog()));

        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Line Wrap", true);
        wrap.addActionListener(e -> textArea.setLineWrap(wrap.isSelected()));
        view.add(wrap);

        help.add(menuItem("About", null, e -> showAbout()));

        bar.add(file);
        bar.add(edit);
        bar.add(view);
        bar.add(help);

        setJMenuBar(bar);
    }

    private JMenuItem menuItem(String name, Integer key, ActionListener action) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(action);

        if (key != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(
                    key,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
            ));
        }
        return item;
    }

    private JMenuItem menuItem(String name, Integer key, int modifiers, ActionListener action) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(action);

        if (key != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(
                    key,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | modifiers
            ));
        }
        return item;
    }

    private void newFile() {
        if (!confirm()) return;
        textArea.setText("");
        currentFile = null;
        modified = false;
        undoManager.discardAllEdits();
        updateTitle();
    }

    private void openFile() {
        if (!confirm()) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            currentFile = chooser.getSelectedFile().toPath();
            textArea.setText(Files.readString(currentFile));
            modified = false;
            updateTitle();
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }

        try {
            Files.writeString(currentFile, textArea.getText(), StandardCharsets.UTF_8);
            modified = false;
            updateTitle();
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        currentFile = chooser.getSelectedFile().toPath();
        saveFile();
    }

    private void undo() {
        try {
            if (undoManager.canUndo()) undoManager.undo();
        } catch (CannotUndoException e) {
            error("Undo failed");
        }
    }

    private void redo() {
        try {
            if (undoManager.canRedo()) undoManager.redo();
        } catch (CannotRedoException e) {
            error("Redo failed");
        }
    }

    private void showFindDialog() {
        String q = JOptionPane.showInputDialog(this, "Find:");
        if (q == null || q.isEmpty()) return;

        String text = textArea.getText();
        int index = text.indexOf(q, textArea.getCaretPosition());

        if (index < 0) index = text.indexOf(q);

        if (index >= 0) {
            textArea.select(index, index + q.length());
        } else {
            JOptionPane.showMessageDialog(this, "Not found");
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this, "Swing Editor\nJava 17");
    }

    private boolean confirm() {
        if (!modified) return true;

        int r = JOptionPane.showConfirmDialog(
                this, "Unsaved changes. Save?",
                "Warning",
                JOptionPane.YES_NO_CANCEL_OPTION
        );

        if (r == JOptionPane.CANCEL_OPTION) return false;
        if (r == JOptionPane.YES_OPTION) saveFile();
        return true;
    }

    private void exit() {
        if (!confirm()) return;
        System.exit(0);
    }

    private void updateTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getFileName().toString();
        setTitle(name + (modified ? "*" : "") + " - Swing Editor");
    }

    private void updateCaret() {
        try {
            int pos = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(pos);
            int col = pos - textArea.getLineStartOffset(line);
            statusLabel.setText("Line " + (line + 1) + ", Col " + (col + 1));
        } catch (BadLocationException ignored) {}
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new SwingEditorApp().setVisible(true);
        });
    }
}