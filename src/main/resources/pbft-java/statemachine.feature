Feature: PBFT state machine replication

  The main feature file for the PBFT implementation

  Scenario Outline: Normal Case Addition

    Uses an addition operation to parse a numerical result
    that results from distributing the computation across
    a given number of total and faulty replicas

    Given <total> state machines where <faulty> replicas are faulty
    When <operation> is requested
    Then the reply should be <result>

    Examples:
      | total | faulty | operation | result
      | 4     | 1      | 1 + 1     | 2
      | 7     | 2      | 1 + 1     | 2
