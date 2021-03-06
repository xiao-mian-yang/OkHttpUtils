package com.lzy.okhttputils.request;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.cache.CacheEntity;
import com.lzy.okhttputils.cache.CacheManager;
import com.lzy.okhttputils.cache.CacheMode;
import com.lzy.okhttputils.callback.AbsCallback;
import com.lzy.okhttputils.exception.OkHttpException;
import com.lzy.okhttputils.https.HttpsUtils;
import com.lzy.okhttputils.model.HttpHeaders;
import com.lzy.okhttputils.model.HttpParams;
import com.lzy.okhttputils.utils.HeaderParser;
import com.lzy.okhttputils.utils.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ================================================
 * 作    者：廖子尧
 * 版    本：1.0
 * 创建日期：2016/1/12
 * 描    述：所有请求的基类，其中泛型 R 主要用于属性设置方法后，返回对应的子类型，以便于实现链式调用
 * 修订历史：
 * ================================================
 */
public abstract class BaseRequest<R extends BaseRequest> {

    protected String url;
    protected String baseUrl;
    protected Object tag;
    protected long readTimeOut;
    protected long writeTimeOut;
    protected long connectTimeout;
    protected CacheMode cacheMode;
    protected String cacheKey;
    protected long cacheTime = CacheEntity.CACHE_NEVER_EXPIRE;      //默认缓存的超时时间
    private HttpsUtils.SSLParams sslParams;
    protected HostnameVerifier hostnameVerifier;
    protected HttpParams params = new HttpParams();                 //添加的param
    protected HttpHeaders headers = new HttpHeaders();              //添加的header
    protected List<Interceptor> interceptors = new ArrayList<>();   //额外的拦截器
    protected List<Cookie> userCookies = new ArrayList<>();         //用户手动添加的Cookie

    private AbsCallback mCallback;
    private HttpUrl httpUrl;
    private Request mRequest;

    public BaseRequest(String url) {
        this.url = url;
        baseUrl = url;
        httpUrl = HttpUrl.parse(url);
        OkHttpUtils okHttpUtils = OkHttpUtils.getInstance();
        //默认添加 Accept-Language
        String acceptLanguage = HttpHeaders.getAcceptLanguage();
        if (!TextUtils.isEmpty(acceptLanguage)) headers(HttpHeaders.HEAD_KEY_ACCEPT_LANGUAGE, acceptLanguage);
        //默认添加 User-Agent
        String userAgent = HttpHeaders.getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) headers(HttpHeaders.HEAD_KEY_USER_AGENT, userAgent);
        //添加公共请求参数
        if (okHttpUtils.getCommonParams() != null) params.put(okHttpUtils.getCommonParams());
        if (okHttpUtils.getCommonHeaders() != null) headers.put(okHttpUtils.getCommonHeaders());
        //添加缓存模式
        if (okHttpUtils.getCacheMode() != null) cacheMode = okHttpUtils.getCacheMode();
        cacheTime = okHttpUtils.getCacheTime();
    }

    @SuppressWarnings("unchecked")
    public R url(@NonNull String url) {
        this.url = url;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R tag(Object tag) {
        this.tag = tag;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R readTimeOut(long readTimeOut) {
        this.readTimeOut = readTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R writeTimeOut(long writeTimeOut) {
        this.writeTimeOut = writeTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R connTimeOut(long connTimeOut) {
        this.connectTimeout = connTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R cacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R cacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return (R) this;
    }

    /** 传入 -1 表示永久有效,默认值即为 -1 */
    @SuppressWarnings("unchecked")
    public R cacheTime(long cacheTime) {
        if (cacheTime <= -1) cacheTime = CacheEntity.CACHE_NEVER_EXPIRE;
        this.cacheTime = cacheTime;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCertificates(InputStream... certificates) {
        sslParams = HttpsUtils.getSslSocketFactory(null, null, certificates);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCertificates(InputStream bksFile, String password, InputStream... certificates) {
        sslParams = HttpsUtils.getSslSocketFactory(bksFile, password, certificates);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R headers(HttpHeaders headers) {
        this.headers.put(headers);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R headers(String key, String value) {
        headers.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeHeader(String key) {
        headers.remove(key);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeAllHeaders() {
        headers.clear();
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(HttpParams params) {
        this.params.put(params);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(Map<String, String> params) {
        params(params, HttpParams.IS_REPLACE);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(Map<String, String> params, boolean isReplace) {
        this.params.put(params, isReplace);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, String value) {
        params(key, value, HttpParams.IS_REPLACE);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, String value, boolean isReplace) {
        params.put(key, value, isReplace);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addUrlParams(String key, List<String> values) {
        params.putUrlParams(key, values);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeParam(String key) {
        params.remove(key);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeAllParams() {
        params.clear();
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookie(@NonNull String name, @NonNull String value) {
        Cookie.Builder builder = new Cookie.Builder();
        Cookie cookie = builder.name(name).value(value).domain(httpUrl.host()).build();
        userCookies.add(cookie);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookie(@NonNull Cookie cookie) {
        userCookies.add(cookie);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookies(@NonNull List<Cookie> cookies) {
        userCookies.addAll(cookies);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCallback(AbsCallback callback) {
        this.mCallback = callback;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return (R) this;
    }

    /** 默认返回第一个参数 */
    public String getUrlParam(String key) {
        List<String> values = params.urlParamsMap.get(key);
        if (values != null && values.size() > 0) return values.get(0);
        return null;
    }

    /** 默认返回第一个参数 */
    public HttpParams.FileWrapper getFileParam(String key) {
        List<HttpParams.FileWrapper> values = params.fileParamsMap.get(key);
        if (values != null && values.size() > 0) return values.get(0);
        return null;
    }

    public HttpParams getParams() {
        return params;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Object getTag() {
        return tag;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public Request getRequest() {
        return mRequest;
    }

    /**
     * 返回当前的请求方法
     * GET,POST,HEAD,PUT,DELETE,OPTIONS
     */
    public String getMethod() {
        return mRequest.method();
    }

    /** 根据不同的请求方式和参数，生成不同的RequestBody */
    protected abstract RequestBody generateRequestBody();

    /** 对请求body进行包装，用于回调上传进度 */
    protected RequestBody wrapRequestBody(RequestBody requestBody) {
        ProgressRequestBody progressRequestBody = new ProgressRequestBody(requestBody);
        progressRequestBody.setListener(new ProgressRequestBody.Listener() {
            @Override
            public void onRequestProgress(final long bytesWritten, final long contentLength, final long networkSpeed) {
                OkHttpUtils.getInstance().getDelivery().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) mCallback.upProgress(bytesWritten, contentLength, bytesWritten * 1.0f / contentLength, networkSpeed);
                    }
                });
            }
        });
        return progressRequestBody;
    }

    /** 根据不同的请求方式，将RequestBody转换成Request对象 */
    protected abstract Request generateRequest(RequestBody requestBody);

    /** 根据当前的请求参数，生成对应的 Call 任务 */
    protected Call generateCall(Request request) {
        mRequest = request;
        if (readTimeOut <= 0 && writeTimeOut <= 0 && connectTimeout <= 0 && sslParams == null && userCookies.size() == 0) {
            return OkHttpUtils.getInstance().getOkHttpClient().newCall(request);
        } else {
            OkHttpClient.Builder newClientBuilder = OkHttpUtils.getInstance().getOkHttpClient().newBuilder();
            if (readTimeOut > 0) newClientBuilder.readTimeout(readTimeOut, TimeUnit.MILLISECONDS);
            if (writeTimeOut > 0) newClientBuilder.writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS);
            if (connectTimeout > 0) newClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            if (hostnameVerifier != null) newClientBuilder.hostnameVerifier(hostnameVerifier);
            if (sslParams != null) newClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
            if (userCookies.size() > 0) OkHttpUtils.getInstance().getCookieJar().addCookies(userCookies);
            if (interceptors.size() > 0) {
                for (Interceptor interceptor : interceptors) {
                    newClientBuilder.addInterceptor(interceptor);
                }
            }
            return newClientBuilder.build().newCall(request);
        }
    }

    /** 获取同步call对象 */
    public Call getCall() {
        //添加缓存头和其他的公共头，同步请求不做缓存，缓存为空
        HeaderParser.addCacheHeaders(this, null, null);
        //构建请求体，返回call对象
        RequestBody requestBody = generateRequestBody();
        mRequest = generateRequest(wrapRequestBody(requestBody));
        return generateCall(mRequest);
    }

    /** 阻塞方法，同步请求执行 */
    public Response execute() throws IOException {
        return getCall().execute();
    }

    /** 非阻塞方法，异步请求，但是回调在子线程中执行 */
    @SuppressWarnings("unchecked")
    public <T> void execute(AbsCallback<T> callback) {
        mCallback = callback;
        if (mCallback == null) mCallback = AbsCallback.CALLBACK_DEFAULT;

        //请求执行前UI线程调用
        mCallback.onBefore(this);

        //请求之前获取缓存信息，添加缓存头和其他的公共头
        if (cacheKey == null) cacheKey = HttpUtils.createUrlFromParams(baseUrl, params.urlParamsMap);
        if (cacheMode == null) cacheMode = CacheMode.DEFAULT;
        //无缓存模式,不需要进入缓存逻辑
        CacheEntity<T> cacheEntity = null;
        if (cacheMode != CacheMode.NO_CACHE) {
            cacheEntity = (CacheEntity<T>) CacheManager.INSTANCE.get(cacheKey);
            //检查缓存的有效时间,判断缓存是否已经过期
            if (cacheEntity != null && cacheEntity.checkExpire(cacheMode, cacheTime, System.currentTimeMillis())) {
                cacheEntity.setExpire(true);
            }
            HeaderParser.addCacheHeaders(this, cacheEntity, cacheMode);
        }
        //构建请求
        RequestBody requestBody = generateRequestBody();
        Request request = generateRequest(wrapRequestBody(requestBody));
        Call call = generateCall(request);

        if (cacheMode == CacheMode.IF_NONE_CACHE_REQUEST) {
            //如果没有缓存，或者缓存过期,就请求网络，否者直接使用缓存
            if (cacheEntity != null && !cacheEntity.isExpire()) {
                T data = cacheEntity.getData();
                HttpHeaders headers = cacheEntity.getResponseHeaders();
                if (data == null || headers == null) {
                    //由于没有序列化等原因,可能导致数据为空
                    sendFailResultCallback(true, call, null, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), mCallback);
                } else {
                    sendSuccessResultCallback(true, data, call, null, mCallback);
                    return;//获取缓存成功,不请求网络
                }
            } else {
                sendFailResultCallback(true, call, null, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), mCallback);
            }
        } else if (cacheMode == CacheMode.FIRST_CACHE_THEN_REQUEST) {
            //先使用缓存，不管是否存在，仍然请求网络
            if (cacheEntity != null && !cacheEntity.isExpire()) {
                T data = cacheEntity.getData();
                HttpHeaders headers = cacheEntity.getResponseHeaders();
                if (data == null || headers == null) {
                    //由于没有序列化等原因,可能导致数据为空
                    sendFailResultCallback(true, call, null, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), mCallback);
                } else {
                    sendSuccessResultCallback(true, data, call, null, mCallback);
                }
            } else {
                sendFailResultCallback(true, call, null, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), mCallback);
            }
        }

        final CacheEntity<T> finalCacheEntity = cacheEntity;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mCallback.parseNetworkFail(call, e);
                //请求失败，一般为url地址错误，网络错误等,并且过滤用户主动取消的网络请求
                if (!"Canceled".equals(e.getMessage())) {
                    sendFailResultCallback(false, call, null, e, mCallback);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int responseCode = response.code();
                //304缓存数据
                if (responseCode == 304 && cacheMode == CacheMode.DEFAULT) {
                    if (finalCacheEntity == null) {
                        sendFailResultCallback(true, call, response, OkHttpException.INSTANCE("服务器响应码304，但是客户端没有缓存！"), mCallback);
                    } else {
                        T data = finalCacheEntity.getData();
                        HttpHeaders headers = finalCacheEntity.getResponseHeaders();
                        if (data == null || headers == null) {
                            //由于没有序列化等原因,可能导致数据为空
                            sendFailResultCallback(true, call, response, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), mCallback);
                        } else {
                            sendSuccessResultCallback(true, data, call, response, mCallback);
                        }
                    }
                    return;
                }
                //响应失败，一般为服务器内部错误，或者找不到页面等
                if (responseCode == 404 || responseCode >= 500) {
                    sendFailResultCallback(false, call, response, OkHttpException.INSTANCE("服务器数据异常!"), mCallback);
                    return;
                }

                try {
                    T data = (T) mCallback.parseNetworkResponse(response);
                    sendSuccessResultCallback(false, data, call, response, mCallback);
                    //网络请求成功，保存缓存数据
                    handleCache(response.headers(), data);
                } catch (Exception e) {
                    //一般为服务器响应成功，但是数据解析错误
                    sendFailResultCallback(false, call, response, e, mCallback);
                }
            }
        });
    }

    /**
     * 请求成功后根据缓存模式，更新缓存数据
     *
     * @param headers 响应头
     * @param data    响应数据
     */
    @SuppressWarnings("unchecked")
    private <T> void handleCache(Headers headers, T data) {
        if (cacheMode == CacheMode.NO_CACHE) return;    //不需要缓存,直接返回
        if (data instanceof Bitmap) return;             //Bitmap没有实现Serializable,不能缓存

        CacheEntity<T> cache = HeaderParser.createCacheEntity(headers, data, cacheMode, cacheKey);
        if (cache == null) {
            //服务器不需要缓存，移除本地缓存
            CacheManager.INSTANCE.remove(cacheKey);
        } else {
            //缓存命中，更新缓存
            CacheManager.INSTANCE.replace(cacheKey, (CacheEntity<Object>) cache);
        }
    }

    /** 失败回调，发送到主线程 */
    @SuppressWarnings("unchecked")
    private <T> void sendFailResultCallback(final boolean isFromCache, final Call call, final Response response, final Exception e, final AbsCallback<T> callback) {
        OkHttpUtils.getInstance().getDelivery().post(new Runnable() {
            @Override
            public void run() {
                if (isFromCache) {
                    callback.onCacheError(call, e);           //缓存失败回调 （UI线程）
                    if (cacheMode == CacheMode.DEFAULT || cacheMode == CacheMode.REQUEST_FAILED_READ_CACHE) {
                        callback.onAfter(null, e);      //请求结束回调 （UI线程）
                    }
                } else {
                    callback.onError(call, response, e);                //请求失败回调 （UI线程）
                    if (cacheMode != CacheMode.REQUEST_FAILED_READ_CACHE) {
                        callback.onAfter(null, e);      //请求结束回调 （UI线程）
                    }
                }
            }
        });

        //不同的缓存模式，可能会导致该失败进入两次，一次缓存失败，一次网络请求失败
        if (!isFromCache && cacheMode == CacheMode.REQUEST_FAILED_READ_CACHE) {
            CacheEntity<T> cacheEntity = (CacheEntity<T>) CacheManager.INSTANCE.get(cacheKey);
            if (cacheEntity != null && !cacheEntity.isExpire()) {
                T data = cacheEntity.getData();
                HttpHeaders headers = cacheEntity.getResponseHeaders();
                if (data == null || headers == null) {
                    //由于没有序列化等原因,可能导致数据为空
                    sendFailResultCallback(true, call, response, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), callback);
                } else {
                    sendSuccessResultCallback(true, data, call, response, callback);
                }
            } else {
                sendFailResultCallback(true, call, response, OkHttpException.INSTANCE("没有获取到缓存,或者缓存已经过期!"), callback);
            }
        }
    }

    /** 成功回调，发送到主线程 */
    private <T> void sendSuccessResultCallback(final boolean isFromCache, final T t, final Call call, final Response response, final AbsCallback<T> callback) {
        OkHttpUtils.getInstance().getDelivery().post(new Runnable() {
            @Override
            public void run() {
                if (isFromCache) {
                    callback.onCacheSuccess(t, call);   //缓存成功回调 （UI线程）
                    if (cacheMode == CacheMode.DEFAULT || cacheMode == CacheMode.REQUEST_FAILED_READ_CACHE || cacheMode == CacheMode.IF_NONE_CACHE_REQUEST) {
                        callback.onAfter(t, null);          //请求结束回调 （UI线程）
                    }
                } else {
                    callback.onSuccess(t, call, response);        //请求成功回调 （UI线程）
                    callback.onAfter(t, null);              //请求结束回调 （UI线程）
                }
            }
        });
    }
}