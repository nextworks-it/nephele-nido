#!/bin/bash

curl -v -X DELETE http://localhost:8989/nido/management/domain/DCN_01

curl -v -X DELETE http://localhost:8989/nido/management/domain/DCN_02

curl -v -X DELETE http://localhost:8989/nido/management/domain/DCN_03

curl -v -X DELETE http://localhost:8989/nido/management/domain/InterDC_01
