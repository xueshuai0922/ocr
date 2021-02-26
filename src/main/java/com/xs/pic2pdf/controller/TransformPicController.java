package com.xs.pic2pdf.controller;

import com.xs.CommonResult;
import com.xs.pic2pdf.service.TransformPicService;
import org.opencv.core.Point;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author xueshuai
 * @date 2020/10/15 13:08
 * @description 利用OpenCV进行图片透视变换
 */

@RestController
@RequestMapping("/pic2pdf")
public class TransformPicController {

    @Resource
    private TransformPicService service;


    @PostMapping("/getPoints")
    public CommonResult getPoints(String imgPath){
      return   service.getPoints(imgPath);
    }


    @PostMapping("/transformPic")
    public CommonResult transformPic(@RequestBody Map<String, Object> data ){
        return   service.transformPic(data);
    }

    @PostMapping("/toPdf")
    public CommonResult toPdf(@RequestParam List<String> imgPaths,HttpServletRequest request){
        return   service.toPdf(imgPaths,request);
    }

    @PostMapping("/toSmartPdf")
    public CommonResult toSmartPdf(@RequestParam List<String> imgPaths,HttpServletRequest request){
        return   service.toSmartPdf(imgPaths,request);
    }

}
