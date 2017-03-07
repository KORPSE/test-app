# test-app

## Overview:

Clients have balances in three supported currencies (USD, EUR, GBP). They can send money to each other.
We do not get any fees for their transactions :)
We can view transaction history and balances of clients

## Endpoints:

app binds to port `8080`

* `GET`: /client/\<id\> - Information about client
* `GET`: /client/\<id\>/balance/\<currency\> - Balance in particular currency (USD, EUR, GBP)
* `POST`: /client - Register new client
  - Request example: ```{"firstName":"John","lastName":"Doe"}```
* `POST`: /client/topUp - Top up money to client
  - Request example: ```{"clientId":1,"currency":"USD","amount":100.00}```
* `POST`: /transaction - send money from one client to another
  - Request example: ```{"payer":1, "peer":2,"currency":"USD","amount":10.00}```
* `GET`: /transaction/\<client_id\>: get transaction history of particular client

For more detailed examples please look at `IntegrationalTest.scala`

## Build

```sbt assembly```

## Get started

```java -jar target/scala-2.12/revolut-test-app-assembly-0.1.0-SNAPSHOT.jar```
