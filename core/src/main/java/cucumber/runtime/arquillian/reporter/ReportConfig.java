package cucumber.runtime.arquillian.reporter;

import java.io.Serializable;

/**
 * Created by pestano on 12/07/15.
 */
public class ReportConfig implements Serializable {

    private String directory = "target/cucumber-report/";
    private String[] formats = new String[]{"html5"};
    private ReportAttributes reportAttributes = new ReportAttributes();
    private String fileName = "report"; //report output filename

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String[] getFormats() {
        return formats;
    }

    public void setFormats(String[] formats) {
        this.formats = formats;
    }

    public ReportAttributes getReportAttributes() {
        return reportAttributes;
    }

    public void setReportAttributes(ReportAttributes reportAttributes) {
        this.reportAttributes = reportAttributes;
    }

    public boolean hasAdocConfig() {
        return reportAttributes != null &&
                reportAttributes.getAdocAtributes() != null &&
                !reportAttributes.getAdocAtributes().isEmpty();
    }

    public boolean hasHtmlConfig() {
        return reportAttributes != null &&
                reportAttributes.getHtmlAtributes() != null &&
                !reportAttributes.getHtmlAtributes().isEmpty();
    }

    public boolean hasPdfConfig() {
        return reportAttributes != null &&
                reportAttributes.getPdfAtributes() != null &&
                !reportAttributes.getPdfAtributes().isEmpty();
    }

    public Object getAdocAttribute(String key) {
        return hasAdocConfig() ? getReportAttributes().getAdocAtributes().get(key) : null;
    }

    public Object getHtmlAttribute(String key) {
        return hasHtmlConfig() ? getReportAttributes().getHtmlAtributes().get(key) : null;
    }

    public Object getPdfAttribute(String key) {
        return hasPdfConfig() ? getReportAttributes().getPdfAtributes().get(key) : null;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
