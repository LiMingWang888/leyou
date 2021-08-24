package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.pojo.User;
import com.leyou.user.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author wlm
 * @date 2021/8/24 - 9:51
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    public static final String KEY_PREFIX = "user:verify:";

    public Boolean checkUser(String data, Integer type) {
        User record = new User();
        if(type == 1){
            record.setUsername(data);
        }else if(type == 2){
            record.setPhone(data);
        }else {
            return null;
        }
        return this.userMapper.selectCount(record) == 0;
    }

    public void verifyCode(String phone) {
        //判断phone是否为空
        if(StringUtils.isEmpty(phone)){
            return;
        }

        //生成验证码
        String code = NumberUtils.generateCode(6);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        //发送消息给队列
        this.amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE","sms.verify",msg);

        //缓存验证码
        this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 3, TimeUnit.MINUTES);

    }
}
