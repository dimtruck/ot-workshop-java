package com.satjug.workshop.opentracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Consumer extends DefaultConsumer {
    private Logger logger;

    private final String userEndpoint = System.getenv("USER_ENDPOINT");

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
        ObjectMapper objectMapper = new ObjectMapper();
        User message = objectMapper.readValue(body, User.class);

        logger.info(" [x] Received '{}'", message);
        try {

            String resultsJson = objectMapper.writeValueAsString(message);

            Request request = new Request.Builder()
                    .url(userEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", userEndpoint, resultsJson);

            OkHttpClient client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(request).execute()) {
                logger.info("resp {}", response);
            }
        } finally {
            logger.info(" [x] Finished processing '{}'", message);
            this.getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }

}
