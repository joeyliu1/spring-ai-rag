package com.lss.springairag.controller;

import com.lss.springairag.common.ApplicationConstant;
import com.lss.springairag.common.BaseResponse;
import com.lss.springairag.common.PageResult;
import com.lss.springairag.common.ResultUtils;
import com.lss.springairag.config.JwtProperties;
import com.lss.springairag.constant.JwtClaimsConstant;
import com.lss.springairag.context.BaseContext;
import com.lss.springairag.entity.User;
import com.lss.springairag.pojo.dto.PasswordDTO;
import com.lss.springairag.pojo.dto.UserLoginDTO;
import com.lss.springairag.pojo.dto.UserDTO;
import com.lss.springairag.pojo.dto.UserPageQueryDTO;
import com.lss.springairag.pojo.vo.UserLoginVO;
import com.lss.springairag.service.UserService;
import com.lss.springairag.utils.JwtUtil;
import com.lss.springairag.utils.PasswordUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Tag(name = "UserController", description = "用户管理")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;


    /**
     * 修改密码
     */
    @PostMapping("/updatePassword")
    @Operation(summary = "updatePassword", description = "修改密码")
    public BaseResponse updatePassword(@RequestBody PasswordDTO passwordDTO) {
        Long currentUserId = BaseContext.getCurrentId();
        log.info("修改密码，userId：{}", currentUserId);
        if (passwordDTO == null
                || !StringUtils.hasText(passwordDTO.getOldPassword())
                || !StringUtils.hasText(passwordDTO.getNewPassword())
                || !StringUtils.hasText(passwordDTO.getConfirmPassword())) {
            return ResultUtils.error("密码参数不能为空");
        }
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            return ResultUtils.error("新密码与确认密码不一致");
        }
        if (passwordDTO.getNewPassword().length() < 6 || passwordDTO.getNewPassword().length() > 64) {
            return ResultUtils.error("密码长度需在 6 到 64 个字符之间");
        }
        User user = userService.getById(currentUserId);
        if (user == null) {
            return ResultUtils.error("当前登录用户不存在，请重新登录");
        }
        if (!PasswordUtils.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            return ResultUtils.error("旧密码错误");
        }
        user.setPassword(PasswordUtils.encode(passwordDTO.getNewPassword()));
        userService.updateById(user);
        return ResultUtils.success("修改密码成功");
    }

    /**
     * 注册
     *
     */
    @PostMapping("/register")
    @Operation(summary = "register", description = "注册")
    public BaseResponse register(@RequestBody User user) {
        log.info("注册用户：{}", user == null ? null : user.getUserName());

        if (user == null || !StringUtils.hasText(user.getUserName()) || !StringUtils.hasText(user.getPassword())) {
            return ResultUtils.error("用户名和密码不能为空");
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 64) {
            return ResultUtils.error("密码长度需在 6 到 64 个字符之间");
        }
        if (userService.getByUsername(user.getUserName())) {
            return ResultUtils.error("用户名已存在");
        } else {
            userService.register(user);
        }
        return ResultUtils.success("注册成功");
    }


    /**
     * 登录
     *
     * @param
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "login", description = "登录")
    public BaseResponse login(@RequestBody UserLoginDTO loginDTO) {
        if (loginDTO == null || !StringUtils.hasText(loginDTO.getUserName()) || !StringUtils.hasText(loginDTO.getPassword())) {
            return ResultUtils.error("用户名和密码不能为空");
        }
        log.info("登录用户：{}", loginDTO.getUserName());

        User user = userService.login(loginDTO.getUserName(), loginDTO.getPassword());

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .name(user.getName())
                .role(normalizeRole(user.getRole(), user.getUserName()))
                .token(token)
                .build();

        return ResultUtils.success(userLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "logout", description = "退出")
    public BaseResponse<String> logout() {
        return ResultUtils.success("退出成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "me", description = "获取当前登录用户信息")
    public BaseResponse<User> me() {
        User user = userService.getById(BaseContext.getCurrentId());
        if (user == null) {
            return ResultUtils.error("当前登录用户不存在，请重新登录");
        }
        return ResultUtils.success(user);
    }

    /**
     * 新增
     *
     * @param userDTO
     * @return
     */
    @PostMapping("/addUser")
    @Operation(summary = "addUser", description = "新增user")
    public BaseResponse save(@RequestBody UserDTO userDTO) {
        log.info("新增员工：{}", userDTO);
        if (userService.getByUsername(userDTO.getUserName())) {
            return ResultUtils.error("用户名已存在");
        }
        userService.saveUser(userDTO);
        return ResultUtils.success("新增成功");
    }

    /**
     * 员工分页查询
     *
     * @param userPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "page", description = "user分页查询")
    public BaseResponse<PageResult> page(UserPageQueryDTO userPageQueryDTO) {
        log.info("员工分页查询，参数为：{}", userPageQueryDTO);
        PageResult pageResult = userService.pageQuery(userPageQueryDTO);
        return ResultUtils.success(pageResult);
    }

    /**
     * 启用禁用员工账号
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "status", description = "启用禁用账号")
    public BaseResponse startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用禁用员工账号：{},{}", status, id);
        userService.startOrStop(status, id);
        return ResultUtils.success("禁用成功");
    }

    /**
     * 根据id查询员工信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "info", description = "根据id查询user信息")
    public BaseResponse<User> getById(@PathVariable Long id) {
        User employee = userService.getById(id);
        return ResultUtils.success(employee);
    }

    /**
     * 编辑员工信息
     *
     * @param user
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "info", description = "编辑user信息")
    public BaseResponse update(@RequestBody User user) {
        log.info("编辑员工信息：{}", user);
        userService.updateById(user);
        return ResultUtils.success("编辑成功");
    }

    private String normalizeRole(String role, String userName) {
        if ("admin".equals(role) || "user".equals(role)) {
            return role;
        }
        return "admin".equals(userName) ? "admin" : "user";
    }
}
