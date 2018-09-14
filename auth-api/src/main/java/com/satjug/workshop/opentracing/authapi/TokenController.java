package com.satjug.workshop.opentracing.authapi;

import com.satjug.workshop.opentracing.tracing.OpenTracingService;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class TokenController {
    private static Logger logger = LoggerFactory.getLogger(TokenController.class);

    final static OpenTracingService openTracingService = new OpenTracingService();

    @RequestMapping(
            value = "/tokens",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity authenticate(@RequestBody UserRequest userRequest, HttpServletRequest request) {
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
        try (Scope scope = tracer.scopeManager().activate(span, false)) {
            if (userRequest.getUsername() == null || userRequest.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid request.  Needs to be '{\"username\":\"someuser\", \"password\":\"somepass\"}'");
            } else if (userRequest.getPassword().equals("somepass") && userRequest.getUsername().equals("someuser")) {
                return ResponseEntity.ok(new TokenResponse(UUID.randomUUID()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
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
