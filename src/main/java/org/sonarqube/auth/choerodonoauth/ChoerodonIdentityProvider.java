package org.sonarqube.auth.choerodonoauth;

import static java.lang.String.format;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.http.HttpServletRequest;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.auth.dto.GroupDTO;
import org.sonarqube.auth.dto.GsonUser;
import org.sonarqube.auth.dto.SonarInfoDTO;
import org.sonarqube.auth.util.HttpConnectionPoolUtil;
import org.sonarqube.auth.util.PermissionType;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:16 2019/4/30
 * Description:
 */
@ServerSide
public class ChoerodonIdentityProvider implements OAuth2IdentityProvider {
    private static final Logger LOGGER = Loggers.get(ChoerodonIdentityProvider.class);
    private static final Gson gson = new Gson();

    private static final Token EMPTY_TOKEN = null;
    private static final String API_CREATE_GROUP = "api/user_groups/create";
    private static final String API_ADD_GROUP = "api/permissions/add_group";
    private static final String API_GET_GROUPS = "/api/users/groups";
    private static final String API_SONAR = "/sonar/info";
    private static final String API_CALLBACK = "/oauth2/callback/choerodon";
    private static final String API_USER = "/iam/v1/users/self";
    private static final String PAR_NAME = "name";
    private static final String PAR_PROJECT_KEY = "projectKey";
    private static final String PAR_GROUP_NAME = "groupName";
    private static final String PAR_PERMISSION = "permission";
    private static final String PAR_ORGANIZATION = "organization";
    private static final String PAR_GROUPS = "groups";
    private static final String PAR_LOING = "login";
    private static LinkedBlockingQueue queue = new LinkedBlockingQueue();
    private final ChoerodonConfiguration configuration;

    public ChoerodonIdentityProvider(ChoerodonConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getKey() {
        return "choerodon";
    }

    @Override
    public String getName() {
        return "Choerodon";
    }

    @Override
    public Display getDisplay() {
        return Display.builder()
                // URL of src/main/resources/static/gitlab.svg at runtime
                .setIconPath("/static/choerodon/choerodon.png").setBackgroundColor("#333c47").build();
    }

    @Override
    public boolean isEnabled() {
        return configuration.isEnabled();
    }

    @Override
    public boolean allowsUsersToSignUp() {
        return configuration.allowUsersToSignUp();
    }

    @Override
    public void init(InitContext context) {
        //获取sonarToken
        HttpConnectionPoolUtil connectionPoolUtil = new HttpConnectionPoolUtil();
        SonarInfoDTO sonarInfoDTO = connectionPoolUtil.getSonarInfo(configuration.url() + API_SONAR);
        OAuthService scribe = prepareScribe(sonarInfoDTO.getUrl()).build();
        String url = scribe.getAuthorizationUrl(EMPTY_TOKEN);
        try {
            //群组数据
            String queryStr = URLDecoder.decode(context.getRequest().getQueryString(), "UTF-8");
            String groupName = StringUtils.substringBetween(queryStr, "id=", "%3A");
            String projectName = StringUtils.substringAfterLast(queryStr, "%3A");
            LOGGER.info("queryStr------:" + queryStr);
            LOGGER.info("groupName------:" + groupName);
            LOGGER.info("projectName------:" + projectName);
            if (groupName != null && !groupName.isEmpty() && projectName != null && !projectName.isEmpty()) {
                //创建群组
                List<NameValuePair> createParameters = new ArrayList<>(0);
                createParameters.add(new BasicNameValuePair(PAR_NAME, groupName));
                connectionPoolUtil.doPost(sonarInfoDTO.getUrl() + API_CREATE_GROUP, sonarInfoDTO.getToken(), createParameters);

                //设置项目权限
                List<NameValuePair> addParameters = new ArrayList<>(0);
                addParameters.add(new BasicNameValuePair(PAR_PROJECT_KEY, groupName + ":" + projectName));
                addParameters.add(new BasicNameValuePair(PAR_GROUP_NAME, groupName));
                addParameters.add(new BasicNameValuePair(PAR_PERMISSION, PermissionType.USER.toValue()));
                addParameters.add(new BasicNameValuePair(PAR_ORGANIZATION, "default-organization"));
                connectionPoolUtil.doPost(sonarInfoDTO.getUrl() + API_ADD_GROUP, sonarInfoDTO.getToken(), addParameters);

                addParameters.remove(new BasicNameValuePair(PAR_PERMISSION, PermissionType.USER.toValue()));
                addParameters.remove(new BasicNameValuePair(PAR_PERMISSION, PermissionType.SCAN.toValue()));
                connectionPoolUtil.doPost(sonarInfoDTO.getUrl() + API_ADD_GROUP, sonarInfoDTO.getToken(), addParameters);
                queue.put(groupName);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        connectionPoolUtil.closeConnectionPool();
        context.redirectTo(url);
    }

    @Override
    public void callback(CallbackContext context) {
        //获取用户详情
        HttpConnectionPoolUtil connectionPoolUtil = new HttpConnectionPoolUtil();
        SonarInfoDTO sonarInfoDTO = connectionPoolUtil.getSonarInfo(configuration.url() + API_SONAR);
        HttpServletRequest request = context.getRequest();
        OAuthService scribe = prepareScribe(sonarInfoDTO.getUrl()).build();
        String oAuthVerifier = request.getParameter("code");
        Token accessToken = scribe.getAccessToken(EMPTY_TOKEN, new Verifier(oAuthVerifier));

        OAuthRequest userRequest = new OAuthRequest(Verb.GET, configuration.url() + API_USER, scribe);
        scribe.signRequest(accessToken, userRequest);

        com.github.scribejava.core.model.Response userResponse = userRequest.send();
        if (!userResponse.isSuccessful()) {
            throw new IllegalStateException(format("Fail to authenticate the user. Error code is %s, Body of the response is %s", userResponse.getCode(), userResponse.getBody()));
        }
        String userResponseBody = userResponse.getBody();
        LOGGER.trace("User response received : %s", userResponseBody);
        GsonUser gsonUser = GsonUser.parse(userResponseBody);
        UserIdentity.Builder builder = UserIdentity.builder().setProviderLogin(gsonUser.getLoginName()).setLogin(gsonUser.getLoginName()).setName(gsonUser.getRealName()).setEmail(gsonUser.getEmail());
        Set<String> groups = new HashSet<>();

        //获取原有组
        List<NameValuePair> valuePairs = new ArrayList<>();
        valuePairs.add(new BasicNameValuePair(PAR_LOING, gsonUser.getLoginName()));
        JsonObject object = connectionPoolUtil.doGet(sonarInfoDTO.getUrl() + API_GET_GROUPS, sonarInfoDTO.getToken(), valuePairs);
        if (object != null && object.get(PAR_GROUPS) != null) {
            for (JsonElement element : object.get(PAR_GROUPS).getAsJsonArray()) {
                LOGGER.info("----" + element.toString());
                groups.add(gson.fromJson(element, GroupDTO.class).getName());
            }
        }
        connectionPoolUtil.closeConnectionPool();
        //设置当前组
        try {
            if (queue.size() > 0) {
                groups.add(queue.take().toString());
            }
            builder.setGroups(groups);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        context.authenticate(builder.build());
        context.redirectToRequestedPage();
    }


    private ServiceBuilder prepareScribe(String callBackUrl) {
        if (!isEnabled()) {
            throw new IllegalStateException("Choerodon Authentication is disabled");
        }
        ServiceBuilder serviceBuilder = new ServiceBuilder()
                .provider(new ChoerodonOAuthApi(configuration.url()))
                .apiKey(configuration.applicationId())
                .apiSecret(configuration.secret())
                .grantType(OAuthConstants.AUTHORIZATION_CODE)
                .callback(callBackUrl + API_CALLBACK);
        if (configuration.scope() != null && !ChoerodonAuthPlugin.NONE_SCOPE.equals(configuration.scope())) {
            serviceBuilder.scope(configuration.scope());
        }
        return serviceBuilder;
    }

}
