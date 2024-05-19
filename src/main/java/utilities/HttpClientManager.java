package utilities;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class HttpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientManager.class);

    private static CloseableHttpClient httpClientInstance;
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    private static final int SOCKET_TIMEOUT_MS = 30000; // 30 seconds
    static String KEY_STORE_PATH = "";
    static String KEY_STORE_PASSWORD = "";

    private HttpClientManager() {
    }

    public static synchronized CloseableHttpClient getInstance() {
        if (httpClientInstance == null) {
            httpClientInstance = createHttpClient();
            if (httpClientInstance == null){
                throw new IllegalStateException("Failed to create HttpClient instance.");
            }
        }
        return httpClientInstance;
    }

    private static CloseableHttpClient createHttpClient() {
        KeyStore keyStore;
        CloseableHttpClient httpClient = null;
        try {
            ReadConfigFile configs = ReadConfigFile.getInstance();
            KEY_STORE_PATH = configs.getProperty("TRUSTSTORE.PATH");
            KEY_STORE_PASSWORD = configs.getProperty("TRUSTSTORE.PASSWORD");
            keyStore = KeyStore.getInstance("jks");
            try (InputStream keyStoreInput = new FileInputStream(KEY_STORE_PATH)) {
                keyStore.load(keyStoreInput, KEY_STORE_PASSWORD.toCharArray());
            }

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(keyStore, null)
                    .build();

            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                    sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
            connManager.setMaxTotal(10);
            connManager.setDefaultMaxPerRoute(5);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .build();

            httpClient = HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        } catch (KeyStoreException | IOException | CertificateException |
                NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            logger.error("Exception caught while creating HttpClient: {}", e.getMessage(), e);
            return null;
        }
        return httpClient;
    }
}