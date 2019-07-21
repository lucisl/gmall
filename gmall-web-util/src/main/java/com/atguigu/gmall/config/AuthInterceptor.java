package com.atguigu.gmall.config;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    //表示用户在进入处理器之前执行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取到token放入cookie中
        String token = request.getParameter("newToken");
        if (token != null) {
            //将token放入cookie
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        }
        //用户访问的可能是其他模块
        if (token == null) {
            token = CookieUtil.getCookieValue(request, "token", false);
        }
        //token不为空的时候才能得到用户名
        if (token != null) {
            // 解密token
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            // 存储用户昵称
            request.setAttribute("nickName", nickName);
        }
        //获取方法上的自定义注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        //获取到注解对象，判断方法上是否有注解
        if (methodAnnotation != null) {
            //如果认证成功，则表示用户已登陆，如果失败，且注解属性为false，不需要拦截，如果注解属性为true，则需要跳转到登陆页面
            String currentIp = request.getHeader("X-forwarded-for");
            //调用verify
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + currentIp);
            if ("success".equals(result)) {
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId", userId);
                return true;
            } else {
                if (methodAnnotation.autoRedirect()) {//注解默认值为true，需要登陆
                    //跳转到登陆页面
                    //获取到当前的页面 赋值给originUrl
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL:" + requestURL);
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println("encodeURL:" + encodeURL);
                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);
                    return false;
                }
            }
        }
        return true;
    }

    //解密获取用户名称的方法
    private Map getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);
        String result = null;
        try {
            result = new String(decode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(result, Map.class);
        return map;
    }

    //表示用户在进入处理器之后执行，渲染视图之前
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    //视图渲染之后执行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
