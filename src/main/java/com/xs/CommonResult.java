package com.xs;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * @author xueshuai
 * @date 2020/9/2 14:11
 * @description 对外接口返回结果集
 */
public class CommonResult {
    Object data;
    String code;  // 0 成功，1  失败
    String message;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CommonResult(Object data) {
        this.data = data;
    }

    public CommonResult(String message) {
        this.message = message;
    }

    public CommonResult(Object data, String code, String message) {
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public CommonResult(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public CommonResult() {
    }

    public static CommonResult SUCCESS() {
        return new CommonResult("0", "成功");
    }

//    public static CommonResult SUCCESS(String message) {
//        return new CommonResult("0", message);
//    }

    public static CommonResult SUCCESS(Object data) {
        return new CommonResult(data, "0", "成功");
    }

    public static CommonResult FAIL(String message) {
        return new CommonResult("1", message);
    }

    public static CommonResult FAIL() {
        return new CommonResult("1", "失败");
    }

    @Override
    public String toString() {
        return "CommonResult{" +
                "data=" + data +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
