package HuaweiUACApp.compiler;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class InMemoryJavaCompiler {

    private final JavaCompiler m_compiler;
    private final DiagnosticCollector<JavaFileObject> m_diagnostics;
    private final InMemoryFileManager m_fileManager;
    private final StandardJavaFileManager m_standardFileManager;
    private final List<InMemoryJavaSourceCode> m_sources;

    public InMemoryJavaCompiler() {
        m_compiler = ToolProvider.getSystemJavaCompiler();
        m_diagnostics = new DiagnosticCollector<>();
        m_standardFileManager = m_compiler.getStandardFileManager(m_diagnostics, null, null);
        m_fileManager = new InMemoryFileManager(m_standardFileManager);
        m_sources = new ArrayList<>();
    }

    public void addInMemoryJavaSource(InMemoryJavaSourceCode source) {
        m_sources.add(source);
    }

    public ClassLoader getClassLoader() {
        return m_fileManager.getClassLoader(null);
    }

    public boolean compile() {
        CompilationTask compilationTask = m_compiler.getTask(
                null, m_fileManager, null, null, null, m_sources);
        return compilationTask.call();
    }

    public String getDiagnostics() {
        StringBuilder builder = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : m_diagnostics.getDiagnostics()) {
            builder.append(diagnostic.getCode()).append("\n");
            builder.append(diagnostic.getKind()).append("\n");
            builder.append(diagnostic.getPosition()).append("\n");
            builder.append(diagnostic.getStartPosition()).append("\n");
            builder.append(diagnostic.getEndPosition()).append("\n");
            builder.append(diagnostic.getSource()).append("\n");
            builder.append(diagnostic.getMessage(null)).append("\n\n");
        }
        return builder.toString();
    }
}
