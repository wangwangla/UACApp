package HuaweiUACApp.compiler;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final InMemoryClassLoader m_classLoader = new InMemoryClassLoader();

    public InMemoryFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        InMemoryJavaByteCode byteCode = new InMemoryJavaByteCode(name);
        m_classLoader.addClass(name, byteCode);
        return byteCode;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return m_classLoader;
    }
}
