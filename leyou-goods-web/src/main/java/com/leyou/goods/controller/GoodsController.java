package com.leyou.goods.controller;

import com.leyou.goods.service.GoodsHtmlService;
import com.leyou.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.xml.ws.soap.Addressing;
import java.util.Map;

/**
 * @author wlm
 * @date 2021/8/23 - 9:48
 */
@Controller
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsHtmlService goodsHtmlService;

    @GetMapping("item/{spuId}.html")
    public String toItemPage(Model model, @PathVariable("spuId")Long spuId){

        Map<String,Object> modelMap = this.goodsService.loadData(spuId);
        model.addAllAttributes(modelMap);

        //this.goodsHtmlService.createHtml(spuId);

        return "item";
    }
}
