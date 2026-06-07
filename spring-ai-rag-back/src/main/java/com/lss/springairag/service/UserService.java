package com.lss.springairag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.entity.User;
import com.lss.springairag.pojo.dto.UserDTO;
import com.lss.springairag.pojo.dto.UserPageQueryDTO;

import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;

public interface UserService extends IService<User> {

    User login(String userName, String password) throws AccountNotFoundException, AccountLockedException;

    void saveUser(UserDTO userDTO);

    PageResult pageQuery(UserPageQueryDTO userPageQueryDTO);

    void startOrStop(Integer status, Long id);

    void updateUser(UserDTO userDTO);

    void register(User user);

    boolean getByUsername(String userName);
}
