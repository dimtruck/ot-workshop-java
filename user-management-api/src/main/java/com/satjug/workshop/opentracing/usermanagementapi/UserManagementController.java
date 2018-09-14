package com.satjug.workshop.opentracing.usermanagementapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.satjug.workshop.opentracing.tracing.OpenTracingService;
import com.satjug.workshop.opentracing.tracing.TracingInterceptor;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class UserManagementController {
    private static Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    private static final String authEndpoint = System.getenv("AUTH_ENDPOINT");
    private static final String userEndpoint = System.getenv("USER_ENDPOINT");
    private static final String rabbitHost = System.getenv("RABBITMQ_HOST");
    private static final int rabbitPort = Integer.parseInt(System.getenv("RABBITMQ_PORT"));

    private OkHttpClient client;

    private static final String EXCHANGE_NAME = "workshop";
    private static final String TOPIC = "direct";
    private static final String ROUTING_KEY= "opentracing";

    private static final ConnectionFactory factory;

    static {
        factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(rabbitPort);

    }

    final static OpenTracingService openTracingService = new OpenTracingService();


    @RequestMapping(
            value = "/users",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity getUsers(HttpServletRequest request) {
        logger.info("Get Users");
        Tracer tracer = openTracingService.getGlobalTracer();
        Span span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                request.getRequestURI()))
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {

            UserRequest authRequestBody = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(authRequestBody);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                logger.info("resp {}", response);
                if (response.code() != 200) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Wrong username or password provided");
                } else {
                    Request userRequest = new Request.Builder()
                            .url(userEndpoint)
                            .build();
                    logger.info("Send a request to {}", userEndpoint);

                    try (Response response1 = client.newCall(userRequest).execute()) {
                        logger.info("resp {}", response1);
                        if (response1.code() != 200) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Unable to retrieve users");
                        } else {
                            return ResponseEntity.ok(response1.body().string());
                        }
                    }
                }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        } finally {
            span.finish();
        }
    }

    @RequestMapping(
            value = "/users/{username}",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity getUser(@PathVariable("username") String username, HttpServletRequest request) {
        logger.info("Get User");
        Tracer tracer = openTracingService.getGlobalTracer();
        Span span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                request.getRequestURI()))
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {

            UserRequest authRequestBody = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(authRequestBody);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                logger.info("resp {}", response);
                if (response.code() != 200) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Wrong username or password provided");
                } else {
                    Request userRequest = new Request.Builder()
                            .url(userEndpoint + "/" + username)
                            .build();
                    logger.info("Send a request to {}", userEndpoint);

                    try (Response response1 = client.newCall(userRequest).execute()) {
                        logger.info("resp {}", response1);
                        if (response1.code() != 200) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Unable to retrieve user");
                        } else {
                            return ResponseEntity.ok(response1.body().string());
                        }
                    }
                }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        } finally {
            span.finish();
        }
    }

    @RequestMapping(
            value = "/users",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity addUser(@org.springframework.web.bind.annotation.RequestBody User user,
                                  HttpServletRequest request) {
        logger.info("Add User");
        Tracer tracer = openTracingService.getGlobalTracer();
        Span span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                request.getRequestURI()))
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {

            UserRequest userRequest = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(userRequest);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                logger.info("resp {}", response);
                if (response.code() != 200) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info("Send a rabbitmq message to {}:{}", rabbitHost, rabbitPort);
                    com.rabbitmq.client.Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel();
                    channel.exchangeDeclare(EXCHANGE_NAME, TOPIC, true, false, false, null);

                    String userJson = objectMapper.writeValueAsString(user);

                    Map<String, String> headers = new HashMap<String, String>();
                    tracer.inject(scope.span().context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(headers));

                    Map<String, Object> propHeaders = new HashMap<>();
                    headers.forEach(propHeaders::putIfAbsent);

                    channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY,
                            new AMQP.BasicProperties.Builder().headers(propHeaders).build(), userJson.getBytes());

                    return ResponseEntity.status(HttpStatus.CREATED).build();
                }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        } finally {
            span.finish();
        }
    }


}
