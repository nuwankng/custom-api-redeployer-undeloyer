package utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReadConfigFile {

    private static final Logger logger = LoggerFactory.getLogger(ReadConfigFile.class);
    private static ReadConfigFile instance;
    private Properties properties = new Properties();

    public static synchronized ReadConfigFile getInstance() throws IOException {
        if (instance == null) {
            instance = new ReadConfigFile();
        }
        return instance;
    }

    private ReadConfigFile() throws IOException {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (FileNotFoundException e) {
            logger.error("Can't find 'config.properties' file. Make sure the 'config.properties' is located with the running jar file.");
            throw e;
        } catch (IOException e) {
            logger.error("Error reading 'config.properties' file.");
            throw e;
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}