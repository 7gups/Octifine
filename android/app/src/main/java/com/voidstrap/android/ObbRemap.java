package com.voidstrap.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class ObbRemap {
    private static final String FINGERPRINT = "assets/fingerprint.txt";

    private ObbRemap() {
    }

    static File remap(File baseApk, File workDir, Map<String, ApkPatcher.Source> changes, String planFingerprint,
                      AtomicBoolean cancel) throws IOException {
        Set<String> apkNames = new HashSet<>();
        String obbName = null;
        try (ZipFile apk = new ZipFile(baseApk)) {
            for (java.util.Enumeration<? extends ZipEntry> en = apk.entries(); en.hasMoreElements(); ) {
                String n = en.nextElement().getName();
                apkNames.add(n);
                if (obbName == null && n.startsWith("assets/") && n.endsWith(".obb")) obbName = n;
            }
            if (obbName == null) return null;

            Map<String, String> obbNames = new HashMap<>();
            File src = new File(workDir, "obb_src.zip");
            File out = new File(workDir, "obb_out.zip");
            src.delete();
            out.delete();
            try {
                ZipEntry obbEntry = apk.getEntry(obbName);
                try (InputStream in = apk.getInputStream(obbEntry); OutputStream os = new FileOutputStream(src)) {
                    byte[] buf = new byte[1 << 16];
                    int r;
                    while ((r = in.read(buf)) > 0) {
                        if (cancel != null && cancel.get()) throw new IOException("cancelled");
                        os.write(buf, 0, r);
                    }
                }
                try (ZipFile obb = new ZipFile(src)) {
                    for (java.util.Enumeration<? extends ZipEntry> en = obb.entries(); en.hasMoreElements(); ) {
                        String n = en.nextElement().getName();
                        obbNames.put(n.toLowerCase(Locale.ROOT), n);
                    }
                }

                Map<String, ApkPatcher.Source> inObb = new TreeMap<>();
                for (Iterator<Map.Entry<String, ApkPatcher.Source>> it = changes.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, ApkPatcher.Source> e = it.next();
                    String key = e.getKey();
                    if (!key.startsWith("assets/") || apkNames.contains(key)) continue;
                    String rel = key.substring("assets/".length());
                    String actual = obbNames.get(rel.toLowerCase(Locale.ROOT));
                    if (actual != null) {
                        inObb.put(actual, e.getValue());
                        it.remove();
                    } else {
                        ModLog.add("obb remap: " + rel + " is not in the OBB either, left as a plain APK entry");
                    }
                }
                if (inObb.isEmpty()) {
                    src.delete();
                    return null;
                }

                ApkPatcher.patch(src, out, inObb, cancel);
                src.delete();
                changes.put(obbName, new ApkPatcher.FileSource(out));
                changes.put(FINGERPRINT, new ApkPatcher.BytesSource(fingerprint(planFingerprint)));
                ModLog.add("obb remap: " + inObb.size() + " file(s) moved into " + obbName);
                return out;
            } catch (IOException | RuntimeException e) {
                src.delete();
                out.delete();
                throw e;
            }
        }
    }

    private static byte[] fingerprint(String planFingerprint) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(("voidstrap|" + planFingerprint).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
