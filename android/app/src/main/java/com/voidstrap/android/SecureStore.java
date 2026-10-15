package com.voidstrap.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts small secrets at rest. Every vault has its own non-exportable AES-256-GCM key in the Android
 * Keystore (StrongBox when the device has it). The vault name is bound into each ciphertext as associated
 * data, so a blob cannot be moved into another vault and still decrypt. Nothing here logs, and there is
 * no plaintext fallback.
 */
final class SecureStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String BLOB = "blob";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;

    private SecureStore() {
    }

    static synchronized boolean has(Context c, String vault) {
        return prefs(c, vault).contains(BLOB);
    }

    static synchronized boolean put(Context c, String vault, String plain) {
        if (plain == null || plain.isEmpty()) return false;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key(vault));
            cipher.updateAAD(vault.getBytes(StandardCharsets.UTF_8));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return prefs(c, vault).edit().putString(BLOB, encode(cipher.getIV()) + ":" + encode(ct)).commit();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the plaintext, or null when the vault is empty or unreadable. The blob is erased only when it
     * can never be decrypted again (corrupt, moved, or its key is gone). Transient failures keep it.
     */
    static synchronized String get(Context c, String vault) {
        String blob = prefs(c, vault).getString(BLOB, null);
        if (blob == null) return null;
        try {
            int sep = blob.indexOf(':');
            if (sep <= 0) throw new IllegalArgumentException("malformed blob");
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key(vault), new GCMParameterSpec(TAG_BITS, decode(blob.substring(0, sep))));
            cipher.updateAAD(vault.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(decode(blob.substring(sep + 1))), StandardCharsets.UTF_8);
        } catch (AEADBadTagException | IllegalArgumentException | UnrecoverableKeyException | KeyStoreException e) {
            clear(c, vault);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    static synchronized void clear(Context c, String vault) {
        prefs(c, vault).edit().remove(BLOB).commit();
    }

    private static SharedPreferences prefs(Context c, String vault) {
        return c.getApplicationContext().getSharedPreferences(vault, Context.MODE_PRIVATE);
    }

    private static SecretKey key(String vault) throws Exception {
        String alias = "voidstrap." + vault;
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(alias)) return (SecretKey) ks.getKey(alias, null);
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                return generate(alias, true);
            } catch (Exception ignored) {
                // No StrongBox on this device: fall back to the TEE-backed key below.
            }
        }
        return generate(alias, false);
    }

    private static SecretKey generate(String alias, boolean strongBox) throws Exception {
        KeyGenParameterSpec.Builder b = new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true);
        if (strongBox && Build.VERSION.SDK_INT >= 28) b.setIsStrongBoxBacked(true);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        gen.init(b.build());
        return gen.generateKey();
    }

    private static String encode(byte[] b) {
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }

    private static byte[] decode(String s) {
        return Base64.decode(s, Base64.NO_WRAP);
    }
}
