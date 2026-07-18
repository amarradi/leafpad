package com.git.amarradi.leafpad.backup;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupCodec {

    public static final int SCHEMA_VERSION = 2;

    private static final String ENTRY_METADATA = "metadata.json";
    private static final String ENTRY_NOTES = "notes.jsonl";
    private static final String ENTRY_CATEGORIES = "categories.jsonl";
    private static final String ENTRY_NOTE_CATEGORIES = "note_categories.jsonl";

    public BackupCodec() {
    }

    public void writeBackup(
            OutputStream out,
            BackupMetadata metadata,
            List<NoteBackupDto> notes,
            List<CategoryBackupDto> categories,
            List<NoteCategoryBackupDto> noteCategories
    ) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(out);

        // metadata.json
        ZipEntry metaEntry = new ZipEntry(ENTRY_METADATA);
        zos.putNextEntry(metaEntry);

        JSONObject meta = new JSONObject();
        meta.put("backupSchemaVersion", metadata.backupSchemaVersion);
        meta.put("createdAtEpochMillis", metadata.createdAtEpochMillis);
        meta.put("noteCount", metadata.noteCount);
        meta.put("categoryCount", categories.size());

        byte[] metaBytes = meta.toString().getBytes(StandardCharsets.UTF_8);
        zos.write(metaBytes);
        zos.closeEntry();

        // notes.jsonl
        ZipEntry notesEntry = new ZipEntry(ENTRY_NOTES);
        zos.putNextEntry(notesEntry);
        BufferedWriter notesWriter = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
        for (NoteBackupDto dto : notes) {
            JSONObject obj = new JSONObject();
            obj.put("id", dto.id);
            obj.put("title", dto.title);
            obj.put("body", dto.body);
            obj.put("date", dto.date);
            obj.put("time", dto.time);
            obj.put("created", dto.created);
            obj.put("hide", dto.hide);
            obj.put("category", dto.category);

            notesWriter.write(obj.toString());
            notesWriter.newLine();
        }
        notesWriter.flush();
        zos.closeEntry();

        // categories.jsonl
        ZipEntry categoriesEntry = new ZipEntry(ENTRY_CATEGORIES);
        zos.putNextEntry(categoriesEntry);
        BufferedWriter catWriter = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
        for (CategoryBackupDto dto : categories) {
            JSONObject obj = new JSONObject();
            obj.put("name", dto.name);
            obj.put("normalizedName", dto.normalizedName);
            obj.put("colorHex", dto.colorHex);
            obj.put("sortOrder", dto.sortOrder);
            obj.put("isArchived", dto.isArchived);

            catWriter.write(obj.toString());
            catWriter.newLine();
        }
        catWriter.flush();
        zos.closeEntry();

        // note_categories.jsonl
        ZipEntry noteCategoriesEntry = new ZipEntry(ENTRY_NOTE_CATEGORIES);
        zos.putNextEntry(noteCategoriesEntry);
        BufferedWriter ncWriter = new BufferedWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
        for (NoteCategoryBackupDto dto : noteCategories) {
            JSONObject obj = new JSONObject();
            obj.put("noteId", dto.noteId);
            obj.put("categoryNormalizedName", dto.categoryNormalizedName);

            ncWriter.write(obj.toString());
            ncWriter.newLine();
        }
        ncWriter.flush();
        zos.closeEntry();

        zos.finish();
        zos.flush();
    }

    public BackupReadResult readBackup(InputStream in) throws Exception {
        ZipInputStream zis = new ZipInputStream(in);

        BackupMetadata metadata = null;
        List<NoteBackupDto> notes = new ArrayList<>();
        List<CategoryBackupDto> categories = new ArrayList<>();
        List<NoteCategoryBackupDto> noteCategories = new ArrayList<>();

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();

            if (ENTRY_METADATA.equals(name)) {
                String json = readAllUtf8(zis);
                JSONObject meta = new JSONObject(json);

                metadata = new BackupMetadata();
                metadata.backupSchemaVersion = meta.optInt("backupSchemaVersion", -1);
                metadata.createdAtEpochMillis = meta.optLong("createdAtEpochMillis", 0L);
                metadata.noteCount = meta.optInt("noteCount", 0);

            } else if (ENTRY_NOTES.equals(name)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    JSONObject obj = new JSONObject(trimmed);
                    NoteBackupDto dto = new NoteBackupDto();
                    dto.id = obj.optString("id", "");
                    dto.title = obj.optString("title", "");
                    dto.body = obj.optString("body", "");
                    dto.date = obj.optString("date", "");
                    dto.time = obj.optString("time", "");
                    dto.created = obj.optString("created", "");
                    dto.hide = obj.optBoolean("hide", false);
                    dto.category = obj.optString("category", "");

                    notes.add(dto);
                }

            } else if (ENTRY_CATEGORIES.equals(name)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    JSONObject obj = new JSONObject(trimmed);
                    CategoryBackupDto dto = new CategoryBackupDto();
                    dto.name = obj.optString("name", "");
                    dto.normalizedName = obj.optString("normalizedName", "");
                    dto.colorHex = obj.optString("colorHex", "#CCCCCC");
                    dto.sortOrder = obj.optInt("sortOrder", 0);
                    dto.isArchived = obj.optBoolean("isArchived", false);

                    categories.add(dto);
                }

            } else if (ENTRY_NOTE_CATEGORIES.equals(name)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    JSONObject obj = new JSONObject(trimmed);
                    NoteCategoryBackupDto dto = new NoteCategoryBackupDto();
                    dto.noteId = obj.optString("noteId", "");
                    dto.categoryNormalizedName = obj.optString("categoryNormalizedName", "");

                    noteCategories.add(dto);
                }
            }

            zis.closeEntry();
        }

        if (metadata == null) {
            throw new IllegalStateException("Backup ungültig: metadata.json fehlt");
        }

        // Rückwärtskompatibilität: v1-Backups haben keine Kategorien, sind aber gültig
        if (metadata.backupSchemaVersion != 1 && metadata.backupSchemaVersion != SCHEMA_VERSION) {
            throw new IllegalStateException("Backup-Version nicht unterstützt: " + metadata.backupSchemaVersion);
        }

        return new BackupReadResult(metadata, notes, categories, noteCategories);
    }

    private String readAllUtf8(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    public static class BackupReadResult {
        public final BackupMetadata metadata;
        public final List<NoteBackupDto> notes;
        public final List<CategoryBackupDto> categories;
        public final List<NoteCategoryBackupDto> noteCategories;

        public BackupReadResult(
                BackupMetadata metadata,
                List<NoteBackupDto> notes,
                List<CategoryBackupDto> categories,
                List<NoteCategoryBackupDto> noteCategories
        ) {
            this.metadata = metadata;
            this.notes = notes;
            this.categories = categories;
            this.noteCategories = noteCategories;
        }
    }
}