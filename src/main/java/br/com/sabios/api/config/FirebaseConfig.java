package br.com.sabios.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.credentials-file:firebase-service-account.json}")
    private String credentialsFile;

    @Value("${app.firebase.credentials-json:#{null}}")
    private String credentialsJson;

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp já inicializado.");
            return;
        }

        try {
            GoogleCredentials credentials = loadCredentials();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK inicializado com sucesso.");

        } catch (IOException e) {
            log.error("Falha ao inicializar Firebase Admin SDK: {}", e.getMessage());
            throw new RuntimeException("Não foi possível inicializar o Firebase. " +
                    "Verifique se o arquivo de credenciais existe ou a variável FIREBASE_CREDENTIALS_JSON está configurada.", e);
        }
    }

    /**
     * Carrega as credenciais do Firebase.
     * Prioridade:
     *   1. Variável de ambiente FIREBASE_CREDENTIALS_JSON (JSON inline — ideal para deploy em cloud)
     *   2. Arquivo no classpath (firebase-service-account.json — ideal para desenvolvimento local)
     */
    private GoogleCredentials loadCredentials() throws IOException {
        // 1. Tenta JSON inline (env var / application.yaml)
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            log.info("Carregando credenciais Firebase via JSON inline (env var).");
            InputStream stream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            return GoogleCredentials.fromStream(stream);
        }

        // 2. Tenta arquivo no classpath
        log.info("Carregando credenciais Firebase do arquivo: {}", credentialsFile);
        ClassPathResource resource = new ClassPathResource(credentialsFile);
        if (!resource.exists()) {
            throw new IOException("Arquivo de credenciais Firebase não encontrado: " + credentialsFile +
                    ". Coloque o arquivo em src/main/resources/ ou defina a variável FIREBASE_CREDENTIALS_JSON.");
        }
        return GoogleCredentials.fromStream(resource.getInputStream());
    }
}
