package UACApp;

import java.util.Map;
import java.util.Map.Entry;

import org.voltdb.utils.InMemoryJarfile;

import UACApp.compiler.InMemoryClassLoader;
import UACApp.compiler.InMemoryJavaByteCode;

public class VoltInMemoryJarFileBuilder {

    public static InMemoryJarfile buildFromInMemoryClassLoader(InMemoryClassLoader classLoader) throws Exception {
        InMemoryJarfile jarFile = new InMemoryJarfile();
        Map<String, InMemoryJavaByteCode> classByteCodeMap = classLoader.getClassByteCodeMap();
        for (Entry<String, InMemoryJavaByteCode> entry : classByteCodeMap.entrySet()) {
            String packagePath = entry.getKey();
            packagePath = packagePath.replace('.', '/');
            packagePath += ".class";
            jarFile.put(packagePath, entry.getValue().getBytes());
        }
        return jarFile;
    }
}
