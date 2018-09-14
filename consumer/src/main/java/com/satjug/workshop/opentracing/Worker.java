package com.satjug.workshop.opentracing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Worker {
    private static final String EXCHANGE_NAME = "workshop";
    private static final String TOPIC = "direct";
    private static final String ROUTING_KEY= "opentracing";
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final int RABBITMQ_PORT = Integer.parseInt(System.getenv("RABBITMQ_PORT"));

    private static final int RETRY_COUNT = 60;
    private static final int WAIT_IN_SECONDS = 1;

    private static Logger logger = LoggerFactory.getLogger(Worker.class);


    /**
     * Main method.  Sets up connections
     * @param argv
     * @throws Exception
     */
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setPort(RABBITMQ_PORT);
        factory.setHost(RABBITMQ_HOST);
        final Connection connection = getConnection(factory, RETRY_COUNT);
        if (connection == null) throw new RuntimeException("Unable to connect to rabbitmq server");
        final Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, TOPIC, true, false, false, null);

        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, EXCHANGE_NAME, ROUTING_KEY);

        logger.info(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(0);

        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, new Consumer(channel, null));
    }

    private static Connection getConnection(ConnectionFactory factory, int retryCount) {
        if (retryCount <= 0 ) {
            return null;
        } else {
            try {
                return factory.newConnection();
            } catch (IOException | TimeoutException e) {
                logger.error("unable to connect", e);
                try {
                    Thread.sleep(WAIT_IN_SECONDS * 1000);
                    return getConnection(factory, retryCount - 1);
                } catch (InterruptedException ie) {
                    logger.error("unable to sleep", ie);
                    return null;
                }
            }
        }
    }
}
