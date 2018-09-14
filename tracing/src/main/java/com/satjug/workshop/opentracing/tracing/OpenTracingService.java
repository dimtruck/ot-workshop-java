package com.satjug.workshop.opentracing.tracing;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;

public class OpenTracingService {
    private static final Logger logger = LoggerFactory.getLogger(OpenTracingService.class);

    private static boolean isTracingEnabled = Boolean.parseBoolean(System.getenv("TRACING_ENABLED"));

    private static boolean isTracingSpanLogged = Boolean.parseBoolean(System.getenv("TRACING_LOG_SPANS"));

    private static final String tracer = System.getenv("TRACER");

    private static final String tracerHost = System.getenv("TRACER_HOST");

    private static final int tracerPort = Integer.parseInt(System.getenv("TRACER_PORT"));

    private static final String tracerServiceName = System.getenv("TRACER_SERVICE_NAME");

    private static final String tracerSamplingType = System.getenv("TRACER_SAMPLING_TYPE");

    private static final String tracerSamplingValue = System.getenv("TRACER_SAMPLING_VALUE");

    private static final int tracerFlushInternal = Integer.parseInt(System.getenv("TRACER_FLUSH_INTERVAL"));

    private static final int tracerMaxBatchSize = Integer.parseInt(System.getenv("TRACER_MAX_BATCH_SIZE"));

    public Tracer getGlobalTracer(){
        if (!GlobalTracer.isRegistered()) {
            logger.error("GlobalTracer not yet registered");

            if (isTracingEnabled) {
                switch (tracer) {
                    case "jaeger":
                        try {
                            GlobalTracer.register(
                                    new Configuration(tracerServiceName).withSampler(
                                            new Configuration.SamplerConfiguration().withType(
                                                    tracerSamplingType
                                            ).withParam(
                                                    NumberFormat.getInstance().parse(tracerSamplingValue)
                                            )
                                    ).withReporter(
                                            new Configuration.ReporterConfiguration()
                                                    .withFlushInterval(tracerFlushInternal)
                                                    .withLogSpans(isTracingSpanLogged)
                                                    .withMaxQueueSize(tracerMaxBatchSize)
                                                    .withSender((new Configuration.SenderConfiguration())
                                                            .withAgentHost(tracerHost)
                                                            .withAgentPort(tracerPort))).getTracer()
                            );
                        } catch (ParseException p) {
                            logger.error("error parsing value", p);
                            throw new RuntimeException();
                        }

                        logger.info("registered a tracer");
                        break;
                    default:
                        logger.error("Invalid tracer specified.  Problem with " + tracer);
                }
            }
        }
        return GlobalTracer.get();
    }

}

