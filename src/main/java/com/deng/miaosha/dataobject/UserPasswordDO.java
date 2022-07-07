package com.deng.miaosha.dataobject;

import lombok.Data;

@Data
public class UserPasswordDO {

    private Integer id;

    private String encrptPassword;

    private Integer userId;

}