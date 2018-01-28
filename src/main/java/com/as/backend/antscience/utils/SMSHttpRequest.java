package com.as.backend.antscience.utils;

import com.as.backend.antscience.dto.SMSdto;
import com.as.backend.antscience.exceptions.SMSException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.*;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Data
@NoArgsConstructor
@Slf4j
public class SMSHttpRequest {
    private String appid;
    private String to;
    private String project;
    private String signature;
    private String URL;

    @Resource(name = "redisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    public SMSdto execute() {
        SMSdto smSdto = new SMSdto();
        String code = RandomCode.generateCode(4);
        redisTemplate.opsForValue().set(to, code);
        AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
        List<Param> params = new ArrayList<>();
        params.add(new Param("appid", appid));
        params.add(new Param("to", to));
        params.add(new Param("project", project));
        params.add(new Param("signature", signature));
        params.add(new Param("vars", "{\"code\":\"" + code + "\"}"));
        Request request = asyncHttpClient.preparePost(URL).setFormParams(params).build();

        ListenableFuture<String> response = asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) {
                log.info(to + "短信发送成功");
                return response.getResponseBody();
            }

            @Override
            public void onThrowable(Throwable t) {
                log.info(to + "短信发送失败", t);
                throw new SMSException(to + "短信发送失败", t);
            }
        });
        String result = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            result = response.get();
            smSdto = mapper.readValue(result,SMSdto.class);
        } catch (InterruptedException | ExecutionException | IOException e) {
            log.info(to + "短信发送失败"+e.getMessage());
            e.printStackTrace();
        }

        return smSdto;
    }
}
