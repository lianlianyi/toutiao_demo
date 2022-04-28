package clojure;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import nginx.clojure.NativeInputStream;
import nginx.clojure.NginxClojureRT;
import nginx.clojure.java.NginxJavaRequest;
import nginx.clojure.java.NginxJavaRingHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static nginx.clojure.java.Constants.PHASE_DONE;

public class BalancingHandler implements NginxJavaRingHandler {
    public static final String KEY_BODY = "body";
    public static final String KEY_REQUEST_METHOD = "request-method";
    public static final String KEY_QUERY_STRING = "query-string";

    private static final WeightRandom<String> weightApp = RandomUtil.weightRandom(
            CollUtil.newArrayList(
                    new WeightRandom.WeightObj<>("http://127.0.0.1:8080/app1", 0.5),
                    new WeightRandom.WeightObj<>("http://127.0.0.1:8080/app2", 0.5)
            )
    );

    @Override
    public Object[] invoke(Map<String, Object> req) throws IOException {
        String myhost = computeMyHost(req);
        NginxClojureRT.log.info("访问:" + myhost);
        // 用setVariable方法设置myhost变量的值，这个myhost在这个location中被定义，跳转的时候就用这个值作为path
        ((NginxJavaRequest) req).setVariable("myhost", myhost);
        // 返回PHASE_DONE之后，nginx-clojure框架就会执行proxy_pass逻辑，
        // 如果返回的不是PHONE_DONE，nginx-clojure框架就把这个handler当做content handler处理
        return PHASE_DONE;
    }

    /**
     * 这里写入业务逻辑，根据实际情况确定返回的path
     *
     * @param req
     * @return
     */
    private String computeMyHost(Map<String, Object> req) {
        // 确认是http还是https
        String scheme = (String) req.get("scheme");
        // 确认端口号
        String serverPort = (String) req.get("server-port");

        return weightApp.next();
    }

    /**
     * 获取请求参数, 包括GET和POST
     *
     * @param requestMap
     * @return
     */
    private Map getParams(Map requestMap) {
        Map params = new HashMap<>();
        try {
            params.putAll(getGetParams(requestMap));
            params.putAll(getPostParams(requestMap));
        } catch (Exception e) {
            NginxClojureRT.log.error("获取请求参数失败", e);
        }
        return params;
    }

    /**
     * 获取GET请求参数
     *
     * @param requestMap
     * @return
     */
    private Map getGetParams(Map requestMap) {
        String queryString = MapUtil.getStr(requestMap, KEY_QUERY_STRING);
        return buildQuerys(queryString);
    }

    /**
     * 获取POST请求参数
     *
     * @param requestMap
     * @return
     */
    private Map getPostParams(Map requestMap) throws IOException {
        String requestMethod = MapUtil.getStr(requestMap, KEY_REQUEST_METHOD);
        if (StrUtil.equalsIgnoreCase(requestMethod, "POST")) { // 如果是POST请求,那么处理下POST参数
            Object bodyObj = requestMap.get(KEY_BODY);
            if (bodyObj != null) {
                NativeInputStream nis = (NativeInputStream) bodyObj;
                String body = IoUtil.read(nis, StandardCharsets.UTF_8);
                return buildQuerys(body);
            }
        }
        return new HashMap<>();
    }

    /**
     * 将字符串格式的参数转换成Map
     *
     * @param queryString
     * @return
     */
    private Map buildQuerys(String queryString) {
        Map params = new HashMap<>();
        if (StrUtil.isBlank(queryString)) {
            return params;
        }
        String[] kvs = queryString.split("&");
        if (kvs != null) {
            for (String kv : kvs) {
                String[] pair = kv.split("\\=", 2);
                if (pair.length == 2) {
                    params.put(pair[0], URLUtil.decode(pair[1], StandardCharsets.UTF_8.toString()));
                }
            }
        }
        return params;
    }
}