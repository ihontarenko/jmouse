package org.jmouse.expression;

import org.jmouse.common.ast.configurer.Configurator;
import org.jmouse.common.ast.recognizer.PatternTokenRecognizer;
import org.jmouse.common.ast.token.Tokenizer;
import org.jmouse.common.ast.token.TokenizerPattern;

public class TokenizerConfigurator implements Configurator<Tokenizer> {

    public static final String ANNOTATION = "\\@\\w+";
    public static final String CLASS_NAME = "(\\w+(?:\\.\\w+)+)";
    public static final String VARIABLE = "\\#\\w+";
    public static final String PATH_VARIABLE = "\\:\\w+\\??";

    @Override
    public void configure(Tokenizer tokenizer) {
        new org.jmouse.common.ast.token.TokenizerConfigurator().configure(tokenizer);

        tokenizer.addPattern(new TokenizerPattern(ANNOTATION, 100));
        tokenizer.addPattern(new TokenizerPattern(CLASS_NAME, 200));
        tokenizer.addPattern(new TokenizerPattern(VARIABLE, 300));
        tokenizer.addPattern(new TokenizerPattern(PATH_VARIABLE, 400));

        tokenizer.addRecognizer(new PatternTokenRecognizer(ANNOTATION, ExtendedToken.T_ANNOTATION, 7000));
        tokenizer.addRecognizer(new PatternTokenRecognizer(CLASS_NAME, ExtendedToken.T_CLASS_NAME, 7100));
        tokenizer.addRecognizer(new PatternTokenRecognizer(VARIABLE, ExtendedToken.T_VARIABLE, 7100));
        tokenizer.addRecognizer(new PatternTokenRecognizer(PATH_VARIABLE, ExtendedToken.T_PATH_VARIABLE, 7200));
    }

}
