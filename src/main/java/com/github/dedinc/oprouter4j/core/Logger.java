package com.github.dedinc.oprouter4j.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Simple logging utility for OpRouter4j.
 */
public class Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String name;
    private final Config config;
    private final Level level;

    public Logger(String name, Config config) {
        this.name = name;
        this.config = config;
        this.level = parseLevel(config.getLogLevel());
    }

    private Level parseLevel(String levelStr) {
        try {
            return Level.parse(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Level.INFO;
        }
    }

    public void debug(String message) {
        log(Level.FINE, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void error(String message) {
        log(Level.SEVERE, message);
    }

    public void error(String message, Throwable throwable) {
        log(Level.SEVERE, message + " - " + throwable.getMessage());
        if (config.isEnableLogging()) {
            throwable.printStackTrace();
        }
    }

    private void log(Level logLevel, String message) {
        if (!config.isEnableLogging() && logLevel.intValue() < Level.SEVERE.intValue()) {
            return;
        }

        if (logLevel.intValue() < this.level.intValue()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formattedMessage = String.format("%s - %s - %s - %s%n",
                timestamp, name, logLevel.getName(), message);

        // Write to file only (suppress console spam)
        if (config.isEnableLogging()) {
            try {
                Path logPath = Paths.get(config.getLogFile());
                Files.write(logPath, formattedMessage.getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                // Silently fail - don't spam console
            }
        }

        // Only write to console for SEVERE errors
        // All other logging goes to file only to prevent spam
        if (logLevel.intValue() >= Level.SEVERE.intValue()) {
            System.err.print(formattedMessage);
        }
    }

    public static Logger getLogger(String name) {
        return new Logger(name, Config.load());
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }
}

