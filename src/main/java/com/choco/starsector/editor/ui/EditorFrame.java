package com.choco.starsector.editor.ui;

import com.choco.starsector.editor.logic.ModInfoLogic;
import com.choco.starsector.editor.model.ModContext;
import com.choco.starsector.editor.service.ModFileService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;

public class EditorFrame extends JFrame {

    private final ModContext context = new ModContext();
    private final ModFileService service = new ModFileService();
    private final ModInfoLogic logic = new ModInfoLogic();

    private final JLabel status = new JLabel("No mod selected");

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField authorField = new JTextField();
    private final JTextField gameVersionField = new JTextField("0.97a");

    private final JSpinner major = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
    private final JSpinner minor = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JSpinner patch = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));

    private final JTextArea description = new JTextArea(5, 20);

    public EditorFrame() {
        super("Starsector Mod Editor");

        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        idField.setEditable(false);

        setLayout(new BorderLayout());
        add(createForm(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        setJMenuBar(new EditorMenuBar(
                e -> chooseModFolder(),
                e -> newFile(),
                e -> load(),
                e -> save()
        ));

        setupAutoId();
    }

    private JPanel createForm() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Mod ID:"));
        panel.add(idField);

        panel.add(new JLabel("Name:"));
        panel.add(nameField);

        panel.add(new JLabel("Author:"));
        panel.add(authorField);

        panel.add(new JLabel("Game Version:"));
        panel.add(gameVersionField);

        panel.add(new JLabel("Version:"));
        JPanel v = new JPanel();
        v.add(major);
        v.add(minor);
        v.add(patch);
        panel.add(v);

        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(description));

        return panel;
    }

    // ========================
    // AUTO ID
    // ========================

    private void setupAutoId() {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                String id = logic.generateId(
                        authorField.getText(),
                        nameField.getText()
                );
                idField.setText(id);
            }
        };

        authorField.getDocument().addDocumentListener(listener);
        nameField.getDocument().addDocumentListener(listener);
    }

    // ========================
    // ACTIONS
    // ========================

    private void chooseModFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        context.setModFolder(chooser.getSelectedFile().toPath());
        status.setText("Mod: " + context.getModFolder());
    }

    private void newFile() {
        nameField.setText("");
        authorField.setText("");
        description.setText("");
        status.setText("New mod");
    }

    private void load() {
        if (!context.isLoaded()) {
            error("Select mod folder first");
            return;
        }

        try {
            if (!Files.exists(context.getModInfoFile())) {
                error("mod_info.json not found");
                return;
            }

            String json = service.load(context.getModInfoFile());

            nameField.setText(logic.extract(json, "name"));
            authorField.setText(logic.extract(json, "author"));
            gameVersionField.setText(logic.extract(json, "gameVersion"));
            description.setText(logic.extract(json, "description"));

            status.setText("Loaded");

        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void save() {
        if (!context.isLoaded()) {
            error("Select mod folder first");
            return;
        }

        if (idField.getText().isEmpty()) {
            error("Invalid ID");
            return;
        }

        String json = logic.generateJson(
                idField.getText(),
                nameField.getText(),
                authorField.getText(),
                (int) major.getValue(),
                (int) minor.getValue(),
                (int) patch.getValue(),
                gameVersionField.getText(),
                description.getText()
        );

        try {
            service.save(context.getModInfoFile(), json);
            status.setText("Saved");
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}