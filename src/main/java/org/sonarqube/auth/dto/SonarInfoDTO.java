package org.sonarqube.auth.dto;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  15:42 2019/5/8
 * Description:
 */
public class SonarInfoDTO {
    private String token;
    private String url;

    public SonarInfoDTO(String token, String url) {
        this.token = token;
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}