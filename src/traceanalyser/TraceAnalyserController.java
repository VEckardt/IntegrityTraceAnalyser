/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import com.mks.api.response.APIException;
import com.mks.api.response.Field;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItem;
import com.mks.api.response.WorkItemIterator;
import com.ptc.services.common.api.Command;
import com.ptc.services.common.api.ExceptionHandler;
import com.ptc.services.common.api.IntegrityMessages;
import static com.ptc.services.common.tools.StringUtils.normCharsOnly;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBuilder;
import jfx.messagebox.MessageBox;
import static traceanalyser.TraceAnalyserController2.api;
import static traceanalyser.TraceAnalyserController2.plural;

/**
 *
 * @author veckardt
 */
public class TraceAnalyserController extends TraceAnalyserController2 implements Initializable {

    @FXML
    private CheckMenuItem mOpenWithinGUI;
    @FXML
    private CheckMenuItem mInsertBreakWhenTypeChanges;
    @FXML
    private CheckMenuItem mDisplayProject;
    @FXML
    private GridPane gridPane;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Pane baseItemPane;
    IntegrityMessages MC = new IntegrityMessages(TraceAnalyser.class);
    // 
    private static LinkedList<String> list = new LinkedList<>();
    private static ItemBaseInfo ibi;
    private static int meaningfulCount;

    public void clearBaseItemPane() {
        baseItemPane.getChildren().clear();
    }

    @FXML
    private void bAbout(ActionEvent event) {
        MessageBox.show(TraceAnalyser.stage,
                Copyright.getCopyright(),
                "About", MessageBox.OK);
    }

    @FXML
    private void bClose(ActionEvent event) {
        System.exit(0);
    }

    @FXML
    private void bMoveBackwards(ActionEvent event) {
        list.removeLast();
        display(list.getLast());
    }

    @FXML
    private void mOptionsSave(ActionEvent event) {
        props.setProperty("OpenWithinGUI", Boolean.toString(mOpenWithinGUI.isSelected()));
        props.setProperty("InsertBreakWhenTypeChanges", Boolean.toString(mInsertBreakWhenTypeChanges.isSelected()));
        props.setProperty("DisplayProject", Boolean.toString(mDisplayProject.isSelected()));
        if (props.saveProperties()) {
            log("Property file " + props.getPropFile() + " saved.", 2);
        } else {
            log("Property file " + props.getPropFile() + " not saved.", 2);
        }
    }

    /**
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        mOpenWithinGUI.setSelected(Boolean.getBoolean(props.getProperty("OpenWithinGUI", "false")));
        mInsertBreakWhenTypeChanges.setSelected(Boolean.getBoolean(props.getProperty("InsertBreakWhenTypeChanges", "false")));

        // log (props.getProperty("DisplayProject", "false"),2);
        mDisplayProject.setSelected(Boolean.getBoolean(props.getProperty("DisplayProject", "false")));

        String documentId;
        // 
        // infoPromptVBox.setVisible(false);
        // infoText1.setText(infoMsg);
        // infoText2.setText(" ");
        // infoText3.setText(" ");
        //

        // api = new IntegrityCommands();
        if (api != null) {

            if ((api.getEnv("MKSSI_DOCUMENT") == null || api.getEnv("MKSSI_DOCUMENT").isEmpty()) && !(api.getEnv("MKSSI_PORT") == null)) {
                // MessageBox.show(TraceAnalyser.stage,
                //         MC.getMessage("EXACTLY_ONE_DOCUMENT"),
                //         "Start Option", MessageBox.OK);
                // System.exit(1);
                documentId = api.getEnv("MKSSI_ISSUE0");
            } else {
                documentId = api.getEnv("MKSSI_DOCUMENT");
                if (documentId == null) {
                    documentId = "14056";
                    // documentId = "539";
                    // documentId = "14053-1.0";
                    // documentId = "5531";
                }
            }

            // props = new ApplicationProperties(TraceAnalyser.class, api);
            api.log("documentId = " + documentId, 1);
            // ToDo: displayStructure(documentId);
            display(documentId);

        } else {
            MessageBox.show(TraceAnalyser.stage,
                    MC.getMessage("AT_LEAST_ONE_ITEM"),
                    "Start Option",
                    MessageBox.ICON_ERROR | MessageBox.OK);
        }
    }
    // Task copyWorker;
    Task task;

    /**
     *
     * @param itemId
     */
    public void display(String itemId) {
        if (false) {
            task = createWorker(itemId);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(new EventHandler() {
                @Override
                public void handle(Event t) {

                    // if (client.getAction() == FirstToAct.me) {
                    Platform.setImplicitExit(false);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            // progressBar.setProgress(-1);
                            // showOptions(client.getToCall());
                            displayMap();
                            // opponentBetField.setText(new Integer(opp.chipsInvested).toString());
                            // myBetField.setText(new Integer(client.chipsInvested).toString());
                            // progressBar.setProgress(1);
                            progressBar.progressProperty().unbind();
                            progressBar.setProgress(1);
                            task.cancel();
                            task = null;
                        }
                    });
                }
            });

            new Thread(task).start();
            // thread.setDaemon(true);
            // thread.start();
        } else {
            fillMap(itemId);
            displayMap();
        }
    }

    /**
     *
     * @param itemId
     * @return
     */
    public Task createWorker(final String itemId) {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                // for (int i = 0; i < 10; i++) {
                // Thread.sleep(2000);
                fillMap(itemId);
                // updateMessage("2000 milliseconds");
                //  updateProgress(i + 1, 10);
                // }
                return true;
            }
        };
    }
//    private void display(String itemId) {
//        DisplayStructure myTask = new DisplayStructure(api, gridPane, types, suspectId, mOpenWithinGUI, mInsertBreakWhenTypeChanges);
//
//        if (false) {
//            progressBar.progressProperty().unbind();
//            progressBar.setProgress(0);
//            progressBar.progressProperty().bind(myTask.progressProperty());
//
//            Thread myTaskThread = new Thread(myTask);
//            myTaskThread.start();
//        } else {
//            myTask.displayStructure(itemId);
//        }
//    }
    // Glboals
    // Internal Variables
    // private LinkedList<String> list = new LinkedList<>();
    private static HashSet<String> uniqueIDs = new HashSet<>();
    private static HashSet<String> uniqueDocIDs = new HashSet<>();
    private static Map<String, WorkItem> uniqueWIs = new HashMap<>();
    private static Map<String, ItemCountInfo> relationships = new TreeMap<>();
    private static Map<String, ItemCountInfo> relationships2 = new LinkedHashMap<>();

    public static void initMap() {
        relationships.clear();
        relationships2.clear();
    }

    /**
     *
     */
    public static void sortMap() {
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

    /**
     *
     * @param startItemId
     */
    public void fillMap(String startItemId) {
        // progressBar.setProgress(-1);
        list.add(startItemId);
        initMap();

        gridPane.getChildren().clear();

        // ItemBaseInfo 
        ibi = new ItemBaseInfo(api, startItemId);

        // baseItemPane.getChildren().clear();
        clearBaseItemPane();

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

            // String traceList = "";
            // for (String trace2 : traceNames.split(",")) {
            //     traceList = traceList + (traceList.isEmpty() ? "" : " or ") + "(relationship.exists forwards using [" + trace2 + "])";
            // }
            // api.log("traceList: " + traceList, 1);

            /* */
            WorkItemIterator wit = constructUniqueIdLists(startItemId, assType, traceNames);
            /* */

            // Iterator<String> is = types.keySet().iterator();
            // while (is.hasNext()) {
            //     System.out.println("Type in stack: " + is.next());
            // }
            meaningfulCount = 0;
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

                        // Entry 1: With Trace Name Only
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
                            //get from each related item the type and doc id
                            String relItemId = relItem.replace("?", "");
                            System.out.println("relItemId: "+relItemId);

                            String docID = null;
                            if (uniqueWIs.get(relItemId) != null) {
                                docID = uniqueWIs.get(relItemId).getField("Document ID").getValueAsString();
                            // }
                            String type = uniqueWIs.get(relItemId).getField("Type").getValueAsString();
                            String category = uniqueWIs.get(relItemId).getField("Category").getValueAsString() + "";

                            // TODO: + "-" + (docID == null ? type : api.getItemType(docID))
                            ItemBaseInfo ibi2 = new ItemBaseInfo(uniqueWIs.get(relItemId));

                            // System.out.println("type: " + type);
                            if (docID != null) {
                                WorkItem wiDoc2 = uniqueWIs.get(docID); // , "Type,Summary,State,Assigned User");
                                ibi2.setInfo(wiDoc2);
                            }

                            // Entry with Trace Name and : + Document Type + (ID) as Target    
                            String key = traceName + ":" + (docID == null ? type : ibi2.getType() + " (" + docID + ")");
                            if (relationships.get(key) == null) {
                                relationships.put(key, new ItemCountInfo(0, 0, 0, 0, ibi2));
                            }
                            // Value 1 = Item Counter
                            // Value 2 = Suspect Counter
                            // Value 3 = non-meaningful Counter
                            relationships.put(key, new ItemCountInfo(relationships.get(key).getInt1() + 1,
                                    relationships.get(key).getInt2() + (relItem.contains("?") ? 1 : 0),
                                    relationships.get(key).getInt3() + (category.contains("Heading") || category.contains("Comment") ? 1 : 0),
                                    // this is not correct !!!
                                    relationships.get(key).getInt4() + (relatedItemsSuspect.contains("?") ? 1 : 0),
                                    ibi2,
                                    relationships.get(key).getItemListMap(relItemId)));
                        }
                    }
                    }
                }
            }
//Decomposed From ==> 9/3/0/0
//Decomposed From:Input Document (111) ==> 6/3/0/3
//Decomposed From:Input Document (642) ==> 1/0/0/1
//Decomposed From:Requirement Document (162) ==> 3/0/0/0
//Satisfied By ==> 12/0/0/0
//Satisfied By:Specification Document (2473) ==> 13/0/0/0
//Satisfied By:Specification Document (441) ==> 4/0/0/0
//Satisfied By:Specification Document (442) ==> 2/0/0/0
//Satisfied By:Specification Document (443) ==> 1/0/0/0
//Satisfied By:Specification Document (444) ==> 5/0/0/0
//Validated By ==> 2/0/0/0
//Validated By:Test Suite (1496) ==> 1/0/0/0
//Validated By:Test Suite (539) ==> 2/0/0/0

            // baseItemPane.getChildren()
            // gridPane.add(getDocument(ibi.getType(), "", meaningfulCount + " meaningful nodes", new ItemCountInfo(0, 0, ibi), true),0,0);
            // mainInfo.setText(mainInfo.getText() + "\nwith " + meaningfulCount + " meaningful nodes");
            for (Map.Entry<String, ItemCountInfo> entry : relationships.entrySet()) {
                api.log(entry.getKey() + " ==> " + (entry.getValue()).getInt1() + "/" + (entry.getValue()).getInt2() + "/" + (entry.getValue()).getInt3() + "/" + (entry.getValue()).getInt4(), 1);
            }
            // progressBar.setProgress(50);
        } catch (APIException ex) {
            ExceptionHandler eh = new ExceptionHandler(ex);
            Logger.getLogger(TraceAnalyserController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            api.log(eh.getMessage(), 1);
        }
    }

    /**
     * Get a document GUI Object, with pre-formatted elements, colours and text
     * in it
     */
    VBox getDocument(final String type, final String relname, final String addTextString, final ItemCountInfo itemInfo, final Boolean isBaseItem) {
        VBox vBox = new VBox();
        // hBox.
        // vBox.setMaxHeight(50);
        // vBox.setMaxWidth(50);
        // vBox.setPrefSize(20, 20);
        // hBox.setStyle("-fx-border-width:1px;-fx-border-style:solid;-fx-border-color:#999999;");
        // System.out.println("Type: " + type + ": color = " + getColorByName(type));

        try {
            // System.out.println("if (1==1) return vBox");
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
                // topText2 = topText2 + (itemInfo.getInt4() > 0 ? " (" + itemInfo.getInt4() + ")" : ""); // suspectId
            }

            if ((itemInfo.getInt1() > 1) && (!type.contains("("))) {
                // multiple items
                midText = (itemInfo.getInt1() > 1 ? "" : itemInfo.getDisplayId() + ": ") + plural(type, itemInfo.getInt1());
            } else {
                // sigle item
                midText = itemInfo.getShortSummary();
                idText = itemInfo.getDisplayId();
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

            ImageView iv1 = new ImageView();
            iv1.setImage(TraceAnalyser.warnImage);
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

            // System.out.println("if (1==1) return vBox");
            // if (1 == 1) {
            //     return vBox;
            // }
            Tooltip tt = new Tooltip(itemInfo.getInfo("\n") + "\nClass: " + className + itemInfo.getItemListAsString("\nItems: ", 15));
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
                                    display(itemInfo.getId());
                                    // displayMap();
                                } else {
                                    System.out.println(event.getButton() + " Mouse setOnMouseClicked" + event.getTarget().toString() + ", " + itemInfo.getId());
                                    openTracedItems(itemInfo, mOpenWithinGUI);

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
        } catch (Exception ex) {
            Logger.getLogger(TraceAnalyserController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            api.log(ex.getMessage(), 1);
        }

        return vBox;
    }

    /**
     *
     */
    public void displayMap() {
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
                gridPane.add(getTrace(key, relname, value, meaningfulCount, mOpenWithinGUI), 0, relCount);
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

        if (toBeShown) {
            relCount++;
            gridPane.add(getDocument(ibi.getType(), "", meaningfulCount + " meaningful nodes", new ItemCountInfo(0, 0, 0, 0, ibi), true), 0, relCount);
        }
        // progressBar.setProgress(100);
    }

    /**
     *
     * @param startItemId
     * @param assType
     * @param traceNames
     * @return
     * @throws APIException
     */
    public static WorkItemIterator constructUniqueIdLists(String startItemId, String assType, String traceNames) throws APIException {
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

        // List of meaningfull nodes in a document
        WorkItemIterator wit3 = resp.getWorkItems();
        if (assType != null) {
            // JUmp over the document item if provided
            wit3.next();
        }
        //
        // loop through all traces in the document
        // and add all related item id's into the "uniqueIDs" array 
        //
        while (wit3.hasNext()) {
            WorkItem wid = wit3.next();
            // get all id's from all related fields
            for (String traceField : traceNames.split(",")) {
                // api.log("Checking field: " + field + " ...", 1);
                String relatedItems = wid.getField(traceField).getValueAsString().replace("?", "");
                if (!relatedItems.isEmpty()) {
                    uniqueIDs.addAll(Arrays.asList(relatedItems.split(",")));
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
            // execute for all nodes in the related item list
            cmd3.addOption("fields", "Type,State,Summary,Assigned User,Category,Document ID,Created Date");
            Response resp3 = api.executeCmd(cmd3);
            WorkItemIterator witUnique = resp3.getWorkItems();
            while (witUnique.hasNext()) {
                WorkItem wi3 = witUnique.next();
                // get the document related to this node
                if (wi3.getField("Document ID").getValueAsString() != null) {
                    // and add it to the unique document list
                    uniqueDocIDs.add(wi3.getField("Document ID").getValueAsString());
                }
                // add the information also into the unique WorkItem list
                uniqueWIs.put(wi3.getId(), wi3);
                // add the type also into the type list
                addTypeDetails(wi3.getField("Type").getValueAsString());
            }
            // now, retrieve all document items via uniqueDocIDs list
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
        
        for (String id : uniqueWIs.keySet()) {
            System.out.println("key in uniqueKeySet: " + id);
        }

        // Get the initial Work Items again, and return the nodes only
        WorkItemIterator wit = resp.getWorkItems();
        // skip over the document item itself
        if (assType != null) {
            wit.next();
        }
        return wit;

    }
}
