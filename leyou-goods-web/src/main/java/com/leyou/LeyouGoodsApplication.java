package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author wlm
 * @date 2021/8/23 - 9:41
 */
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class LeyouGoodsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeyouGoodsApplication.class, args);
    }

}
