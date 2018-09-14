package com.satjug.workshop.opentracing.authapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TokenController {
    private static Logger logger = LoggerFactory.getLogger(TokenController.class);

    @RequestMapping(
            value = "/tokens",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity authenticate(@RequestBody UserRequest userRequest) {
        if(userRequest.getUsername() == null || userRequest.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request.  Needs to be '{\"username\":\"someuser\", \"password\":\"somepass\"}'");
        } else if(userRequest.getPassword().equals("somepass") && userRequest.getUsername().equals("someuser")) {
            return ResponseEntity.ok(new TokenResponse(UUID.randomUUID()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Wrong username or password provided");
        }
    }
}
