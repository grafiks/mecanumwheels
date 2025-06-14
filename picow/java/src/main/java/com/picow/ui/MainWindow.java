package com.picow.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;

import org.slf4j.LoggerFactory;

import com.picow.RobotLogger;

import ch.qos.logback.classic.LoggerContext;

public class MainWindow extends JFrame {
    private final JPanel statusPanel;
    private final JTextPane logPanel;
    private final JToolBar toolBar;
    private final AtomicBoolean logsEnabled = new AtomicBoolean(true);
    private Consumer<WindowEvent> windowClosingHandler;
    private LogPanelAppender logPanelAppender;

    public MainWindow() {
        setTitle("Mecanum Vehicle Bot Control");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create toolbar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton toggleLogsButton = new JButton("Pause Logs");
        toggleLogsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean newState = !logsEnabled.get();
                logsEnabled.set(newState);
                toggleLogsButton.setText(newState ? "Pause Logs" : "Resume Logs");
                if (logPanelAppender != null) {
                    logPanelAppender.setEnabled(newState);
                }
            }
        });
        toolBar.add(toggleLogsButton);

        // Create main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Add toolbar at the top
        mainPanel.add(toolBar, BorderLayout.NORTH);
        
        // Create status panel below toolbar
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        mainPanel.add(statusPanel, BorderLayout.CENTER);

        // Create log panel at the bottom
        logPanel = new JTextPane();
        logPanel.setEditable(false);
        logPanel.setFocusable(false);  // Make log panel non-focusable
        JScrollPane logScrollPane = new JScrollPane(logPanel);
        logScrollPane.setPreferredSize(new Dimension(800, 200));
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));
        mainPanel.add(logScrollPane, BorderLayout.SOUTH);

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

        // Initialize logging
        initializeLogging();
    }

    private void initializeLogging() {
        if (logPanelAppender != null) {
            logPanelAppender.stop();
            // Remove from logger
            ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger("ROBOT")
                .detachAppender(logPanelAppender);
        }
        logPanelAppender = new LogPanelAppender(logPanel);
        logPanelAppender.setEnabled(logsEnabled.get());
        logPanelAppender.start();
        RobotLogger.initializeLogging(logPanel, logPanelAppender);
    }

    public void setWindowClosingHandler(Consumer<WindowEvent> handler) {
        this.windowClosingHandler = handler;
    }

    public void addStatusComponent(Component component) {
        statusPanel.add(component);
        statusPanel.revalidate();
    }

    @Override
    public void dispose() {
        if (logPanelAppender != null) {
            logPanelAppender.stop();
            // Remove from logger
            ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger("ROBOT")
                .detachAppender(logPanelAppender);
            logPanelAppender = null;
        }
        super.dispose();
    }
} 