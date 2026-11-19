package org.jmouse.java.model.version;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Map;

class JavaSpecificationTest {
    @Test
    void anExtensionCatalogDoesNotAdmitUnregisteredVersionsBetweenKnownReleases() {
        JavaSpecificationCatalog standard = JavaSpecificationCatalog.standard();
        JavaRelease target = new JavaRelease(25);
        JavaSpecificationCatalog extended =
                new JavaSpecificationCatalog(
                        "test-with-version-gap",
                        target,
                        Map.of(
                                new JavaRelease(21),
                                new ClassFileVersion(65, 0),
                                target,
                                new ClassFileVersion(69, 0)),
                        standard.features(),
                        standard.classFileRules());
        BytecodeProfile profile = new BytecodeProfile(target, PreviewPolicy.disabled(), extended);
        assertEquals(
                VersionAdmission.UNKNOWN_FORMAT, profile.admission(new ClassFileVersion(67, 0)));
        assertEquals(VersionAdmission.ACCEPTED, profile.admission(new ClassFileVersion(69, 0)));
    }

    @Test
    void languageFeaturesAreReleaseSpecificAndPreviewDoesNotMeanEveryFutureFeature() {
        JavaSpecificationCatalog catalog = JavaSpecificationCatalog.standard();
        JavaFeatureId records = JavaSourceFeatures.RECORD_DECLARATIONS;
        JavaFeatureId patterns = JavaSourceFeatures.RECORD_PATTERNS;
        JavaSourceProfile older = JavaSourceProfile.standard(new JavaRelease(17));
        assertTrue(older.permits(records));
        assertFalse(older.permits(patterns));
        JavaRelease fourteen = new JavaRelease(14);
        assertFalse(JavaSourceProfile.standard(fourteen).permits(records));
        assertTrue(
                new JavaSourceProfile(fourteen, PreviewPolicy.enabled(fourteen), catalog)
                        .permits(records));
        assertFalse(
                new JavaSourceProfile(fourteen, PreviewPolicy.enabled(fourteen), catalog)
                        .permits(patterns));
        assertThrows(
                IllegalArgumentException.class,
                () -> JavaSourceProfile.standard(new JavaRelease(99)));
    }

    @Test
    void binaryHeaderAdmissionDistinguishesLegacyMinorsPreviewAndFutureFormats() {
        JavaSpecificationCatalog catalog = JavaSpecificationCatalog.standard();
        JavaRelease release = new JavaRelease(21);
        BytecodeProfile normal = new BytecodeProfile(release, PreviewPolicy.disabled(), catalog);
        assertEquals(VersionAdmission.ACCEPTED, normal.admission(new ClassFileVersion(55, 123)));
        assertEquals(VersionAdmission.INVALID_MINOR, normal.admission(new ClassFileVersion(65, 1)));
        assertEquals(
                VersionAdmission.PREVIEW_DISABLED,
                normal.admission(new ClassFileVersion(65, 65535)));
        assertEquals(
                VersionAdmission.PREVIEW_RELEASE_MISMATCH,
                normal.admission(new ClassFileVersion(64, 65535)));
        assertEquals(
                VersionAdmission.UNKNOWN_FORMAT, normal.admission(new ClassFileVersion(66, 0)));
        assertEquals(
                VersionAdmission.ACCEPTED,
                new BytecodeProfile(release, PreviewPolicy.enabled(release), catalog)
                        .admission(new ClassFileVersion(65, 65535)));
        assertEquals(new ClassFileVersion(65, 0), catalog.classFileVersion(release));
    }
}
