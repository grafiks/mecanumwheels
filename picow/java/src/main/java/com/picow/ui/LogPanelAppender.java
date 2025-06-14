package com.picow.ui;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.picow.RobotLogger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class LogPanelAppender extends AppenderBase<ILoggingEvent> {
    private JTextPane logPanel;
    private StyleContext styleContext;
    private Style defaultStyle;
    private Style telemetryStyle;    // Blue for TELEMETRY
    private Style sensorCommandStyle; // Green for SENSOR_COMMAND
    private Style motorCommandStyle;  // Red for MOTOR_COMMAND
    private final Gson gson = new Gson();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public LogPanelAppender(JTextPane logPanel) {
        this.logPanel = logPanel;
        this.styleContext = StyleContext.getDefaultStyleContext();
        
        // Create base style
        defaultStyle = styleContext.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);
        
        // Create styles for different log categories
        telemetryStyle = styleContext.addStyle("telemetry", defaultStyle);
        StyleConstants.setForeground(telemetryStyle, new Color(0, 0, 255)); // Blue
        
        sensorCommandStyle = styleContext.addStyle("sensor_command", defaultStyle);
        StyleConstants.setForeground(sensorCommandStyle, new Color(0, 128, 0)); // Green
        
        motorCommandStyle = styleContext.addStyle("motor_command", defaultStyle);
        StyleConstants.setForeground(motorCommandStyle, new Color(255, 0, 0)); // Red
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled.get()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                if (!enabled.get())
                    return;
                Document doc = logPanel.getDocument();
                
                // Parse the log message as JSON to get the category
                String message = event.getFormattedMessage();
                JsonObject logEntry = gson.fromJson(message, JsonObject.class);
                String category = logEntry.get("category").getAsString();
                
                // Get style based on category
                Style style = getStyleForCategory(category);
                
                // Format the log message with timestamp
                String formattedMessage = String.format("%s [%s] %s%n",
                    event.getTimeStamp(),
                    category,
                    message);

                // Append the message with appropriate style
                doc.insertString(doc.getLength(), formattedMessage, style);

                // Auto-scroll to the bottom
                logPanel.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Style getStyleForCategory(String category) {
        switch (category) {
            case RobotLogger.CATEGORY_TELEMETRY:
                return telemetryStyle;
            case RobotLogger.CATEGORY_SENSOR_COMMAND:
                return sensorCommandStyle;
            case RobotLogger.CATEGORY_MOTOR_COMMAND:
                return motorCommandStyle;
            default:
                return defaultStyle;
        }
    }
} 