package org.sonarqube.auth.dto;

import com.google.gson.Gson;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:50 2019/4/30
 * Description:
 */

public class GsonUser {
    private String loginName;
    private String realName;
    private String email;

    public static GsonUser parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, GsonUser.class);
    }

    public String getRealName() {
        return realName;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getEmail() {
        return email;
    }
}