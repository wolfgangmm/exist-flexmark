package org.eeditiones.markdown;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.memtree.NodeImpl;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.modules.ModuleUtils;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.CodeBlock;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableCell;

public class XHTMLRenderer {

    private MemTreeBuilder builder;
    private XQueryContext context;
    private boolean createDivisions;
    private ReferenceRepository referenceRepository;
    private Stack<Heading> headingStack = new Stack<>();

    public XHTMLRenderer(XQueryContext context, MemTreeBuilder builder, boolean divisions, ReferenceRepository referenceRepository) {
        this.context = context;
        this.builder = builder;
        this.createDivisions = divisions;
        this.referenceRepository = referenceRepository;
    }
    
    public void render(@NotNull Node node) {
        headingStack.clear();

        builder.startElement("", "article", "article", null);
        visitor.visit(node);

        if (headingStack.size() > 0) {
            for (int i = 0; i < headingStack.size(); i++) {
                builder.endElement();
            }
        }

        builder.endElement();
    }

    final NodeVisitor visitor = new NodeVisitor(
        new VisitHandler<>(Text.class, this::visit),
        new VisitHandler<>(Emphasis.class, this::visit),
        new VisitHandler<>(StrongEmphasis.class, this::visit),
        new VisitHandler<>(Paragraph.class, this::visit),
        new VisitHandler<>(Heading.class, this::visit),
        new VisitHandler<>(HtmlBlock.class, this::visitHtml),
        new VisitHandler<>(HtmlInline.class, this::visitHtml),
        new VisitHandler<>(ListBlock.class, this::visit),
        new VisitHandler<>(ListItem.class, this::visit),
        new VisitHandler<>(OrderedList.class, this::visit),
        new VisitHandler<>(OrderedListItem.class, this::visit),
        new VisitHandler<>(BlockQuote.class, this::visit),
        new VisitHandler<>(CodeBlock.class, this::visit),
        new VisitHandler<>(FencedCodeBlock.class, this::visit),
        new VisitHandler<>(Code.class, this::visit),
        new VisitHandler<>(Link.class, this::visit),
        new VisitHandler<>(LinkRef.class, this::visit),
        new VisitHandler<>(Image.class, this::visit),
        new VisitHandler<>(SoftLineBreak.class, this::visit),
        new VisitHandler<>(HardLineBreak.class, this::visit),
        new VisitHandler<>(TableBlock.class, this::visit),
        new VisitHandler<>(TableHead.class, this::visit),
        new VisitHandler<>(TableBody.class, this::visit),
        new VisitHandler<>(TableRow.class, this::visit),
        new VisitHandler<>(TableCell.class, this::visit),
        new VisitHandler<>(Reference.class, this::visit)
    );
    
    public void visit(Heading heading) {
        Heading lastHeading = headingStack.isEmpty() ? null : headingStack.peek();
        while (lastHeading != null && lastHeading.getLevel() >= heading.getLevel()) {
            headingStack.pop();
            builder.endElement();
            lastHeading = headingStack.isEmpty() ? null : headingStack.peek();
        }

        if (createDivisions) {
            builder.startElement("", "div", "div", null);
            headingStack.push(heading);
        }
        builder.startElement("", "h" + heading.getLevel(), "h" + heading.getLevel(), null);
        visitor.visitChildren(heading);
        builder.endElement();
    }

    public void visit(Text text) {
        builder.characters(text.getChars());
    }

    public void visit(Emphasis emphasis) {
        builder.startElement("", "em", "em", null);
        visitor.visitChildren(emphasis);
        builder.endElement();
    }

    public void visit(StrongEmphasis strongEmphasis) {
        builder.startElement("", "strong", "strong", null);
        visitor.visitChildren(strongEmphasis);
        builder.endElement();
    }

    public void visit(Paragraph paragraph) {
        builder.startElement("", "p", "p", null);
        visitor.visitChildren(paragraph);
        builder.endElement();
    }

    public void visitHtml(Node node) {
        final Map<String, Boolean> features = new HashMap<>();
        features.put("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
        features.put("http://cyberneko.org/html/features/insert-doctype", false);

        final StringReader reader = new StringReader(node.getChars().toString());
        try {
            final DocumentImpl result = ModuleUtils.htmlToXHtml(context, new InputSource(reader), features, null);
            final DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(builder);
            result.copyTo((NodeImpl)result.getFirstChild(), receiver);
        } catch (IOException | SAXException e) {
            // 
        }
    }

    public void visit(ListBlock listBlock) {
        builder.startElement("", "ul", "ul", null);
        visitor.visitChildren(listBlock);
        builder.endElement();
    }

    public void visit(ListItem listItem) {
        builder.startElement("", "li", "li", null);
        visitor.visitChildren(listItem);
        builder.endElement();
    }

    public void visit(OrderedList orderedList) {
        builder.startElement("", "ol", "ol", null);
        visitor.visitChildren(orderedList);
        builder.endElement();
    }

    public void visit(OrderedListItem orderedListItem) {
        builder.startElement("", "li", "li", null);
        visitor.visitChildren(orderedListItem);
        builder.endElement();
    }

    public void visit(BlockQuote blockQuote) {
        builder.startElement("", "blockquote", "blockquote", null);
        visitor.visitChildren(blockQuote);
        builder.endElement();
    }

    public void visit(CodeBlock codeBlock) {
        builder.startElement("", "pre", "pre", null);
        builder.startElement("", "code", "code", null);
        builder.characters(codeBlock.getChars());
        builder.endElement();
        builder.endElement();
    }

    public void visit(FencedCodeBlock fencedCodeBlock) {
        AttributesImpl attributes = new AttributesImpl();
        if (!fencedCodeBlock.getInfo().isEmpty()) {
            attributes.addAttribute(null, "class", "class", "CDATA", "language-" + fencedCodeBlock.getInfo().toString());
        }

        builder.startElement("", "pre", "pre", attributes);
        builder.startElement("", "code", "code", attributes);
        builder.characters(fencedCodeBlock.getContentChars());
        builder.endElement();
        builder.endElement();
    }

    public void visit(Code code) {
        builder.startElement("", "code", "code", null);
        builder.characters(code.getChars());
        builder.endElement();
    }

    public void visit(Link link) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "href", "href", "CDATA", link.getUrl().toString());
        if (!link.getTitle().isEmpty()) {
            attributes.addAttribute(null, "title", "title", "CDATA", link.getTitle().toString());
        }

        builder.startElement("", "a", "a", attributes);
        visitor.visitChildren(link);
        builder.endElement();
    }

    public void visit(LinkRef linkRef) {
        final Set<Reference> references = referenceRepository.getReferencedElements(linkRef.getDocument());
        String url;
        Reference reference = null;
        if (!linkRef.isDefined()) {
            if (linkRef.getReferenceNode(referenceRepository) != null) {
                linkRef.setDefined(true);
            }
        }
        
        if (linkRef.isDefined()) {
            reference = linkRef.getReferenceNode(referenceRepository);
            url = reference.getUrl().unescape();
        } else {
            url = linkRef.getReference().unescape();
        }

        if (url == null) {
            builder.characters(linkRef.getText());
            return;
        }

        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "href", "href", "CDATA", url.toString());
        if (!linkRef.getText().isEmpty()) {
            attributes.addAttribute(null, "title", "title", "CDATA", linkRef.getText().toString());
        }

        builder.startElement("", "a", "a", attributes);
        visitor.visitChildren(linkRef);
        builder.endElement();
    }

    public void visit(Image image) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, "src", "src", "CDATA", image.getUrl().toString());
        if (!image.getTitle().isEmpty()) {
            attributes.addAttribute(null, "title", "title", "CDATA", image.getTitle().toString());
        }
        if (!image.getText().isEmpty()) {
            attributes.addAttribute(null, "alt", "alt", "CDATA", image.getText().toString());
        }

        builder.startElement("", "img", "img", attributes);
        builder.endElement();
    }

    public void visit(HardLineBreak softLineBreak) {
        builder.startElement(null, "br", "br", null);
        builder.endElement();
    }

    public void visit(SoftLineBreak softLineBreak) {
        builder.characters("\n");
    }

    public void visit(TableRow tableRow) {
        builder.startElement("", "tr", "tr", null);
        visitor.visitChildren(tableRow);
        builder.endElement();
    }

    public void visit(TableCell tableCell) {
        AttributesImpl attributes = new AttributesImpl();
        if (tableCell.getSpan() > 1) {
            attributes.addAttribute(null, "colspan", "colspan", "CDATA", Integer.toString(tableCell.getSpan()));
        }

        builder.startElement("", "td", "td", attributes);
        visitor.visitChildren(tableCell);
        builder.endElement();
    }

    public void visit(TableHead tableHead) {
        builder.startElement("", "thead", "thead", null);
        visitor.visitChildren(tableHead);
        builder.endElement();
    }

    public void visit(TableBody tableBody) {
        builder.startElement("", "tbody", "tbody", null);
        visitor.visitChildren(tableBody);
        builder.endElement();
    }

    public void visit(TableBlock tableBlock) {
        builder.startElement("", "table", "table", null);
        visitor.visitChildren(tableBlock);
        builder.endElement();
    }

    public void visit(Reference reference) {
        // ignore
    }
}