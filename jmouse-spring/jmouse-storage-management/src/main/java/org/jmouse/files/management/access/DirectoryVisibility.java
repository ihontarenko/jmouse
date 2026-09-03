package org.jmouse.files.management.access;

import org.jmouse.access.AccessEngine;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.Subject;
import org.jmouse.access.enforcement.CurrentSubject;
import org.jmouse.access.enforcement.ExternalAccessRules;
import org.jmouse.files.jpa.directory.StorageDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * 🔒 Which folders a caller may actually see, applied to what a listing is about to return.
 *
 * <h2>⚠️ A GUARD AUTHORIZES THE ROW THE CALLER NAMED — NOT THE ROWS IT HANDS BACK (JMF-278)</h2>
 *
 * <p>{@code ExternalAccessRules} gates the <em>request</em>: "may you read the directory you asked
 * about". Every listing then returned its rows unfiltered, so a caller who may read a root could
 * enumerate every folder beneath it — names, paths and all — including ones an explicit deny closed to
 * them. Opening such a folder was correctly refused; its existence was not.
 *
 * <p>That is the whole point of a per-directory scope. "Close this one folder so nobody but me can
 * open it" is not honoured by a product where the folder's name and path sit in every tree the
 * household draws — it reads as a broken screen rather than as a rule.
 *
 * <h2>⚠️ THE QUESTION COMES FROM THE ROUTE'S OWN DECLARATION</h2>
 *
 * <p>The permission and the scope are read from the rules the product declared for
 * {@code DirectoryController.read} — the same declaration the interceptor enforces. Nothing here
 * hard-codes {@code file:read} or {@code DIRECTORY}: a product that guards its tree with different
 * words gets its own words asked back, and the filter cannot drift from the guard because there is one
 * source for both.
 *
 * <h2>⚠️ A PRODUCT THAT DECLARES NOTHING IS FILTERED BY NOTHING</h2>
 *
 * <p>No declaration means no rule to apply, and the module's routes ship switched off until a product
 * declares some. Filtering everything out on a missing declaration would turn "this product does not
 * use the tree" into "this product's tree is empty", which is a sentence about the data rather than
 * about the configuration.
 *
 * <p>⚠️ Likewise a declaration with <strong>no scope</strong>. An installation-wide rule is already
 * decided by the guard, identically for every row, so asking again per directory would be one query
 * per folder to reach an answer that cannot vary.
 */
public final class DirectoryVisibility {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryVisibility.class);

    /**
     * ⚠️ Named rather than derived. The filter has to ask about the same route the guard protects, and
     * the read of a single directory is the one whose declaration means "may you see this folder".
     */
    private static final String READS_ONE_DIRECTORY = "read";

    private final ObjectProvider<AccessEngine>        engine;
    private final ObjectProvider<CurrentSubject>      currentSubject;
    private final ObjectProvider<ExternalAccessRules> rules;
    private final ObjectProvider<ScopeCatalog>        scopes;
    private final Class<?>                            controller;

    /** ⚠️ Said once, not per request — a listing runs on every screen that draws the tree. */
    private volatile boolean announced;

    /**
     * 🏗️ Build the filter.
     *
     * <p>⚠️ Every dependency is an {@link ObjectProvider}, matching every other consumer of these beans.
     * They are built late by auto-configuration, and taking one eagerly changes a startup order this
     * class has no business influencing — the module is mounted by a product that may declare its rules
     * anywhere.
     *
     * @param controller the type whose declaration is read — {@code DirectoryController}
     */
    public DirectoryVisibility(ObjectProvider<AccessEngine> engine,
                               ObjectProvider<CurrentSubject> currentSubject,
                               ObjectProvider<ExternalAccessRules> rules,
                               ObjectProvider<ScopeCatalog> scopes,
                               Class<?> controller) {
        this.engine         = engine;
        this.currentSubject = currentSubject;
        this.rules          = rules;
        this.scopes         = scopes;
        this.controller     = controller;
    }

    /**
     * 🌿 The ones this caller may read, in the order they came.
     *
     * <p>⚠️ A row the caller may not read is <strong>absent</strong>, never marked. A greyed folder in a
     * tree tells somebody exactly what a hidden one does not — that it is there, what it is called, and
     * that it is worth asking about.
     *
     * @param found what the tree returned
     * @return the readable subset, or all of it where nothing can be decided
     */
    public List<StorageDirectory> readable(List<StorageDirectory> found) {
        if (found == null || found.isEmpty()) {
            return found;
        }

        Question question = question().orElse(null);

        if (question != null && !announced) {
            announced = true;
            /*
              ⚠️ Said once, at INFO, and it is not debug scaffolding. "The tree is filtered, by this
              permission, at this scope" is the one fact somebody checking a closed folder needs, and
              the alternative is reading three classes to work out whether the filter is even engaged.
             */
            LOGGER.info("Directory listings are filtered by '{}' at scope {}.",
                        question.permission(), question.scope().name());
        }

        if (question == null) {
            /*
              ⚠️ SAID OUT LOUD, once. A filter that quietly does nothing is the exact failure this class
              was written to fix — a product declares a per-directory deny, sees the row on its access
              screen, and the folder stays in every listing. Whatever the reason (no rules declared, an
              installation-wide rule, a bean missing), it has to be findable in a log rather than by
              somebody noticing a folder they expected to be gone.
             */
            if (!announced) {
                announced = true;
                LOGGER.warn("Directory listings are NOT filtered by access: no per-directory read rule "
                            + "was resolved for {}. A deny at a directory scope will stop somebody "
                            + "opening a folder and will not hide it from a listing.",
                            controller.getSimpleName());
            }
            return found;
        }

        return found.stream()
                .filter(directory -> question.permits(directory.getId()))
                .toList();
    }

    /**
     * What to ask, or empty where the answer cannot vary per folder.
     *
     * <p>⚠️ Resolved per call rather than cached in a field. The engine, the subject and the rules are
     * all request- or context-scoped in at least one consumer, and a cached {@code Question} would hold
     * a subject from whoever called first — which is the one caching mistake in an authorization path
     * that nobody notices until two people are signed in at once.
     */
    private Optional<Question> question() {
        AccessEngine   asking  = engine.getIfAvailable();
        CurrentSubject asker   = currentSubject.getIfAvailable();
        ScopeCatalog   catalog = scopes.getIfAvailable();

        if (asking == null || asker == null || catalog == null) {
            /*
              ⚠️ NOTHING FILTERED rather than everything filtered.

              Without the engine there is no deny to honour, and hiding every folder because a bean is
              missing would turn a wiring mistake into an empty file library — which looks like data
              loss and sends somebody to the database rather than to the configuration.
             */
            return Optional.empty();
        }

        return declaration()
                .filter(declared -> !declared.permission().isBlank())
                .filter(declared -> !declared.scope().isBlank())
                .flatMap(declared -> catalog.byName(declared.scope())
                        .map(scope -> new Question(asking, asker.get(), declared.permission(), scope)));
    }

    /** The rule the product declared for reading ONE directory, or empty where it declared none. */
    private Optional<ExternalAccessRules.Declaration> declaration() {
        ExternalAccessRules declared = rules.getIfAvailable();

        if (declared == null) {
            return Optional.empty();
        }

        for (Method method : controller.getMethods()) {
            if (method.getName().equals(READS_ONE_DIRECTORY)) {
                Optional<ExternalAccessRules.Declaration> found =
                        declared.forMethod(method, controller);

                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * One caller's question, asked once per folder.
     *
     * <p>⚠️ A record rather than four locals, so the subject is read ONCE for the whole listing. Asking
     * {@code CurrentSubject.get()} per row would be a lookup per folder for an answer that cannot change
     * inside one request — and on a deep tree that is the difference between one query and hundreds.
     */
    private record Question(AccessEngine engine, Subject subject, String permission, ScopeKind scope) {

        boolean permits(String directoryId) {
            /*
              ⚠️ Asked at the FOLDER, which is what makes the subtree rule apply. The engine walks the
              containing chain — the folder, then its ancestors — so a deny written on a parent closes
              everything under it without anybody enumerating the children.
             */
            return engine.permits(
                    subject, permission, AccessTarget.installation().at(scope, directoryId));
        }
    }
}
