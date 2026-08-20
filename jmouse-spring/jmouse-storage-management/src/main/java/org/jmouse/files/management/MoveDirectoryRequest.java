package org.jmouse.files.management;

/**
 * 📦 Where a directory should go.
 *
 * <p>⚠️ The destination is in the body, which is precisely where no access rule can see it. A product
 * wiring this route up has to check the destination itself — see {@code DirectoryController}.</p>
 *
 * @param parentId the directory it should sit under
 */
public record MoveDirectoryRequest(String parentId) {
}
