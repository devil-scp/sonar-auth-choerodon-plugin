package org.sonarqube.auth.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.auth.dto.UserDTO;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.List;


/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  10:40 2019/5/7
 * Description:
 */
public class HttpConnectionPoolUtil {
    private static final Logger LOGGER = Loggers.get(HttpConnectionPoolUtil.class);
    private static final int CONNECT_TIMEOUT = 100000;
    private static final int SOCKET_TIMEOUT = 30000;
    private static final int MAX_CONN = 10;
    private static final int MAX_PRE_ROUTE = 10;
    private static final int MAX_ROUTE = 10;
    private static final String CHARSET = "UTF-8";
    private static CloseableHttpClient httpClient;
    private static PoolingHttpClientConnectionManager manager;
    private UserDTO user;

    private static CloseableHttpClient getHttpClient(String url) {
        String hostName = url.split("/")[2];
        LOGGER.info("hostName:{}", hostName);
        int port = 80;
        if (hostName.contains(":")) {
            String[] args = hostName.split(":");
            hostName = args[0];
            port = Integer.parseInt(args[1]);
        }
        if (httpClient == null) {
            httpClient = createHttpClient(hostName, port);
        }
        return httpClient;
    }

    /**
     * 根据host和port构建httpclient实例
     *
     * @param host 要访问的域名
     * @param port 要访问的端口
     * @return
     */
    private static CloseableHttpClient createHttpClient(String host, int port) {
        ConnectionSocketFactory plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", plainSocketFactory)
                .register("https", sslSocketFactory).build();
        manager = new PoolingHttpClientConnectionManager(registry);
        manager.setMaxTotal(MAX_CONN);
        manager.setDefaultMaxPerRoute(MAX_PRE_ROUTE);

        HttpHost httpHost = new HttpHost(host, port);
        manager.setMaxPerRoute(new HttpRoute(httpHost), MAX_ROUTE);

        HttpRequestRetryHandler handler = (e, i, httpContext) -> {
            if (i > 3) {
                //重试超过3次,放弃请求
                LOGGER.error("retry has more than 3 time, give up request");
                return false;
            }
            if (e instanceof NoHttpResponseException) {
                //服务器没有响应,可能是服务器断开了连接,应该重试
                LOGGER.error("receive no response from server, retry");
                return true;
            }
            if (e instanceof SSLHandshakeException) {
                // SSL握手异常
                LOGGER.error("SSL hand shake exception");
                return false;
            }
            if (e instanceof InterruptedIOException) {
                //超时
                LOGGER.error("InterruptedIOException");
                return false;
            }
            if (e instanceof UnknownHostException) {
                // 服务器不可达
                LOGGER.error("server host unknown");
                return false;
            }
            if (e instanceof ConnectTimeoutException) {
                // 连接超时
                LOGGER.error("Connection Time out");
                return false;
            }
            if (e instanceof SSLException) {
                LOGGER.error("SSLException");
                return false;
            }

            HttpClientContext context = HttpClientContext.adapt(httpContext);
            HttpRequest request = context.getRequest();
            if (!(request instanceof HttpEntityEnclosingRequest)) {
                //如果请求不是关闭连接的请求
                return true;
            }
            return false;
        };

        CloseableHttpClient client = HttpClients.custom().setConnectionManager(manager).setRetryHandler(handler).build();
        return client;
    }

    /**
     * 对http请求进行基本设置
     *
     * @param httpRequestBase http请求
     */
    private void setRequestConfig(HttpRequestBase httpRequestBase) {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT).build();

        httpRequestBase.setConfig(requestConfig);
    }

    /**
     * 关闭连接池
     */
    public void closeConnectionPool() {
        try {
            if (httpClient != null) {
                httpClient.close();
                httpClient = null;
            }
            if (manager != null) {
                manager.close();
                manager = null;
            }
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }

    /**
     * 关闭 inputStream和response
     *
     * @param in
     * @param response
     */
    private void closeResponse(InputStream in, CloseableHttpResponse response) {
        try {
            if (in != null) in.close();
            if (response != null) response.close();
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }

    /**
     * @param url
     * @param params
     * @return
     */
    public JsonObject doPost(String url, List<NameValuePair> params) {

        HttpPost httpPost = new HttpPost(url);
        String encoding = null;
        try {
            encoding = DatatypeConverter.printBase64Binary((this.user.getUserName() + ":" + this.user.getPassword()).getBytes(CHARSET));
            httpPost.setHeader("Authorization", "Basic " + encoding);
            setRequestConfig(httpPost);
            httpPost.setEntity(new UrlEncodedFormEntity(params, CHARSET));

        } catch (UnsupportedEncodingException e) {
            LOGGER.info(e.getMessage());
        }

        CloseableHttpResponse response = null;
        InputStream in = null;
        JsonObject object = null;
        try {
            response = getHttpClient(url).execute(httpPost, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                in = entity.getContent();
                String result = IOUtils.toString(in, CHARSET);
                Gson gson = new Gson();
                object = gson.fromJson(result, JsonObject.class);
            }
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        } finally {
            closeResponse(in, response);
        }
        return object;
    }

    public JsonObject doGet(String url, List<NameValuePair> pairs) {
        CloseableHttpResponse response = null;
        InputStream in = null;
        JsonObject object = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            builder.setParameters(pairs);
            HttpGet get = new HttpGet(builder.build());
            String encoding = DatatypeConverter.printBase64Binary((this.user.getUserName() + ":" + this.user.getPassword()).getBytes(CHARSET));
            get.setHeader("Authorization", "Basic " + encoding);
            response = getHttpClient(url).execute(get);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                in = entity.getContent();
                String result = IOUtils.toString(in, CHARSET);
                Gson gson = new Gson();
                object = gson.fromJson(result, JsonObject.class);
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        } finally {
            closeResponse(in, response);
        }
        return object;
    }

    public void setAdminUser(String url) {
        this.user = new UserDTO("admin", "admin");
//        CloseableHttpResponse response = null;
//        InputStream in = null;
//        try {
//            URIBuilder builder = new URIBuilder(url);
//            HttpGet get = new HttpGet(builder.build());
//            response = getHttpClient(url).execute(get);
//            HttpEntity entity = response.getEntity();
//            if (entity != null) {
//                in = entity.getContent();
//                String result = IOUtils.toString(in, CHARSET);
//                Gson gson = new Gson();
//                this.user = gson.fromJson(result, UserDTO.class);
//            }
//        } catch (Exception e) {
//            LOGGER.info(e.getMessage());
//        } finally {
//            closeResponse(in, response);
//        }
    }
}
