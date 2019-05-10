package org.sonarqube.auth.dto;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  14:01 2019/5/8
 * Description:
 */
public class GroupDTO {
    private Long id;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
