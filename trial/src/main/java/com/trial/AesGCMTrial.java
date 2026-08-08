package com.trial;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class AesGCMTrial {
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // init iv vector
        var ivVectors = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(ivVectors);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        SecretKey key = keyGen.generateKey();

        Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, ivVectors);
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, spec);
        encryptCipher.updateAAD("Additional Authenticated Data".getBytes());


        byte[] plaintext = "Hello AES-GCM".getBytes();
        byte[] ciphertext = encryptCipher.doFinal(plaintext);

        System.out.println("Ciphertext: " + Arrays.toString(ciphertext));

        // Decrypt
        Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, key, spec);
        // without AAD, the decryption will fail, got Exception in thread "main" javax.crypto.AEADBadTagException: Tag mismatch
        //	at java.base/com.sun.crypto.provider.GaloisCounterMode$GCMDecrypt.doFinal(GaloisCounterMode.java:1545)
        //	at java.base/com.sun.crypto.provider.GaloisCounterMode.engineDoFinal(GaloisCounterMode.java:417)
        decryptCipher.updateAAD("Additional Authenticated Data".getBytes());

        byte[] decrypted = decryptCipher.doFinal(ciphertext);

        System.out.println("Decrypted: " + new String(decrypted));

    }
}
