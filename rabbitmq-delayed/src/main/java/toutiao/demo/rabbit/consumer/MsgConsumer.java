package toutiao.demo.rabbit.consumer;

import cn.hutool.core.date.DateUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;
import toutiao.demo.rabbit.dto.MsgDemo;
import toutiao.demo.rabbit.product.MsgProduct;

import java.util.Date;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MsgConsumer {

    @RabbitListener(queuesToDeclare = @Queue(MsgProduct.DELAYED_QUEUE_NAME))
    public void receive(@Payload MsgDemo msgDemo, Message message, Channel channel) {
        log.info("调用消费者时间:{},MSG:{}¨", DateUtil.formatDateTime(new Date()),msgDemo);
    }
}
