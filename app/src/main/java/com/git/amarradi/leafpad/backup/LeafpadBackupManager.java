package com.git.amarradi.leafpad.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.git.amarradi.leafpad.model.AppDatabase;
import com.git.amarradi.leafpad.model.CategoryDao;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.NoteCategoryDao;
import com.git.amarradi.leafpad.model.NoteCategoryJoin;
import com.git.amarradi.leafpad.model.NoteDao;
import com.git.amarradi.leafpad.model.NoteEntity;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

                List<NoteEntity> entities = noteDao.getAllNotesForBackup();

                List<NoteBackupDto> dtos = new ArrayList<>();
                for (NoteEntity e : entities) {
                    NoteBackupDto dto = new NoteBackupDto();
                    dto.id = e.id;
                    dto.title = e.title;
                    dto.body = e.body;
                    dto.date = e.notedate;
                    dto.time = e.notetime;
                    dto.created = e.createDate;
                    dto.hide = e.hide;
                    // Solange wir Kategorien nicht mit sichern, leer lassen:
                    dto.category = "";

                    dtos.add(dto);
                }

                BackupMetadata meta = new BackupMetadata();
                meta.backupSchemaVersion = BackupCodec.SCHEMA_VERSION;
                meta.createdAtEpochMillis = System.currentTimeMillis();
                meta.noteCount = dtos.size();

                codec.writeBackup(out, meta, dtos);

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

                    db.runInTransaction(() -> {
                        noteDao.deleteAllNotes();

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
                    });

                    postSuccess(callback, result.notes.size());
                    return;
                }

            } catch (Exception zipError) {
                // ZIP war es nicht oder Backup ungültig → fallback
            }

            // 2) Fallback: Legacy XML
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

                // Rezept-Kategorie-ID holen (sollte existieren)
                Long rezeptIdObj = categoryDao.findIdByNormalized("rezept");
                final long rezeptCategoryId = (rezeptIdObj != null) ? rezeptIdObj : -1L;

                db.runInTransaction(() -> {
                    // Notes löschen -> Join wird per FK-CASCADE mit gelöscht (trotzdem ok)
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

                        // Wenn Kategorie "Rezept" in XML steht -> Join setzen
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
                return;

            } catch (Exception xmlError) {
                postError(callback, "Restore fehlgeschlagen: " + safeMsg(xmlError));
            }
//            // 2) Fallback: Legacy XML
//            try (InputStream inXml = context.getContentResolver().openInputStream(sourceUri)) {
//                if (inXml == null) {
//                    postError(callback, "Datei konnte nicht geöffnet werden.");
//                    return;
//                }
//
//                // TODO: LegacyXmlBackupHelper muss auf Room schreiben!
//                postError(callback, "Legacy-XML Restore ist noch nicht auf Room umgestellt.");
//            } catch (Exception xmlError) {
//                postError(callback, "Restore fehlgeschlagen: " + safeMsg(xmlError));
//            }

        }).start();
    }


    private boolean looksLikeZip(BufferedInputStream in) throws Exception {
        in.mark(4);
        int b0 = in.read();
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        in.reset();
        if (b0 == -1 || b1 == -1 || b2 == -1 || b3 == -1) {
            return false;
        }
        // ZIP magic: 0x50 0x4B 0x03 0x04  (PK..)
        return b0 == 0x50 && b1 == 0x4B && b2 == 0x03 && b3 == 0x04;
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
