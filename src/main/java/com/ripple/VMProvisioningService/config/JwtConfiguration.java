package com.ripple.VMProvisioningService.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Configuration
public class JwtConfiguration {
    @Value("${app.security.jwt.keystore-location}")
    private String keyStorePath;

    @Value("${app.security.jwt.keystore-password}")
    private String keyStorePassword;

    @Value("${app.security.jwt.key-alias}")
    private String keyAlias;

    @Value("${app.security.jwt.private-key-passphrase}")
    private String privateKeyPassphrase;

    @Bean
    public KeyStore keyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(keyStorePath);
            keyStore.load(resourceAsStream, keyStorePassword.toCharArray());
            return keyStore;
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            log.error("Unable to load keystore: {}", keyStorePath, e);
        }
        throw new RuntimeException("Unable to load keystore");
    }

    @Bean
    public RSAPrivateKey jwtSigningKey(KeyStore keyStore) {
        try {
            Key key = keyStore.getKey(keyAlias, privateKeyPassphrase.toCharArray());
            if (key instanceof RSAPrivateKey) {
                return (RSAPrivateKey) key;
            }
        } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            log.error("Unable to load private key from keystore: {}", keyStorePath, e);
        }
        throw new RuntimeException("Unable to load keystore");
    }

    @Bean
    public RSAPublicKey jwtValidationKey(KeyStore keyStore) {
        try {
            Certificate certificate = keyStore.getCertificate(keyAlias);
            PublicKey publicKey = certificate.getPublicKey();

            if (publicKey instanceof RSAPublicKey) {
                return (RSAPublicKey) publicKey;
            }
        } catch (KeyStoreException e) {
            log.error("Unable to validate the public key : {}", keyStorePath, e);
        }
        throw new RuntimeException("Unable to load RSA public key");
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }
}
