package org.jmouse.java.parser.extension;

/** A version or product module contributes grammar through the existing jME contracts. */
public interface JavaSourceExtension {
    JavaSourceExtensionDescriptor descriptor();

    void configure(JavaSourceLanguage.Builder language);
}
