package HuaweiApp;

import java.io.IOException;

import org.junit.Test;

import HuaweiUACApp.TemplateSourceFilePreprocessor;

public class TestTemplateSourceFilePreprocessor {

    @Test
    public void test() throws IOException {
        System.out.println(TemplateSourceFilePreprocessor.process("src/HuaweiApp/procedures/CommonProc.java"));
    }

}
