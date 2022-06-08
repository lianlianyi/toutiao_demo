package toutiao.demo.rabbit.mq;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import com.rabbitmq.client.Channel;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import toutiao.demo.rabbit.service.ConfirmCallbackService;
import toutiao.demo.rabbit.service.ReturnCallbackService;

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

    @SneakyThrows
    @RabbitListener(queuesToDeclare = @Queue(DELAYED_QUEUE_NAME))
    public void receive(@Payload MsgDemo msgDemo, Message message, Channel channel) {
        try {
            Console.log("收到消息：{},时间:{}", msgDemo,DateUtil.formatDateTime(new Date()));
            //TODO 具体业务
            int a = 1 / 0;
            //basicAck：表示成功确认，使用此回执方法后，消息会被rabbitmq broker 删除。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }  catch (Exception e) {
            if (message.getMessageProperties().getRedelivered()) {
                Console.error("消息已重复处理失败,拒绝再次接收...");
                //basicReject：拒绝消息，与basicNack区别在于不能进行批量操作，其他用法很相似。
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                Console.error("消息即将再次返回队列处理...");
                //basicNack ：表示失败确认，一般在消费消息业务异常时用到此方法，可以将消息重新投递入队列。
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
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

    public void sendMq(MsgDemo msg,DateUnit unit,int timeout) {
        rabbitTemplate.convertAndSend(DELAYED_EXCHANGE_NAME, DELAYED_ROUTING_KEY, msg, correlationDate -> {
            //延迟3秒
            correlationDate.getMessageProperties().setDelay((int) (unit.getMillis() * timeout));
            return correlationDate;
        });
        log.info("生产者发送时间:{},msg:{}", DateUtil.formatDateTime(new Date()),msg);
    }

}
