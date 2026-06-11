package com.lss.springairag.pojo.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserDTO implements Serializable {

    private Long id;

    private String userName;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

    private String role;

}
