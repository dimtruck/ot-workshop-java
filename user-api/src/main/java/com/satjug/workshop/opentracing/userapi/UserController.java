package com.satjug.workshop.opentracing.userapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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


    @RequestMapping(
            value = "/users",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity addUser(@org.springframework.web.bind.annotation.RequestBody User user) {
        UserRequest authRequest = new UserRequest("someuser", "somepass");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(authRequest);

            Request request = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());
                    users.add(new UserResponse(user.getUser()));
                    return ResponseEntity.status(HttpStatus.CREATED).build();
                }
            }
        } catch(Exception e) {
            logger.error("send error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        }
    }


    @RequestMapping(
            value = "/users",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity getUsers() {
        UserRequest userRequest = new UserRequest("someuser", "somepass");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(userRequest);

            Request request = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());
                    return ResponseEntity.ok(users);
                }
            }
        } catch(Exception e) {
            logger.error("send error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        }
    }

    @RequestMapping(
            value = "/users/{username}",
            method = RequestMethod.GET,
            produces = "application/json")
    public ResponseEntity getUser(@PathVariable("username") String username) {
        UserRequest userRequest = new UserRequest("someuser", "somepass");

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String resultsJson = objectMapper.writeValueAsString(userRequest);

            Request request = new Request.Builder()
                    .url(authEndpoint)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), resultsJson))
                    .build();
            logger.info("Send a request to {} {}", authEndpoint, resultsJson);

            client =  new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true).build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 200) {
                    logger.info("resp {}", response);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
                } else {
                    logger.info(response.toString());

                    for (UserResponse userResponse: users) {
                        if(userResponse.getUser().equals(username)) {
                            return ResponseEntity.ok(userResponse);
                        }

                    }
                    return ResponseEntity.notFound().build();
                }
            }
        } catch(Exception e) {
            logger.error("send error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        }
    }
}
