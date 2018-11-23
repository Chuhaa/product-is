/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.identity.scenarios.test.scim2;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.identity.scenarios.commons.ScenarioTestBase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ProvisionExistingUserTestCase extends ScenarioTestBase {

    private String userId;
    private CloseableHttpClient client;
    private String USERNAME = "scim2user";
    private String PASSWORD = "scim2pwd";

    @BeforeClass(alwaysRun = true)
    public void testInit() throws Exception {

        setKeyStoreProperties();
        client = HttpClients.createDefault();

        HttpResponse response = testCreateSCSIM2User(USERNAME, PASSWORD);
        assertEquals(response.getStatusLine().getStatusCode(), 201, "User has not been created successfully");

        Object responseObj = JSONValue.parse(EntityUtils.toString(response.getEntity()));
        EntityUtils.consume(response.getEntity());

        String usernameFromResponse = ((JSONObject) responseObj).get(SCIMConstants.USER_NAME_ATTRIBUTE).toString();
        assertEquals(usernameFromResponse, SCIMConstants.USERNAME);

        userId = ((JSONObject) responseObj).get(SCIMConstants.ID_ATTRIBUTE).toString();
        assertNotNull(userId);
    }

    @Test(description = "1.1.1.1.4")
    public void testSCIM2CreateExistingUser() throws Exception {

        HttpResponse response = testCreateSCSIM2User(USERNAME, PASSWORD);
        assertEquals(response.getStatusLine().getStatusCode(), 409,
                "User already exists hence server should not accept user creation");

        Object responseObj = JSONValue.parse(EntityUtils.toString(response.getEntity()));
        EntityUtils.consume(response.getEntity());
        JSONArray schemasArray = (JSONArray) ((JSONObject) responseObj).get("schemas");
        assertNotNull(schemasArray);
        assertEquals(schemasArray.get(0).toString(), SCIMConstants.ERROR_SCHEMA);
        assertTrue(
                responseObj.toString().contains("User with the name: " + USERNAME + " already exists in the system"));

        testDeleteUser();
    }

    private HttpResponse testCreateSCSIM2User(String scimUsername, String scimPassword) throws Exception {

        String scimEndpoint = getDeploymentProperties().getProperty(IS_HTTPS_URL) + SCIMConstants.SCIM2_USERS_ENDPOINT;
        HttpPost request = new HttpPost(scimEndpoint);
        request.addHeader(HttpHeaders.AUTHORIZATION, getAuthzHeader());
        request.addHeader(HttpHeaders.CONTENT_TYPE, SCIMConstants.CONTENT_TYPE_APPLICATION_JSON);

        JSONObject rootObject = new JSONObject();
        JSONArray schemas = new JSONArray();
        rootObject.put(SCIMConstants.SCHEMAS_ATTRIBUTE, schemas);
        JSONObject names = new JSONObject();
        names.put(SCIMConstants.GIVEN_NAME_ATTRIBUTE, SCIMConstants.GIVEN_NAME_CLAIM_VALUE);
        rootObject.put(SCIMConstants.NAME_ATTRIBUTE, names);
        rootObject.put(SCIMConstants.USER_NAME_ATTRIBUTE, scimUsername);
        rootObject.put(SCIMConstants.PASSWORD_ATTRIBUTE, scimPassword);

        StringEntity entity = new StringEntity(rootObject.toString());
        request.setEntity(entity);

        return client.execute(request);
    }

    private void testDeleteUser() throws Exception {

        String userResourcePath =
                getDeploymentProperties().getProperty(IS_HTTPS_URL) + SCIMConstants.SCIM2_USERS_ENDPOINT + "/" + userId;
        HttpDelete request = new HttpDelete(userResourcePath);
        request.addHeader(HttpHeaders.AUTHORIZATION, getAuthzHeader());
        request.addHeader(HttpHeaders.CONTENT_TYPE, SCIMConstants.CONTENT_TYPE_APPLICATION_JSON);

        HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 204, "User has not been retrieved successfully");
        EntityUtils.consume(response.getEntity());
        userResourcePath =
                getDeploymentProperties().getProperty(IS_HTTPS_URL) + SCIMConstants.SCIM2_USERS_ENDPOINT + "/" + userId;
        HttpGet getRequest = new HttpGet(userResourcePath);
        getRequest.addHeader(HttpHeaders.AUTHORIZATION, getAuthzHeader());
        getRequest.addHeader(HttpHeaders.CONTENT_TYPE, SCIMConstants.CONTENT_TYPE_APPLICATION_JSON);

        response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), 404, "User has not been deleted successfully");
        EntityUtils.consume(response.getEntity());
    }

}