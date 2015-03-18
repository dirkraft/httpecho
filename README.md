HTTP Echo
---------

[![Build Status](https://travis-ci.org/dirkraft/httpecho.svg?branch=master)](https://travis-ci.org/dirkraft/httpecho)

An extremely plain HTTP request echo service. Without ads, not so HTML, for programmatic usage.


### Formats ###

A variety of response formats are supported. It may be specified as a parameter named `echoFormat` (query or form
parameter) or by the standard HTTP `Accept` header.

  - as param works in a browser: http://echo.dirkraft.com?echoFormat=application/json
  - with accept header with curl: `curl -H"Accept: application/json" http://echo.dirkraft.com`

Supported formats:

  - application/json
  - text/html
  - text/plain (default)
  - text/xml


### Deployment on Heroku ###

The deployment there involves a heroku-managed proxy that routes traffic to the actual application and the Internet.
As a consequence proxy forwarding headers are received by HTTP Echo. These headers are filtered out of the response
normally, but can be included with the parameter and value `echoAllHeaders=true` (query or form parameter).
