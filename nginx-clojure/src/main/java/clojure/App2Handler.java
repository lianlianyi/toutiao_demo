package clojure;

import cn.hutool.core.date.DateUtil;
import nginx.clojure.java.ArrayMap;
import nginx.clojure.java.NginxJavaRingHandler;

import java.util.Date;
import java.util.Map;

import static nginx.clojure.MiniConstants.CONTENT_TYPE;
import static nginx.clojure.MiniConstants.NGX_HTTP_OK;

public class App2Handler implements NginxJavaRingHandler {
    @Override
    public Object[] invoke(Map<String, Object> request) {
        return new Object[] {
                NGX_HTTP_OK, //http status 200
                ArrayMap.create(CONTENT_TYPE, "text/plain"), //headers map
                "App2 - " + DateUtil.formatDateTime(new Date())  //response body can be string, File or Array/Collection of them
        };
    }
}