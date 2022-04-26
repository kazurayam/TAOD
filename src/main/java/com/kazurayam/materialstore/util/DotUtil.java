package com.kazurayam.materialstore.util;

import com.kazurayam.materialstore.MaterialstoreException;
import com.kazurayam.materialstore.filesystem.FileType;
import com.kazurayam.materialstore.filesystem.JobName;
import com.kazurayam.materialstore.filesystem.JobTimestamp;
import com.kazurayam.materialstore.filesystem.Material;
import com.kazurayam.materialstore.filesystem.Metadata;
import com.kazurayam.materialstore.filesystem.Store;
import com.kazurayam.subprocessj.Subprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DotUtil class implements helper methods to generate *.dot files.
 * See https://graphviz.org/ to know what Graphviz and dot is.
 */
public class DotUtil {

    private static String IND = "  ";

    public static String standalone(String content) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("digraph G {");
        pw.println(IND + "fontname=\"Helvetica,Arial,sans-serif\"");
        pw.println(IND + "node [fontname=\"Helvetica,Arial,Sans-serif\"]");
        pw.println(IND + "edge [fontname=\"Helvetica,Arial,Sans-serif\"]");
        pw.println(IND + "concentrate=True;");
        pw.println(IND + "rankdir=TB;");
        pw.println(IND + "node [shape=record];");
        List<String> lines = toList(content);
        lines.stream().forEach(s -> pw.println(IND + s));
        pw.println("}");
        pw.flush();
        pw.close();
        return sw.toString();
    }

    public static final List<String> toList(String content) {
        StringReader sr = new StringReader(content);
        BufferedReader br = new BufferedReader(sr);
        List<String> lines = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            // will never fall down into here
            e.printStackTrace();
        }
        return lines;
    }

    public static final String escape(String str) {
        return JsonUtil.escapeAsJsonString(str);
    }

    public static final int runDot(Path dotFile, Path outFile) throws IOException, InterruptedException {
        Subprocess.CompletedProcess cp =
                new Subprocess()
                        .run(Arrays.asList(
                                "/usr/local/bin/dot",
                                "-Tpng",
                                "-o" + outFile.toString(),
                                dotFile.toString()
                                )
                        );
        return cp.returncode();
    }

    public static final Material storeGraph(Store store,
                                            JobName jobName,
                                            JobTimestamp jobTimestamp,
                                            Metadata metadata,
                                            String dot)
            throws IOException, InterruptedException, MaterialstoreException {
        Path dotFile = Files.createTempFile(null, null);
        Files.write(dotFile, dot.getBytes(StandardCharsets.UTF_8));
        Path pngFile = Files.createTempFile(null, null);
        int rc = DotUtil.runDot(dotFile, pngFile);
        assert rc == 0;
        return store.write(jobName, jobTimestamp, FileType.PNG, metadata, pngFile);
    }
}
