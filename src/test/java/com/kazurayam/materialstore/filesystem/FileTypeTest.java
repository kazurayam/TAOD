package com.kazurayam.materialstore.filesystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileTypeTest {
    @Test
    public void test_PNG() {
        assertEquals("png", FileType.PNG.getExtension());
        assertEquals(FileTypeDiffability.AS_IMAGE, FileType.PNG.getDiffability());
        assertTrue(FileType.PNG.getMimeTypes().contains("image/png"));
    }

    @Test
    public void test_CSS() {
        FileType ft = FileType.ofMediaType("text/css");
        assertEquals(FileType.CSS, ft);
        assertEquals(FileTypeDiffability.AS_TEXT, ft.getDiffability());
    }

    @Test
    public void test_JS() {
        FileType ft = FileType.ofMediaType("application/javascript");
        assertEquals(FileType.JS, ft);
        assertEquals(FileTypeDiffability.AS_TEXT, ft.getDiffability());
    }

    @Test
    public void test_HTML() {
        FileType ft = FileType.ofMediaType("text/html");
        assertEquals(FileType.HTML, ft);
        assertEquals(FileTypeDiffability.AS_TEXT, ft.getDiffability());
    }

    @Test
    public void test_WOFF2() {
        FileType ft = FileType.ofMediaType("font/woff2");
        assertEquals(FileType.WOFF2, ft);
        assertEquals(FileTypeDiffability.UNABLE, ft.getDiffability());
    }





    @Test
    public void test_toString() {
        String s = FileType.PNG.toString();
        //println JsonOutput.prettyPrint(s)
    }

    @Test
    public void test_forTemplate() {
        Map<String, Object> map = FileType.PNG.toTemplateModel();
        // print map keys and values
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println gson.toJson(map)
        //
        assertEquals("png", map.get("extension"));
        assertEquals("image/png", ((List) map.get("mimeTypes")).get(0));
        assertEquals("AS_IMAGE", map.get("diffability"));
    }

}
