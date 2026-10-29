package com.hang.shiying.storage;

import static org.junit.Assert.assertEquals;
import android.content.ContentValues;
import android.provider.MediaStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public final class MediaStoreExporterTest {
    @Test
    public void targetsShiYingDownloadsWithMp4Metadata() {
        ContentValues values = MediaStoreExporter.metadataForApi("周末散步", 34);

        assertEquals(
                "周末散步.mp4",
                values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME));
        assertEquals(
                "video/mp4",
                values.getAsString(MediaStore.MediaColumns.MIME_TYPE));
        assertEquals(
                "Download/ShiYing",
                values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
        assertEquals(
                Integer.valueOf(1),
                values.getAsInteger(MediaStore.MediaColumns.IS_PENDING));
        assertEquals(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                MediaStoreExporter.videoCollectionForApi(34));
    }

    @Test
    public void targetsShiYingPicturesWithImageMimeType() {
        ContentValues values = MediaStoreExporter.imageMetadataForApi(
                "周末散步_01.jpg",
                34);

        assertEquals(
                "周末散步_01.jpg",
                values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME));
        assertEquals(
                "image/jpeg",
                values.getAsString(MediaStore.MediaColumns.MIME_TYPE));
        assertEquals(
                "Pictures/ShiYing",
                values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH));
    }
}
