#!/bin/bash


curl -v -X POST -d @dummyDomainOceania1.json http://localhost:8989/nido/management/domain --header "Content-Type:application/json"
curl -v -X POST -d @dummyDomainOceania2.json http://localhost:8989/nido/management/domain --header "Content-Type:application/json"
curl -v -X POST -d @dummyDomainOceania3.json http://localhost:8989/nido/management/domain --header "Content-Type:application/json"
curl -v -X POST -d @dummyJulius.json http://localhost:8989/nido/management/domain --header "Content-Type:application/json"

