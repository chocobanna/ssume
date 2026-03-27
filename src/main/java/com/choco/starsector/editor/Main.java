package com.choco.starsector.editor;

import com.choco.starsector.editor.ui.EditorFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new EditorFrame().setVisible(true);
        });
    }
}