package com.example.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


class TokenGenerationSimulation extends Simulation {

  // Define the HTTP configuration
  val httpProtocol = http
    .baseUrl("https://pricing-adapter-dev.metrosystems.net") // Base URL
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate, br")
    .connectionHeader("keep-alive")

  // Define the scenario
  val scn = scenario("Generate Multiple Tokens and Use One")
    // Generate first token
    .exec(
      http("Get First Token")
        .post("https://idam-pp.metrosystems.net/authorize/api/oauth2/access_token")
        .header("Content-Type", "application/json")
        .body(StringBody("""{"username": "POLARIS_API_CALLER_APP", "password": "r8XrVJquf9"}""")).asJson
        .check(jsonPath("$.access_token").saveAs("firstToken"))
    )
    // Generate second token
    .exec(
      http("Get Second Token")
        .post("https://sts.googleapis.com/v1/token")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """{
            "audience": "//iam.googleapis.com/projects/392157600214/locations/global/workloadIdentityPools/pricing-adapter-wif/providers/metro-idam-provider",
            "grantType": "urn:ietf:params:oauth:grant-type:token-exchange",
            "requestedTokenType": "urn:ietf:params:oauth:token-type:access_token",
            "scope": "https://www.googleapis.com/auth/cloud-platform",
            "subjectTokenType": "urn:ietf:params:oauth:token-type:jwt",
            "subjectToken": "${firstToken}"
          }"""
        )).asJson
        .check(jsonPath("$.access_token").saveAs("secondToken"))
    )
    // Generate third token
    .exec(
      http("Get Third Token")
        .post("https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/pa-idam-external-invoker@cf-pricingada-pricing-adapt-3j.iam.gserviceaccount.com:generateIdToken")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """{
            "audience": "api-fsd-audience-for-it",
            "includeEmail": true
          }"""
        )).asJson
        .check(jsonPath("$.token").saveAs("thirdToken")) // Adjust the JSON path if needed
    )
    // Use the third token in the subsequent API request
    .exec(
      http("Use Third Token")
        .get("/fsd/it/data")
        .queryParam("startEntryId", "1001")
        .queryParam("endEntryId", "3001")
        .queryParam("rpp", "2000")
        .header("Authorization", "Bearer ${thirdToken}")
    )

  // Set up the simulation
  setUp(
    scn.inject(atOnceUsers(1)) // Adjust the load profile as needed
  ).protocols(httpProtocol)
}