package toutiao.demo.rabbit.service;

import cn.hutool.core.lang.Console;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 生产端确认
 */
@Component
public class ConfirmCallbackService implements RabbitTemplate.ConfirmCallback {
    /**
     * @param correlationData 对象内部只有一个 id 属性，用来表示当前消息的唯一性。
     * @param ack 消息投递到broker 的状态，true表示成功。
     * @param cause 表示投递失败的原因。
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            Console.error("消息发送异常!");
        } else {
            // warn 延迟队列返回null
            Console.log("发送者爸爸已经收到确认，correlationData={} ,ack={}, cause={}", correlationData, ack, cause);
        }
    }
}