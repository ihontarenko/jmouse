package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;

import java.util.ArrayList;
import java.util.List;

public final class JvmClassTypeSignatureParser
        implements ParseRule<JvmTextInput, JvmClassTypeSignatureNode> {
    @Override
    public JvmClassTypeSignatureNode parse(JvmTextInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JvmSignatureToken.CLASS, session);
        int packageStart = input.offset();
        int segmentStart = packageStart;
        TokenCursor.Savepoint segment = input.cursor().savepoint();
        while (true) {
            JvmSignatureIdentifiers.read(input, session);
            if (!input.take(JvmSignatureToken.PACKAGE_SEPARATOR, session)) {
                break;
            }
            segmentStart = input.offset();
            segment = input.cursor().savepoint();
        }
        String packageSpecifier = input.slice(packageStart, segmentStart);
        // Reuse the common token cursor checkpoint for the final simple type, including its
        // arguments.
        input.cursor().restore(segment);
        List<JvmClassTypeSegmentNode> segments = new ArrayList<>();
        segments.add(session.parse(JvmSignatureRoles.SEGMENT, input));
        while (input.take(JvmSignatureToken.INNER_TYPE, session)) {
            segments.add(session.parse(JvmSignatureRoles.SEGMENT, input));
        }
        input.expect(JvmSignatureToken.SEMICOLON, session);
        return new JvmClassTypeSignatureNode(
                input.cursor().source(), start, input.offset(), packageSpecifier, segments);
    }
}
