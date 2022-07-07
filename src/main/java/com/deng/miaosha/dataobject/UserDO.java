package com.deng.miaosha.dataobject;

import lombok.Data;

@Data
public class UserDO {

    private Integer id;

    private String name;

    private Byte gender;

    private Integer age;

    private String telphone;

    private String registerMode;

    private String thirdPartyId;

}