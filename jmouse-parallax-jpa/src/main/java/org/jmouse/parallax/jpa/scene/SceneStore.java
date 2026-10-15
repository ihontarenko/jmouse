package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.parallax.jpa.StoredPage;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 🎬 Scenes, their revisions and snapshots, and the index of which stored objects a scene's head uses.
 *
 * <h3>⚠️ The object index is written by native statements, and that orders the writes</h3>
 *
 * <p>{@code scene_object_references} has no entity: it is rewritten whole on every save. The scene and its newest
 * revision may still sit in the persistence context when it is, so {@link #replaceReferences} flushes first.</p>
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>It does not authorize anything, and transaction demarcation is the caller's throughout.</p>
 */
public class SceneStore {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public SceneStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ── Scenes ───────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One scene.
     *
     * @param sceneId the scene
     * @return the scene, or empty
     */
    public Optional<Scene> find(String sceneId) {
        return Optional.ofNullable(entityManager.find(Scene.class, sceneId));
    }

    /**
     * 💾 Keep a scene: a new one is persisted, a loaded one is already tracked.
     *
     * @param scene the scene
     * @return the same scene, now managed
     */
    public Scene save(Scene scene) {
        if (!entityManager.contains(scene)) {
            entityManager.persist(scene);
        }

        return scene;
    }

    /**
     * 🗑️ Remove a scene; its revisions, snapshots and object index go with it (the schema cascades).
     *
     * @param scene the scene
     */
    public void remove(Scene scene) {
        entityManager.remove(scene);
    }

    /**
     * 📂 One owner's scenes that are not archived, most recently changed first.
     *
     * @param owner the owner (a project)
     * @param page  the page number, from zero
     * @param size  the page size
     * @return the page
     */
    public StoredPage<Scene> owned(ParallaxOwner owner, int page, int size) {
        String conditions = " where scene.ownerType = :type and scene.ownerId = :id and scene.archivedAt is null";
        TypedQuery<Scene> rows = entityManager.createQuery(
                "select scene from Scene scene" + conditions + " order by scene.updatedAt desc", Scene.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(scene) from Scene scene" + conditions, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("type", owner.type()).setParameter("id", owner.id());
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 🔢 How many scenes there are, archived ones included.
     *
     * @return the count
     */
    public long count() {
        return entityManager.createQuery("select count(scene) from Scene scene", Long.class).getSingleResult();
    }

    /**
     * 🕘 The newest scenes of a realm that are not archived, most recently changed first.
     *
     * @param realm the realm (a workspace)
     * @param limit how many at most
     * @return the scenes
     */
    public List<Scene> recentInRealm(ParallaxOwner realm, int limit) {
        return entityManager.createQuery("""
                        select scene from Scene scene
                        where scene.realmType = :type and scene.realmId = :id and scene.archivedAt is null
                        order by scene.updatedAt desc
                        """, Scene.class)
                .setParameter("type", realm.type())
                .setParameter("id", realm.id())
                .setMaxResults(limit)
                .getResultList();
    }

    // ── Revisions ────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One revision of a scene.
     *
     * @param sceneId        the scene
     * @param revisionNumber the revision
     * @return the revision, or empty
     */
    public Optional<SceneRevision> findRevision(String sceneId, int revisionNumber) {
        return entityManager.createQuery("""
                        select revision from SceneRevision revision
                        where revision.sceneId = :sceneId and revision.revisionNumber = :number
                        """, SceneRevision.class)
                .setParameter("sceneId", sceneId)
                .setParameter("number", revisionNumber)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * 📜 Every revision of a scene, newest first.
     *
     * @param sceneId the scene
     * @return the revisions
     */
    public List<SceneRevision> revisionsNewestFirst(String sceneId) {
        return entityManager.createQuery("""
                        select revision from SceneRevision revision
                        where revision.sceneId = :sceneId order by revision.revisionNumber desc
                        """, SceneRevision.class)
                .setParameter("sceneId", sceneId)
                .getResultList();
    }

    /**
     * ➕ Keep a new revision. Revisions are never changed once written.
     *
     * @param revision the revision
     * @return the same revision, now managed
     */
    public SceneRevision addRevision(SceneRevision revision) {
        entityManager.persist(revision);

        return revision;
    }

    // ── Snapshots ────────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One snapshot.
     *
     * @param snapshotId the snapshot
     * @return the snapshot, or empty
     */
    public Optional<SceneSnapshot> findSnapshot(String snapshotId) {
        return Optional.ofNullable(entityManager.find(SceneSnapshot.class, snapshotId));
    }

    /**
     * 📸 Every snapshot of a scene, newest first.
     *
     * @param sceneId the scene
     * @return the snapshots
     */
    public List<SceneSnapshot> snapshotsNewestFirst(String sceneId) {
        return entityManager.createQuery("""
                        select snapshot from SceneSnapshot snapshot
                        where snapshot.sceneId = :sceneId order by snapshot.createdAt desc
                        """, SceneSnapshot.class)
                .setParameter("sceneId", sceneId)
                .getResultList();
    }

    /**
     * ➕ Keep a new snapshot.
     *
     * @param snapshot the snapshot
     * @return the same snapshot, now managed
     */
    public SceneSnapshot addSnapshot(SceneSnapshot snapshot) {
        entityManager.persist(snapshot);

        return snapshot;
    }

    /**
     * 🗑️ Remove a snapshot.
     *
     * @param snapshot the snapshot
     */
    public void removeSnapshot(SceneSnapshot snapshot) {
        entityManager.remove(snapshot);
    }

    // ── Object index ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔗 Rewrite which stored objects a scene's head uses.
     *
     * <p>⚠️ Flushes first: the scene and its revision must reach the database before the index points at them.</p>
     *
     * @param sceneId   the scene
     * @param objectIds the objects its head revision uses
     */
    public void replaceReferences(String sceneId, Set<String> objectIds) {
        entityManager.flush();
        entityManager.createNativeQuery("DELETE FROM scene_object_references WHERE scene_id = :sceneId")
                .setParameter("sceneId", sceneId)
                .executeUpdate();

        for (String objectId : objectIds) {
            entityManager.createNativeQuery(
                            "INSERT INTO scene_object_references (scene_id, object_id) VALUES (:sceneId, :objectId)")
                    .setParameter("sceneId", sceneId)
                    .setParameter("objectId", objectId)
                    .executeUpdate();
        }
    }
}
