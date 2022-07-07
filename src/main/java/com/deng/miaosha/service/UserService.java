package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.UserModel;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface UserService {
    UserModel login(String telphone, String encrptPassword) throws BusinessException;
    UserModel getUserById(Integer id);
    void register(@NotNull @Valid UserModel userModel) throws BusinessException;
}
