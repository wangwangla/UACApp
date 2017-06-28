package UACApp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import UACApp.compiler.InMemoryJavaSourceCode;

import java.util.TreeMap;

public class BusinessLogic {

    private final String m_name;
    private final Map<Integer, String> m_queries = new TreeMap<>();
    private final InMemoryJavaSourceCode m_source;

    public BusinessLogic(String name, List<String> queries, int querySeqIDStart) {
        m_name = name;
        StringBuilder codeBuilder = new StringBuilder();
        for (String query : queries) {
            m_queries.put(querySeqIDStart, query);
            codeBuilder.append(
                    String.format("%squeriesToQueue.add(CommonProc.stmt%06d);\n",
                            INDENTATION, querySeqIDStart++));
        }
        m_source = new InMemoryJavaSourceCode(m_name, String.format(
                            CodeGeneration.BIZ_LOGIC_TEMPLATE_SOURCE.replaceAll(
                                    CodeGeneration.BIZ_LOGIC_TEMPLATE_CLASS_NAME, m_name),
                            codeBuilder.toString()));
    }

    public InMemoryJavaSourceCode getJavaSourceCode() {
        return m_source;
    }

    public Map<Integer, String> getQueries() {
        return Collections.unmodifiableMap(m_queries);
    }

    public String getName() {
        return m_name;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append(
                String.format("Business Logic [%s], %d queries:\n",
                        m_name, m_queries.size()));
        for (Entry<Integer, String> entry : m_queries.entrySet()) {
            toStringBuilder.append(String.format("%6d  %s\n", entry.getKey(), entry.getValue()));
        }
        return toStringBuilder.toString();
    }

    private static final String INDENTATION = "        ";
}
