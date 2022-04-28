package toutiao.demo.rabbit.product;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import toutiao.demo.rabbit.dto.MsgDemo;

import java.util.Date;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MsgProduct {
    private final RabbitTemplate rabbitTemplate;

    public static final String DELAYED_EXCHANGE_NAME = "delayed.exchange_toutiao_demo";
    public static final String DELAYED_QUEUE_NAME = "delayed.queue_toutiao_demo";
    public static final String DELAYED_ROUTING_KEY = "delayed.routing_key_toutiao_demo";

    public void sendMq(MsgDemo msg) {
        rabbitTemplate.convertAndSend(DELAYED_EXCHANGE_NAME, DELAYED_ROUTING_KEY, msg, correlationDate -> {
            //延迟3秒
            correlationDate.getMessageProperties().setDelay(1000 * 3);
            return correlationDate;
        });
        log.info("生产者发送时间:{},msg:{}", DateUtil.formatDateTime(new Date()),msg);
    }
}