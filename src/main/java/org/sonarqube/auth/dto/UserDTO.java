package org.sonarqube.auth.dto;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  15:42 2019/5/8
 * Description:
 */
public class UserDTO {
    private String userName;
    private String password;

    public UserDTO(String userName, String password) {
        this.userName = userName;
        this.password = password;
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
}
