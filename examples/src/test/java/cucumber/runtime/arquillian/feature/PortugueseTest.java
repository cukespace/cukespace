package cucumber.runtime.arquillian.feature;

import cucumber.api.CucumberOptions;
import cucumber.api.java.pt.Dado;
import cucumber.api.java.pt.Entao;
import cucumber.api.java.pt.Quando;
import cucumber.runtime.arquillian.ArquillianCucumber;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(ArquillianCucumber.class)
@CucumberOptions(strict = true)
public class PortugueseTest {
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "portuguese.war").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static int calls = 0;

    @Dado("^O sistema possui o usuario 'admin' cadastrado$")
    public void O_sistema_possui_o_usuario_admin_cadastrado() throws Throwable {
        calls++;
    }

    @Quando("^O usuario preenche o login como 'admin' e a senha O botao de login é clicado$")
    public void O_usuario_preenche_o_login_como_admin_e_a_senha_O_botao_de_login_é_clicado() throws Throwable {
        calls++;
    }

    @Entao("^O usuário é redirecionado para a página de pesquisa de usuarios$")
    public void O_usuário_é_redirecionado_para_a_página_de_pesquisa_de_usuarios() throws Throwable {
        calls++;
    }

    @AfterClass
    public static void checkCalls() {
        assertEquals(3, calls);
    }

}
