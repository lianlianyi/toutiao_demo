package toutiao.demo.rabbit.mq;

import cn.hutool.core.date.DateUtil;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * 延迟队列
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DirectQueue {
    private static final String DELAYED_EXCHANGE_NAME = "demo.delayed.exchange_toutiao_demo";
    private static final String DELAYED_QUEUE_NAME = "demo.delayed.queue_toutiao_demo";
    private static final String DELAYED_ROUTING_KEY = "demo.delayed.routing_key_toutiao_demo";
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(queuesToDeclare = @Queue(DELAYED_QUEUE_NAME))
    public void receive(@Payload MsgDemo msgDemo, Message message, Channel channel) {
        log.info("调用消费者时间:{},MSG:{}¨", DateUtil.formatDateTime(new Date()),msgDemo);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MsgDemo implements Serializable {
        private Integer code;
        private String msg;
    }

    //声明队列
    @Bean
    public org.springframework.amqp.core.Queue delayedQueue() {
        return new org.springframework.amqp.core.Queue(DELAYED_QUEUE_NAME);
    }

    //声明一个延迟交换机
    @Bean("delayedExchange")
    public CustomExchange delayExchange() {
        //自定义交换机类型
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        /**
         * 1.交换机名称
         * 2.交换机类型
         * 3.是否需要持久化
         * 4.是否需要自动删除
         * 5.其他参数
         */
        return new CustomExchange(DELAYED_EXCHANGE_NAME, "x-delayed-message", true, false, map);
    }

    //绑定队列和延迟交换机
    @Bean
    public Binding bingQueueToExchange(@Qualifier("delayedQueue") org.springframework.amqp.core.Queue queue, @Qualifier("delayedExchange") CustomExchange customExchange) {
        return BindingBuilder.bind(queue).to(customExchange).with(DELAYED_ROUTING_KEY).noargs();
    }

    public void sendMq(MsgDemo msg) {
        rabbitTemplate.convertAndSend(DELAYED_EXCHANGE_NAME, DELAYED_ROUTING_KEY, msg, correlationDate -> {
            //延迟3秒
            correlationDate.getMessageProperties().setDelay(1000 * 3);
            return correlationDate;
        });
        log.info("生产者发送时间:{},msg:{}", DateUtil.formatDateTime(new Date()),msg);
    }

}
