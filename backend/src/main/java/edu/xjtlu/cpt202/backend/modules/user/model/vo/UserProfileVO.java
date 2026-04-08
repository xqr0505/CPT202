package edu.xjtlu.cpt202.backend.modules.user.model.vo;

import lombok.Data;

@Data
public class UserProfileVO {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String status;
}
