package toutiao.demo.rabbit.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cn.hutool.core.lang.Console;

/**
 * ReturnCallback 退回模式 如果消息未能投递到目标 queue 里将触发回调 returnCallback ，一旦向 queue 投递消息未成功，这里一般会记录下当前消息的详细投递数据，方便后续做重发或者补偿等操作。
 */
@Component
public class ReturnCallbackService implements RabbitTemplate.ReturnsCallback {
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        Message message = returnedMessage.getMessage();
        int replyCode = returnedMessage.getReplyCode();
        String replyText = returnedMessage.getReplyText();
        String exchange = returnedMessage.getExchange();
        String routingKey = returnedMessage.getRoutingKey();
        Console.log("触发回退 ===> replyCode={} ,replyText={} ,exchange={} ,routingKey={}", replyCode, replyText, exchange,
            routingKey);
    }
}