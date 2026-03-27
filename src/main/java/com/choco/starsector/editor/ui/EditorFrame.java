package com.choco.starsector.editor.ui;

import com.choco.starsector.editor.model.ModContext;
import com.choco.starsector.editor.service.ModFileService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;

public class EditorFrame extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("No mod selected");

    private final ModContext context = new ModContext();
    private final ModFileService service = new ModFileService();

    public EditorFrame() {
        super("Starsector Mod Editor");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        initUI();
    }

    // ========================
    // UI Setup
    // ========================
    private void initUI() {
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        setLayout(new BorderLayout());

        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        setJMenuBar(new EditorMenuBar(
                e -> chooseModFolder(),
                e -> createNew(),
                e -> load(),
                e -> save()
        ));
    }

    // ========================
    // Actions
    // ========================

    private void chooseModFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        context.setModFolder(chooser.getSelectedFile().toPath());
        statusLabel.setText("Mod: " + context.getModFolder());
    }

    private void createNew() {
        if (!context.isLoaded()) {
            error("Select a mod folder first.");
            return;
        }

        textArea.setText(service.defaultTemplate());
        statusLabel.setText("New mod_info.json template");
    }

    private void load() {
        if (!context.isLoaded()) {
            error("No mod selected.");
            return;
        }

        try {
            if (!Files.exists(context.getModInfoFile())) {
                error("mod_info.json not found.");
                return;
            }

            textArea.setText(service.load(context.getModInfoFile()));
            statusLabel.setText("Loaded mod_info.json");

        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void save() {
        if (!context.isLoaded()) {
            error("No mod selected.");
            return;
        }

        String content = textArea.getText();

        if (!service.validate(content)) {
            error("Invalid mod_info.json (missing required fields)");
            return;
        }

        try {
            service.save(context.getModInfoFile(), content);
            statusLabel.setText("Saved mod_info.json");
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    // ========================
    // Utils
    // ========================

    private void error(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}