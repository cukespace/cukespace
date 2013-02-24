package cucumber.runtime.arquillian.test;

import cucumber.runtime.arquillian.controller.BellyController;
import cucumber.runtime.arquillian.domain.Belly;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import static cucumber.runtime.arquillian.controller.BellyController.MSG_ERROR_HUNGRY;
import static cucumber.runtime.arquillian.controller.BellyController.MSG_SUCCESS_NOT_HUNGRY;
import static java.text.MessageFormat.format;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class BellyControllerTest {
    @Test
    public void shouldBeHungry() {
        FacesContext facesContext = mock(FacesContext.class);
        BellyController controller = new BellyController(facesContext, new Belly());
        MessageAnswer answer = new MessageAnswer();
        doAnswer(answer).when(facesContext).addMessage(anyString(), any(FacesMessage.class));
        controller.eatCukes();
        assertThat(answer.getMessage(), equalTo(MSG_ERROR_HUNGRY));
    }

    @Test
    public void shouldNotBeHungry() {
        FacesContext facesContext = mock(FacesContext.class);
        Belly belly = new Belly();
        BellyController controller = new BellyController(facesContext, belly);
        MessageAnswer answer = new MessageAnswer();
        doAnswer(answer).when(facesContext).addMessage(anyString(), any(FacesMessage.class));
        belly.setCukes(3);
        controller.eatCukes();
        assertThat(answer.getMessage(), equalTo(format(MSG_SUCCESS_NOT_HUNGRY, 3)));
    }

    private static class MessageAnswer implements Answer<Object> {
        private String message;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            message = ((FacesMessage) invocation.getArguments()[1]).getDetail();
            return null;
        }

        public String getMessage() {
            return message;
        }
    }
}
