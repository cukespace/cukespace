Feature: Registrar datos generales de un interno
  Para asegurarme que la captura de un ingreso funciona
  Como operador
  Ejecuto el escenario capturar un ingreso

  Scenario Outline: Capturar generales de un interno
    Given visito  la pagina "faces/interno/search.xhtml"
    And doy click en el link  "Nuevo"
    And que he introducido "<nombre>"
    And que he introducido "<apellidoPaterno>"
    And que he introducido "<apellidoMaterno>"
    When oprimo el  "<boton>"
    Then el nombre del  "<interno>" se muestra en la pantalla

  Examples:
    | nombre | apellidoPaterno | apellidoMaterno | boton   | interno                 |
    | Carlos | Monsivais       | Robles          | Guardar | Carlos Monsivais Robles |
    | Carlos | Fuentes         | Treviño         | Guardar | Carlos Fuentes Treviño  |