/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import com.mks.api.response.WorkItem;
import com.ptc.services.common.api.IntegrityAPI;

/**
 *
 * @author veckardt
 */
public class ItemBaseInfo {

    private String summary = "";
    private String assignedUser = "";
    private String state = "";
    private String type = "";
    private String displayId = "";
    private String id = "";
    private String docClass = "";
    private String createdDate = "";
    private int shortSummaryLength = 37;

    public ItemBaseInfo() {
    }

    ItemBaseInfo(String type, String summary, String state, String id, String assignedUser, String createdDate) {
        this.type = type;
        this.summary = summary;
        this.state = state;
        this.id = id;
        this.assignedUser = assignedUser;
        this.createdDate = createdDate;
    }

    void setInfo(WorkItem wi) {
        this.summary = wi.getField("Summary").getString();
        this.assignedUser = wi.getField("Assigned User").getValueAsString();
        assignedUser = (assignedUser == null ? "-" : assignedUser);

        this.state = wi.getField("State").getValueAsString();
        this.type = wi.getField("Type").getValueAsString();
        this.createdDate = wi.getField("Created Date").getDateTime().toString();
        this.id = wi.getId();
        this.displayId = wi.getId();
        if (wi.contains("Live Item ID") && !wi.getField("Live Item ID").getValueAsString().contentEquals(wi.getId())) {
            this.id = wi.getField("Live Item ID").getValueAsString();
            try {
                if (wi.getField("Major Version ID") != null) {
                    this.id = this.id + "-" + getIntValueAsString(wi, "Major Version ID") + "." + getIntValueAsString(wi, "Minor Version ID");
                }
            } catch (NullPointerException ex) {
            }
        }
        System.out.println("this.id => " + this.id);
    }

    private String getIntValueAsString(WorkItem wi, String fieldName) {
        String result = wi.getField(fieldName).getInteger().toString();
        return result;
    }

    // this.docClass = api.getDocumentClass(type);
    public ItemBaseInfo(WorkItem wi) {
        setInfo(wi);
        // this.docClass = api.getDocumentClass(type);
    }

    public ItemBaseInfo(IntegrityAPI api, String itemId) {
        WorkItem wi = api.getWorkItem(itemId, "Type,Summary,State,Assigned User,Created Date,Live Item ID,Minor Version ID,Major Version ID");
        setInfo(wi);
        // this.docClass = api.getDocumentClass(type);
    }

    public String getSummary() {
        return this.summary;
    }

    public String getShortSummary() {
        return (summary == null || summary.isEmpty() ? "" : summary.substring(0, (summary.length() > shortSummaryLength ? shortSummaryLength : summary.length())) + (summary.length() > shortSummaryLength ? " ..." : ""));
    }

    public String getAssignedUser() {
        return this.assignedUser;
    }

    public String getState() {
        return this.state;
    }

    public String getType() {
        return this.type;
    }

    public String setType(String type) {
        return this.type = type;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public String getInfo(String delimiter) {
        if (getId() == null || getId().isEmpty()) {
            return "Multiple Items";
        }
        return "ID: " + getId() + delimiter + "Type: " + getType() + delimiter + "Summary: " + getSummary()
                + delimiter + "State: " + getState() + delimiter + "Assigned User: " + getAssignedUser() + delimiter + "Created Date: " + createdDate;
    }
}
