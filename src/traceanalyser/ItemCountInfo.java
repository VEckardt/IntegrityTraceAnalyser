/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import com.mks.api.response.WorkItem;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author veckardt
 */
public class ItemCountInfo extends ItemBaseInfo {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    // private String itemList = "";
    LinkedHashMap<String, String> itemList;

    public ItemCountInfo(Integer i1, Integer i2, Integer i3, Integer i4) {
        super();
        this.itemList = new LinkedHashMap<>();
        this.int1 = i1;
        this.int2 = i2;
        this.int3 = i3;
        this.int4 = i4;
    }

    public ItemCountInfo(Integer i1, Integer i2, Integer i3, Integer i4, ItemBaseInfo ibi) {
        super(ibi.getType(), ibi.getSummary(), ibi.getState(), ibi.getId(), ibi.getAssignedUser(), ibi.getCreatedDate());
        this.itemList = new LinkedHashMap<>();
        this.itemList = new LinkedHashMap<>();
        this.int1 = i1;
        this.int2 = i2;
        this.int3 = i3;
        this.int4 = i4;
    }

    public ItemCountInfo(Integer i1, Integer i2, Integer i3, Integer i4, ItemBaseInfo ibi, LinkedHashMap<String, String> itemList) {
        super(ibi.getType(), ibi.getSummary(), ibi.getState(), ibi.getId(), ibi.getAssignedUser(), ibi.getCreatedDate());
        this.itemList = new LinkedHashMap<>();
        this.itemList = itemList;
        this.int1 = i1;
        this.int2 = i2;
        this.int3 = i3;
        this.int4 = i4;
    }

    // private ItemCountInfo(Integer i1, Integer i2, String type, String summary, String state, String id, String assignedUser, String createdDate) {
    //     super(type, summary, state, id, assignedUser, createdDate);
    //     this.int1 = i1;
    //     this.int2 = i2;
    // }
    public Integer getInt1() {
        return int1;
    }

    public Integer getInt2() {
        return int2;
    }

    public Integer getInt3() {
        return int3;
    }

    public Integer getInt4() {
        return int4;
    }

    public String getItemListAsString() {
        String list = "";
        for (String entry : itemList.keySet()) {
            list = list + (list.isEmpty() ? "" : " ") + entry;
        }
        return list;
    }

    public String getItemListAsString(String prefix, int maxLen) {
        String list = "";
        int i = 0;
        for (String entry : itemList.keySet()) {
            i++;
            if (i < maxLen) {
                list = list + (list.isEmpty() ? "" : " ") + entry;
            }
        }
        return (i > 0 ? prefix : "") + " " + list + (itemList.size() > maxLen ? " ..." : "");
    }

    public String getClassDisplayName(Map<String, WorkItem> types) {
        try {
            String docClass = types.get(getType()).getField("documentClass").getValueAsString();
            return (docClass.contentEquals("segment") ? "Document" : (docClass.contentEquals("node") ? "Node" : "Item"));
        } catch (Exception ex) {
            return "";
        }
    }

    public LinkedHashMap<String, String> getItemListMap(String item) {
        itemList.put(item, item);
        return (itemList);
    }

    public LinkedHashMap<String, String> getItemListMap() {
        return (itemList);
    }

    public int getItemListCount() {
        return itemList.size();
    }

    public boolean hasTrouble() {
        return int3 > 0;
    }
}
