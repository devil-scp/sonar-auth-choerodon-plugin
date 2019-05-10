package org.sonarqube.auth.choerodonoauth;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:06 2019/4/30
 * Description:
 */
public class ChoerodonAuthPlugin implements Plugin {
    public static final String CHOERODON_AUTH_ENABLED = "sonar.auth.choerodon.enabled";
    public static final String CHOERODON_AUTH_URL = "sonar.auth.choerodon.url";
    public static final String CHOERODON_AUTH_APPLICATIONID = "sonar.auth.choerodon.applicationId";
    public static final String CHOERODON_AUTH_SECRET = "sonar.auth.choerodon.secret";
    public static final String CHOERODON_AUTH_ALLOWUSERSTOSIGNUP = "sonar.auth.choerodon.allowUsersToSignUp";
    public static final String CHOERODON_AUTH_SCOPE = "sonar.auth.choerodon.scope";
    public static final String CATEGORY = "Choerodon";
    public static final String SUBCATEGORY = "authentication";

    public static final String NONE_SCOPE = "none";

    static List<PropertyDefinition> definitions() {
        return Arrays.asList(PropertyDefinition.builder(CHOERODON_AUTH_ENABLED).name("Enabled").description("Enable choerodon users to login. Value is ignored if client ID and secret are not defined.")
                        .category(CATEGORY).subCategory(SUBCATEGORY).type(BOOLEAN).defaultValue(valueOf(false)).index(1).build(),
                PropertyDefinition.builder(CHOERODON_AUTH_URL).name("choerodon url").description("URL to access choerodon.").category(CATEGORY).subCategory(SUBCATEGORY).defaultValue("https://choerodon.com.cn")
                        .index(2).build(),
                PropertyDefinition.builder(CHOERODON_AUTH_APPLICATIONID).name("Application ID").description("Application ID provided by choerodon when registering the application.").category(CATEGORY)
                        .subCategory(SUBCATEGORY).index(3).build(),
                PropertyDefinition
                        .builder(CHOERODON_AUTH_SECRET)
                        .name("Secret")
                        .description("Secret provided by choerodon when registering the application.")
                        .category(CATEGORY)
                        .subCategory(SUBCATEGORY)
                        .type(PropertyType.PASSWORD)
                        .index(4).build(),
                PropertyDefinition.builder(CHOERODON_AUTH_ALLOWUSERSTOSIGNUP).name("Allow users to sign-up")
                        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.").category(CATEGORY)
                        .subCategory(SUBCATEGORY).type(BOOLEAN).defaultValue(valueOf(true)).index(5).build(),
                PropertyDefinition.builder(CHOERODON_AUTH_SCOPE).name("choerodon access scope").description("Scope provided by choerodon when access user info.").category(CATEGORY).subCategory(SUBCATEGORY)
                        .type(SINGLE_SELECT_LIST).options(NONE_SCOPE).defaultValue(NONE_SCOPE).index(6).build());
    }

    @Override
    public void define(Context context) {
        context.addExtensions(ChoerodonConfiguration.class, ChoerodonIdentityProvider.class).addExtensions(definitions());
    }
}
