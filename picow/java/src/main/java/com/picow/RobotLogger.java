package com.picow;

import javax.swing.JTextPane;
import java.time.Instant;
import com.google.gson.Gson;
import com.picow.ui.LogPanelAppender;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.LoggerFactory;

public class RobotLogger {
    // Category constants for robot data
    public static final String CATEGORY_TELEMETRY = "TELEMETRY";
    public static final String CATEGORY_SENSOR_COMMAND = "SENSOR_COMMAND";
    public static final String CATEGORY_MOTOR_COMMAND = "MOTOR_COMMAND";

    // Single logger instance for all logging
    private static final Logger log = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("ROBOT");
    private static final Gson gson = new Gson();

    // Robot data logging methods - these will show up in both file and panel with colors
    public static void logTelemetry(Object data) {
        LogEntry entry = new LogEntry(CATEGORY_TELEMETRY, data, Instant.now().toEpochMilli());
        log.info(gson.toJson(entry));
    }

    public static void logSensorPullCommand(Object data) {
        LogEntry entry = new LogEntry(CATEGORY_SENSOR_COMMAND, data, Instant.now().toEpochMilli());
        log.info(gson.toJson(entry));
    }

    public static void logMotorCommand(Object data) {
        LogEntry entry = new LogEntry(CATEGORY_MOTOR_COMMAND, data, Instant.now().toEpochMilli());
        log.info(gson.toJson(entry));
    }

    // System logging methods - these will only go to the log file
    public static void info(String message, Object... args) {
        log.info(message, args);
    }

    public static void error(String message, Object... args) {
        log.error(message, args);
    }

    public static void error(String message, Throwable t) {
        log.error(message, t);
    }

    private static class LogEntry {
        private final String category;  // One of the CATEGORY_* constants
        private final Object data;      // the actual data
        private final long timestamp;   // milliseconds since epoch

        public LogEntry(String category, Object data, long timestamp) {
            this.category = category;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
    
    public static void initializeLogging(JTextPane logPanel, LogPanelAppender panelAppender) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        // Create file appender for all logs
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("ROBOT_FILE");
        fileAppender.setFile("logs/robot.log");

        // Set up rolling policy
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern("logs/robot.%d{yyyy-MM-dd}.log");
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.start();

        // Set up encoder
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level - %msg%n");
        fileEncoder.start();

        fileAppender.setEncoder(fileEncoder);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        // Configure the unified logger
        Logger robotLogger = loggerContext.getLogger("ROBOT");
        robotLogger.setLevel(Level.INFO);
        robotLogger.setAdditive(false);  // Don't propagate to parent loggers
        robotLogger.addAppender(fileAppender);  // All logs go to file
        
        // Add the provided panel appender
        if (panelAppender != null) {
            robotLogger.addAppender(panelAppender);
        }
    }
} 