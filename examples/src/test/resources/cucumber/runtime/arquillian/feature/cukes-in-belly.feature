Feature: Eat Cukes

  Scenario: Eating 4 cukes

    Given I have a belly
    When I eat 4 cukes
    Then I should have 4 cukes in my belly

  Scenario: Eating 5 cukes

    Given I have a belly
    When I eat 5 cukes
    Then I should have 5 cukes in my belly
