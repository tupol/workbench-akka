#!/usr/bin/env bash

HTTP_PORT=8001
MANAGEMENT_PORT=8559
ARTERY_PORT=2552

sbt "runMain org.tupol.akka.demo.session.sharded.ShardedSessionsApp -Dakka.http.server.default-http-port=$HTTP_PORT -Dakka.remote.artery.canonical.port=$ARTERY_PORT -Dakka.management.http.port=$MANAGEMENT_PORT"
