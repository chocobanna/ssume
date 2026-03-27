package com.choco.starsector.editor.ui;

import javax.swing.*;
import java.awt.event.ActionListener;

public class EditorMenuBar extends JMenuBar {

    public EditorMenuBar(
            ActionListener selectMod,
            ActionListener newFile,
            ActionListener open,
            ActionListener save
    ) {
        JMenu file = new JMenu("File");

        file.add(item("Select Mod Folder", selectMod));
        file.add(item("New mod_info.json", newFile));
        file.add(item("Open mod_info.json", open));
        file.add(item("Save", save));

        add(file);
    }

    private JMenuItem item(String name, ActionListener a) {
        JMenuItem i = new JMenuItem(name);
        i.addActionListener(a);
        return i;
    }
}