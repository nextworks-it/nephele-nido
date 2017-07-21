# NEPHELE Inter DC Orchestrator (NIDO)

## 1. What is NIDO?

NIDO is a multi Data Centre Network orchestrator able to communicate with several intra-domain (i.e. DCN)
SDN controllers, in order to establish end-to-end data paths, developed in the
[NEPHELE](
  http://www.nepheleproject.eu/ "NEPHELE project"
)
project.

At the moment, it supports the OCEANIA DCN controller and Julius
inter-DC network controller, both developed in the NEPHELE project.

NEPHELE is a research project on optical network technologies,
supported by the [Horizon2020](
  https://ec.europa.eu/programmes/horizon2020/en "H2020 programme"
)
Framework Programme for Research
and Innovation of the European Commission.



## 2. Dependencies
NIDO has the following dependencies:
+ A [postgresql](https://www.postgresql.org/ "PostgreSQL") DB, accessible via
  password authentication.
+ A [rabbitMQ](https://www.rabbitmq.com/ "rabbitMQ")
  [AMQP](https://www.amqp.org/ "AMQP") server.
+ [Apache Maven](https://maven.apache.org "Maven") version >=3.3

Minimal system requirements:
+ 2 CPUs
+ 8 GB RAM
+ 10+ GB disk space

## 3. Installation guide
To install NIDO just run
```
cd NIDO
mvn clean package
```
NIDO can then be started by running `mvn spring-boot:run` in the `NIDO` folder,
or with the jar file that will be created in `NIDO/target`.

## 4. Configuration
All of NIDO's functional parameters (e.g.: postgres database name, user and
password, rabbitMQ coordinates, port bindings etc.) are configurable through
a file named `application.properties` placed into the jar file folder.

The file `NIDO/src/main/resources/application.properties` contains the
application's default configuration, and can be used as an example.

Similarily, the logging configuration can be found in the
`NIDO/src/main/resources/log4j.properties` file.

## 5. Usage Guide
All REST API calls are documented through swagger-ui, at
http://&lt;NIDO-IP&gt;:&lt;NIDO-port&gt;/swagger-ui.html

Before the instantiation of any datapath, domains must be added to the NIDO
DB through a POST request at the `/nido/management/domain` URI. The request
must contain all of the relevant domain informations, such as the IP address and
type (Oceania or Julius) of the domain controller.

When all domains are configured, a path request can be sent through a POST
request at the URI `/nido/operation/path`, containing information such as
source and destination of the path, traffic classifiers to be used (e.g.
destination IP, vlan, etc.), traffic profile and path ID.

The status of the requested path can then be queried through a GET request at
the `nido/operation/path/<pathID>` URI. If the path is not needed anymore, by
sending a DELETE request at `nido/operation/path/<pathID>` it will be
deallocated.

## 6. GUI
