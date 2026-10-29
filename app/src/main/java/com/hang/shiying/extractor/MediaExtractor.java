package com.hang.shiying.extractor;

import com.hang.shiying.core.MediaInfo;

public interface MediaExtractor {
    MediaInfo extract(String normalizedUrl) throws Exception;
}
