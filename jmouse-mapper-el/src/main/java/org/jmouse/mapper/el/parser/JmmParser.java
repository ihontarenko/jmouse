package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.mapper.el.lexer.JmmToken;
import org.jmouse.mapper.el.node.MappingDocumentNode;
import org.jmouse.mapper.el.node.RuleBlockNode;
import org.jmouse.mapper.el.node.UseNode;

/**
 * Reads a whole {@code .jmm} file: {@code mapping "name" { … }}.
 *
 * <h2>⚠️ Recursive descent, and no parser registry — unlike {@code .jmp}</h2>
 *
 * <p>The policy language registers a parser per construction and dispatches by priority because its
 * shapes overlap: a scope declaration and a grant both open {@code @ NAME NAME}, so something has to
 * decide between them. <strong>This grammar has no such position.</strong> Inside a {@code mapping}
 * body the only words are {@code use}, {@code fragment} and {@code target}; inside a {@code target},
 * {@code unmapped}, {@code refuse}, {@code always} and {@code from}; inside a rule block, {@code let},
 * {@code include}, or a rule. Every one is keyword-led or the default, and nothing is ambiguous.</p>
 *
 * <p>Registering nine parsers to be asked "do you support this?" in priority order would be machinery
 * answering a question that never gets asked — and it would hide the one property this grammar has and
 * that one does not.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JmmParser {

    private JmmParser() {
    }

    /**
     * Reads a document.
     *
     * @param cursor the cursor over a whole file
     * @return the document
     * @throws JmmSyntaxException when the file cannot be read, naming the line
     */
    public static MappingDocumentNode parse(TokenCursor cursor) {
        MappingDocumentNode document = new MappingDocumentNode();

        Separators.skip(cursor);
        cursor.ensure(JmmToken.T_MAPPING);
        document.setName(SourceReading.literal(cursor.ensure(BasicToken.T_STRING)));

        cursor.ensure(BasicToken.T_OPEN_CURLY);
        Separators.skip(cursor);

        while (cursor.hasNext() && !cursor.isCurrent(BasicToken.T_CLOSE_CURLY)) {
            readEntry(cursor, document);
            Separators.skip(cursor);
        }

        cursor.ensure(BasicToken.T_CLOSE_CURLY);

        return document;
    }

    /**
     * Reads one construction at document level.
     *
     * @param cursor   the cursor, positioned on its first token
     * @param document the document being filled
     */
    private static void readEntry(TokenCursor cursor, MappingDocumentNode document) {
        if (cursor.isCurrent(JmmToken.T_USE)) {
            readImport(cursor, document);
            return;
        }

        if (cursor.isCurrent(JmmToken.T_FRAGMENT)) {
            readFragment(cursor, document);
            return;
        }

        if (cursor.isCurrent(JmmToken.T_TARGET)) {
            document.add(TargetParser.parse(cursor));
            return;
        }

        throw new JmmSyntaxException(cursor,
                "a mapping file holds 'use', 'fragment' and 'target' at the top level");
    }

    /**
     * Reads {@code use fully.qualified.Type}.
     *
     * @param cursor   the cursor, positioned on {@code use}
     * @param document the document being filled
     */
    private static void readImport(TokenCursor cursor, MappingDocumentNode document) {
        UseNode node = new UseNode();

        cursor.ensure(JmmToken.T_USE);
        node.setQualifiedName(TypeNames.read(cursor));

        UseNode existing = document.add(node);

        if (existing != null) {
            throw new JmmSyntaxException(cursor, ("'%s' is imported twice — as '%s' and as '%s'. A file "
                    + "where a name means whichever line came last is a file nobody can read")
                    .formatted(node.getSimpleName(), existing.getQualifiedName(), node.getQualifiedName()));
        }
    }

    /**
     * Reads {@code fragment name { … }}.
     *
     * @param cursor   the cursor, positioned on {@code fragment}
     * @param document the document being filled
     */
    private static void readFragment(TokenCursor cursor, MappingDocumentNode document) {
        cursor.ensure(JmmToken.T_FRAGMENT);

        String        name  = cursor.ensure(JmmToken.nameTokens()).value();
        RuleBlockNode rules = RuleBlockParser.parse(cursor);

        // ⚠️ Flat by design. A fragment including another would need a resolution order and could be
        // written as a cycle; refusing it here costs a line and removes both problems permanently.
        if (!rules.getIncludes().isEmpty()) {
            throw new JmmSyntaxException(cursor,
                    "a fragment does not include another fragment — write the rules out");
        }

        if (document.add(name, rules) != null) {
            throw new JmmSyntaxException(cursor, "'%s' is declared twice".formatted(name));
        }
    }
}
