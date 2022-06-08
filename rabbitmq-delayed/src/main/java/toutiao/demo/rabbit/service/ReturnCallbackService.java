package toutiao.demo.rabbit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cn.hutool.core.lang.Console;
import toutiao.demo.rabbit.autoconfig.RabbitConfig;

/**
 * ReturnCallback 退回模式 如果消息未能投递到目标 queue 里将触发回调 returnCallback ，一旦向 queue 投递消息未成功，这里一般会记录下当前消息的详细投递数据，方便后续做重发或者补偿等操作。
 *
 *
 * 延迟问题描述：
 * 使用了x-delayed-message 延迟插件，结果每次都强制触发returnedMessage回调方法？？？？
 * 解决方案
 * 如果配置了发送回调ReturnCallback，插件延迟队列则会回调该方法，因为发送方确实没有投递到队列上，只是在交换器上暂存，等过期时间到了 才会发往队列。
 * 并非是BUG，而是有原因的，建议利用if 去拦截这个异常，判断延迟队列交换机名称，然后break;
 */
@Slf4j
@Component
public class ReturnCallbackService implements RabbitTemplate.ReturnsCallback {
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        Message message = returnedMessage.getMessage();
        int replyCode = returnedMessage.getReplyCode();
        String replyText = returnedMessage.getReplyText();
        String exchange = returnedMessage.getExchange();
        String routingKey = returnedMessage.getRoutingKey();
        //请注意!如果你使用了延迟队列插件，那么一定会调用该callback方法，因为数据并没有提交上去，而是提交在交换器中，过期时间到了才提交上去，并非是bug！你可以用if进行判断交换机名称来捕捉该报错
        // 需要提前约定好延迟队列交换机前缀,跳过
        if(exchange.startsWith("delayed.")){
            return;
        }
        log.info("触发回退 ===> replyCode={} ,replyText={} ,exchange={} ,routingKey={}", replyCode, replyText, exchange,
            routingKey);
    }
}