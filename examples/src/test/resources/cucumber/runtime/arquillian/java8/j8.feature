Feature: A person says hi

  Scenario: Hi

    Given I have a person
    When I say "hi"
    Then I should have said "hi"
