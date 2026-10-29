package com.hang.shiying.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public final class MediaStoreExporter {
    private MediaStoreExporter() {}

    public static String save(
            ContentResolver resolver,
            File source,
            String title) throws Exception {
        String displayName = FileNames.safe(title) + ".mp4";
        ContentValues values = metadataForApi(title, Build.VERSION.SDK_INT);

        File legacyTarget = null;
        if (Build.VERSION.SDK_INT < 29) {
            File directory = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS),
                    "ShiYing");
            if (!directory.mkdirs() && !directory.isDirectory()) {
                throw new IllegalStateException("无法创建系统下载目录");
            }
            legacyTarget = uniqueFile(directory, displayName, ".mp4");
            values.put(MediaStore.MediaColumns.DATA, legacyTarget.getAbsolutePath());
        }

        Uri uri = resolver.insert(
                videoCollectionForApi(Build.VERSION.SDK_INT),
                values);
        if (uri == null) {
            throw new IllegalStateException("系统没有创建下载文件");
        }

        try (FileInputStream input = new FileInputStream(source);
             OutputStream output = resolver.openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IllegalStateException("无法打开系统下载文件");
            }
            byte[] buffer = new byte[128 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            output.flush();
        } catch (Exception error) {
            resolver.delete(uri, null, null);
            if (legacyTarget != null && legacyTarget.exists()) {
                //noinspection ResultOfMethodCallIgnored
                legacyTarget.delete();
            }
            throw error;
        }

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, ready, null, null);
        }
        return uri.toString();
    }

    public static String saveImages(
            ContentResolver resolver,
            List<File> sources,
            String title) throws Exception {
        String firstUri = "";
        for (int index = 0; index < sources.size(); index++) {
            File source = sources.get(index);
            String displayName = FileNames.safe(title)
                    + String.format(Locale.ROOT, "_%02d", index + 1)
                    + extensionOf(source.getName());
            String uri = saveImage(resolver, source, displayName);
            if (firstUri.isEmpty()) {
                firstUri = uri;
            }
        }
        return firstUri;
    }

    private static String saveImage(
            ContentResolver resolver,
            File source,
            String displayName) throws Exception {
        ContentValues values = imageMetadataForApi(
                displayName,
                Build.VERSION.SDK_INT);
        File legacyTarget = null;
        if (Build.VERSION.SDK_INT < 29) {
            File directory = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES),
                    "ShiYing");
            if (!directory.mkdirs() && !directory.isDirectory()) {
                throw new IllegalStateException("无法创建图片目录");
            }
            legacyTarget = uniqueFile(directory, displayName, ".jpg");
            values.put(MediaStore.MediaColumns.DATA, legacyTarget.getAbsolutePath());
        }

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IllegalStateException("系统没有创建图片文件");
        }
        try (FileInputStream input = new FileInputStream(source);
             OutputStream output = resolver.openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IllegalStateException("无法打开系统图片文件");
            }
            byte[] buffer = new byte[128 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            output.flush();
        } catch (Exception error) {
            resolver.delete(uri, null, null);
            if (legacyTarget != null && legacyTarget.exists()) {
                //noinspection ResultOfMethodCallIgnored
                legacyTarget.delete();
            }
            throw error;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, ready, null, null);
        }
        return uri.toString();
    }

    static ContentValues metadataForApi(String title, int apiLevel) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, FileNames.safe(title) + ".mp4");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (apiLevel >= 29) {
            values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/ShiYing");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        return values;
    }

    static Uri videoCollectionForApi(int apiLevel) {
        return apiLevel >= 29
                ? MediaStore.Downloads.EXTERNAL_CONTENT_URI
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    }

    static ContentValues imageMetadataForApi(String displayName, int apiLevel) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, imageMimeType(displayName));
        if (apiLevel >= 29) {
            values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ShiYing");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }
        return values;
    }

    private static File uniqueFile(
            File directory,
            String displayName,
            String fallbackExtension) {
        File first = new File(directory, displayName);
        if (!first.exists()) {
            return first;
        }
        int dot = displayName.lastIndexOf('.');
        String base = dot > 0 ? displayName.substring(0, dot) : displayName;
        String extension = dot > 0 ? displayName.substring(dot) : fallbackExtension;
        for (int index = 2; index < 10_000; index++) {
            File candidate = new File(
                    directory,
                    base + " (" + index + ")" + extension);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        throw new IllegalStateException("下载目录中同名文件过多");
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : ".jpg";
    }

    private static String imageMimeType(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
