package com.restaurant.builder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PackageLayoutTest {
    @Test void productionSourcesUseSixPatternPackagesAndOneRootEntryPoint() throws Exception {
        Path root = Path.of("src/main/java/com/restaurant");
        try (var entries = Files.list(root)) {
            Set<String> directories = entries.filter(Files::isDirectory)
                .map(path -> path.getFileName().toString()).collect(Collectors.toSet());
            assertThat(directories).containsExactlyInAnyOrder("builder", "factory", "strategy", "bridge", "observer", "decorator");
        }
        try (var entries = Files.list(root)) {
            assertThat(entries.filter(path -> path.toString().endsWith(".java"))
                .map(path -> path.getFileName().toString()).toList()).containsExactly("Main.java");
        }
        try (var sources = Files.walk(root)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String expected = "com.restaurant";
                Path parent = root.relativize(source.getParent());
                if (!parent.toString().isEmpty()) expected += "." + parent.toString().replace(java.io.File.separatorChar, '.');
                assertThat(Files.readString(source)).as(source.toString()).contains("package " + expected + ";");
            }
        }
    }
}
