package org.eeditiones.markdown;


import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarkdownModuleTest {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(false, true);

    @Test
    public void simple() throws XPathException, PermissionDeniedException, EXistException {
        executeAndAssert("Hello *world*", "<article><p>Hello <em>world</em></p></article>\n");
    }

    @Test
    public void fencedCodeBlock() throws XPathException, PermissionDeniedException, EXistException {
        executeAndAssert("\n```xml\n<test>xxx</test>\n```", "<article><pre class='language-xml'><code class='language-xml'>&lt;test&gt;xxx&lt;/test&gt;\n</code></pre></article>");
    }

    @Test
    public void autoDivs() throws XPathException, PermissionDeniedException, EXistException {
        executeAndAssert(
            "# 1\n## 2\n### 3\n# 1", 
            "<article><div><h1>1</h1><div><h2>2</h2><div><h3>3</h3></div></div></div><div><h1>1</h1></div></article>\n");
    }

    @Test
    public void linkRef() throws XPathException, PermissionDeniedException, EXistException {
        executeAndAssert(
            "[link1]: https://exist-db.org\n[Link][link1]", 
            "<article><p><a href=\"https:exist-db.org\" title=\"eXist Homepage\">Link</a></p></article>");
    }

    private void executeAndAssert(final String markdown, final String expected) throws EXistException, PermissionDeniedException, XPathException {
        final String query =
            "import module namespace md = \"https://e-editiones.org/exist-db/markdown\";\n" +
            "md:parse('" + markdown + "', map { 'indent': 0, 'divisions': true() })";
        final Sequence result = executeQuery(query);

        assertTrue(result.hasOne());

        final Source inExpected = Input.fromString(expected).build();
        final Source inActual = Input.fromNode((Document) result.itemAt(0)).build();

        final Diff diff = DiffBuilder.compare(inExpected)
                .withTest(inActual)
                .checkForSimilar()
                .build();

        assertFalse(diff.fullDescription(), diff.hasDifferences());
    }

    private Sequence executeQuery(final String xquery) throws EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            return xqueryService.execute(broker, xquery, null);
        }
    }
}
