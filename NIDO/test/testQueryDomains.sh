#!/bin/bash

curl -v -X GET http://localhost:8989/nido/management/domains

curl -v -X GET http://localhost:8989/nido/management/domain/DCN_01

curl -v -X GET http://localhost:8989/nido/management/domain/DCN_02

curl -v -X GET http://localhost:8989/nido/management/domain/DCN_03

curl -v -X GET http://localhost:8989/nido/management/domain/InterDC_01
