package egghead.swish.swishcreatepayment.swish;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Service
public class SslContextFactory {

    private final String swishApiCertificateFile;
    private final String swishApiCertificatePassword;

    public SslContextFactory(@Value("${swish.api.certificate.file}") String swishApiCertificateFile, @Value("${swish.api.certificate.password}") String swishApiCertificatePassword) {
        this.swishApiCertificateFile = swishApiCertificateFile;
        this.swishApiCertificatePassword = swishApiCertificatePassword;
    }

    private KeyManagerFactory getKeyManagerFactory(String file, String password) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(fileInputStream, password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, password.toCharArray());
            return keyManagerFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SslContext createSslContext() {
        try {
            String pkcs12FilePath = SslContextFactory.class.getClassLoader().getResource(swishApiCertificateFile).getPath();
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(pkcs12FilePath, swishApiCertificatePassword);
            return SslContextBuilder
                .forClient()
                .startTls(true)
                .keyManager(keyManagerFactory)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSlContext.", e);
        }
    }
}
