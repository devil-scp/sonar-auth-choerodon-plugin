package org.sonarqube.auth.choerodonoauth;

import javax.annotation.CheckForNull;

import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  12:50 2019/5/4
 * Description:
 */
@ServerSide
public class ChoerodonConfiguration {

    private final Configuration configuration;

    public ChoerodonConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


    @CheckForNull
    public String url() {
        return configuration.get(ChoerodonAuthPlugin.CHOERODON_AUTH_URL).orElse(null);
    }

    @CheckForNull
    public String applicationId() {
        return configuration.get(ChoerodonAuthPlugin.CHOERODON_AUTH_APPLICATIONID).orElse(null);
    }

    @CheckForNull
    public String secret() {
        return configuration.get(ChoerodonAuthPlugin.CHOERODON_AUTH_SECRET).orElse(null);
    }

    public String scope() {
        return configuration.get(ChoerodonAuthPlugin.CHOERODON_AUTH_SCOPE).orElse(null);
    }

    public boolean isEnabled() {
        return configuration.getBoolean(ChoerodonAuthPlugin.CHOERODON_AUTH_ENABLED).orElse(false) && applicationId() != null && secret() != null;
    }

    public boolean allowUsersToSignUp() {
        return configuration.getBoolean(ChoerodonAuthPlugin.CHOERODON_AUTH_ALLOWUSERSTOSIGNUP).orElse(false);
    }

}
