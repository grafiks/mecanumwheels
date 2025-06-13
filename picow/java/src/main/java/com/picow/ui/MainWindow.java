package com.picow.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class MainWindow extends JFrame {
    private final JPanel statusPanel;
    private final JTextArea logArea;
    private Consumer<WindowEvent> windowClosingHandler;

    public MainWindow() {
        setTitle("Mecanum Vehicle Bot Control");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create status panel at the top
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        mainPanel.add(statusPanel, BorderLayout.NORTH);

        // Create log panel at the bottom
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setRows(5);
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Add window listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (windowClosingHandler != null) {
                    windowClosingHandler.accept(e);
                }
            }
        });
    }

    public void setWindowClosingHandler(Consumer<WindowEvent> handler) {
        this.windowClosingHandler = handler;
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clearLog() {
        SwingUtilities.invokeLater(() -> {
            logArea.setText("");
        });
    }

    public void addStatusComponent(Component component) {
        statusPanel.add(component);
        statusPanel.revalidate();
    }
} 