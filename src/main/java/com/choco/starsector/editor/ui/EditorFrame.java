package com.choco.starsector.editor.ui;

import com.choco.starsector.editor.logic.ModInfoLogic;
import com.choco.starsector.editor.model.ModContext;
import com.choco.starsector.editor.model.ModInfo;
import com.choco.starsector.editor.service.ModFileService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class EditorFrame extends JFrame {

    private final ModContext context = new ModContext();
    private final ModFileService service = new ModFileService();
    private final ModInfoLogic logic = new ModInfoLogic();

    private final JLabel status = new JLabel("No mod selected");

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField authorField = new JTextField();
    private final JTextField gameVersionField = new JTextField("0.97a");

    private final JCheckBox utilityBox = new JCheckBox("Utility Mod");
    private final JCheckBox totalConversionBox = new JCheckBox("Total Conversion");

    private final JSpinner major = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
    private final JSpinner minor = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JSpinner patch = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));

    private final JTextArea description = new JTextArea(3, 20);

    public EditorFrame() {
        super("Starsector Mod Editor");

        setSize(650, 500);
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

        setJMenuBar(createMenu());
        setupAutoId();
    }

    // ========================
    // MENU (clean + usable)
    // ========================
    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");

        file.add(menuItem("Open Mod Folder", e -> chooseModFolder()));
        file.add(menuItem("New", e -> clearForm()));
        file.add(menuItem("Reload", e -> load()));
        file.add(menuItem("Save", e -> save()));

        bar.add(file);
        return bar;
    }

    private JMenuItem menuItem(String name, java.awt.event.ActionListener a) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(a);
        return item;
    }

    // ========================
    // FORM
    // ========================
    private JPanel createForm() {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        p.add(new JLabel("ID:")); p.add(idField);
        p.add(new JLabel("Name:")); p.add(nameField);
        p.add(new JLabel("Author:")); p.add(authorField);
        p.add(new JLabel("Game Version:")); p.add(gameVersionField);

        p.add(new JLabel("Version:"));
        JPanel v = new JPanel();
        v.add(major); v.add(minor); v.add(patch);
        p.add(v);

        p.add(new JLabel("Flags:"));
        JPanel flags = new JPanel();
        flags.add(utilityBox);
        flags.add(totalConversionBox);
        p.add(flags);

        p.add(new JLabel("Description:"));
        p.add(new JScrollPane(description));

        return p;
    }

    // ========================
    // AUTO ID
    // ========================
    private void setupAutoId() {
        DocumentListener l = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                idField.setText(
                        logic.generateId(authorField.getText(), nameField.getText())
                );
            }
        };

        authorField.getDocument().addDocumentListener(l);
        nameField.getDocument().addDocumentListener(l);
    }

    // ========================
    // ACTIONS
    // ========================

    private void chooseModFolder() {
        JFileChooser c = new JFileChooser();
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        context.setModFolder(c.getSelectedFile().toPath());

        if (Files.exists(context.getModInfoFile())) {
            load();
        } else {
            clearForm();
            status.setText("New mod");
        }
    }

    private void clearForm() {
        nameField.setText("");
        authorField.setText("");
        description.setText("");
        utilityBox.setSelected(false);
        totalConversionBox.setSelected(false);
    }

    private void load() {
        try {
            String json = service.load(context.getModInfoFile());
            ModInfo mod = logic.parse(json);

            nameField.setText(mod.name);
            authorField.setText(mod.author);
            gameVersionField.setText(mod.gameVersion);
            description.setText(mod.description);

            utilityBox.setSelected(mod.utility);
            totalConversionBox.setSelected(mod.totalConversion);

            if (mod.version != null) {
                major.setValue(mod.version.major);
                minor.setValue(mod.version.minor);
                patch.setValue(mod.version.patch);
            }

            status.setText("Loaded");

        } catch (Exception e) {
            error("Invalid JSON");
        }
    }

    private void save() {
        if (idField.getText().isEmpty()) {
            error("Invalid ID");
            return;
        }

        if (nameField.getText().isEmpty()) {
            error("Name required");
            return;
        }

        try {
            ModInfo mod = new ModInfo();

            mod.id = idField.getText();
            mod.name = nameField.getText();
            mod.author = authorField.getText();
            mod.gameVersion = gameVersionField.getText();
            mod.description = description.getText();

            mod.utility = utilityBox.isSelected();
            mod.totalConversion = totalConversionBox.isSelected();

            ModInfo.Version v = new ModInfo.Version();
            v.major = (int) major.getValue();
            v.minor = (int) minor.getValue();
            v.patch = (int) patch.getValue();
            mod.version = v;

            mod.dependencies = new ArrayList<>();

            String json = logic.toJson(mod);
            service.save(context.getModInfoFile(), json);

            status.setText("Saved");

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}