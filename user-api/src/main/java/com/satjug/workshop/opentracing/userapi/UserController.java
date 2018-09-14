package com.satjug.workshop.opentracing.userapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satjug.workshop.opentracing.tracing.OpenTracingService;
import com.satjug.workshop.opentracing.tracing.TracingInterceptor;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.*;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
public class UserController {
    private static Logger logger = LoggerFactory.getLogger(UserController.class);

    private final String authEndpoint = System.getenv("AUTH_ENDPOINT");
    private OkHttpClient client;

    private static final List<UserResponse> users = new ArrayList<>();

    static {
        users.add(new UserResponse("test"));
    }

    final static OpenTracingService openTracingService = new OpenTracingService();


    @RequestMapping(
            value = "/users",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity addUser(@org.springframework.web.bind.annotation.RequestBody User user,
                                  HttpServletRequest request) {
        logger.info("Add User");
        Tracer tracer = openTracingService.getGlobalTracer();
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        SpanContext context = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
        logger.info("got context {}", context);
        Span span = null;

        if(context == null)
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        else
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .asChildOf(context)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {

            UserRequest authRequestObject = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(authRequestObject);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());
                    users.add(new UserResponse(user.getUser()));
                    return ResponseEntity.status(HttpStatus.CREATED).build();
                }
            }
        } catch (Exception e) {
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
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity getUsers(HttpServletRequest request) {
        logger.info("Get Users");
        Tracer tracer = openTracingService.getGlobalTracer();
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        SpanContext context = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
        logger.info("got context {}", context);
        Span span = null;

        if(context == null)
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        else
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .asChildOf(context)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {
            UserRequest userRequestObject = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(userRequestObject);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());
                    return ResponseEntity.ok(users);
                }
            }
        } catch (Exception e) {
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
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        SpanContext context = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
        logger.info("got context {}", context);
        Span span = null;

        if(context == null)
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        else
            span = tracer.buildSpan(String.format("%s %s", request.getMethod(),
                    request.getRequestURI()))
                    .asChildOf(context)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
        try (Scope scope = tracer.scopeManager().activate(span,false)) {
            UserRequest userRequest = new UserRequest("someuser", "somepass");

            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(userRequest);

            Request authRequest = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new TracingInterceptor(tracer))
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(authRequest).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());

                    for (UserResponse userResponse : users) {
                        if (userResponse.getUser().equals(username)) {
                            return ResponseEntity.ok(userResponse);
                        }

                    }
                    return ResponseEntity.notFound().build();
                }
            }
        } catch (Exception e) {
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
