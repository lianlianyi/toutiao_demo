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
     * 生产端确认
     */
    @Component
    public static class ConfirmCallbackService implements RabbitTemplate.ConfirmCallback {
        /**
         * @param correlationData 对象内部只有一个 id 属性，用来表示当前消息的唯一性。
         * @param ack             消息投递到broker 的状态，true表示成功。
         * @param cause           表示投递失败的原因。
         */
        @Override
        public void confirm(CorrelationData correlationData, boolean ack, String cause) {
            if (!ack) {
                Console.error("消息发送异常!");
            } else {
                Console.log("发送者爸爸已经收到确认，correlationData={} ,ack={}, cause={}", correlationData.getId(), ack, cause);
            }
        }
    }

    /**
     * ReturnCallback 退回模式
     * 如果消息未能投递到目标 queue 里将触发回调 returnCallback ，一旦向 queue 投递消息未成功，这里一般会记录下当前消息的详细投递数据，方便后续做重发或者补偿等操作。
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
            Console.log("触发回退 ===> replyCode={} ,replyText={} ,exchange={} ,routingKey={}", replyCode, replyText, exchange, routingKey);
        }
    }

    /**
     * 生产者
     */
    @Component
    @RequiredArgsConstructor
    public static class Sender {
        private final RabbitTemplate rabbitTemplate;
        private final ConfirmCallbackService confirmCallbackService;
        private final ReturnCallbackService returnCallbackService;

        public void sendMessage(Object msg) {

            /**
             * 确保消息发送失败后可以重新返回到队列中
             * 注意：yml需要配置 publisher-returns: true
             */
            rabbitTemplate.setMandatory(true);

            /**
             * 消费者确认收到消息后，手动ack回执回调处理
             */
            rabbitTemplate.setConfirmCallback(confirmCallbackService);

            /**
             * 消息投递到队列失败回调处理
             */
            rabbitTemplate.setReturnsCallback(returnCallbackService);

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
                //basicAck：表示成功确认，使用此回执方法后，消息会被rabbitmq broker 删除。
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                //basicNack ：表示失败确认，一般在消费消息业务异常时用到此方法，可以将消息重新投递入队列。
                //basicReject：拒绝消息，与basicNack区别在于不能进行批量操作，其他用法很相似。
            }  catch (Exception e) {
                if (message.getMessageProperties().getRedelivered()) {
                    Console.error("消息已重复处理失败,拒绝再次接收...");
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
                } else {
                    Console.error("消息即将再次返回队列处理...");
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                }
            }
        }
    }
}
