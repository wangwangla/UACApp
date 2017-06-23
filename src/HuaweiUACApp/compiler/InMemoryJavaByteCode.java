package HuaweiUACApp.compiler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class InMemoryJavaByteCode extends SimpleJavaFileObject {

    private ByteArrayOutputStream m_outputStream;

    public InMemoryJavaByteCode(String className) {
        super(URI.create(
                String.format("class:///%s%s",
                className.replace('.', '/'),
                Kind.CLASS.extension)),
              Kind.CLASS);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new IllegalStateException();
    }

    @Override
    public OutputStream openOutputStream() {
        m_outputStream = new ByteArrayOutputStream();
        return m_outputStream;
    }

    @Override
    public InputStream openInputStream() {
        throw new IllegalStateException();
    }

    public byte[] getBytes() {
        return m_outputStream.toByteArray();
    }
}
