package HuaweiApp;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

import HuaweiUACApp.compiler.InMemoryJavaCompiler;
import HuaweiUACApp.compiler.InMemoryJavaSourceCode;

public class TestJavaCompiler {

    @Test
    public void test() throws IOException {
        String source = "public class test {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(\"hello world\");\n" +
                        "    }\n" +
                        "}\n";
        InMemoryJavaSourceCode sourceObject = new InMemoryJavaSourceCode("test", source);

        InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
        compiler.addInMemoryJavaSource(sourceObject);

        if (compiler.compile()) {
            try {
                Class<?> testClass = Class.forName("test", true, compiler.getClassLoader());
                Method mainMethod = testClass.getDeclaredMethod("main", new Class[] { String[].class });
                mainMethod.invoke(null, new Object[] { null });
            } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {
            fail("Compilation failed.");
        }
    }

}
