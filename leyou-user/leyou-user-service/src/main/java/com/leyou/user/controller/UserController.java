package com.leyou.user.controller;

import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wlm
 * @date 2021/8/24 - 9:52
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("check/{data}/{type}")
    public ResponseEntity<Boolean> checkUser(@PathVariable("data")String data, @PathVariable("type")Integer type){
        Boolean b = this.userService.checkUser(data, type);
        if(b == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(b);
    }

    @PostMapping("code")
    public ResponseEntity<Void> verifyCode(@RequestParam("phone")String phone){
        this.userService.verifyCode(phone);
        return ResponseEntity.noContent().build();
    }
}
