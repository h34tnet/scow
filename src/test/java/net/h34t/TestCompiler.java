package net.h34t;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class TestCompiler {

    private TestCompiler() {
    }

    public static boolean test(List<File> files) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {

            // List<File> ffiles = files.stream().map(Path::toFile).collect(Collectors.toList());

            List<String> optionList = new ArrayList<>();
            optionList.add("-classpath");
            optionList.add(System.getProperty("java.class.path") + ";dist/InlineCompiler.jar");

            Iterable<? extends JavaFileObject> compilationUnit
                    = fileManager.getJavaFileObjectsFromFiles(files);

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    optionList,
                    null,
                    compilationUnit);

            if (task.call()) {
                return true;
            } else {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    System.out.format("Error %s on line %d in %s%n",
                            diagnostic.getMessage(Locale.getDefault()),
                            diagnostic.getLineNumber(),
                            diagnostic.getSource().toUri());
                }

                // Assert.fail("Compilation failed");

                return false;
            }
        }
    }
}
