@myTag
Feature: Eat Cukes in Test mode

  Scenario: Eating 4 cukes

    Given I have a belly
    When I eat 4 cukes
    Then I should have 4 cukes in my belly
