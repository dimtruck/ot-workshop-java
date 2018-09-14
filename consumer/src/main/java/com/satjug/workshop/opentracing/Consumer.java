package com.satjug.workshop.opentracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.satjug.workshop.opentracing.tracing.OpenTracingService;
import com.satjug.workshop.opentracing.tracing.TracingInterceptor;
import io.opentracing.*;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Consumer extends DefaultConsumer {
    private Logger logger;

    private final String userEndpoint = System.getenv("USER_ENDPOINT");

    final static OpenTracingService openTracingService = new OpenTracingService();

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public Consumer(Channel channel, Logger logger) {
        super(channel);
        if (logger == null) this.logger = LoggerFactory.getLogger(Consumer.class);
        else this.logger = logger;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        logger.info("handle delivery {} {}", consumerTag, properties);
        Tracer tracer = openTracingService.getGlobalTracer();
        SpanContext context = null;

        Map<String, Object> headers = properties.getHeaders();

        if(headers != null) {
            Map<String, String> stringHeaders = new HashMap<>();
            headers.forEach((k, v) -> stringHeaders.putIfAbsent(k, v.toString()));
            context = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(stringHeaders));

        }
        logger.info("got context {}", context);

        Span span = null;

        if(context == null)
            span = tracer.buildSpan(consumerTag)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        else
            span = tracer.buildSpan(consumerTag)
                    .addReference(References.FOLLOWS_FROM, context)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();

        ObjectMapper objectMapper = new ObjectMapper();
        User message = objectMapper.readValue(body, User.class);

        logger.info(" [x] Received '{}'", message);

        try (Scope scope = tracer.scopeManager().activate(span,false)) {

            String resultsJson = objectMapper.writeValueAsString(message);

            Request request = new Request.Builder()
                    .url(userEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", userEndpoint, resultsJson);

            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(request).execute()) {
                logger.info("resp {}", response);
            }
        } catch(Exception e) {
            logger.error("send error", e);
            Tags.ERROR.set(span, true);
            Map map = new HashMap();
            map.put(Fields.EVENT, "error");
            map.put(Fields.ERROR_OBJECT, e);
            map.put(Fields.MESSAGE, e.getLocalizedMessage());

            span.log(map);
            logger.info("gotta send that error out");
        } finally {
            span.finish();
            logger.info(" [x] Finished processing '{}'", message);
            this.getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }
}
