package UACApp.compiler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

import UACApp.CodeGeneration;

public class InMemoryJavaSourceCode extends SimpleJavaFileObject {

    private final String m_codeString;

    public InMemoryJavaSourceCode(String className, String codeString) {
        super(URI.create(
                String.format("source:///%s%s",
                className.replace('.', '/'),
                Kind.SOURCE.extension)),
              Kind.SOURCE);
        m_codeString = codeString;
        CodeGeneration.writeSourceToDiskIfNeeded(this);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return m_codeString;
    }

    @Override
    public OutputStream openOutputStream() {
        throw new IllegalStateException();
    }

    @Override
    public InputStream openInputStream() {
        throw new IllegalStateException();
    }
}

