package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.annotations.LoginRequired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class indexController {
    @LoginRequired
    @RequestMapping("index")
    public String index(){
        return "index";
    }
}
