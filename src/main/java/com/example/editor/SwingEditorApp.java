package com.example.editor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class SwingEditorApp extends JFrame {
    private final JTextArea textArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final UndoManager undoManager = new UndoManager();

    private Path currentFile;
    private boolean modified = false;

    public SwingEditorApp() {
        super("Swing Editor");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setLocationByPlatform(true);

        initEditor();
        initMenuBar();
        initLayout();
        initListeners();
        updateTitle();
    }

    private void initEditor() {
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setTabSize(4);

        textArea.getDocument().addUndoableEditListener(undoManager);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onDocumentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onDocumentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onDocumentChanged();
            }
        });

        InputMap inputMap = textArea.getInputMap();
        ActionMap actionMap = textArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });
    }

    private void initLayout() {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void initListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        textArea.addCaretListener(e -> updateCaretStatus());
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu helpMenu = new JMenu("Help");

        fileMenu.add(menuItem("New", KeyEvent.VK_N, e -> newFile()));
        fileMenu.add(menuItem("Open...", KeyEvent.VK_O, e -> openFile()));
        fileMenu.add(menuItem("Save", KeyEvent.VK_S, e -> saveFile()));
        fileMenu.add(menuItem("Save As...", KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK, e -> saveFileAs()));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", null, e -> exitApplication()));

        editMenu.add(menuItem("Undo", KeyEvent.VK_Z, e -> undo()));
        editMenu.add(menuItem("Redo", KeyEvent.VK_Y, e -> redo()));
        editMenu.addSeparator();
        editMenu.add(menuItem("Cut", null, e -> textArea.cut()));
        editMenu.add(menuItem("Copy", null, e -> textArea.copy()));
        editMenu.add(menuItem("Paste", null, e -> textArea.paste()));
        editMenu.addSeparator();
        editMenu.add(menuItem("Select All", KeyEvent.VK_A, e -> textArea.selectAll()));
        editMenu.add(menuItem("Find...", KeyEvent.VK_F, e -> showFindDialog()));

        JCheckBoxMenuItem lineWrapItem = new JCheckBoxMenuItem("Line Wrap", true);
        lineWrapItem.addActionListener(e -> {
            boolean wrap = lineWrapItem.isSelected();
            textArea.setLineWrap(wrap);
            textArea.setWrapStyleWord(wrap);
        });
        viewMenu.add(lineWrapItem);

        helpMenu.add(menuItem("About", null, e -> showAboutDialog()));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JMenuItem menuItem(String title, Integer keyCode, AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setText(title);
        if (keyCode != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }
        return item;
    }

    private JMenuItem menuItem(String title, Integer keyCode, int modifiers, AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setText(title);
        if (keyCode != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | modifiers));
        }
        return item;
    }

    private void newFile() {
        if (!confirmDiscardChanges()) {
            return;
        }

        textArea.setText("");
        currentFile = null;
        modified = false;
        undoManager.discardAllEdits();
        updateTitle();
        setStatus("New file");
    }

    private void openFile() {
        if (!confirmDiscardChanges()) {
            return;
        }

        JFileChooser chooser = createFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            textArea.setText(content);
            textArea.setCaretPosition(0);
            currentFile = file;
            modified = false;
            undoManager.discardAllEdits();
            updateTitle();
            setStatus("Opened: " + file);
        } catch (IOException ex) {
            showError("Failed to open file:\n" + ex.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }

        writeToFile(currentFile);
    }

    private void saveFileAs() {
        JFileChooser chooser = createFileChooser();
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        if (file.getFileName() != null && !file.getFileName().toString().contains(".")) {
            file = file.resolveSibling(file.getFileName() + ".txt");
        }

        writeToFile(file);
    }

    private void writeToFile(Path file) {
        try {
            Files.writeString(file, textArea.getText(), StandardCharsets.UTF_8);
            currentFile = file;
            modified = false;
            updateTitle();
            setStatus("Saved: " + file);
        } catch (IOException ex) {
            showError("Failed to save file:\n" + ex.getMessage());
        }
    }

    private void undo() {
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
                setStatus("Undo");
            }
        } catch (CannotUndoException ex) {
            showError("Cannot undo:\n" + ex.getMessage());
        }
    }

    private void redo() {
        try {
            if (undoManager.canRedo()) {
                undoManager.redo();
                setStatus("Redo");
            }
        } catch (CannotRedoException ex) {
            showError("Cannot redo:\n" + ex.getMessage());
        }
    }

    private void showFindDialog() {
        String query = JOptionPane.showInputDialog(this, "Find:", "Find Text", JOptionPane.PLAIN_MESSAGE);
        if (query == null || query.isBlank()) {
            return;
        }

        String text = textArea.getText();
        String selected = textArea.getSelectedText();
        int startIndex;

        if (selected != null && Objects.equals(selected, query)) {
            startIndex = textArea.getSelectionEnd();
        } else {
            startIndex = textArea.getCaretPosition();
        }

        int index = text.indexOf(query, startIndex);
        if (index < 0 && startIndex > 0) {
            index = text.indexOf(query);
        }

        if (index >= 0) {
            textArea.requestFocusInWindow();
            textArea.select(index, index + query.length());
            setStatus("Found: \"" + query + "\"");
        } else {
            JOptionPane.showMessageDialog(this, "Text not found.", "Find", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Text not found");
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
                this,
                "Swing Editor\nJava 17 + Gradle 8.14.4\n\nA small desktop text editor built with Swing.",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private boolean confirmDiscardChanges() {
        if (!modified) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Save before continuing?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
            return false;
        }

        if (result == JOptionPane.YES_OPTION) {
            if (currentFile == null) {
                JFileChooser chooser = createFileChooser();
                int saveResult = chooser.showSaveDialog(this);
                if (saveResult != JFileChooser.APPROVE_OPTION) {
                    return false;
                }

                Path file = chooser.getSelectedFile().toPath();
                if (file.getFileName() != null && !file.getFileName().toString().contains(".")) {
                    file = file.resolveSibling(file.getFileName() + ".txt");
                }

                writeToFile(file);
                return !modified;
            } else {
                writeToFile(currentFile);
                return !modified;
            }
        }

        return true;
    }

    private void exitApplication() {
        if (!confirmDiscardChanges()) {
            return;
        }
        dispose();
        System.exit(0);
    }

    private JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt, *.md, *.java, *.log)", "txt", "md", "java", "log"));
        return chooser;
    }

    private void onDocumentChanged() {
        modified = true;
        updateTitle();
        updateCaretStatus();
    }

    private void updateTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getFileName().toString();
        String marker = modified ? " *" : "";
        setTitle(name + marker + " - Swing Editor");
    }

    private void updateCaretStatus() {
        try {
            int caret = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(caret);
            int lineStart = textArea.getLineStartOffset(line);
            int column = caret - lineStart;
            statusLabel.setText("Line " + (line + 1) + ", Column " + (column + 1) + (modified ? "  •  Modified" : ""));
        } catch (BadLocationException e) {
            statusLabel.setText(modified ? "Modified" : "Ready");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // If system LAF fails, default Swing LAF is fine.
            }

            SwingEditorApp app = new SwingEditorApp();
            app.setVisible(true);
        });
    }

    @FunctionalInterface
    private interface AbstractAction extends ActionListener {
        @Override
        void actionPerformed(ActionEvent e);
    }
}