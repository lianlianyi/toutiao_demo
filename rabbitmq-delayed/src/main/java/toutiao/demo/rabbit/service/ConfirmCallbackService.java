package toutiao.demo.rabbit.service;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 生产端确认
 */
@Slf4j
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
            log.error("消息发送异常!");
            return;
        }
        log.info("消息投递成功，correlationData={} ,ack={}, cause={}", correlationData.getId(), ack, cause);
        // todo 进行消息记录的数据库更新
    }
}