package com.git.amarradi.leafpad.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.git.amarradi.leafpad.model.AppDatabase;
import com.git.amarradi.leafpad.model.CategoryDao;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.NoteCategoryDao;
import com.git.amarradi.leafpad.model.NoteCategoryJoin;
import com.git.amarradi.leafpad.model.NoteDao;
import com.git.amarradi.leafpad.model.NoteEntity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeafpadBackupManager {

    public static final String BASE_NAME = "leafpad";

    public interface Callback {
        void onSuccess(int noteCount);

        void onError(String message);
    }

    private final Handler mainHandler;
    private final BackupCodec codec;

    public LeafpadBackupManager() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.codec = new BackupCodec();
    }

    public static String generateTimestamp() {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("ddMMyyyy_HHmm", java.util.Locale.getDefault());
        return formatter.format(new java.util.Date());
    }

    public void exportBackup(Context context, Uri targetUri, Callback callback) {
        new Thread(() -> {
            try (OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {
                if (out == null) {
                    postError(callback, "Datei konnte nicht zum Schreiben geöffnet werden.");
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(context);
                NoteDao noteDao = db.noteDao();
                CategoryDao categoryDao = db.categoryDao();

                // ------------------------------------------------------------
                // Notizen
                // ------------------------------------------------------------
                List<NoteEntity> entities = noteDao.getAllNotesForBackup();

                List<NoteBackupDto> noteDtos = new ArrayList<>();
                for (NoteEntity e : entities) {
                    NoteBackupDto dto = new NoteBackupDto();
                    dto.id = e.id;
                    dto.title = e.title;
                    dto.body = e.body;
                    dto.date = e.notedate;
                    dto.time = e.notetime;
                    dto.created = e.createDate;
                    dto.hide = e.hide;
                    dto.category = ""; // Legacy-Feld, Kategorien laufen jetzt separat
                    noteDtos.add(dto);
                }

                // ------------------------------------------------------------
                // Kategorien (alle, inkl. archivierte, damit nichts verloren geht)
                // ------------------------------------------------------------
                List<CategoryEntity> allCategories = categoryDao.getAllCategoriesForBackup();

                List<CategoryBackupDto> categoryDtos = new ArrayList<>();
                for (CategoryEntity c : allCategories) {
                    CategoryBackupDto dto = new CategoryBackupDto();
                    dto.name = c.name;
                    dto.normalizedName = c.normalizedName;
                    dto.colorHex = c.colorHex;
                    dto.sortOrder = c.sortOrder;
                    dto.isArchived = c.isArchived;
                    categoryDtos.add(dto);
                }

                // ------------------------------------------------------------
                // Notiz-Kategorie-Verknüpfungen
                // ------------------------------------------------------------
                Map<Long, String> categoryIdToNormalized = new HashMap<>();
                for (CategoryEntity c : allCategories) {
                    categoryIdToNormalized.put(c.id, c.normalizedName);
                }

                List<com.git.amarradi.leafpad.model.NoteCategoryJoin> allJoins =
                        db.noteCategoryDao().getAllJoinsForBackup();

                List<NoteCategoryBackupDto> noteCategoryDtos = new ArrayList<>();
                for (com.git.amarradi.leafpad.model.NoteCategoryJoin join : allJoins) {
                    String normalized = categoryIdToNormalized.get(join.categoryId);
                    if (normalized == null) continue; // verwaiste Referenz überspringen

                    NoteCategoryBackupDto dto = new NoteCategoryBackupDto();
                    dto.noteId = join.noteId;
                    dto.categoryNormalizedName = normalized;
                    noteCategoryDtos.add(dto);
                }

                // ------------------------------------------------------------
                // Schreiben
                // ------------------------------------------------------------
                BackupMetadata meta = new BackupMetadata();
                meta.backupSchemaVersion = BackupCodec.SCHEMA_VERSION;
                meta.createdAtEpochMillis = System.currentTimeMillis();
                meta.noteCount = noteDtos.size();

                codec.writeBackup(out, meta, noteDtos, categoryDtos, noteCategoryDtos);

                postSuccess(callback, meta.noteCount);

            } catch (Exception e) {
                postError(callback, "Backup fehlgeschlagen: " + safeMsg(e));
            }
        }).start();
    }

    public void restoreBackup(Context context, Uri sourceUri, Callback callback) {
        new Thread(() -> {
            try {
                // 1) Erst versuchen: ZIP Backup
                try (InputStream inZip = context.getContentResolver().openInputStream(sourceUri)) {
                    if (inZip == null) {
                        postError(callback, "Datei konnte nicht geöffnet werden.");
                        return;
                    }

                    BackupCodec.BackupReadResult result = codec.readBackup(inZip);

                    AppDatabase db = AppDatabase.getInstance(context);
                    NoteDao noteDao = db.noteDao();
                    CategoryDao categoryDao = db.categoryDao();
                    NoteCategoryDao noteCategoryDao = db.noteCategoryDao();

                    db.runInTransaction(() -> {
                        // Alles Alte weg (Join fliegt per FK-CASCADE mit)
                        noteDao.deleteAllNotes();
                        // Bei Kategorien nicht CASCADE, also separat leeren:
                        categoryDao.deleteAllCategories();

                        // ---- Notizen wiederherstellen ----
                        for (NoteBackupDto dto : result.notes) {
                            if (dto.id == null || dto.id.trim().isEmpty()) {
                                continue;
                            }

                            NoteEntity e = new NoteEntity(
                                    dto.id,
                                    dto.title,
                                    dto.body,
                                    dto.date,
                                    dto.time,
                                    dto.created,
                                    dto.hide
                            );
                            noteDao.insert(e);
                        }

                        // ---- Kategorien wiederherstellen ----
                        Map<String, Long> normalizedToNewId = new HashMap<>();
                        for (CategoryBackupDto catDto : result.categories) {
                            if (catDto.normalizedName == null || catDto.normalizedName.trim().isEmpty()) {
                                continue;
                            }

                            CategoryEntity entity = new CategoryEntity(
                                    catDto.name,
                                    catDto.normalizedName,
                                    catDto.colorHex,
                                    catDto.sortOrder,
                                    catDto.isArchived
                            );
                            long newId = categoryDao.insert(entity);
                            normalizedToNewId.put(catDto.normalizedName, newId);
                        }

                        // ---- Verknüpfungen wiederherstellen ----
                        for (NoteCategoryBackupDto ncDto : result.noteCategories) {
                            Long categoryId = normalizedToNewId.get(ncDto.categoryNormalizedName);
                            if (categoryId == null) continue; // Kategorie fehlt im Backup

                            noteCategoryDao.insert(new NoteCategoryJoin(ncDto.noteId, categoryId));
                        }
                    });

                    postSuccess(callback, result.notes.size());
                    return;
                }

            } catch (Exception zipError) {
                // ZIP war es nicht oder Backup ungültig → fallback
            }

            // 2) Fallback: Legacy XML (unverändert, ohne Kategorien-Support)
            try (InputStream inXml = context.getContentResolver().openInputStream(sourceUri)) {
                if (inXml == null) {
                    postError(callback, "Datei konnte nicht geöffnet werden.");
                    return;
                }

                List<Note> legacyNotes = LegacyXmlBackupHelper.parseNotesFromStream(inXml);

                AppDatabase db = AppDatabase.getInstance(context);
                NoteDao noteDao = db.noteDao();
                CategoryDao categoryDao = db.categoryDao();
                NoteCategoryDao noteCategoryDao = db.noteCategoryDao();

                Long rezeptIdObj = categoryDao.findIdByNormalized("rezept");
                final long rezeptCategoryId = (rezeptIdObj != null) ? rezeptIdObj : -1L;

                db.runInTransaction(() -> {
                    noteDao.deleteAllNotes();

                    for (Note n : legacyNotes) {
                        String noteId = n.getId();
                        if (noteId == null || noteId.trim().isEmpty()) {
                            noteId = Note.makeId();
                        }

                        NoteEntity e = new NoteEntity(
                                noteId,
                                n.getTitle(),
                                n.getBody(),
                                n.getDate(),
                                n.getTime(),
                                n.getCreateDate(),
                                n.isHide()
                        );
                        noteDao.insert(e);

                        if (rezeptCategoryId > 0) {
                            String cat = n.getCategory();
                            if (cat != null && cat.trim().equalsIgnoreCase("Rezept")) {
                                noteCategoryDao.insert(new NoteCategoryJoin(noteId, rezeptCategoryId));
                            }
                        }
                    }
                });

                if (rezeptCategoryId <= 0) {
                    postError(callback, "Restore ok, aber Standardkategorie 'Rezept' wurde in der DB nicht gefunden.");
                    return;
                }

                postSuccess(callback, legacyNotes.size());

            } catch (Exception xmlError) {
                postError(callback, "Restore fehlgeschlagen: " + safeMsg(xmlError));
            }
        }).start();
    }

    private void postSuccess(Callback callback, int count) {
        mainHandler.post(() -> callback.onSuccess(count));
    }

    private void postError(Callback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private String safeMsg(Exception e) {
        String msg = e.getLocalizedMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return msg;
    }
}