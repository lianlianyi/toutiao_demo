package toutiao.demo.rabbit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MsgDemo implements Serializable {
    private Integer code;
    private String msg;
}
