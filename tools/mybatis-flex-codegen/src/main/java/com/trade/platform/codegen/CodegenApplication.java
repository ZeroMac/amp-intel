package com.hl.platform.codegen;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CodegenApplication {

    private CodegenApplication() {
    }

    public static void main(String[] args) {
        String profileName = args.length > 0 ? args[0] : "system";
        CodegenProfile profile = loadProfile(profileName);
        validateProfile(profile, profileName);

        Path repoRoot = findRepoRoot();
        Path sourceDir = repoRoot.resolve(profile.getModule()).resolve("src/main/java").normalize();

        String jdbcUrl = requireEnv("CODEGEN_DB_URL");
        String username = requireEnv("CODEGEN_DB_USERNAME");
        String password = requireEnv("CODEGEN_DB_PASSWORD");

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.getPackageConfig()
                .setSourceDir(sourceDir.toString())
                .setBasePackage(profile.getBasePackage());

        if (hasText(profile.getTablePrefix())) {
            globalConfig.getStrategyConfig().setTablePrefix(profile.getTablePrefix());
        }
        if (hasText(profile.getSchema())) {
            globalConfig.getStrategyConfig().setGenerateSchema(profile.getSchema());
        }
        if (profile.getTables() != null && !profile.getTables().isEmpty()) {
            globalConfig.getStrategyConfig()
                    .setGenerateTable(profile.getTables().toArray(String[]::new));
        }

        globalConfig.enableEntity().setJdkVersion(21);
        globalConfig.enableMapper();

        System.out.printf("Codegen profile: %s%n", profileName);
        System.out.printf("Output: %s%n", sourceDir);
        System.out.printf("Base package: %s%n", profile.getBasePackage());

        try (HikariDataSource dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            new Generator(dataSource, globalConfig).generate();
        }
    }

    private static CodegenProfile loadProfile(String profileName) {
        String resource = "profiles/" + profileName + ".yml";
        try (InputStream input = CodegenApplication.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Codegen profile not found: " + resource);
            }
            return new Yaml().loadAs(input, CodegenProfile.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load codegen profile: " + resource, e);
        }
    }

    private static void validateProfile(CodegenProfile profile, String profileName) {
        if (profile == null || !hasText(profile.getModule()) || !hasText(profile.getBasePackage())) {
            throw new IllegalArgumentException(
                    "Invalid codegen profile '" + profileName + "': module and basePackage are required");
        }
    }

    private static Path findRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("mvnw")) || Files.exists(current.resolve("mvnw.cmd"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root not found from user.dir=" + System.getProperty("user.dir"));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (!hasText(value)) {
            throw new IllegalStateException("Required environment variable is not set: " + name);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
