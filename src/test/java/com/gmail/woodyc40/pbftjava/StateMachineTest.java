package com.gmail.woodyc40.pbftjava;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class StateMachineTest {
    @Given("^(\\d+) state machines where (\\d+) replicas are faulty$")
    public void stateMachinesWhereReplica(int machines, int faulty) throws Exception {
    }

    @When("^(.*) is requested$")
    public void isRequested(String operation) throws Exception {
    }

    @Then("^the reply should be (.*)$")
    public void theReplyShouldBe(String result) throws Exception {
    }
}
