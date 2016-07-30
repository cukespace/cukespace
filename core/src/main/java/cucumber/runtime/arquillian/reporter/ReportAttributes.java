package cucumber.runtime.arquillian.reporter;

import java.util.Map;

public class ReportAttributes {

    private Map<String,Object> adocAtributes;
    private Map<String,Object> htmlAtributes;
    private Map<String,Object> pdfAtributes;

    public ReportAttributes() {
    }

    public Map<String, Object> getAdocAtributes() {
        return adocAtributes;
    }

    public void setAdocAtributes(Map<String, Object> adocAtributes) {
        this.adocAtributes = adocAtributes;
    }

    public Map<String, Object> getHtmlAtributes() {
        return htmlAtributes;
    }

    public void setHtmlAtributes(Map<String, Object> htmlAtributes) {
        this.htmlAtributes = htmlAtributes;
    }

    public Map<String, Object> getPdfAtributes() {
        return pdfAtributes;
    }

    public void setPdfAtributes(Map<String, Object> pdfAtributes) {
        this.pdfAtributes = pdfAtributes;
    }
}
