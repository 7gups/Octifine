package com.voidstrap.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Lists the files inside the OBB that Pekora keeps in its APK assets and caches the names for the Rust planner,
 * which cannot read the OBB itself when the APK stores it compressed.
 */
final class ObbNames {
    private ObbNames() {
    }

    static File file(File stateDir, String pkg) {
        return new File(stateDir, "obb_names_" + pkg + ".txt");
    }

    static void ensure(File stateDir, String pkg, File apk, String identity) {
        if (apk == null || identity == null || identity.isEmpty() || identity.indexOf('\n') >= 0) return;
        File out = file(stateDir, pkg);
        try {
            if (out.isFile()) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(out), StandardCharsets.UTF_8))) {
                    if (identity.equals(r.readLine())) return;
                }
            }
            try (ZipFile z = new ZipFile(apk)) {
                String obbName = null;
                for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                    String n = en.nextElement().getName();
                    if (n.startsWith("assets/") && n.endsWith(".obb")) {
                        obbName = n;
                        break;
                    }
                }
                if (obbName == null) {
                    out.delete();
                    return;
                }
                List<String> names;
                try {
                    names = stream(z, obbName);
                } catch (IOException | RuntimeException e) {
                    names = extracted(z, obbName, stateDir);
                }
                File tmp = new File(stateDir, out.getName() + ".tmp");
                try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
                    w.write(identity + "\n");
                    for (String n : names) w.write(n + "\n");
                }
                out.delete();
                if (!tmp.renameTo(out)) tmp.delete();
            }
        } catch (IOException | RuntimeException e) {
            ModLog.add("obb names failed: " + e);
        }
    }

    private static List<String> stream(ZipFile z, String obbName) throws IOException {
        List<String> names = new ArrayList<>();
        try (InputStream in = z.getInputStream(z.getEntry(obbName));
             ZipInputStream zin = new ZipInputStream(new BufferedInputStream(in, 1 << 16))) {
            for (ZipEntry e; (e = zin.getNextEntry()) != null; ) {
                if (!e.isDirectory()) names.add(e.getName());
            }
        }
        if (names.isEmpty()) throw new IOException("no entries");
        return names;
    }

    private static List<String> extracted(ZipFile z, String obbName, File dir) throws IOException {
        File tmp = new File(dir, "obb_names_src.zip");
        tmp.delete();
        try {
            try (InputStream in = z.getInputStream(z.getEntry(obbName)); OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[1 << 16];
                int r;
                while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
            }
            List<String> names = new ArrayList<>();
            try (ZipFile obb = new ZipFile(tmp)) {
                for (Enumeration<? extends ZipEntry> en = obb.entries(); en.hasMoreElements(); ) {
                    ZipEntry e = en.nextElement();
                    if (!e.isDirectory()) names.add(e.getName());
                }
            }
            return names;
        } finally {
            tmp.delete();
        }
    }
}
