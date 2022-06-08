package toutiao.demo.rabbit.mq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import toutiao.demo.rabbit.service.ConfirmCallbackService;
import toutiao.demo.rabbit.service.ReturnCallbackService;

import java.io.IOException;
import java.util.Date;

/**
 *
 */
@Configuration
public class ConfirmQueue {
    @Bean(name = "confirmTestQueue")
    public Queue confirmTestQueue() {
        return new Queue("confirm_test_queue", true, false, false);
    }

    @Bean(name = "confirmTestExchange")
    public FanoutExchange confirmTestExchange() {
        return new FanoutExchange("confirmTestExchange");
    }

    @Bean
    public Binding confirmTestFanoutExchangeAndQueue(@Qualifier("confirmTestExchange") FanoutExchange confirmTestExchange, @Qualifier("confirmTestQueue") Queue confirmTestQueue) {
        return BindingBuilder.bind(confirmTestQueue).to(confirmTestExchange);
    }

    /**
     * 生产者
     */
    @Component
    @RequiredArgsConstructor
    public static class Sender {
        private final RabbitTemplate rabbitTemplate;

        public void sendMessage(Object msg) {
            /**
             * 发送消息
             */
            rabbitTemplate.convertAndSend("confirmTestExchange", "confirm_test_queue", msg, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                Console.log("发送mq:{},时间:{}",msg, DateUtil.formatDateTime(new Date()));
                return message;
            }, new CorrelationData(IdUtil.fastUUID()));
        }
    }

    @Component
    @RabbitListener(queues = "confirm_test_queue")
    public static class ReceiverMessage1 {
        @RabbitHandler
        public void processHandler(String msg, Channel channel, Message message) throws IOException {
            try {
                Console.log("收到消息：{}", msg);
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
    }
}
