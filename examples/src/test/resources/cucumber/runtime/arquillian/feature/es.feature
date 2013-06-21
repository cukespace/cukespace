# language: es

Característica: Registrar datos generales de un interno
                Para asegurarme que la captura de un ingreso funciona
                Como operador
                Ejecuto el escenario capturar un ingreso

  Escenario: Capturar generales de un interno

      Dado visito la pagina "faces/interno/search.xhtml"
      Y doy click en el link "Nuevo"
      Y que he introducido nombre "Carlos"
      Y que he introducido apellidoPaterno "Fuentes"
      Y que he introducido apellidoMaterno "Diaz"
      Cuando oprimo el "boton"
      Entonces el nombre del "interno" se muestra en la pantalla
