/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package traceanalyser;

import com.mks.api.Option;
import com.mks.api.response.APIException;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItem;
import com.ptc.services.common.api.ApplicationProperties;
import com.ptc.services.common.api.Command;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import traceanalyser.api.IntegrityCommands;

/**
 *
 * @author veckardt
 */
public class TraceAnalyserController2 {

    static Rectangle2D boxBounds = new Rectangle2D(100, 100, 200, 90);
    
    static Map<String, WorkItem> types = new HashMap<>();
    static final IntegrityCommands api = new IntegrityCommands();
    static final ApplicationProperties props = new ApplicationProperties(TraceAnalyser.class);
    static String suspectId = props.getProperty("SuspectIdent", "susp.");    
    
    static String plural(String text, int count) {
        return text + (count > 1 ? "s" : "");
    }
    void log(String text, int level) {
        api.log(text, level);
        // System.out.println(text);
    }    
    static void addTypeDetails(String typeName) throws APIException {
        if (!types.containsKey(typeName)) {
            types.put(typeName, api.getTypeDetails(typeName, "name,documentClass"));
        }
    }
    
    
    
    /**
     * Get a trace GUI Object, with pre-formatted elements, colours and text in
     * it
     */
    HBox getTrace(String traceName, String relname, final ItemCountInfo itemInfo, int meaningfulCount, CheckMenuItem mOpenWithinGUI) {
        final CheckMenuItem mOpenGUI = mOpenWithinGUI;
        
        HBox hBox = new HBox();

        try {
            Boolean isForward = api.fieldIsForward(traceName);
            WorkItem wiField = api.allFields.get(traceName);
            String displayName = wiField.getField("DisplayName").getValueAsString();
            String percent = String.format("%.0f", Float.valueOf(itemInfo.getInt1().toString()) / meaningfulCount * 100);
            String label1 = itemInfo.getInt1() + "/" + meaningfulCount + " " + relname;
            String label2 = "(" + percent + "%)";
            String label3 = (itemInfo.getInt2() > 0 ? "  (" + itemInfo.getInt2() + " " + suspectId + ")" : "");

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
            // System.out.println("if (1==1) return vBox");
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
                    openTracedItems(itemInfo, mOpenGUI);
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
                            openTracedItems(itemInfo, mOpenGUI);
                        }
                    }
                    event.consume();
                }
            });

            // hBox.getChildren().addAll(stackPane, statusBox);
            hBox.getChildren().addAll(stackPane);
            hBox.setId("Trace");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hBox;
    }

   

    /**
     *
     * @param itemInfo
     */
    public static void openTracedItems(ItemCountInfo itemInfo, CheckMenuItem mOpenWithinGUI) {
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

    /**
     *
     * @param color
     * @param direction
     * @return
     */
    public static LinearGradient getGradient(String color, Boolean direction) {
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

    /**
     *
     * @param typeName
     * @return
     */
    public static String getColorByName(String typeName) {
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
}
