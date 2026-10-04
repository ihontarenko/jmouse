package org.jmouse.files.management;

import org.jmouse.files.OwnerReference;
import org.jmouse.files.jpa.ManagedFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 🗑️ The trash, for the installation as a whole: what is in it, and emptying it.
 *
 * <p>⚠️ <strong>Its own controller, guarded like storage administration</strong> — see
 * {@link FilesAccessRules}. Putting one file in the trash is a write on that file and lives on
 * {@link FileController}; the trash itself spans every folder and every tree, so no scoped permission can
 * express "may see it", and emptying it is the one act in this library that cannot be taken back.</p>
 */
@RestController
public class TrashController {

    private final FileManagement management;

    /**
     * 🏗️ Serve the trash.
     *
     * @param management what the routes actually do
     */
    public TrashController(FileManagement management) {
        this.management = management;
    }

    /**
     * 🗑️ Everything in the trash, most recently trashed first.
     *
     * @return the trashed files, with where each is filed
     */
    @GetMapping(ManagementRoutes.TRASH)
    public List<TrashedFileView> list() {
        List<ManagedFile> trashed = management.listTrash();
        Map<String, List<OwnerReference>> owners =
                management.ownersOfAll(trashed.stream().map(ManagedFile::getId).toList());

        return trashed.stream()
                .map(file -> TrashedFileView.of(file, owners.getOrDefault(file.getId(), List.of())))
                .toList();
    }

    /**
     * 🔥 Empty it.
     *
     * @param olderThanDays only what has been in the trash at least this many days; omitted, everything
     * @return how many files were deleted
     */
    @DeleteMapping(ManagementRoutes.TRASH)
    public Emptied empty(@RequestParam(required = false) Integer olderThanDays) {
        LocalDateTime before = olderThanDays == null ? null : LocalDateTime.now().minusDays(olderThanDays);

        return new Emptied(management.emptyTrash(before));
    }

    /**
     * What emptying did.
     *
     * @param deleted how many files were really deleted
     */
    public record Emptied(int deleted) {
    }
}
