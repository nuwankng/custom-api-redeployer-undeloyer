package utilities;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestRequests {

    private static final Logger logger = LoggerFactory.getLogger(RestRequests.class);
    private static final String AUTH_BASIC = "Basic ";
    private static final String AUTH_BEARER = "Bearer ";

    private RestRequests() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static JSONObject getToken(String url, String clientId, String clientSecret) {

        JSONObject tokenDetails = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        try{
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BASIC + credentials);
            List<NameValuePair> namevaluePairs = new ArrayList<>();
            namevaluePairs.add(new BasicNameValuePair("grant_type", "client_credentials"));
            namevaluePairs.add(new BasicNameValuePair("scope", "apim:apim:api_view apim:api_manage" +
                    " apim:api_import_export apim:api_list_view apim:api_create apim:api_publish"));

            httpPost.setEntity(new UrlEncodedFormEntity(namevaluePairs, HTTP.UTF_8));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                tokenDetails = (JSONObject) parser.parse(responseString);
            } else {
                logger.error("Error in getToken REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in getToken: {}", e.getMessage(), e);
        }
        return tokenDetails;
    }

    public static ArrayList<JSONObject> getAPIList(String url, String accessToken, String limit,
                                                   String offset, String sortBy, String orderBy)
    {
        ArrayList<JSONObject> apiDetailsList = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "?limit="+limit+"&offset="+offset+"&sortBy="+sortBy+"&sortOrder="+orderBy;

        try {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(responseString);
                if (responseJson.get("list") instanceof ArrayList) {
                    apiDetailsList = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                logger.error("Error in getAPIList REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in getAPIList: {}", e.getMessage(), e);
        }
        return apiDetailsList;
    }


    public static JSONObject getApiDetails(String url, String accessToken, String apiId) {

        JSONObject apiDetails = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "/" + apiId;

        try {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                apiDetails = (JSONObject) parser.parse(responseString);
            } else {
                logger.error("Error in getApiDetails REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in getApiDetails: {}", e.getMessage(), e);
        }
        return apiDetails;
    }


    public static Boolean updateApi(String url, String accessToken, String apiId, String apiData) {

        Boolean successState = false;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "/" + apiId;

        try {
            HttpPut httpPut = new HttpPut(url);
            httpPut.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            HttpEntity stringEntity = new StringEntity(apiData, ContentType.APPLICATION_JSON);
            httpPut.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPut);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                successState = true;
            } else {
                logger.error("Error in updateApi REST request: {} | Response: {}", url, responseString);
            }
        } catch (IOException e) {
            logger.error("Exception in updateApi: {}", e.getMessage(), e);
        }
        return successState;
    }

    public static ArrayList<JSONObject> getRevisionDetails(String url, String accessToken, String apiId){

        ArrayList<JSONObject> revisionDetails = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "/" + apiId + "/revisions?query=deployed:true";

        try {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(responseString);
                if (responseJson.get("list") instanceof ArrayList) {
                    revisionDetails = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                logger.error("Error in getRevisionDetails REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in getRevisionDetails: {}", e.getMessage(), e);
        }
        return revisionDetails;
    }

    public static JSONObject createRevision(String url,  String accessToken, String apiId, String description){

        JSONObject createRevisionResponse = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "/" + apiId + "/revisions";

        try {

            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            JSONObject requestJson = new JSONObject();
            requestJson.put("description", description);
            StringEntity requestEntity = new StringEntity(requestJson.toJSONString());
            httpPost.setEntity(requestEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                JSONParser parser = new JSONParser();
                createRevisionResponse = (JSONObject) parser.parse(responseString);
            } else {
                logger.error("Error in createRevision REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in createRevision: {}", e.getMessage(), e);
        }
        return createRevisionResponse;
    }

    public static ArrayList<JSONObject> deployRevision(String url, String accessToken, String apiId,
                                         List<Map<String, String>> deploymentMap, String newRevisionId) {

        ArrayList<JSONObject> deployRevisionResponse = null;
        CloseableHttpClient httpClient = HttpClientManager.getInstance();
        url = url + "/" + apiId + "/deploy-revision?revisionId=" + newRevisionId;
        String payloadJson = "[\n%s\n]";

        String jsonArray = deploymentMap.stream()
                .map(deployment -> String.format(
                        "    {\n" +
                                "      \"revisionUuid\": \"%s\",\n" +
                                "      \"name\": \"%s\",\n" +
                                "      \"vhost\": \"%s\",\n" +
                                "      \"displayOnDevportal\": %s\n" +
                                "    }",
                        newRevisionId,
                        deployment.get("name"),
                        deployment.get("vhost"),
                        deployment.get("displayOnDevportal")
                ))
                .collect(Collectors.joining(",\n"));

        String jsonPayload = String.format(payloadJson, jsonArray);

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, AUTH_BEARER + accessToken);
            HttpEntity stringEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                JSONParser parser = new JSONParser();
                deployRevisionResponse = (ArrayList<JSONObject>) parser.parse(responseString);
            } else {
                logger.error("Error in deployRevision REST request: {} | Response: {}", url, responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            logger.error("Exception in deployRevision: {}", e.getMessage(), e);
        }
        return deployRevisionResponse;
    }
}
