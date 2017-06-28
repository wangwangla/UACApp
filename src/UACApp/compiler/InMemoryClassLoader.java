package UACApp.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InMemoryClassLoader extends ClassLoader {

    private Map<String, InMemoryJavaByteCode> m_classByteCodeMap = new HashMap<>();
    private Map<String, Class<?>> m_definedClasses = new HashMap<>();

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = m_definedClasses.get(name);
        if (clazz == null) {
            InMemoryJavaByteCode byteCode = m_classByteCodeMap.get(name);
            if (byteCode == null) {
                return super.findClass(name);
            }
            clazz = defineClass(name, byteCode.getBytes(), 0, byteCode.getBytes().length);
            m_definedClasses.put(name, clazz);
        }
        return clazz;
    }

    public void addClass(String name, InMemoryJavaByteCode byteCode) {
        m_classByteCodeMap.put(name, byteCode);
    }

    public Map<String, InMemoryJavaByteCode> getClassByteCodeMap() {
        return Collections.unmodifiableMap(m_classByteCodeMap);
    }
}
