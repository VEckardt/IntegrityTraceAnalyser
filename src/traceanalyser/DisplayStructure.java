/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import com.mks.api.Option;
import com.mks.api.response.APIException;
import com.mks.api.response.Field;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItem;
import com.mks.api.response.WorkItemIterator;
import com.ptc.services.common.api.Command;
import com.ptc.services.common.api.ExceptionHandler;
import static com.ptc.services.common.tools.StringUtils.normCharsOnly;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBuilder;
import traceanalyser.api.IntegrityCommands;

/**
 *
 * @author veckardt
 */
public class DisplayStructure extends Task<Void> {

    // Glboals
    private final Rectangle2D boxBounds = new Rectangle2D(100, 100, 200, 90);
    // Internal Variables
    private final LinkedList<String> list = new LinkedList<>();
    private final HashSet<String> uniqueIDs = new HashSet<>();
    private final HashSet<String> uniqueDocIDs = new HashSet<>();
    private final Map<String, WorkItem> uniqueWIs = new HashMap<>();
    private final Map<String, ItemCountInfo> relationships = new TreeMap<>();
    private final Map<String, ItemCountInfo> relationships2 = new LinkedHashMap<>();
    // Constructor Variables
    private final IntegrityCommands api;
    private final GridPane gridPane;
    private final Map<String, WorkItem> types; //  = new HashMap<>();
    private final String suspectId;
    private final CheckMenuItem mInsertBreakWhenTypeChanges;
    private final CheckMenuItem mOpenWithinGUI;
    private final Pane baseItemPane;

    // Constructor
    public DisplayStructure(IntegrityCommands api, GridPane gridPane, Map<String, WorkItem> types, String suspectId, CheckMenuItem mOpenWithinGUI, CheckMenuItem mInsertBreakWhenTypeChanges, Pane baseItemPane) {
        this.api = api;
        this.gridPane = gridPane;
        this.types = types;
        this.suspectId = suspectId;
        this.mInsertBreakWhenTypeChanges = mInsertBreakWhenTypeChanges;
        this.mOpenWithinGUI = mOpenWithinGUI;
        this.baseItemPane = baseItemPane;
    }

    public void initMap() {
        relationships.clear();
        relationships2.clear();
    }

    public void sortMap() {
        for (Map.Entry<String, ItemCountInfo> entry : relationships.entrySet()) {
            String relationship = entry.getKey().toString();
            if (relationship.contains(":")) {
                relationship = relationship.split(":")[0];
            }
            if (!api.fieldIsForward(relationship)) {
                relationships2.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, ItemCountInfo> entry : relationships.entrySet()) {
            String relationship = entry.getKey().toString();
            if (relationship.contains(":")) {
                relationship = relationship.split(":")[0];
            }
            if (api.fieldIsForward(relationship)) {
                relationships2.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void displayStructure(String startItemId) {
        list.add(startItemId);
        initMap();

        gridPane.getChildren().clear();

        ItemBaseInfo ibi = new ItemBaseInfo(api, startItemId);

        baseItemPane.getChildren().clear();

        try {
            // TraceAnalyser.stage.setTitle("Trade Details for "+mainType+" " + documentId);
            // get the associated type, which should be the "node" type

            WorkItem wi = api.getTypeDetails(ibi.getType(), "name,associatedType,documentClass");
            String assType = wi.getField("associatedType").getValueAsString();
            api.log(assType, 1);
            types.put(wi.getId(), wi);

            // get all visible fields from node
            wi = api.getTypeDetails(assType != null ? assType : ibi.getType(), "name,visibleFields,documentClass");
            String visibleFields = wi.getField("visibleFields").getValueAsString();
            types.put(wi.getId(), wi);
            // api.log(visibleFields, 1);

            // get all fields from type "relationship"
            api.readFields("relationship", visibleFields);

            String traceNames = "";
            for (String field : visibleFields.split(",")) {
                if (api.allFields.containsKey(field)) {
                    // api.log("Is relationship: " + field, 1);
                    traceNames = traceNames + (traceNames.isEmpty() ? "" : ",") + field;
                }
            }
            api.log("traceNames: " + traceNames, 1);

            String traceList = "";
            for (String trace2 : traceNames.split(",")) {
                traceList = traceList + (traceList.isEmpty() ? "" : " or ") + "(relationship.exists forwards using [" + trace2 + "])";
            }
            // api.log("traceList: " + traceList, 1);

            /* */
            WorkItemIterator wit = constructUniqueIdLists(startItemId, assType, traceNames);
            /* */

            // Iterator<String> is = types.keySet().iterator();
            // while (is.hasNext()) {
            //     System.out.println("Type in stack: " + is.next());
            // }

            int meaningfulCount = 0;
            while (wit.hasNext()) {
                WorkItem wid = wit.next();
                // api.log("In loop ...", 1);
                meaningfulCount++;

                // loop through all retrieved trace names
                for (String traceName : traceNames.split(",")) {
                    String relatedItems = wid.getField(traceName).getValueAsString().replace("?", "");
                    String relatedItemsSuspect = wid.getField(traceName).getValueAsString();
                    // api.log("In loop for field '" + field + "': " + relatedItems, 1);

                    // get the doc
                    if (!relatedItems.isEmpty()) {
                        // here we count the rows in the base document, and if this row has at least one related item

                        if (relationships.get(traceName) == null) {
                            relationships.put(traceName, new ItemCountInfo(0, 0, 0, 0));
                        }
                        ItemBaseInfo ibi3 = new ItemBaseInfo(wid);
                        relationships.put(traceName, new ItemCountInfo(relationships.get(traceName).getInt1() + 1,
                                relationships.get(traceName).getInt2() + (relatedItemsSuspect.contains("?") ? 1 : 0),
                                0,
                                0,
                                ibi3,
                                relationships.get(traceName).getItemListMap(wid.getId())));
                        // loop throught all related items, one by one
                        for (String relItem : relatedItemsSuspect.split(",")) {
                            //get from each related item get the type and doc id
                            String relItemId = relItem.replace("?", "");

                            String docID = uniqueWIs.get(relItemId).getField("Document ID").getValueAsString();
                            String type = uniqueWIs.get(relItemId).getField("Type").getValueAsString();
                            String category = uniqueWIs.get(relItemId).getField("Category").getValueAsString() + "";

                            // TODO: + "-" + (docID == null ? type : api.getItemType(docID))
                            ItemBaseInfo ibi2 = new ItemBaseInfo(uniqueWIs.get(relItemId));

                            // System.out.println("type: " + type);
                            if (docID != null) {
                                WorkItem wiDoc2 = uniqueWIs.get(docID); // , "Type,Summary,State,Assigned User");
                                ibi2.setInfo(wiDoc2);
                            }

                            String key = traceName + ":" + (docID == null ? type : ibi2.getType() + " (" + docID + ")");
                            if (relationships.get(key) == null) {
                                relationships.put(key, new ItemCountInfo(0, 0, 0, 0, ibi2));
                            }
                            relationships.put(key, new ItemCountInfo(relationships.get(key).getInt1() + 1,
                                    relationships.get(key).getInt2() + (relItem.contains("?") ? 1 : 0),
                                    relationships.get(key).getInt3() + (category.contains("Heading") || category.contains("Comment") ? 1 : 0),
                                    0,
                                    ibi2,
                                    relationships.get(key).getItemListMap(relItemId)));
                        }
                    }
                }
            }
//Spawns => 2/0
//Spawns:Defect => 15/0
//Tests For => 4/0
//Tests For:Test Session => 20/0
//Validates => 17/7
//Validates:Requirement Document (162) => 10/2
//Validates:Requirement Document (163) => 2/2
//Validates:Requirement Document (7869) => 4/1
//Validates:Requirement Document (7881) => 3/3
//Validates:Specification Document (441) => 4/0
//Validates:Specification Document (442) => 5/0
//Validates:Specification Document (444) => 6/0

            // baseItemPane.getChildren()
            // gridPane.add(getDocument(ibi.getType(), "", meaningfulCount + " meaningful nodes", new ItemCountInfo(0, 0, ibi), true),0,0);

            // mainInfo.setText(mainInfo.getText() + "\nwith " + meaningfulCount + " meaningful nodes");

            for (Map.Entry<String, ItemCountInfo> entry : relationships.entrySet()) {
                api.log(entry.getKey() + " ==> " + (entry.getValue()).getInt1() + "/" + (entry.getValue()).getInt2() + "/" + (entry.getValue()).getInt3(), 1);
            }

            // Step 1:
            // addNode(documentId, docType + "\n (" + documentId + ")", "", "text-size:18px; text-style:bold;");
            int relCount = -1;
            int colCount = 0;

            Boolean toBeShown = true;
            String lastKey = "";

            // Sort the list
            sortMap();

            System.out.println("Count in relationships2: " + relationships2.size());
            for (Map.Entry entry : relationships2.entrySet()) {
                // System.out.println("1");
                String key = entry.getKey().toString();
                ItemCountInfo value = (ItemCountInfo) entry.getValue();
                // System.out.println("2");
                if (!key.contains(":")) {
                    // System.out.println("3");
                    relCount++;
                    String relname = plural("node", value.getInt1());
                    if (api.fieldIsForward(key) && toBeShown) {
                        // System.out.println("5");
                        Node node = getDocument(ibi.getType(), "", meaningfulCount + " meaningful nodes", new ItemCountInfo(0, 0, 0, 0, ibi), true);
                        gridPane.add(node, 0, relCount);
                        relCount++;
                        toBeShown = false;
                    }
                    // System.out.println("Adding: getTrace");
                    gridPane.add(getTrace(key, relname, value, meaningfulCount), 0, relCount);
                    colCount = 0;
                    // relCount++;

                } else {
                    // System.out.println("4");
                    colCount++;
                    // addNode(key.split(":")[1], key.split(":")[1], "", "");
                    WorkItem wiField = api.allFields.get(key.split(":")[0]);
                    String relname = "relationship";
                    if (wiField != null) {
                        Field field = wiField.getField("trace");
                        if (field != null && field.getBoolean() != null && field.getBoolean()) {
                            relname = "trace";
                        }
                    }
                    relname = plural(relname, value.getInt1());

                    if (mInsertBreakWhenTypeChanges.isSelected()) {
                        if (colCount > 1 && !lastKey.isEmpty()) {
                            if (!lastKey.contentEquals(normCharsOnly(key.split(":")[1]))) {
                                colCount = 1;
                                relCount++;
                            }
                        }
                    }

                    // System.out.println("Adding: getDocument");
                    gridPane.add(getDocument(key.split(":")[1], relname, "", value, false), colCount, relCount);
                    lastKey = normCharsOnly(key.split(":")[1]);
                }
            }

            // gridPane.getChildren().clear();
            if (toBeShown) {
                relCount++;
                gridPane.add(getDocument(ibi.getType(), "", meaningfulCount + " meaningful nodes", new ItemCountInfo(0, 0, 0, 0, ibi), true), 0, relCount);
            }

            // ListIterator<Node> nodeList = gridPane.getChildren().listIterator();
            // while (nodeList.hasNext()) {
            //     Node node = nodeList.next();
            //     api.log(node.getId()+" "+node.layoutXProperty(),1);
            //     api.log(node.getId()+" "+node.getLayoutY(),1);
            // }

        } catch (APIException ex) {
            ExceptionHandler eh = new ExceptionHandler(ex);
            Logger.getLogger(TraceAnalyserController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            api.log(eh.getMessage(), 1);
        }
    }

    public WorkItemIterator constructUniqueIdLists(String startItemId, String assType, String traceNames) throws APIException {
        Command cmd;
        if (assType != null) {
            cmd = new Command(Command.IM, "viewsegment");
            // cmd.addOption("queryDefinition", "((field[Type] = \"" + "Requirement" + "\") and (field[Project] = \"" + queryProject + "\") and (" + traceList + "))");
            // cmd.addOption("filterQueryDefinition", "(" + traceList + ")");
            cmd.addOption("filterQueryDefinition", "(item.meaningful)");
        } else {
            cmd = new Command(Command.IM, "issues");
        }
        cmd.addOption("fields", "ID,Summary,Document ID,Assigned User,Type,State,Created Date,Category," + traceNames);
        cmd.addSelection(startItemId);
        Response resp = api.executeCmd(cmd);


        WorkItemIterator wit3 = resp.getWorkItems();
        if (assType != null) {
            wit3.next();
        }
        while (wit3.hasNext()) {
            WorkItem wid = wit3.next();
            // get all id's from all related fields
            for (String field : traceNames.split(",")) {
                // api.log("Checking field: " + field + " ...", 1);
                String relatedItems = wid.getField(field).getValueAsString().replace("?", "");
                if (!relatedItems.isEmpty()) {
                    for (String relItem : relatedItems.split(",")) {
                        // api.log("Field: " + field + " " + relItem, 10);
                        uniqueIDs.add(relItem);
                    }
                }
            }
        }

        // with these two commands we are seleting ALL relevant items and documents
        // it is the fastest way to get those data
        // retrieve all related items
        if (uniqueIDs.size() > 0) {
            Command cmd3 = new Command(Command.IM, "issues");
            for (String id : uniqueIDs) {
                cmd3.addSelection(id);
            }
            cmd3.addOption("fields", "Type,State,Summary,Assigned User,Category,Document ID,Created Date");
            Response resp3 = api.executeCmd(cmd3);
            WorkItemIterator witUnique = resp3.getWorkItems();
            while (witUnique.hasNext()) {
                WorkItem wi3 = witUnique.next();
                if (wi3.getField("Document ID").getValueAsString() != null) {
                    uniqueDocIDs.add(wi3.getField("Document ID").getValueAsString());
                }
                uniqueWIs.put(wi3.getId(), wi3);
                addTypeDetails(wi3.getField("Type").getValueAsString());
            }
            // now, retrieve all document items via uniqueDocIDs
            if (uniqueDocIDs.size() > 0) {
                Command cmd4 = new Command(Command.IM, "issues");
                for (String id : uniqueDocIDs) {
                    // System.out.println(id);
                    cmd4.addSelection(id);
                }
                cmd4.addOption("fields", "Type,Summary,State,Assigned User,Created Date");
                Response resp4 = api.executeCmd(cmd4);
                WorkItemIterator witUnique4 = resp4.getWorkItems();
                while (witUnique4.hasNext()) {
                    WorkItem wi4 = witUnique4.next();
                    uniqueWIs.put(wi4.getId(), wi4);
                    addTypeDetails(wi4.getField("Type").getValueAsString());
                }
            }
        }
        System.out.println("uniqueIDs.size(): " + uniqueIDs.size());
        System.out.println("uniqueDocIDs.size(): " + uniqueDocIDs.size());
        System.out.println("uniqueWIs.size(): " + uniqueWIs.size());

        WorkItemIterator wit = resp.getWorkItems();
        // skip over the document item itself
        if (assType != null) {
            wit.next();
        }
        return wit;

    }

    public String plural(String text, int count) {
        return text + (count > 1 ? "s" : "");
    }

    /**
     * Get a trace GUI Object, with pre-formatted elements, colours and text in
     * it
     */
    public HBox getTrace(String traceName, String relname, final ItemCountInfo itemInfo, int meaningfulCount) {

        Boolean isForward = api.fieldIsForward(traceName);
        WorkItem wiField = api.allFields.get(traceName);
        String displayName = wiField.getField("DisplayName").getValueAsString();
        String percent = String.format("%.0f", Float.valueOf(itemInfo.getInt1().toString()) / meaningfulCount * 100);
        String label1 = itemInfo.getInt1() + "/" + meaningfulCount + " " + relname;
        String label2 = "(" + percent + "%)";
        String label3 = (itemInfo.getInt2() > 0 ? "  (" + itemInfo.getInt2() + " " + suspectId + ")" : "");

        HBox hBox = new HBox();
        // hBox.setMaxWidth(70);
        // hBox.autosize();
        // hBox.setPrefSize(70, 70);
        // hBox.setMinWidth(100);
        // Pane stackPaneText = new Pane();
        // stackPaneText.setMaxWidth(50);
        Label traceText = LabelBuilder.create().text(displayName).font(Font.font("Arial", 16)).build();
        // traceBox.setStyle("-fx-insets: 10 10 1 0;");
        // traceBox..setPadding(new Insets(10, 50, 50, 50));
        // traceBox.setRotate(90);
        traceText.setStyle("-fx-padding: 2px;");
        // item.set
        // traceText.setFontSmoothingType(FontSmoothingType.LCD);
        // stackPaneText.getChildren().add(item);

        final StackPane stackPane = new StackPane();
        // topPane.setMaxWidth(10);
        stackPane.setStyle("-fx-border-width:0px 0px 0px 0px;-fx-border-style:solid;-fx-border-color:#999999;");
        // "#FFFF00"
        final Rectangle rect = RectangleBuilder.create().width(boxBounds.getWidth()).height(90).fill(getGradient("#f6f6f6", isForward)).build();
        // Tooltip t = new Tooltip("A Square");
        rect.setArcHeight(20);
        rect.setArcWidth(20);
        // Tooltip.install(rect, t);
        Label statusText1 = LabelBuilder.create().text(label1).font(Font.font("Arial", 16)).build();
        Label statusText2 = LabelBuilder.create().text(label2).font(Font.font("Arial", 14)).build();
        Label statusText3 = LabelBuilder.create().text(label3).font(Font.font("Arial", 16)).build();
        if (label3.contains(suspectId)) {
            // statusBox.setFill(Color.RED);
            statusText3.setTextFill(Color.web("#E00000"));
        }
        // http://stackoverflow.com/questions/25042517/javafx-2-resizable-rectangle-containing-text
        // statusText1.setStyle("-fx-padding: 2px;");
        // statusText2.setStyle("-fx-padding: 3px;");
        // statusText3.setStyle("-fx-padding: 2px;");
        statusText1.setTextAlignment(TextAlignment.CENTER);

        // text.setTextOrigin(VPos.BOTTOM);
        // text.setTextAlignment(TextAlignment.CENTER);
        // statusBox.setFontSmoothingType(FontSmoothingType.LCD);
        HBox hb2 = new HBox(5);
        hb2.getChildren().addAll(statusText1, statusText2);
        hb2.setAlignment(Pos.CENTER);

        VBox hb = new VBox(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(new Label(" " + (isForward ? "\u2193" : "\u2191") + "  " + displayName), hb2, statusText3);
        // hb.setSpacing(50);
        // hb.setPadding(new Insets(0, 0, 50, 0));

        stackPane.getChildren().addAll(rect, hb); // , stackPaneText);

        // hBox.setStyle(status);
        final String className = itemInfo.getClassDisplayName(types);
        Tooltip t = new Tooltip(itemInfo.getType() + "\nClass: " + className + itemInfo.getItemListAsString("\nItems: ", 15));
        Tooltip.install(stackPane, t);

        final ContextMenu cm = new ContextMenu();
        MenuItem cmItem1 = new MenuItem("Open Traced Items");
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                // Clipboard clipboard = Clipboard.getSystemClipboard();
                // ClipboardContent content = new ClipboardContent();
                // content.putString("Data");
                // clipboard.setContent(content);
                openTracedItems(itemInfo);
            }
        });
        MenuItem cmItem2 = new MenuItem("Copy Traced Item Id's to Clipboard");
        cmItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(itemInfo.getItemListAsString());
                clipboard.setContent(content);
            }
        });

        cm.getItems().add(cmItem1);
        cm.getItems().add(cmItem2);
        stackPane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                if (e.getButton() == MouseButton.SECONDARY) {
                    cm.show(stackPane, e.getScreenX(), e.getScreenY());
                }
            }
        });

        stackPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (itemInfo.getId() != null && !itemInfo.getId().isEmpty()) {
                    if (event.getButton().toString().startsWith("SECONDARY")) {
                        // displayStructure(itemInfo.getId());
                    } else {
                        // System.out.println(event.getButton() + " Mouse setOnMouseClicked" + event.getTarget().toString() + ", " + itemInfo.getId());
                        openTracedItems(itemInfo);
                    }
                }
                event.consume();
            }
        });

        // hBox.getChildren().addAll(stackPane, statusBox);
        hBox.getChildren().addAll(stackPane);
        hBox.setId("Trace");
        return hBox;
    }

    /**
     * Get a document GUI Object, with pre-formatted elements, colours and text
     * in it
     */
    private VBox getDocument(final String type, final String relname, final String addTextString, final ItemCountInfo itemInfo, final Boolean isBaseItem) {
        VBox vBox = new VBox();
        // hBox.
        // vBox.setMaxHeight(50);
        // vBox.setMaxWidth(50);
        // vBox.setPrefSize(20, 20);
        // hBox.setStyle("-fx-border-width:1px;-fx-border-style:solid;-fx-border-color:#999999;");
        // System.out.println("Type: " + type + ": color = " + getColorByName(type));


        String topText1 = "";
        String topText2 = "";
        String midText;
        String idText = "";
        String botText = "multiple";
        if (itemInfo.getInt1() > 0) {

            if (itemInfo.getInt1() != itemInfo.getItemListCount()) {
                topText1 = itemInfo.getInt1() + " " + relname + "/ " + itemInfo.getItemListCount() + plural(" node", itemInfo.getItemListCount());
            } else {
                topText1 = itemInfo.getInt1() + " " + relname;
            }
            topText2 = (itemInfo.getInt2() > 0 ? " (" + itemInfo.getInt2() + ")" : ""); // suspectId
        }

        if ((itemInfo.getInt1() > 1) && (!type.contains("("))) {
            // multiple items
            midText = (itemInfo.getInt1() > 1 ? "" : itemInfo.getId() + ": ") + plural(type, itemInfo.getInt1());
        } else {
            // sigle item
            midText = itemInfo.getShortSummary();
            idText = itemInfo.getId();
            botText = (type.indexOf("(") > 0 ? type.substring(0, type.indexOf("(")) : type);
        }
        Text itemIdText = TextBuilder.create().text(idText).font(Font.font("Arial", 14)).build();
        itemIdText.setTextAlignment(TextAlignment.CENTER);

        Text bottomText = TextBuilder.create().text(botText).font(Font.font("Arial", 12)).build();
        // bottomText.setStyle("-fx-padding: 3px 3px 3px; ");
        bottomText.setTextAlignment(TextAlignment.CENTER);

        StackPane stackPaneText = new StackPane();
        Text item = TextBuilder.create().text(midText).wrappingWidth(185).font(Font.font("Arial", FontWeight.BOLD, 16)).build();
        // item.setStyle("-fx-padding: 3px;");
        item.setTextAlignment(TextAlignment.CENTER);
        item.setFontSmoothingType(FontSmoothingType.LCD);

        Text addText = TextBuilder.create().text("  " + addTextString).font(Font.font("Arial", 14)).build();
        addText.setTextAlignment(TextAlignment.CENTER);
        addText.setFontSmoothingType(FontSmoothingType.LCD);

        VBox vb = new VBox();
        // vb.setPadding(new Insets(10, 50, 50, 50));
        vb.getChildren().addAll(bottomText, item, itemIdText);
        vb.setAlignment(Pos.CENTER);
        stackPaneText.getChildren().addAll(vb);

        // topPane.setStyle("-fx-border-width:1px;-fx-border-style:solid;-fx-border-color:#999999;");
        Rectangle rect = RectangleBuilder.create().width(boxBounds.getWidth()).height(boxBounds.getHeight()).fill(getGradient(getColorByName(type), null)).build();
        rect.setArcHeight(20);
        rect.setArcWidth(20);
        rect.setStroke(Color.DARKGRAY);
        if (isBaseItem) {
            rect.setStroke(Color.DARKRED);
            rect.setStrokeWidth(4);
        }
        // rect.setStyle("-fx-border-width:1px;-fx-border-style:solid;-fx-border-color:#FFFFFF;");

        StackPane topPane = new StackPane();
        topPane.getChildren().addAll(rect, stackPaneText);

        Text text1 = TextBuilder.create().text(" " + topText1).font(Font.font("Arial", 14)).build();
        Text text2 = TextBuilder.create().text(" " + topText2).font(Font.font("Arial", 14)).build();
        text2.setFill(Color.RED);
        text1.setFontSmoothingType(FontSmoothingType.LCD);
        text2.setFontSmoothingType(FontSmoothingType.LCD);
        vBox.setAlignment(Pos.CENTER);

        Image image = new Image(getClass().getResourceAsStream("resources/warn.png"));
        ImageView iv1 = new ImageView();
        iv1.setImage(image);
        // iv1.setStyle("-fx-padding: 2px 2px 2px 2px;-fx-spacing: 8;");

        HBox topText = new HBox();
        if (itemInfo.hasTrouble()) {
            topText.getChildren().addAll(text1, text2, new Label("   "), iv1);
            Tooltip.install(iv1, new Tooltip("Attention: One or more non-meaningful traces!"));

        } else {
            topText.getChildren().addAll(text1, text2);
        }
        if (!topText2.isEmpty()) {
            Tooltip.install(topText, new Tooltip("Hint: Document references suspect nodes!"));
        }

        if (topText1.isEmpty()) {
            vBox.getChildren().addAll(topPane, addText); // , text2);
        } else {
            vBox.getChildren().addAll(topText, topPane, addText); // , text2);
        }
        final String className = itemInfo.getClassDisplayName(types);

        Tooltip tt = new Tooltip(itemInfo.getInfo("\n") + "\nClass: " + className + itemInfo.getItemListAsString("\nItems: ", 15));
// if (1==1) return vBox;
        Tooltip.install(topPane, tt);
        // final String typeName = "Item";


        /*
         * topPane.setOnMouseEntered(
         new EventHandler<MouseEvent>() {
         @Override
         public void handle(MouseEvent event) {
         // if (event.isSynthesized()) {
         // System.out.println("Mouse pressed" + event.getTarget().toString());
         infoPromptVBox.setVisible(true);
         infoPrompt1.setText(className + " " + itemInfo.getId() + ":");
         infoPrompt2.setText("Assigned User:  ");
         infoPrompt3.setText("Summary:");
         infoText1.setText(type + " in state " + itemInfo.getState());
         infoText2.setText(itemInfo.getAssignedUser());
         // infoText2.setFill(textAbove.contains("susp") ? Color.RED : Color.BLACK);
         infoText3.setText(itemInfo.getSummary());
         // }
         event.consume();
         }
         }); */

        topPane.setOnMouseClicked(
                new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (itemInfo.getId() != null && !itemInfo.getId().isEmpty()) {

                    if (event.getButton().toString().startsWith("SECONDARY")) {
                        displayStructure(itemInfo.getId());
                    } else {
                        System.out.println(event.getButton() + " Mouse setOnMouseClicked" + event.getTarget().toString() + ", " + itemInfo.getId());
                        openTracedItems(itemInfo);

                    }
                }
                event.consume();
            }
        });

        /*
         * topPane.setOnMouseExited(new EventHandler<MouseEvent>() {
         @Override
         public void handle(MouseEvent event) {
         // if (event.isSynthesized()) {
         // System.out.println("Mouse pressed"                        + event.getTarget().toString());
         infoPromptVBox.setVisible(false);
         infoText1.setText(infoMsg);
         infoText2.setText(" ");
         infoText3.setText(" ");
         // }
         event.consume();
         }
         }); */

        vBox.setId("Document" + vBox.getLayoutX());
        return vBox;
    }

    private void addTypeDetails(String typeName) throws APIException {
         if (!types.containsKey(typeName)) {
             types.put(typeName, api.getTypeDetails(typeName, "name,documentClass"));
         }
    }

    public void openTracedItems(ItemCountInfo itemInfo) {
        Command cmd;
        String className = itemInfo.getClassDisplayName(types);

        if (itemInfo.getItemListCount() > 1 || (className.contentEquals("Document")) && itemInfo.getItemListCount() > 0) {
            cmd = new Command(Command.IM, "issues");
            // simple version, we just show the items
            // --queryDefinition="(field[ID]=639) or (field[ID]=637)"
            // advanced version, we show the 
            // (relationship["Document ID"] forwards using ["Decomposes To"] = 162)
            String queryDefinition = "";
            for (String key : itemInfo.getItemListMap().keySet()) {
                queryDefinition = queryDefinition + (queryDefinition.isEmpty() ? "" : " or ") + "(field[ID]=" + key + ")";
            }
            queryDefinition = "(" + queryDefinition + ")";
            cmd.addOption(new Option("queryDefinition", queryDefinition));
            // Columns to display
            if (className.contentEquals("Document") || className.contentEquals("Node")) {
                cmd.addOption("fields", "ID,Category,Text,State,Assigned User,Trace Status");
            } else {
                cmd.addOption("fields", "ID,Category,Summary,State,Assigned User,Trace Status");
            }

        } else if (itemInfo.getItemListCount() > 0) {
            cmd = new Command(Command.IM, "viewissue");
            cmd.addSelection(itemInfo.getItemListAsString());
        } else {
            if (className.contentEquals("Document")) {
                cmd = new Command(Command.IM, "viewsegment");
                cmd.addSelection(itemInfo.getId());
            } else {
                cmd = new Command(Command.IM, "viewissue");
                cmd.addSelection(itemInfo.getId());
            }
        }

        //if (className.contentEquals("Document")) {
        //    cmd = new Command(Command.IM, "viewsegment");
        //} else {
        //    cmd = new Command(Command.IM, "viewissue");
        //}
        cmd.addOption("gui");
        if (mOpenWithinGUI.isSelected()) {
            cmd.addOption("tvigui");
        }
        // cmd.addOption("filterRule", "(not (field[Trace Status] = \"none\"))");
        try {
            Response resp = api.executeCmd(cmd);
        } catch (APIException ex) {
            Logger.getLogger(TraceAnalyserController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public LinearGradient getGradient(String color, Boolean direction) {
        Stop[] stops2 = new Stop[]{new Stop(0, Color.web("#FFFFFF")), new Stop(1, Color.web(color))};
        if (direction != null) {
            if (!direction) {
                return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops2);
            } else {
                return new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, stops2);
            }
        }
        return new LinearGradient(1, 0, 0, 0, true, CycleMethod.NO_CYCLE, stops2);
    }

    public String getColorByName(String typeName) {
        String color = "#E1A95F";
        if (typeName.contains("Req")) {
            color = "#FF6633";
        } else if (typeName.contains("Test")) {
            color = "#ADD8E6";
        } else if (typeName.contains("Spec")) {
            color = "#90EE90";
        } else if (typeName.contains("Input")) {
            color = "#CC6699";
        }
        return color;
    }

    @Override
    protected Void call() {
        displayStructure("618");
        return null;
    }
}
