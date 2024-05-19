import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.ReadConfigFile;
import utilities.RestRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DeploymentService {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);

    static String residentTokenUrl;
    static String publisherRestUrl;
    static String jsonFilePath;
    static String revisionDescription;
    static String apiListLimit;
    static String apiListOffset;
    static String apiListSortBy;
    static String apiListOrderBy;
    static String keyStorePath;
    static String keyStorePassword;

    public static void main(String[] args) {
        logger.info("Starting the API deployment service...");
        try {
            // Load configurations
            loadConfigurations();
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Configuration error: {}", e.getMessage());
            e.printStackTrace();
        }

        // Parse JSON token string from the config.properties file
        if (jsonFilePath != null && !jsonFilePath.trim().isEmpty()) {
            JSONParser parser = new JSONParser();
            try (FileReader reader = new FileReader(jsonFilePath)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                Set<String> tenants = jsonObject.keySet();

                // Iterate over each tenant
                logger.info("Starting tenant-specific API deployment");
                for (String tenant : tenants) {
                    JSONObject credentials = (JSONObject) jsonObject.get(tenant);
                    String consumerKey = (String) credentials.get("consumerKey");
                    String consumerSecret = (String) credentials.get("consumerSecret");

                    logger.info("Starting the process of redeploying APIs in the tenant: {}", tenant);
                    logger.info("Retrieving access token for tenant: {} with consumer key: {}", tenant, consumerKey);
                    JSONObject tokenDetails = RestRequests.getToken(residentTokenUrl, consumerKey, consumerSecret);
                    if (tokenDetails == null || tokenDetails.isEmpty()) {
                        logger.error("Failed to obtain access token for consumerKey: {}", consumerKey);
                        continue;
                    }

                    String accessToken = (String) tokenDetails.get("access_token");

                    logger.info("Retrieving tenant-specific APIs for deployment");
                    ArrayList<JSONObject> apiDetailsArray = RestRequests.getAPIList(publisherRestUrl, accessToken,
                            apiListLimit, apiListOffset, apiListSortBy, apiListOrderBy);

                    if (apiDetailsArray == null || apiDetailsArray.isEmpty()) {
                        logger.error("No APIs found for consumerKey: {}", consumerKey);
                        continue;
                    }

                    logger.info("Updating and creating new revisions for {} APIs", apiDetailsArray.size());
                    for (JSONObject apiDetails : apiDetailsArray) {
                        String apiId = (String) apiDetails.get("id");
                        String apiName = (String) apiDetails.get("name");

                        logger.info("Retrieving details for API: {} with ID: {}", apiName, apiId);
                        JSONObject apiData = RestRequests.getApiDetails(publisherRestUrl, accessToken, apiId);
                        if (apiData == null) {
                            logger.error("Failed to retrieve details for API: {} with ID: {}", apiName, apiId);
                            continue;
                        }

                        logger.info("Updating API: {} with ID: {}", apiName, apiId);
                        Boolean updateStatus = RestRequests.updateApi(publisherRestUrl, accessToken, apiId, apiData.toJSONString());

                        if (!updateStatus) {
                            logger.error("Failed to update API: {} with ID: {}", apiName, apiId);
                            continue;
                        }

                        logger.info("Finished updating API: {} with ID: {}", apiName, apiId);
                        logger.info("Creating and deploying new revision for API: {} with ID: {}", apiName, apiId);

                        ArrayList<JSONObject> deployedRevisionDetails = RestRequests.getRevisionDetails(publisherRestUrl, accessToken, apiId);
                        if (deployedRevisionDetails == null || deployedRevisionDetails.isEmpty()) {
                            logger.warn("No deployed revisions found for API: {} with ID: {}", apiName, apiId);
                            continue;
                        }

                        Map<String, List<Map<String, String>>> revisionMap = extractDeploymentInfo(deployedRevisionDetails);

                        logger.info("Creating new revision for API: {} with ID: {}", apiName, apiId);
                        JSONObject createNewRevisionResponse = RestRequests.createRevision(publisherRestUrl, accessToken, apiId, revisionDescription);
                        if (createNewRevisionResponse == null || createNewRevisionResponse.isEmpty()) {
                            logger.error("Failed to create new revision for API: {} with ID: {}", apiName, apiId);
                            continue;
                        }
                        String newRevisionId = (String) createNewRevisionResponse.get("id");
                        logger.info("Created new revision with ID: {}", newRevisionId);

                        deployNewRevision(publisherRestUrl, accessToken, apiId, revisionMap, newRevisionId);
                    }
                    logger.info("API redeployment process for the tenant: {} with consumer key: {} has been completed", tenant, consumerKey);
                }
            } catch (FileNotFoundException e) {
                logger.error("JSON file not found: {}", jsonFilePath);
            } catch (IOException e) {
                logger.error("IOException when reading JSON file: {}", e.getMessage());
            } catch (ParseException e) {
                logger.error("Error parsing JSON file: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error: {}", e.getMessage());
                e.printStackTrace();
            }
            logger.info("All API redeployment processes have been successfully completed");
        }
    }

    private static void loadConfigurations() throws IOException {
        ReadConfigFile configs = ReadConfigFile.getInstance();
        logger.info("Loading the relevant configuration from the file system");

        keyStorePath = loadAndValidateProperty(configs, "TRUSTSTORE.PATH");
        keyStorePassword = loadAndValidateProperty(configs, "TRUSTSTORE.PASSWORD");
        residentTokenUrl = loadAndValidateProperty(configs, "RESIDENTKM.TOKEN.URL");
        publisherRestUrl = loadAndValidateProperty(configs, "PUBLISHER.REST.URL");
        revisionDescription = loadAndValidateProperty(configs, "REVISION.DESCRIPTION");
        jsonFilePath = loadAndValidateProperty(configs, "JSON.FILE.PATH");
        apiListLimit = loadAndValidateProperty(configs, "API.LIST.LIMIT");
        apiListOffset = loadAndValidateProperty(configs, "API.LIST.OFFSET");
        apiListSortBy = loadAndValidateProperty(configs, "API.LIST.SORTBY");
        apiListOrderBy = loadAndValidateProperty(configs, "API.LIST.ORDERBY");
    }

    private static String loadAndValidateProperty(ReadConfigFile configs, String propertyName) {
        String propertyValue = configs.getProperty(propertyName);
        if (propertyValue == null || propertyValue.isEmpty()) {
            logger.error("Missing or empty configuration property: {}", propertyName);
            throw new IllegalArgumentException("Missing required configuration property: " + propertyName);
        }
        return propertyValue;
    }

    private static Map<String, List<Map<String, String>>> extractDeploymentInfo(ArrayList<JSONObject> deployedRevisionDetails) {
        Map<String, List<Map<String, String>>> revisionMap = new HashMap<>();
        for (JSONObject revisionDetail : deployedRevisionDetails) {
            String revisionId = (String) revisionDetail.get("id");
            ArrayList<JSONObject> deploymentInfoList = (ArrayList<JSONObject>) revisionDetail.get("deploymentInfo");
            List<Map<String, String>> deploymentRevisionList = new ArrayList<>();
            for (JSONObject deploymentInfo : deploymentInfoList) {
                String name = (String) deploymentInfo.get("name");
                String vhost = (String) deploymentInfo.get("vhost");
                String displayOnDevportal = deploymentInfo.get("displayOnDevportal").toString();
                Map<String, String> deploymentMap = new HashMap<>();
                deploymentMap.put("name", name);
                deploymentMap.put("vhost", vhost);
                deploymentMap.put("displayOnDevportal", displayOnDevportal);
                deploymentRevisionList.add(deploymentMap);
            }
            revisionMap.put(revisionId, deploymentRevisionList);
        }
        return revisionMap;
    }

    private static void deployNewRevision(String publisherRestUrl, String accessToken, String apiId, Map<String, List<Map<String, String>>> revisionMap, String newRevisionId) {
        for (Map.Entry<String, List<Map<String, String>>> entry : revisionMap.entrySet()) {
            ArrayList<JSONObject> deployRevision = RestRequests.deployRevision(publisherRestUrl, accessToken, apiId, entry.getValue(), newRevisionId);
            if (deployRevision == null || deployRevision.isEmpty()) {
                logger.error("Failed to deploy new revision with ID: {}", newRevisionId);
                continue;
            }
            logger.info("New revision deployed successfully with ID: {}", newRevisionId);
        }
    }
}
