package com.doopp.reactor.guice.publisher;

import com.doopp.reactor.guice.annotation.RequestAttribute;
import com.doopp.reactor.guice.annotation.FileParam;
import com.doopp.reactor.guice.json.HttpMessageConverter;
import com.doopp.reactor.guice.view.ModelMap;
import com.doopp.reactor.guice.view.TemplateDelegate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCounted;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class HandlePublisher {

    private HttpMessageConverter httpMessageConverter;

    private TemplateDelegate templateDelegate;

    private static String APPLICATION_PROTO = "application/x-protobuf";

    public void setHttpMessageConverter(HttpMessageConverter httpMessageConverter) {
        this.httpMessageConverter = httpMessageConverter;
    }

    public HttpMessageConverter getHttpMessageConverter() {
        return this.httpMessageConverter;
    }

    public void setTemplateDelegate(TemplateDelegate templateDelegate) {
        this.templateDelegate = templateDelegate;
    }

    public Mono<Object> sendResult(HttpServerRequest req, HttpServerResponse resp, Method method, Object handleObject, Object requestAttribute) {
        // ModelMap
        ModelMap modelMap = new ModelMap();
        // http content type
        String contentType = methodProductsValue(method);
        // result
        return this.invokeMethod(
            req, resp, method, handleObject, (com.doopp.reactor.guice.RequestAttribute) requestAttribute, modelMap
        )
            .map(result -> {
                resp.header(HttpHeaderNames.CONTENT_TYPE, contentType);
                if (result instanceof String && ((String) result).startsWith("redirect:")) {
                    String uri = ((String) result).substring(9);
                    return resp.sendRedirect(uri);
                }

                // byte[] binary
                if (result instanceof byte[]) {
                    return Unpooled.wrappedBuffer((byte[]) result);
                }
                // ByteBuf binary
                else if (result instanceof ByteBuf) {
                    return ((ByteBuf) result).retain();
                }
                // String
                else if (result instanceof String && (contentType.contains(MediaType.TEXT_HTML) || contentType.contains(MediaType.TEXT_PLAIN))) {
                    if (this.templateDelegate == null) {
                        return result;
                    }
                    return this.templateDelegate.template(handleObject, modelMap, (String) result);
                }
                // json
                else {
                    if (this.httpMessageConverter == null) {
                        resp.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        return "{\"err_code\":500, \"err_msg\":\"A Message Converter instance is required\", \"data\":null}";
                    }
                    return this.httpMessageConverter.toJson(result);
                }
            });
    }

    private Mono<?> invokeMethod(HttpServerRequest request, HttpServerResponse response, Method method, Object handleObject, com.doopp.reactor.guice.RequestAttribute requestAttribute, ModelMap modelMap) {

        // value of url quest
        Map<String, List<String>> questParams = new HashMap<>();
        // values of form post
        Map<String, List<String>> formParams = new HashMap<>();
        // values of file upload
        Map<String, List<FileUpload>> fileParams = new HashMap<>();
        // get results
        this.queryParams(request, questParams);

        Mono<Object[]> objectMono;

        if (request.method() == HttpMethod.POST || request.method() == HttpMethod.PUT || request.method() == HttpMethod.DELETE) {
            objectMono = request.receive()
                .aggregate()
                .flatMap(byteBuf -> {
                    this.formParams(request, byteBuf, formParams, fileParams);
                    return methodParams(
                            method,
                            request,
                            response,
                            requestAttribute,
                            modelMap,
                            byteBuf,
                            questParams,
                            formParams,
                            fileParams
                    );
                });
        } else {
                // this.formParams(request, null, formParams, fileParams);
                objectMono = methodParams(
                        method,
                        request,
                        response,
                        requestAttribute,
                        modelMap,
                        null,
                        questParams,
                        formParams,
                        fileParams
                );
        }
        return objectMono.flatMap(oo -> {
            fileParams
                    .forEach((name, fileUploads)->fileUploads
                            .forEach(ReferenceCounted::release));
            try {
                Object result = method.invoke(handleObject, oo);
                return (result instanceof Mono<?>) ? (Mono<?>) result : Mono.just(result);
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public String methodProductsValue(Method method) {
        String contentType = MediaType.TEXT_HTML;
        if (method != null && method.isAnnotationPresent(Produces.class)) {
            StringBuilder _contentType = new StringBuilder();
            for (String mediaType : method.getAnnotation(Produces.class).value()) {
                _contentType.append((_contentType.toString().equals("")) ? mediaType : "; " + mediaType);
            }
            contentType = _contentType.toString().contains("charset") ? _contentType.toString() : _contentType + "; charset=UTF-8";
        }
        return contentType;
    }

    private Mono<Object[]> methodParams(Method method,
                                  HttpServerRequest request,
                                  HttpServerResponse response,
                                  com.doopp.reactor.guice.RequestAttribute requestAttribute,
                                  ModelMap modelMap,
                                  ByteBuf content,
                                  Map<String, List<String>> questParams,
                                  Map<String, List<String>> formParams,
                                  Map<String, List<FileUpload>> fileParams) {

        // values of method parameters
        ArrayList<Object> objectList = new ArrayList<>();

        // is a json request
        String requestContentType = getRequestContentType(request);

        for (Parameter parameter : method.getParameters()) {
            Class<?> parameterClazz = parameter.getType();
            // RequestAttribute
            if (parameterClazz == com.doopp.reactor.guice.RequestAttribute.class) {
                objectList.add(requestAttribute);
            }
            // request
            else if (parameterClazz == HttpServerRequest.class) {
                objectList.add(request);
            }
            // response
            else if (parameterClazz == HttpServerResponse.class) {
                objectList.add(response);
            }
            // modelMap
            else if (parameterClazz == ModelMap.class) {
                objectList.add(modelMap);
            }
            // RequestAttribute item
            else if (parameter.getAnnotation(RequestAttribute.class) != null) {
                String annotationKey = parameter.getAnnotation(RequestAttribute.class).value();
                objectList.add(requestAttribute.getAttribute(annotationKey, parameterClazz));
            }
            // CookieParam : Set<Cookie>
            else if (parameter.getAnnotation(CookieParam.class) != null) {
                String annotationKey = parameter.getAnnotation(CookieParam.class).value();
                // Collections.addAll(annotationVal, request.cookies().get(annotationKey).toString());
                objectList.add(request.cookies().get(annotationKey));
            }
            // HeaderParam : String
            else if (parameter.getAnnotation(HeaderParam.class) != null) {
                String annotationKey = parameter.getAnnotation(HeaderParam.class).value();
                objectList.add(request.requestHeaders().get(annotationKey));
            }
            // PathParam
            else if (parameter.getAnnotation(PathParam.class) != null) {
                String annotationKey = parameter.getAnnotation(PathParam.class).value();
                List<String> annotationVal = new ArrayList<>();
                annotationVal.add(request.param(annotationKey));
                objectList.add(classCastStringValue(annotationVal, parameterClazz));
            }
            // QueryParam
            else if (parameter.getAnnotation(QueryParam.class) != null) {
                String annotationKey = parameter.getAnnotation(QueryParam.class).value();
                objectList.add(classCastStringValue(questParams.get(annotationKey), parameterClazz));
            }
            // FormParam
            else if (parameter.getAnnotation(FormParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FormParam.class).value();
                objectList.add(classCastStringValue(formParams.get(annotationKey), parameterClazz));
            }
            // upload file
            else if (parameter.getAnnotation(FileParam.class) != null) {
                String annotationKey = parameter.getAnnotation(FileParam.class).value();
                String annotationPath = parameter.getAnnotation(FileParam.class).path();
                try {
                    objectList.add(classCastFileUploadValue(fileParams.get(annotationKey), annotationPath, parameterClazz));
                }
                catch(IOException e) {
                    return Mono.error(e);
                }
            }
            // BeanParam
            else {
                // if json request
                if (requestContentType.contains(MediaType.APPLICATION_JSON)) {
                    if (parameterClazz.equals(ByteBuf.class)) {
                        byte[] byteArray = new byte[content.capacity()];
                        content.readBytes(byteArray);
                        objectList.add(byteArray);
                    }
                    else {
                        objectList.add(jsonBeanParam(content, parameterClazz));
                    }
                }
                // if protobuf request
                else if (requestContentType.contains(APPLICATION_PROTO)) {
                    objectList.add(protobufBeanParam(content, parameterClazz));
                }
                // default is form request
                else {
                    try {
                        objectList.add(formBeanParam(formParams, fileParams, parameterClazz));
                    }
                    catch(IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        return Mono.error(e);
                    }
                }
            }
            // default
            // else {
            //    objectList.add(parameterClazz.cast(null));
            // }
        }
        return Mono.just(objectList.toArray());
    }

    private Object jsonBeanParam(ByteBuf content, Class<?> parameterClazz) {
        if (httpMessageConverter == null) {
            return null;
        }
        byte[] byteArray = new byte[content.capacity()];
        content.readBytes(byteArray);
        // Type type = TypeToken.get(parameter.getAnnotatedType().getType()).getType();
        // objectList.add((new Gson()).fromJson(new String(byteArray), parameter.getAnnotatedType().getType()));
        return httpMessageConverter.fromJson(new String(byteArray), parameterClazz);
    }

    private Object protobufBeanParam(ByteBuf content, Class<?> parameterClazz) {
        try {
            Method method = parameterClazz.getMethod("parseFrom", byte[].class);
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes);
            return method.invoke(parameterClazz, (Object) bytes);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object formBeanParam(Map<String, List<String>> formParams,
                                 Map<String, List<FileUpload>> fileParams,
                                 Class<?> parameterClazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {

        Object parameterObject = parameterClazz.newInstance();
        Field[] parameterFields = parameterObject.getClass().getDeclaredFields();
        for(Field parameterField : parameterFields) {
            try {
                Method parameterMethod = parameterObject.getClass().getMethod("set" + captureName(parameterField.getName()), parameterField.getType());
                if (parameterMethod != null && Modifier.isPublic(parameterMethod.getModifiers())) {
                    if (parameterField.getType()==FileUpload[].class) {
                        parameterMethod.invoke(parameterObject, fileParams.get(parameterField.getName()));
                    }
                    else if (parameterField.getType()==FileUpload.class) {
                        parameterMethod.invoke(parameterObject, fileParams.get(parameterField.getName()).get(0));
                    }
                    else {
                        parameterMethod.invoke(parameterObject, classCastStringValue(formParams.get(parameterField.getName()), parameterField.getType()));
                    }
                }
            }
            catch(NoSuchMethodException ignored) {}
        }
        return parameterObject;

        /*
        Method[] parameterMethods = parameterObject.getClass().getMethods();
        for (Method parameterMethod : parameterMethods) {
            if (parameterMethod.getName().startsWith("set")) {
                parameterMethod.invoke(parameterObject, methodParams(parameterMethod,
                        request,
                        response,
                        requestAttribute,
                        modelMap,
                        content,
                        questParams,
                        formParams,
                        fileParams
                ));
            }
        }
        */

    }

    // GET request type
    private String getRequestContentType(HttpServerRequest request) {
        // is a json request
        String requestContentType = "";
        List<String> requestHeaderContentTypes = request.requestHeaders().getAll(HttpHeaderNames.CONTENT_TYPE);
        for (String contentType : requestHeaderContentTypes) {
            if (contentType.contains(MediaType.APPLICATION_JSON)) {
                requestContentType = MediaType.APPLICATION_JSON;
                break;
            }
            else if (contentType.contains(APPLICATION_PROTO)) {
                requestContentType = APPLICATION_PROTO;
                break;
            }
        }
        return requestContentType;
    }

    // 进行字母的ascii编码前移，效率要高于截取字符串进行转换的操作
    private static String captureName(String str) {
        char[] cs=str.toCharArray();
        cs[0]-=32;
        return String.valueOf(cs);
    }

    private <T> T classCastFileUploadValue(List<FileUpload> value, String path, Class<T> clazz) throws IOException {
        // if value is null
        if (value == null) {
            return clazz.cast(null);
        }
        // one
        else if (clazz == File.class) {
            String[] fileNameSplit = value.get(0).getFilename().split("\\.");
            String fileName = fileNameSplit.length<=1
                ? UUID.randomUUID().toString()
                : UUID.randomUUID().toString() + "." + fileNameSplit[fileNameSplit.length-1];

            File file = new File(path + "/" + fileName);
            saveFile(file, value.get(0));
            return clazz.cast(file);
        }
        // more
        else if (clazz == File[].class) {
            ArrayList<File> files = new ArrayList<>();
            for (int ii=0; ii<value.size(); ii++) {
                String[] fileNameSplit = value.get(ii).getFilename().split("\\.");
                String fileName = fileNameSplit.length<=1
                    ? UUID.randomUUID().toString()
                    : UUID.randomUUID().toString() + "." + fileNameSplit[fileNameSplit.length-1];
                files.add(ii, new File(path + "/" + fileName));
                saveFile(files.get(ii), value.get(ii));
            }
            return clazz.cast(files.toArray(new File[0]));
        }
        // one
        else if (clazz == FileUpload.class) {
            return clazz.cast(value.get(0));
        }
        // more
        else if (clazz == FileUpload[].class) {
            return clazz.cast(value.toArray(new FileUpload[0]));
        }
//        // one
//        else if (clazz == MemoryFileUpload.class) {
//            return clazz.cast(value.get(0));
//        }
//        // more
//        else if (clazz == MemoryFileUpload[].class) {
//            return clazz.cast(value.toArray(new MemoryFileUpload[0]));
//        }
        // one byte[]
        else if (clazz == byte[].class) {
            return clazz.cast(value.get(0).get());
        }
        // more byte[]
        else if (clazz == byte[][].class) {
            ArrayList<byte[]> byteValues = new ArrayList<>();
            for (FileUpload s : value) {
                byteValues.add(s.get());
            }
            return clazz.cast(byteValues.toArray());
        } else {
            return clazz.cast(null);
        }
    }

    private void saveFile(File file, FileUpload fileUpload) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(0);
            randomAccessFile.write(fileUpload.get());
        }
        catch(Exception ignored) {}

//        AsynchronousFileChannel channel = null;
//        try {
//            channel = AsynchronousFileChannel.open(Paths.get(file.getPath()), StandardOpenOption.WRITE);
//            channel.write(ByteBuffer.wrap(fileUpload.get()), 0L);
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            if (channel!=null && channel.isOpen()) {
//                channel.close();
//            }
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//        }
    }

    private <T> T classCastStringValue(List<String> value, Class<T> clazz) {
        // if value is null
        if (value == null) {
            return clazz.cast(null);
        }
        // Long
        else if (clazz == Long.class) {
            return clazz.cast(Long.valueOf(value.get(0)));
        }
        // Integer
        else if (clazz == Integer.class) {
            return clazz.cast(Integer.valueOf(value.get(0)));
        }
        // Boolean
        else if (clazz == Boolean.class) {
            return clazz.cast(Boolean.valueOf(value.get(0)));
        }
        // String
        else if (clazz == String.class) {
            return clazz.cast(value.get(0));
        }
        // Float
        else if (clazz == Float.class) {
            return clazz.cast(Float.valueOf(value.get(0)));
        }
        // Double
        else if (clazz == Double.class) {
            return clazz.cast(Double.valueOf(value.get(0)));
        }
        // Short
        else if (clazz == Short.class) {
            return clazz.cast(Short.valueOf(value.get(0)));
        }
        // Long[]
        else if (clazz == Long[].class) {
            ArrayList<Long> longValues = new ArrayList<>();
            for (String s : value) {
                longValues.add(Long.valueOf(s));
            }
            return clazz.cast(longValues.toArray(new Long[0]));
        }
        // Integer[]
        else if (clazz == Integer[].class) {
            ArrayList<Integer> intValues = new ArrayList<>();
            for (String s : value) {
                intValues.add(Integer.valueOf(s));
            }
            return clazz.cast(intValues.toArray(new Integer[0]));
        }
        // String[]
        else if (clazz == String[].class) {
            return clazz.cast(value.toArray(new String[0]));
        }
        // default return null;
        else {
            return clazz.cast(value);
        }
    }

    // Get 请求
    private void queryParams(HttpServerRequest request, Map<String, List<String>> questParams) {
        // Map<String, String> requestParams = new HashMap<>();
        // Query Params
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = decoder.parameters();
        for (Map.Entry<String, List<String>> next : params.entrySet()) {
            questParams.put(next.getKey(), next.getValue());
        }
    }

    // Post 请求
    private void formParams(HttpServerRequest request, ByteBuf content, Map<String, List<String>> formParams, Map<String, List<FileUpload>> fileParams) {
        if (content != null && getRequestContentType(request).equals("")) {
            // Request headers
            // HttpHeaders requestHttpHeaders = request.requestHeaders();
            // POST Params
            FullHttpRequest dhr = new DefaultFullHttpRequest(request.version(), request.method(), request.uri(), content);
            dhr.headers().set(request.requestHeaders());
            // set Request Decoder
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), dhr, CharsetUtil.UTF_8);
            // loop data
            for (InterfaceHttpData data : postDecoder.getBodyHttpDatas()) {
                String name = data.getName();
                if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    formParams.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(((MemoryAttribute) data).getValue());
                }
                // 上传文件的内容
                else if (name!=null && data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    fileParams.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(((MemoryFileUpload) data).retain());
                }
            }
            postDecoder.destroy();
            dhr.release();
            // content.release();
        }
    }
}
