package com.satjug.workshop.opentracing.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TracingInterceptor implements Interceptor {
    private static Logger logger = LoggerFactory.getLogger(TracingInterceptor.class);

    private Tracer tracer;

    public TracingInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override public Response intercept(Chain chain) throws IOException {
        logger.info("intercept the request");
        Span activeSpan = tracer.activeSpan();
        Span childSpan = null;

        String name = chain.request().method() + " " + chain.request().url();

        if(activeSpan == null)
            childSpan = tracer.buildSpan(name).start();
        else
            childSpan = tracer.buildSpan(name).asChildOf(activeSpan.context()).start();

        Response response = null;

        try (Scope scope = tracer.scopeManager().activate(childSpan,false)) {
            Request.Builder requestBuilder = chain.request().newBuilder();

            tracer.inject(scope.span().context(), Format.Builtin.HTTP_HEADERS, new TextMap() {
                @Override
                public Iterator<Map.Entry<String, String>> iterator() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void put(String s, String s1) {
                    logger.info("add tracing header {}", s);
                    requestBuilder.addHeader(s, s1).build();
                }
            });
            logger.info("send the request");
            response = chain.proceed(requestBuilder.build());
            logger.info("get the response");
        } catch (Exception e) {
            Tags.ERROR.set(childSpan, true);
            Map map = new HashMap();
            map.put(Fields.EVENT, "error");
            map.put(Fields.ERROR_OBJECT, e);
            map.put(Fields.MESSAGE, e.getLocalizedMessage());

            childSpan.log(map);
        } finally {
            childSpan.finish();
        }

        return response;

    }
}
