package com.as.backend.antscience.service;

import com.as.backend.antscience.dao.UserDao;
import com.as.backend.antscience.dto.LoginUser;
import com.as.backend.antscience.dto.RegisterUserDto;
import com.as.backend.antscience.dto.UserDto;
import com.as.backend.antscience.entity.User;
import com.as.backend.antscience.enums.Authority;
import com.as.backend.antscience.enums.Gender;
import com.as.backend.antscience.exceptions.UserExistedException;
import com.as.backend.antscience.exceptions.UserNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource
    private UserDao userDao;

    @Resource
    private TokenService tokenService;

    private String[] authorities = {Authority.GENERAL.toString()};

    public void createUser(User user) {
        userDao.saveAndFlush(user);
    }

    public User findUserByIdentity(String identity) {
        User user = userDao.findIdentity(identity);
        if (!Objects.nonNull(user)) {
            throw new UserNotFoundException("用户不存在");
        }
        return userDao.findIdentity(identity);
    }

    @Override
    public UserDto login(LoginUser loginUser) {
        User user = findUserByIdentity(loginUser.getIdentity());
        if (!loginUser.getPassword().equals(user.getPassword())) {
            throw new UserNotFoundException("用户密码有误");
        }
        return User2UserDto(user);
    }

    public UserDto register(RegisterUserDto registerUserDto) {
        String phone = registerUserDto.getPhone();
        String email = registerUserDto.getEmail();
        String username = registerUserDto.getUsername();
        String password = registerUserDto.getPassword();
        User foundUser = userDao.findIdentity(username);
        if (Objects.nonNull(foundUser)){
            throw new UserExistedException("用户名、手机号或邮箱已被使用或注册");
        }
        Gender gender = registerUserDto.getGender();
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setEmail(email);
        user.setGender(gender);
        user.setRoles(authorities);
        userDao.saveAndFlush(user);
        user = userDao.saveAndFlush(user);;
        return User2UserDto(user);
    }


    @Override
    public User findUserByUsername(String username) {
        return userDao.findUserByUsername(username);
    }

    public Long getUserIdByUserName(String username) {
        return findUserByUsername(username).getId();
    }

    private UserDto User2UserDto(User user) {
        String token = tokenService.buildToken(user.getUsername());
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), user.getGender(), token);
    }
}
