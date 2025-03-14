package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.omg.CORBA.UserException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    // 前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1 获取 session
        HttpSession session = request.getSession();

        // 2 获取 session 中的 用户
        Object user = session.getAttribute("user");

        // 3 判断用户是否存在
        if(user == null){
            response.setStatus(401);
            return false;
        }
        // 4 不存在 拦截 返回401 未授权状态码
        UserHolder.saveUser((UserDTO) user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
