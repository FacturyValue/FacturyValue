package org.cboard.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http请求工具类
 * @author
 * @since 2016年9月9日 上午10:20:54
 */
@SuppressWarnings("deprecation")
public class HttpUtils {

	private static RequestConfig requestConfig;
	private static final int MAX_TIMEOUT = 20000;

	private static final  String  HTTPS = "https";
	
	private static SSLConnectionSocketFactory socketFactory = null;
	
	static {  
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        // 设置连接超时  
	    configBuilder.setConnectTimeout(MAX_TIMEOUT);  
	    // 设置读取超时  
	    configBuilder.setSocketTimeout(MAX_TIMEOUT);  
	    requestConfig = configBuilder.build();  
	    
	    try {  
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
            	public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            socketFactory = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					 return true;
				}

				@Override
				public void verify(String host, SSLSocket ssl)throws IOException {
				}

				@Override
				public void verify(String host, X509Certificate cert)throws SSLException {
				}

				@Override
				public void verify(String host, String[] cns,String[] subjectAlts) throws SSLException {
				}  
            });  
        } catch (GeneralSecurityException e) {  
            e.printStackTrace();  
        }
	}
	
	private static CloseableHttpClient getClient(URI uri) {
		CloseableHttpClient client;
		if (HTTPS.equals(uri.getScheme())) {
			client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		}else {
			client = HttpClients.custom().build();
		}
		return client;
	}
	
	public static String doGet(String url){
		String result = null;
		try {
			URI uri = new URI(url);
			CloseableHttpClient client = getClient(uri);
			HttpGet get = new HttpGet(uri);
	        HttpResponse response = client.execute(get);
	        result = EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (Exception e) {  
	        e.printStackTrace();
	    }
		return result;
	}
	
	public static String doFormPost(String url, Map<String,String> params){
		return doFormPost(url,params,null);
	}
	
	public static String doFormPost(String url, Map<String,String> params, Map<String, String> headers){
		String result = null;
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return result;
		}
		CloseableHttpClient client = getClient(uri);
		HttpPost post = new HttpPost(uri);
		CloseableHttpResponse response = null;
		try {  
			//配置请求的超时设置
		    post.setConfig(requestConfig);
		    //设置请求头信息
		    if(headers != null){
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
	        }
		    //设置请求体信息
		    List<NameValuePair> parameters = new ArrayList <NameValuePair>();
		    if(params != null){
				for (Map.Entry<String, String> entry : params.entrySet()) {
					parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
		    }
	        //创建UrlEncodedFormEntity对象
	        post.setEntity(new UrlEncodedFormEntity(parameters,"utf-8"));
	        response = client.execute(post);
	        result = EntityUtils.toString(response.getEntity(),"utf-8");
	    } catch (Exception e) {  
	        e.printStackTrace();
	    } finally {
	    	if (response != null) {  
                try {  
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }
	    }
		return result;
	}
	
	public static String doJsonPost(String url, JSONObject json){
		return doJsonPost(url,json,null);
	}

	public static String doJsonPost(String url, JSONObject json, Map<String, String> headers){
		String result = null;
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return result;
		}
		CloseableHttpClient client = getClient(uri);
		HttpPost post = new HttpPost(uri);
		CloseableHttpResponse response = null;
		try {  
			//配置请求的超时设置
			post.setConfig(requestConfig);
			 //设置请求头信息
		    if(headers != null){
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
	        }
		    //设置请求体信息
			StringEntity s = new StringEntity(json.toJSONString(),"utf-8");
            s.setContentType("application/json");
			post.setEntity(s);
			response = client.execute(post);
			result = EntityUtils.toString(response.getEntity(),"utf-8");
		} catch (Exception e) {  
			e.printStackTrace();
		} finally {
			if (response != null) {  
                try {  
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }
		}
		return result;
	}

	public static byte[] doJsonPostBytes(String url, JSONObject json){
		return doJsonPostBytes(url,json,null);
	}
	
	public static byte[] doJsonPostBytes(String url, JSONObject json, Map<String, String> headers){
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		CloseableHttpClient client = getClient(uri);
		HttpPost post = new HttpPost(uri);
		CloseableHttpResponse response;
		try {
			//配置请求的超时设置
			post.setConfig(requestConfig);
			//设置请求头信息
			if(headers != null){
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					post.addHeader(entry.getKey(), entry.getValue());
				}
			}
			//设置请求体信息
			StringEntity s = new StringEntity(json.toJSONString(),"utf-8");
			s.setContentType("application/json");
			post.setEntity(s);
			response = client.execute(post);
			try {
				InputStream in = response.getEntity().getContent();
				return IOUtils.toByteArray(in);
			} finally {
				response.close(); 
			}
		} catch (Exception e) {  
			e.printStackTrace();
			return null;
		} finally {
			// 关闭连接,释放资源   
	    	try {
	    		client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
