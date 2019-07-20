package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.beans.UmsMember;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    UserService userService;


   @RequestMapping("viogin")
    public String vlogin(String code, ModelMap map,HttpServletRequest request){

        // 处理授权码
        // 通过授权码，再加入服务器密钥，交换access_token
        String url3 = "https://api.weibo.gmall/oauth2/access_token";// 交换access_token的地址
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","610870119");
        paramMap.put("client_secret","001d2b1a94efd60ecd6d86c2327289e7");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/viogin");
        paramMap.put("code",code);
        String json = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", paramMap);
        String token = "";
        if(StringUtils.isNotBlank(json)){
            HashMap<String,String> hashMap = JSON.parseObject(json, new HashMap<String, String>().getClass());

            String access_token = hashMap.get("access_token");
            String uid = hashMap.get("uid");

            System.out.println(access_token);
            System.out.println(uid);
            String JSONUser = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid);// 根据access_token查询用户信息

            HashMap<String,Object> vloginUserMap = JSON.parseObject(JSONUser, new HashMap<String, Object>().getClass());

            // 将第三方账号存入数据库(联合账号类型)
            UmsMember umsMember = new UmsMember();
            umsMember.setUsername((String)vloginUserMap.get("name"));
            umsMember.setNickname((String)vloginUserMap.get("screen_name"));
            umsMember.setAccessCode(code);
            umsMember.setAccessToken(access_token);
            umsMember.setSourceUid(Long.parseLong((String)vloginUserMap.get("idstr")));
            umsMember.setCreateTime(new Date());
            umsMember.setCity((String)vloginUserMap.get("city"));
            String gender = (String)vloginUserMap.get("gender");
            int genderi = 0 ;
            if(gender.equals("m")){
                genderi = 1;
            }

            if(gender.equals("f")){
                genderi = 2;
            }
            umsMember.setGender(genderi);
            umsMember.setSourceType(2);
            UmsMember umsMemberReturn = userService.addOauthUser(umsMember);

            // 生成token继续访问应用

            String memeberId = umsMemberReturn.getId()+"";
            String nickname = umsMemberReturn.getNickname();
            String salt = request.getHeader("x-forward-for");
            if(StringUtils.isBlank(salt)){
                salt = request.getRemoteAddr();
                if(StringUtils.isBlank(salt)){
                    return "redirect:http://search.gmall.com:8083/index?newToken="+token;
                }
            }
            HashMap<String,Object> jwtUserMap = new HashMap<>();
            jwtUserMap.put("memberId",memeberId);
            jwtUserMap.put("nickname",nickname);
            token = JwtUtil.encode("gmall", jwtUserMap, salt);

        }

        return "redirect:http://search.gmall.com:8083/index?newToken="+token;
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember,String ReturnUrl,ModelMap map,HttpServletRequest request){
        String token="fail";
        UmsMember umsMemberFormUsre=userService.login(umsMember);
        if(umsMemberFormUsre!=null){
            String salt = request.getHeader("x-forward-for");
            if(StringUtils.isBlank(salt)){
                salt = request.getRemoteAddr();
                if(StringUtils.isBlank(salt)){
                    return token;
                }
            }
            HashMap<String ,Object>userMap=new HashMap<>();
            userMap.put("memberId",umsMemberFormUsre.getId()+"");
            userMap.put("nickname",umsMemberFormUsre.getNickname());
            String gmallToken = JwtUtil.encode("gmall", userMap, salt);
            //在缓存中 备份 一分
            userService.putTOken(gmallToken,umsMemberFormUsre.getId()+"");
            token=gmallToken;
        }
        return token;
    }

   @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map,HttpServletRequest request){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }
    @RequestMapping("verify")
    @ResponseBody
    public Map<String,String>verify(String token, HttpServletRequest request){
      Map<String,String>returnMap=new HashMap<>();
        String salt = request.getHeader("x-forward-for");
        if(StringUtils.isBlank(salt)){
          salt  = request.getRemoteAddr();
          if(StringUtils.isBlank(salt)){
              returnMap.put("success","fail");
              return  returnMap;
          }
        }
     Map<String,Object>gmallMap=JwtUtil.decode(token,"gmall",salt);
        if(gmallMap!=null){
            returnMap.put("success","success");
            returnMap.put("memberId",(String) gmallMap.get("memberId"));
            returnMap.put("nickname",(String) gmallMap.get("nickname"));
        }else {
            returnMap.put("success","fail");
        }
        return returnMap;
    }

}
