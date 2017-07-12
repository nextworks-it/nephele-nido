#!/bin/bash

curl -v -X POST -d @pathRequest.json http://localhost:8989/nido/operation/path --header "Content-Type:application/json"
