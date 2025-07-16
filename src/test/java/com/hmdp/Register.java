package com.hmdp;

import com.hmdp.entity.User;
import com.hmdp.service.IUserService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.List;
@SpringBootTest
public class Register {

    @Autowired
    private IUserService userService;

    @Test
    public void function(){
        String loginUrl = "http://localhost:8080/api/user/register"; // 替换为实际的登录URL
        String tokenFilePath = "tokens.txt"; // 存储Token的文件路径

        try {
            HttpClient httpClient = HttpClients.createDefault();

            BufferedWriter writer = new BufferedWriter(new FileWriter(tokenFilePath));

            // 从数据库中获取用户手机号
            List<User> users = userService.list();

            for(User user : users) {
                String phoneNumber = user.getPhone();

                // 构建登录请求
                HttpPost httpPost = new HttpPost(loginUrl);
                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("phone", phoneNumber);
                jsonRequest.put("code", "000000"); // 添加虚拟验证码
                StringEntity requestEntity = new StringEntity(
                        jsonRequest.toString(),
                        ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);
                httpPost.setHeader("Content-Type", "application/json");

                // 发送登录请求
                HttpResponse response = httpClient.execute(httpPost);

                // 处理登录响应，获取token
                int statusCode = response.getStatusLine().getStatusCode();
                System.out.println(statusCode);
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity);
                    System.out.println(responseString);
                    // 解析响应，获取token，这里假设响应是JSON格式的
                    // 根据实际情况使用合适的JSON库进行解析
                    String token = parseTokenFromJson(responseString);
                    System.out.println("手机号 " + phoneNumber + " 登录成功，Token: " + token);
                    // 将token写入txt文件
                    writer.write(token);
                    writer.newLine();
                } else {
                    System.out.println("手机号 " + phoneNumber + " 登录失败");
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 解析JSON响应获取token的方法，这里只是示例，具体实现需要根据实际响应格式进行解析
    private static String parseTokenFromJson(String json) {
        try {
            // 将JSON字符串转换为JSONObject
            JSONObject jsonObject = new JSONObject(json);
            // 从JSONObject中获取名为"token"的字段的值
            String token = jsonObject.getString("data");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // 解析失败，返回null或者抛出异常，具体根据实际需求处理
        }
    }
}
