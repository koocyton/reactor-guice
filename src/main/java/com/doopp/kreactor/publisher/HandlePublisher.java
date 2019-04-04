package com.doopp.kreactor.publisher;


import com.doopp.kreactor.annotation.RequestAttributeParam;
import com.doopp.kreactor.annotation.UploadFilesParam;
import com.doopp.kreactor.common.JsonResponse;
import com.doopp.kreactor.common.KReactorException;
import com.doopp.kreactor.common.ModelMap;
import com.doopp.kreactor.common.RequestAttribute;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import freemarker.template.*;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.*;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class HandlePublisher {

    private static final Gson gson = new GsonBuilder()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .create();

    private final Configuration configuration = templateConfiguration();

    public Mono<Object> sendResult(HttpServerRequest req, HttpServerResponse resp, Method method, Object handleObject, Object requestAttribute) {

        ModelMap modelMap = new ModelMap();
        return Mono.just(new ModelMap())
                .flatMap(m ->
                    this.invokeMethod(req, resp, method, handleObject, (RequestAttribute) requestAttribute, modelMap)
                )
                .flatMap(o -> {
                    // content type
                    String contentType = MediaType.TEXT_HTML;
                    if (method.isAnnotationPresent(Produces.class)) {
                        String _contentType = "";
                        for (String mediaType : method.getAnnotation(Produces.class).value()) {
                            _contentType += (_contentType.equals("")) ? mediaType : "; " + mediaType;
                        }
                        contentType = _contentType.contains("charset") ? _contentType : _contentType + "; charset=UTF-8";
                    }

                    resp.addHeader(HttpHeaderNames.CONTENT_TYPE, contentType);

                    // json
                    if (contentType.contains(MediaType.APPLICATION_JSON)) {
                        return Mono.just(o).map(JsonResponse::new).map(gson::toJson).map(s->{
                            resp.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(s.length()));
                            return s;
                        });
                    }
                    // template
                    else if (o instanceof String) {
                        return this.templateMono(handleObject, modelMap, (String) o).map(s->{
                            resp.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(s.length()));
                            return s;
                        });
                    }
                    // binary
                    else if (o instanceof ByteBuf) {
                        return ByteBufMono.just((ByteBuf) o).map(s->{
                            resp.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(s.readableBytes()));
                            return s;
                        });
                    }
                    // other ...
                    else {
                        return Mono.error(new KReactorException(HttpResponseStatus.NOT_FOUND));
                    }
                });
    }

    private Mono<Object> invokeMethod(HttpServerRequest req, HttpServerResponse resp, Method method, Object handleObject, RequestAttribute requestAttribute, ModelMap modelMap) {
        if (req.method() == HttpMethod.POST) {
            return req
                    .receive()
                    .aggregate()
                    .flatMap(byteBuf -> {
                        try {
                            return (Mono<Object>) method.invoke(handleObject, getMethodParams(method, req, resp, requestAttribute, modelMap, byteBuf));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                    });
        } else {
            try {
                return (Mono<Object>) method.invoke(handleObject, getMethodParams(method, req, resp, requestAttribute, modelMap, null));
            } catch (Exception e) {
                return Mono.error(e);
            }
        }
    }

    private Object[] getMethodParams(Method method, HttpServerRequest request, HttpServerResponse response, RequestAttribute requestAttribute, ModelMap modelMap, ByteBuf content) {
        ArrayList<Object> objectList = new ArrayList<>();

        Map<String, String> questParams = new HashMap<>();
        Map<String, String> formParams = new HashMap<>();
        Map<String, MemoryFileUpload> fileParams = new HashMap<>();

        this.queryParams(request, questParams);
        this.formParams(request, content, formParams, fileParams);

        for (Parameter parameter : method.getParameters()) {
            Class<?> parameterClass = parameter.getType();
            // RequestAttribute
            if (parameterClass == RequestAttribute.class) {
                objectList.add(requestAttribute);
            }
            // request
            else if (parameterClass == HttpServerRequest.class) {
                objectList.add(request);
            }
            // response
            else if (parameterClass == HttpServerResponse.class) {
                objectList.add(response);
            }
            // modelMap
            else if (parameterClass == ModelMap.class) {
                objectList.add(modelMap);
            }
            // upload file
            else if (parameter.getAnnotation(UploadFilesParam.class) != null) {
                String annotationKey = parameter.getAnnotation(UploadFilesParam.class).value();
                objectList.add(fileParams.get(annotationKey));
            }
            // RequestAttribute item
            else if (parameter.getAnnotation(RequestAttributeParam.class) != null) {
                String annotationKey = parameter.getAnnotation(RequestAttributeParam.class).value();
                objectList.add(requestAttribute.getAttribute(annotationKey, parameterClass));
            }
            // CookieParam
            else if (parameter.getAnnotation(CookieParam.class) != null) {
                String annotationKey = parameter.getAnnotation(CookieParam.class).value();
                objectList.add(getParamTypeValue(request.cookies().get(annotationKey).toString(), parameterClass));
            }
            // HeaderParam
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                objectList.add(getParamTypeValue(request.requestHeaders().get(annotationKey), parameterClass));
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                objectList.add(getParamTypeValue(questParams.get(annotationKey), parameterClass));
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                objectList.add(getParamTypeValue(request.param(annotationKey), parameterClass));
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                objectList.add(getParamTypeValue(formParams.get(annotationKey), parameterClass));
            }
            // BeanParam
            else if (parameter.getAnnotation(BeanParam.class) != null) {
                byte[] byteArray = new byte[content.capacity()];
                content.readBytes(byteArray);
                // Type type = TypeToken.get(parameter.getAnnotatedType().getType()).getType();
                objectList.add((new Gson()).fromJson(new String(byteArray), parameter.getAnnotatedType().getType()));
            }
            // default
            else {
                objectList.add(parameterClass.cast(null));
            }
        }
        return objectList.toArray();
    }

    private <T> T getParamTypeValue(String value, Class<T> clazz) {
        if (clazz == Long.class) {
            return clazz.cast(Long.valueOf(value));
        } else if (clazz == Integer.class) {
            return clazz.cast(Integer.valueOf(value));
        } else if (clazz == Boolean.class) {
            return clazz.cast(Boolean.valueOf(value));
        } else if (clazz == String.class) {
            return clazz.cast(value);
        } else {
            return new GsonBuilder().create().fromJson(value, clazz);
        }
    }

    // Get 请求
    private void queryParams(HttpServerRequest request, Map<String, String> questParams) {
        // Map<String, String> requestParams = new HashMap<>();
        // Query Params
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = decoder.parameters();
        for (Map.Entry<String, List<String>> next : params.entrySet()) {
            questParams.put(next.getKey(), next.getValue().get(0));
        }
    }

    // Post 请求
    private void formParams(HttpServerRequest request, ByteBuf content, Map<String, String> formParams, Map<String, MemoryFileUpload> fileParams) {
        if (content != null) {
            // POST Params
            FullHttpRequest dhr = new DefaultFullHttpRequest(request.version(), request.method(), request.uri(), content, request.requestHeaders(), EmptyHttpHeaders.INSTANCE);
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), dhr);
            List<InterfaceHttpData> postData = postDecoder.getBodyHttpDatas();
            for (InterfaceHttpData data : postData) {
                // 一般 post 内容
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    MemoryAttribute attribute = (MemoryAttribute) data;
                    formParams.put(attribute.getName(), attribute.getValue());
                }
                // 上传文件的内容
                else if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    MemoryFileUpload fileUpload = (MemoryFileUpload) data;
                    fileParams.put(fileUpload.getName(), fileUpload);
                }
            }
        }
    }

    // 输出模版
    private Mono<String> templateMono(Object handleObject, ModelMap modelMap, String templateName) {
        return Mono.create(slink -> {
            String controllerName = handleObject.getClass().getSimpleName();
            String templateDirectory = controllerName.toLowerCase().substring(0, controllerName.length()-"handle".length());
            this.configuration.setClassForTemplateLoading(this.getClass(), "/template/" + templateDirectory);
            try {
                Template template = this.configuration.getTemplate(templateName + ".html");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                template.process(modelMap, new OutputStreamWriter(outputStream));
                slink.success(outputStream.toString("UTF-8"));
            }
            catch(Exception e) {
                slink.success(templateName);
            }
        });
    }

    // 配置模版
    private Configuration templateConfiguration() {
        Version version = new Version("2.3.23");
        DefaultObjectWrapperBuilder defaultObjectWrapperBuilder = new DefaultObjectWrapperBuilder(version);

        Configuration cfg = new Configuration(version);
        cfg.setObjectWrapper(defaultObjectWrapperBuilder.build());
        cfg.setDefaultEncoding("UTF-8");
        // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
        cfg.setLogTemplateExceptions(false);
        // Sets how errors will appear. Here we assume we are developing HTML pages.
        // For production systems TemplateExceptionHandler.RETHROW_HANDLER is better.
        // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setClassForTemplateLoading(this.getClass(), "/template");
        // Bind instance for DI
        // bind(Configuration.class).toInstance(cfg);
        return cfg;
    }
}
