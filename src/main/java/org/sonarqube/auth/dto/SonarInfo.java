package org.sonarqube.auth.dto;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  15:42 2019/5/8
 * Description:
 */
public class SonarInfo{
    private String userName;
    private String password;
    private String url;

    public SonarInfo(String userName, String password, String url) {
        this.userName = userName;
        this.password = password;
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
