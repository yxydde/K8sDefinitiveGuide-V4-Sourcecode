package com.yangxy.k8sweb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

@Controller
@SpringBootApplication
public class K8sWebApplication {

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 默认页<br/>
     *
     * @RequestMapping("/") 和 @RequestMapping 是有区别的
     * 如果不写参数，则为全局默认页，加入输入404页面，也会自动访问到这个页面。
     * 如果加了参数“/”，则只认为是根页面。
     * 可以通过localhost:8080或者localhost:8080/index访问该方法
     */
    @RequestMapping(value = {"/", "/index"})
    public ModelAndView index(HttpServletRequest request) {
        Map<String, String> map = System.getenv();
        StringBuilder sysenv = new StringBuilder();
        for (String key : map.keySet()) {
            sysenv.append(key).append(":").append(map.get(key)).append("<br>");
        }

        StringBuilder prop = new StringBuilder();
        Properties sysProperties = System.getProperties();
        for (Object key : sysProperties.keySet()) {
            prop.append(key).append(":").append(sysProperties.get(key)).append("<br>");
        }
        ModelAndView model = new ModelAndView();
        model.addObject("app_version", System.getenv("APP_VERSION"));
        model.addObject("server_ip", getNetworkInterfaces());
        model.addObject("request", request.getRemoteHost() + ":" + request.getRemotePort());
        model.addObject("sysenv", sysenv.toString());
        model.addObject("sysProperties", prop.toString());
        model.addObject("time", dateFormat.format(new Date()));
        model.setViewName("/index.html");
        return model;
    }


    @RequestMapping("/redis")
    public ModelAndView redis(HttpServletRequest request) {
        ModelAndView model = new ModelAndView();
        model.addObject("app_version", System.getenv("APP_VERSION"));
        Object redis_info = redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                StringBuilder result = new StringBuilder();
                Properties redis_info = redisConnection.info();
                for (Object key : redis_info.keySet()) {
                    result.append(key).append(":").append(redis_info.get(key)).append("<br>");
                }
                return result;
            }
        });
        model.setViewName("/redis.html");
        model.addObject("redis_info", redis_info);
        return model;
    }

    private String getNetworkInterfaces() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> enumInetAddr = ni.getInetAddresses();
                while (enumInetAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
                            && inetAddress.isSiteLocalAddress()) {
                        sb.append(ni.getDisplayName()).append("  :  ");
                        sb.append(inetAddress.getHostAddress()).append("<br>");

                    }
                }
            }
        } catch (SocketException e) {
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SpringApplication.run(K8sWebApplication.class, args);
    }

}
