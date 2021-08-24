package com.leyou.sms.listen;

import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * @author wlm
 * @date 2021/8/24 - 12:21
 */
@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsListener {

    @Autowired
    private SmsUtils smsUtils;

    @Autowired
    private SmsProperties smsProperties;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU.SMS.VERIFY.QUEUE", durable = "true"),
            exchange = @Exchange(value = "LEYOU.SMS.EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"sms.verify"}
    ))
    public void listen(Map<String, String> msg) throws Exception {
        if (CollectionUtils.isEmpty(msg)){
            return;
        }
        String phone = msg.get("phone");
        String code = msg.get("code");
        if (StringUtils.isNoneBlank(phone) && StringUtils.isNoneBlank(code)) {
            this.smsUtils.sendSms(phone, code, this.smsProperties.getSignName(), this.smsProperties.getVerifyCodeTemplate());
        }
    }
}