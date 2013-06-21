package cucumber.runtime.arquillian.feature;

import cucumber.api.java.es.Cuando;
import cucumber.api.java.es.Dado;
import cucumber.api.java.es.Entonces;
import cucumber.runtime.arquillian.ArquillianCucumber;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(ArquillianCucumber.class)
public class EsTest {
    @Deployment
    public static Archive<?> war() {
        return ShrinkWrap.create(WebArchive.class).addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Dado("^visito la pagina \"([^\"]*)\"$")
    public void visito_la_pagina(final String pagina) throws Throwable {
        assertEquals("faces/interno/search.xhtml", pagina);
    }

    @Dado("^doy click en el link \"([^\"]*)\"$")
    public void doy_click_en_el_link(final String link) throws Throwable {
        assertEquals("Nuevo", link);
    }

    @Dado("^que he introducido nombre \"([^\"]*)\"$")
    public void que_he_introducido_nombre(final String nombre) throws Throwable {
        assertEquals("Carlos", nombre);
    }

    @Dado("^que he introducido apellidoPaterno \"([^\"]*)\"$")
    public void que_he_introducido_apellidoPaterno(final String apellidoPaterno) throws Throwable {
        assertEquals("Fuentes", apellidoPaterno);
    }

    @Dado("^que he introducido apellidoMaterno \"([^\"]*)\"$")
    public void que_he_introducido_apellidoMaterno(final String apellidoMaterno) throws Throwable {
        assertEquals("Diaz", apellidoMaterno);
    }

    @Cuando("^oprimo el \"([^\"]*)\"$")
    public void oprimo_el(final String boton) throws Throwable {
        assertEquals("boton", boton);
    }

    @Entonces("^el nombre del \"([^\"]*)\" se muestra en la pantalla$")
    public void el_nombre_del_se_muestra_en_la_pantalla(final String resultado) throws Throwable {
        // Express the Regexp above with the code you wish you had
        assertEquals("interno", resultado);
    }
}
