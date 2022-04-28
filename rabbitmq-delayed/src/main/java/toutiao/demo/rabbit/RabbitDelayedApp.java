package toutiao.demo.rabbit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import toutiao.demo.rabbit.dto.MsgDemo;
import toutiao.demo.rabbit.product.MsgProduct;

import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@SpringBootApplication
public class RabbitDelayedApp implements ApplicationRunner {
    private final MsgProduct msgProduct;

    public static void main(String[] args) {
        SpringApplication.run(RabbitDelayedApp.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        MsgDemo demo = new MsgDemo(1, "头条测试延迟队列demo");
        msgProduct.sendMq(demo);
    }

    //声明队列
    @Bean
    public Queue delayedQueue() {
        return new Queue(MsgProduct.DELAYED_QUEUE_NAME);
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
        return new CustomExchange(MsgProduct.DELAYED_EXCHANGE_NAME, "x-delayed-message", true, false, map);
    }

    //绑定队列和延迟交换机
    @Bean
    public Binding bingQueueToExchange(@Qualifier("delayedQueue") Queue queue, @Qualifier("delayedExchange") CustomExchange customExchange) {
        return BindingBuilder.bind(queue).to(customExchange).with(MsgProduct.DELAYED_ROUTING_KEY).noargs();
    }
}
