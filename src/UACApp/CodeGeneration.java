package UACApp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import UACApp.compiler.InMemoryJavaSourceCode;

public class CodeGeneration {

    public static final int MAX_BIZ_LOGIC_STMT_COUNT = 20;
    public static final String BIZ_LOGIC_NAME_PREFIX = "BizLogic";
    public static final int DB_TABLE_COUNT = 10;
    // UAC operation is triggered for every 5 seconds.
    public static final long UAC_INTERVAL = 5000;
    // Every time at most 15 new business logics will be added.
    public static final int MAX_BIZ_LOGIC_BATCH_SIZE = 15;
    // Every procedure will have a probability of 5% to be deleted in a UAC operation.
    public static final int PERCENTAGE_BE_DROPPED = 5;
    public static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    public static final String PACKAGE_PATH = "UACApp.procedures.";
    public static final String SRC_PATH = "src/UACApp/procedures/";

    // For the abstract business logic
    private static final String ABSTRACT_BIZ_LOGIC_CLASS_NAME = "AbstractBusinessLogic";
    private static final String ABSTRACT_BIZ_LOGIC_SOURCE_PATH = SRC_PATH + ABSTRACT_BIZ_LOGIC_CLASS_NAME + ".java";
    public static final InMemoryJavaSourceCode ABSTRACT_BIZ_LOGIC_SOURCE =
            new InMemoryJavaSourceCode(ABSTRACT_BIZ_LOGIC_CLASS_NAME,
                    TemplateSourceFilePreprocessor.process(ABSTRACT_BIZ_LOGIC_SOURCE_PATH));

    // For business logics
    public static final String BIZ_LOGIC_TEMPLATE_CLASS_NAME = "BusinessLogicTemplate";
    private static final String BIZ_LOGIC_TEMPLATE_SOURCE_PATH = SRC_PATH + BIZ_LOGIC_TEMPLATE_CLASS_NAME + ".java";
    public static final String BIZ_LOGIC_TEMPLATE_SOURCE = TemplateSourceFilePreprocessor.process(BIZ_LOGIC_TEMPLATE_SOURCE_PATH);

    // For the common procedure
    public static final String COMMON_PROC_TEMPLATE_CLASS_NAME = "CommonProcTemplate";
    public static final String COMMON_PROC_CLASS_NAME = "CommonProc";
    private static final String COMMON_PROC_TEMPLATE_SOURCE_PATH = SRC_PATH + COMMON_PROC_TEMPLATE_CLASS_NAME + ".java";
    public static final String COMMON_PROC_TEMPLATE_SOURCE =
            TemplateSourceFilePreprocessor.process(COMMON_PROC_TEMPLATE_SOURCE_PATH)
            .replaceAll(COMMON_PROC_TEMPLATE_CLASS_NAME, COMMON_PROC_CLASS_NAME);

    // For the weird library
    private static final String WEIRD_LIBRARY_SOURCE_PATH = SRC_PATH + "SomeWeirdLibrary.java";
    public static final InMemoryJavaSourceCode WEIRD_LIBRARY_SOURCE =
            new InMemoryJavaSourceCode("SomeWeirdLibrary",
                    TemplateSourceFilePreprocessor.process(WEIRD_LIBRARY_SOURCE_PATH));

    // Change to true to save the source code files on disk.
    public static final boolean WRITE_SOURCE_TO_DISK = false;

    public static void writeSourceToDiskIfNeeded(InMemoryJavaSourceCode source) {
        if (! WRITE_SOURCE_TO_DISK) {
            return;
        }
        if (source.getName().equals(ABSTRACT_BIZ_LOGIC_CLASS_NAME + ".java")) {
            return;
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(SRC_PATH + source.getName()));
            bw.write(source.getCharContent(true).toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
