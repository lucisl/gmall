package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin
public class PassportController {

    @Reference
    private UserService userService;

    @Value("${token.key}")
    private String key;


    @RequestMapping(value = "/index")
    public String index(HttpServletRequest request) {
        //获取到登陆成功后跳转的页面
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);
        return "index";
    }

    @RequestMapping(value = "/login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request) {
        String salt = request.getHeader("X-forwarded-for");
        UserInfo info = userService.login(userInfo);
        if (info != null) {

            // 生成token
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", info.getId());
            map.put("nickName", info.getNickName());
            String token = JwtUtil.encode(key, map, salt);
            System.out.println("token:" + token);
            return token;
        }
        return "fail";
    }

    @RequestMapping(value = "/verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
        //获取到key和盐
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");
        //解密token
        Map<String, Object> map = JwtUtil.decode(token, key, currentIp);
        if (map != null) {
            // 检查redis信息
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            if (userInfo != null) {
                return "success";
            }
        }
        return "fail";
    }


}
