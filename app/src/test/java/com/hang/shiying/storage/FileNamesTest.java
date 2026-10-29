package com.hang.shiying.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FileNamesTest {
    @Test
    public void removesPathSeparatorsAndQuotes() {
        assertEquals(
                "a_b_c_d_e_f",
                FileNames.safe("a/b\\c:d*e?f"));
    }

    @Test
    public void keepsChineseText() {
        assertEquals("周末散步", FileNames.safe("周末散步"));
    }

    @Test
    public void limitsLongTitles() {
        String result = FileNames.safe("视".repeat(120));
        assertEquals(80, result.length());
    }

    @Test
    public void fallsBackForBlankTitle() {
        assertFalse(FileNames.safe("   ").isEmpty());
        assertTrue(FileNames.safe(null).contains("视频"));
    }
}
