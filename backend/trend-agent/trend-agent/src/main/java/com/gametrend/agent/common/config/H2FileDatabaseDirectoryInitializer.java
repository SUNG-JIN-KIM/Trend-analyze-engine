package com.gametrend.agent.common.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class H2FileDatabaseDirectoryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String H2_FILE_PREFIX = "jdbc:h2:file:";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        createDirectoryIfH2FileDatabase(applicationContext.getEnvironment().getProperty(DATASOURCE_URL_PROPERTY));
    }

    static void createDirectoryIfH2FileDatabase(String datasourceUrl) {
        if (datasourceUrl == null || !datasourceUrl.toLowerCase(Locale.ROOT).startsWith(H2_FILE_PREFIX)) {
            return;
        }

        String databasePathValue = datasourceUrl.substring(H2_FILE_PREFIX.length());
        int optionStartIndex = databasePathValue.indexOf(';');
        if (optionStartIndex >= 0) {
            databasePathValue = databasePathValue.substring(0, optionStartIndex);
        }

        if (databasePathValue.isBlank() || databasePathValue.startsWith("mem:")) {
            return;
        }

        Path databasePath = resolveDatabasePath(databasePathValue);
        Path dataDirectory = databasePath.getParent();
        if (dataDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("H2 file DB data directory를 생성하지 못했습니다: " + dataDirectory, exception);
        }
    }

    private static Path resolveDatabasePath(String databasePathValue) {
        if (databasePathValue.equals("~") || databasePathValue.startsWith("~/") || databasePathValue.startsWith("~\\")) {
            String homeRelativePath = databasePathValue.substring(1);
            while (homeRelativePath.startsWith("/") || homeRelativePath.startsWith("\\")) {
                homeRelativePath = homeRelativePath.substring(1);
            }
            return Path.of(System.getProperty("user.home"), homeRelativePath);
        }
        return Path.of(databasePathValue);
    }
}
