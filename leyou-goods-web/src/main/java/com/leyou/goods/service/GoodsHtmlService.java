package com.leyou.goods.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.crypto.interfaces.PBEKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * @author wlm
 * @date 2021/8/23 - 15:51
 */
@Service
public class GoodsHtmlService {

    @Autowired
    private TemplateEngine engine;

    @Autowired
    private GoodsService goodsService;

    public void createHtml(Long spuId){

        // 初始化上下文对象
        Context context = new Context();
        // 设置数据模型给上下文
        context.setVariables(this.goodsService.loadData(spuId));

        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new File("C:\\hm53\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html"));

            this.engine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null){
                printWriter.close();
            }
        }
    }
}