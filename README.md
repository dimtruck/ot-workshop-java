# San Antonio JUG 2018 OpenTracing Workshop

Our goal is for Java developers to understand benefits for distributed tracing,
what OpenTracing brings to the table, and have an opportunity to work
with real examples to better understand how to get started.

With this project, we've provided a simple set of services that invoke
one another in different ways.  We leave it as an exercise for the reader
to instrument with OpenTracing and then reconfigure the service calls
to generate different possible scenarios to visualize with a tracer.


## Positive Scenarios

* Create a trace where every service is invoked through an initial call
from the User Management API

* Make your traces include errors

## Negative Scenarios

* Create a trace where one service is taking too long to respond

* Create a trace demonstrating a circular dependency (what happens?)

* Create a trace with large fanout: one call to the entrypoint results
in 100's of downstream service calls

## Available api calls

* GET /users
* GET /users/test
* POST /users {"user": "test"}
