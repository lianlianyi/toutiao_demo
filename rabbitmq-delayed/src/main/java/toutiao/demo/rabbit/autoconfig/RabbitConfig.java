package toutiao.demo.rabbit.autoconfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import toutiao.demo.rabbit.service.ConfirmCallbackService;
import toutiao.demo.rabbit.service.ReturnCallbackService;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {
    private final ConfirmCallbackService confirmCallbackService;
    private final ReturnCallbackService returnCallbackService;

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(factory);
        /**
         * 确保消息发送失败后可以重新返回到队列中
         * 注意：yml需要配置 publisher-returns: true
         * 当mandatory标志位设置为true时
         * 如果exchange根据自身类型和消息routingKey无法找到一个合适的queue存储消息
         * 那么broker会调用basic.return方法将消息返还给生产者
         * 当mandatory设置为false时，出现上述情况broker会直接将消息丢弃
         */
        rabbitTemplate.setMandatory(true);

        // 消费者确认收到消息后，手动ack回执回调处理
        rabbitTemplate.setConfirmCallback(confirmCallbackService);
        //消息投递到队列失败回调处理
        rabbitTemplate.setReturnsCallback(returnCallbackService);
        //使用单独的发送连接，避免生产者由于各种原因阻塞而导致消费者同样阻塞
        rabbitTemplate.setUsePublisherConnection(true);

        return rabbitTemplate;
    }
}
