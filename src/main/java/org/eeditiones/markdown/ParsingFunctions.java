package org.eeditiones.markdown;

import org.exist.dom.QName;
import org.exist.dom.QName.IllegalQNameException;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.AbstractMapType;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import static org.exist.xquery.FunctionDSL.*;

import java.util.Arrays;

import static org.eeditiones.markdown.MarkdownModule.functionSignature;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;

/**
 * Some very simple XQuery example functions implemented
 * in Java.
 */
public class ParsingFunctions extends BasicFunction {

    private static final String FS_PARSE_NAME = "parse";
    static final FunctionSignature FS_PARSE = functionSignature(
        FS_PARSE_NAME,
        "An example function that returns <hello>world</hello>.",
        returns(Type.DOCUMENT),
        param("markdown", Type.STRING, "A markdown string"),
        param("options", Type.MAP, "Options for the parser")
    );

    public ParsingFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final AbstractMapType options = (AbstractMapType) args[1].itemAt(0);

        switch (getName().getLocalPart()) {

            case FS_PARSE_NAME:
            final StringValue markdown = (StringValue) args[0].itemAt(0);
                return parse(markdown, options);

            default:
                throw new XPathException(this, "No function: " + getName() + "#" + getSignature().getArgumentCount());
        }
    }

    private DocumentImpl parse(final StringValue markdown, final AbstractMapType options) throws XPathException {
        final MutableDataSet parserOptions = new MutableDataSet();
        parserOptions.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AnchorLinkExtension.create()));
        parserOptions.set(HtmlRenderer.RENDER_HEADER_ID, true);

        final Sequence indentOpt = options.get(new StringValue("indent"));
        if (!indentOpt.isEmpty()) {
            final int indent = ((IntegerValue)indentOpt.itemAt(0).convertTo(Type.INTEGER)).getInt();
            parserOptions.set(HtmlRenderer.INDENT_SIZE, indent);
        }

        boolean createDivisions = false;
        final Sequence divsOpt = options.get(new StringValue("divisions"));
        if (!divsOpt.isEmpty()) {
            createDivisions = ((BooleanValue)divsOpt.itemAt(0).convertTo(Type.BOOLEAN)).effectiveBooleanValue();
        }

        final Parser parser = Parser.builder(parserOptions).build();
        final Node document = parser.parse(markdown.getStringValue());

        final ReferenceRepository referenceRepository = Parser.REFERENCES.get(parserOptions);
        final MemTreeBuilder builder = context.getDocumentBuilder();
        builder.startDocument();
        final XHTMLRenderer xmlRenderer = new XHTMLRenderer(context, builder, createDivisions, referenceRepository);
        xmlRenderer.render(document);
        builder.endDocument();
        return builder.getDocument();
    }
}
