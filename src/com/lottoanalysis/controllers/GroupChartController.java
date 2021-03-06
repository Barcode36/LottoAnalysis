package com.lottoanalysis.controllers;

import com.lottoanalysis.models.charts.LineChartWithHover;
import com.lottoanalysis.common.LotteryGameConstants;
import com.lottoanalysis.models.lottogames.LottoGame;
import com.lottoanalysis.models.numbertracking.NumberMultipleAnalyzer;
import com.lottoanalysis.utilities.chartutility.ChartHelperTwo;
import com.lottoanalysis.utilities.gameoutviewutilities.GameOutLottoHitFinder;
import com.lottoanalysis.utilities.numbergrouputilites.NextProbableGroupFinder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public class GroupChartController {

    private LottoGame lotteryGame;
    private int[][] drawPositionalNumbers;
    private List<Object> allGameData;
    private Map<Integer, String> data;
    private ObservableList<ObservableList> dataItems = FXCollections.observableArrayList();
    private BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private Graphics2D g = image.createGraphics();
    private FontMetrics fm = g.getFontMetrics(new Font("System", Font.PLAIN, 24));
    private String currentGameName = "";
    private int drawPositionGlobal;
    private Integer[] RANGE = {1, 10};
    private List<Integer> globalListOfNumbers;

    private NumberMultipleAnalyzer numberMultipleAnalyzer;

    private static int globalDrawPosition, rowIndex;

    @FXML
    private VBox groupRadioButtonVbox;

    @FXML
    private HBox drawPositionHbox, radioBtnAndChartHbox;

    @FXML
    private MenuButton groupSizeMenuButton;

    @FXML
    private Label lblGame, lblAnalyzedPosition, groupHitOutlookLabel, patternOutlookLabel, gameOutPerfLabel;

    @FXML
    private GridPane groupGridPane;

    @FXML
    private StackPane chartPane, gapSpacingPane, gapSpacingInfoPane, lottoNumberPane;

    @FXML
    private TableView groupInfoTable, patternTable, tbl_gameOutPerformance;

    @FXML
    public void initialize() {
        groupInfoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patternTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public GroupChartController() {
        data = new HashMap<>();
        data.put(0, "One");
        data.put(1, "Two");
        data.put(2, "Three");
        data.put(3, "Four");
        data.put(4, "Five");
    }

    /**
     * Clear all undynamic content
     */
    private void clearUnDynamicContent() {
        groupRadioButtonVbox.getChildren().clear();
        drawPositionHbox.getChildren().clear();
        drawPositionHboxSet();

        Node node = radioBtnAndChartHbox.getChildren().get(0);
        radioBtnAndChartHbox.getChildren().setAll(node);
    }

    private void clearVbox() {
        //((VBox) radioBtnAndChartHbox.getChildren().get(0)).getChildren().clear();

        radioBtnAndChartHbox.getChildren().removeIf(obj -> obj instanceof VBox);
    }

    public void initFields(LottoGame game, List<Object> drawPositionalNumbers) {

        this.allGameData = drawPositionalNumbers;
        setLotteryGame(game);
        setDrawPositionalNumbers((int[][]) allGameData.get(0));
        //PatternFinder.analyze(this.drawPositionalNumbers);

        numberMultipleAnalyzer = new NumberMultipleAnalyzer(game);

    }

    public void startSceneLayoutSequence() {

        clearUnDynamicContent();
        insertPositionRadioButtonsIntoHbox();
        placeActionsOnMenuButtonItems();
    }

    private void placeActionsOnMenuButtonItems() {

        ObservableList<MenuItem> items = groupSizeMenuButton.getItems();


        if (lotteryGame.getGameName().equalsIgnoreCase(LotteryGameConstants.POWERBALL) ||
                lotteryGame.getGameName().equalsIgnoreCase(LotteryGameConstants.MEGA_MILLIONS)) {
            items.remove(0);
        }

        items.forEach(menuItem -> {

            menuItem.setOnAction(e -> {

                //groupSizeMenuButton.setText(menuItem.getText());
                retrieveDataForChartViewing(globalDrawPosition, Integer.parseInt(menuItem.getText()));
            });
        });


    }

    private void insertPositionRadioButtonsIntoHbox() {
        setGlobalDrawPosition(0);
        setPositionBeingAnalyzedLabel();
        retrieveDataForChartViewing(0, 10);
    }

    private void drawPositionHboxSet() {
        // clearUnDynamicContent();
        ToggleGroup group = new ToggleGroup();

        for (int i = 0; i < drawPositionalNumbers.length; i++) {

            RadioButton button = new RadioButton("Position " + data.get(i));
            final int position = i;

            button.setOnAction(e -> {
                ObservableList<Toggle> toggles = group.getToggles();
                for (Toggle t : toggles) {
                    RadioButton b = (RadioButton) t;
                    if (b.getText().equalsIgnoreCase(button.getText()))
                        b.setSelected(true);
                }

                patternTable.getItems().clear();
                patternTable.getColumns().clear();

                setGlobalDrawPosition(position);
                setPositionBeingAnalyzedLabel();
                retrieveDataForChartViewing(position, 10);
            });
            button.setStyle("-fx-text-fill: #dac6ac;");

            drawPositionHbox.getChildren().add(button);
            group.getToggles().add(button);
        }

        ((RadioButton) group.getToggles().get(0)).setSelected(true);
    }

    private void setPositionBeingAnalyzedLabel() {

        lblAnalyzedPosition.setText("Analyzing Position " + data.get(globalDrawPosition));
        lblAnalyzedPosition.setPrefWidth(fm.stringWidth(lblAnalyzedPosition.getText()));
    }

    /**
     * @param drawPosition
     */
    private void retrieveDataForChartViewing(int drawPosition, int drawSize) {

        //clearUnDynamicContent();

        patternTable.getItems().clear();
        patternTable.getColumns().clear();
        groupInfoTable.getItems().clear();
        groupInfoTable.getColumns().clear();

        clearVbox();
        lblGame.setText("Group Chart Analysis: " + lotteryGame.getGameName());
        groupHitOutlookLabel.setText("Group Hit Outlook Position " + data.get(globalDrawPosition));
        patternOutlookLabel.setText(String.format("%40s", patternOutlookLabel.getText()));
        gameOutPerfLabel.setText(String.format("%45s", gameOutPerfLabel.getText()));

        NextProbableGroupFinder.analyze(drawPositionalNumbers);
        //GameOutViewPatternFinder.analyze(drawPositionalNumbers);;

        //PositionalGameOutPositionTracker.analyze(lotteryGame,drawPositionalNumbers);
        this.drawPositionGlobal = drawPosition;
        int[] drawingPos = drawPositionalNumbers[drawPosition];

        for (int num : drawingPos)
            numberMultipleAnalyzer.analyzeLottoNumber(num);

//        numberMultipleAnalyzer.computeHitsAtGamesOutAndLastAppearance();
//        numberMultipleAnalyzer.print();

        //NextProbableGroupFinder.analyze( drawPositionalNumbers );
//        SplitDigitAnalyzer splitDigitAnalyzer = new SplitDigitAnalyzer();
//        splitDigitAnalyzer.analyze(drawingPos, drawPosition, lotteryGame);

        //SumGroupAnalyzer.analyze(drawingPos, lotteryGame);
        //LineSpacingHelperTwo.analyze( Arrays.asList( Arrays.stream(drawingPos).boxed().toArray(Integer[]::new)), false);
        //ProbableSumFinder.analyze(drawingPos, lotteryGame, drawPositionalNumbers);

        //int[] dd = p.stream().mapToInt(i -> i).toArray();

//        GameSelectionObject obj = (GameSelectionObject) CacheManager.getCache(lotteryGame.getGameName(),drawPosition);
//        if(obj == null) {
//            UpperLowerRangeAnalyzer upperLowerRangeAnalyzer = new UpperLowerRangeAnalyzer(drawPositionalNumbers, drawPosition, lotteryGame);
//            GameSelectionObject cachedObject = new GameSelectionObject(upperLowerRangeAnalyzer.getUpperLowerRangeAnalyzers(),lotteryGame.getGameName(), drawPosition);
//            CacheManager.putCache(cachedObject,drawPosition);
//        }
//        else{
//
//            UpperLowerRangeAnalyzer[] upperLowerRangeAnalyzers = (UpperLowerRangeAnalyzer[])obj.getObject();
//            IntStream.range(0, upperLowerRangeAnalyzers.length).forEach(i -> {
//
//                upperLowerRangeAnalyzers[i].printData(upperLowerRangeAnalyzers[i], i);
//            });
//            System.out.println();
//        }

//        ChartDataBuilder chartDataBuilder = new ChartDataBuilder(drawingPos);
//
//        DayGrouperAnalyzer dayGrouperAnalyzer = new DayGrouperAnalyzer();
//        dayGrouperAnalyzer.analzye(lotteryGame);

        ChartHelperTwo.clearGroupHitInformation();
        ChartHelperTwo.processIncomingData(lotteryGame, drawingPos, drawSize);

        Map<String, Object[]> positionData = ChartHelperTwo.getGroupHitInformation();

        setupChartForViewing(positionData);
    }

    private void setupChartForViewing(Map<String, Object[]> positionData) {
        //clearUnDynamicContent();
        radioBtnAndChartHbox.getChildren().add(0, new VBox());
        radioBtnAndChartHbox.getChildren().add(1, new VBox());

        ToggleGroup group = new ToggleGroup();
        int count = 0;
        int countTwo = 1;

        for (Iterator<String> iterator = positionData.keySet().iterator(); iterator.hasNext(); ) {

            RadioButton button = new RadioButton(iterator.next());
            button.setOnAction(event -> {

                patternTable.getItems().clear();
                patternTable.getColumns().clear();
                injectChartWithData(positionData, button.getText());
                List<Integer> specialList = (List<Integer>) ChartHelperTwo.getRepeatedNumberList((List<Integer>) positionData.get(button.getText())[0])[0];
                setUpGroupHitGridPane(positionData, button.getText(), (List<Integer>) positionData.get(button.getText())[0]);

                int[] num = (((List<Integer>) positionData.get(button.getText())[0])).stream().mapToInt(Integer::intValue).toArray();


                //fLineAnalyzer.analyzeData(num);

//               LineSpacingHelperTwo.analyze( ChartHelperTwo.extractAppropriatePosition(positionData, button.getText()));
//                CompanionNumberFinder.analyzeIncomingInformation(
//                      ChartHelperTwo.extractAppropriatePosition( positionData, button.getText())
//                );
                //LineSpacingHelper.determineMostProbableLineSpacing(ChartHelperTwo.extractAppropriatePosition(positionData,button.getText()));
            });

            if (count == 0) {
                button.setSelected(true);
                count++;
            }

            VBox box2 = (VBox) radioBtnAndChartHbox.getChildren().get(1);
            button.setStyle("-fx-text-fill: #dac6ac;");
            VBox box = (VBox) radioBtnAndChartHbox.getChildren().get(0);
            box.setSpacing(2);


            if (box.getChildren().size() > 13) {

                box2.setVisible(true);
                box2.setSpacing(2);

                box2.getChildren().add(button);
            } else {
                box.getChildren().add(button);
                box2.setVisible(false);
            }

            group.getToggles().add(button);
        }

        radioBtnAndChartHbox.setSpacing(20);
        injectChartWithData(positionData, ((RadioButton) group.getToggles().get(0)).getText());
        List<Integer> specialList = (List<Integer>) ChartHelperTwo.getRepeatedNumberList(
                (List<Integer>) positionData.get(((RadioButton) group.getToggles().get(0)).getText())[0])[0];

        setUpGroupHitGridPane(positionData, ((RadioButton) group.getToggles().get(0)).getText(),
                (List<Integer>) positionData.get(((RadioButton) group.getToggles().get(0)).getText())[0]);

        int[] nums = ((List<Integer>) positionData.get(((RadioButton) group.getToggles().get(0)).getText())[0]).stream().mapToInt(i -> i).toArray();


        //TrendLineAnalyzer.analyzeData(nums);

        Object[] data = null;

//        if(currentGameName.isEmpty() || !currentGameName.equals(lotteryGame.getGameName())) {
//            currentGameName = lotteryGame.getGameName();
//            BetSlipAnalyzer betSlipAnalyzer = new BetSlipAnalyzer();
//            betSlipAnalyzer.analyzeDrawData(drawPositionalNumbers, lotteryGame);
//            data = betSlipAnalyzer.getBetSlipData();
//        }
        System.out.println(Arrays.toString(data));
//        setUpPatternChart((List<Integer>)positionData.get(((RadioButton)group.getToggles().get(0)).getText())[0],
//                ((RadioButton)group.getToggles().get(0)).getText());
        //LineSpacingHelperTwo.analyze(ChartHelperTwo.extractAppropriatePosition(positionData,"1"));
        //CompanionNumberFinder.analyzeIncomingInformation(ChartHelperTwo.extractAppropriatePosition( positionData, "1"));
        // LineSpacingHelper.determineMostProbableLineSpacing(ChartHelperTwo.extractAppropriatePosition(positionData,"1"));

    }

    private void setUpPatternChart(List<Integer> integers, String text) {

        patternTable.getItems().clear();
        patternTable.getColumns().clear();

        Map<String, Object[]> positionData = ChartHelperTwo.getPatternData(integers, text);
        //getDataItemsTwo.clear();

        ObservableList<ObservableList> getDataItemsTwo = FXCollections.observableArrayList();
        // Create columns
        String[] colNames = {"Pattern", "Hits", "Games Out", "Game Out Hits", "Last Seen"};
        for (int i = 0; i < colNames.length; i++) {

            final int j = i;
            TableColumn col = new TableColumn(colNames[i]);
            col.setCellFactory(new Callback<TableColumn<ObservableList, String>, TableCell<ObservableList, String>>() {

                @Override
                public TableCell<ObservableList, String> call(TableColumn<ObservableList, String> param) {
                    return new TableCell<ObservableList, String>() {

                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {

                                setText(item);
                                this.setTextFill(Color.BEIGE);
                                // System.out.println(param.getText());

                                ObservableList observableList = getTableView().getItems().get(getIndex());
                                if (observableList.get(2).toString().equalsIgnoreCase("0") &&
                                        !observableList.get(1).toString().equalsIgnoreCase("0")) {
                                    getTableView().getSelectionModel().select(getIndex());

                                    if (getTableView().getSelectionModel().getSelectedItems().contains(observableList)) {

                                        this.setTextFill(Color.valueOf("#76FF03"));
                                    }

                                    //System.out.println(getItem());
                                    // Get fancy and change color based on data
                                    //if (item.contains("X"))
                                    //this.setTextFill(Color.valueOf("#EFA747"));
                                }
                            }
                        }
                    };
                }
            });

            col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>,
                    ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString())


            );

            col.setSortable(false);
            patternTable.getColumns().addAll(col);
        }

        /********************************
         * Data added to ObservableList *
         ********************************/
        int size = positionData.size();
        for (Map.Entry<String, Object[]> data : positionData.entrySet()) {

            //Iterate Row
            ObservableList<String> row = FXCollections.observableArrayList();

            String key = data.getKey();
            Object[] values = data.getValue();

            row.add(key);
            row.add(values[0] + "");
            row.add(values[1] + "");
            row.add(values[2] + "");
            row.add(values[3] + "");

            getDataItemsTwo.add(row);
        }

        patternTable.setItems(getDataItemsTwo);
    }


    private void setUpGroupHitGridPane(Map<String, Object[]> positionData, String text, List<Integer> points) {

        groupInfoTable.refresh();

        groupInfoTable.getItems().clear();
        groupInfoTable.getColumns().clear();

        // Create columns
        String[] colNames = {"Group", "Hits", "Games Out", "Hits At", "Avg Skips", "Hits Above", "Hits Below", "Equal To", "Last Seen"};
        for (int i = 0; i < colNames.length; i++) {

            final int j = i;
            TableColumn col = new TableColumn(colNames[i]);
            col.setCellFactory(new Callback<TableColumn<ObservableList, String>, TableCell<ObservableList, String>>() {

                @Override
                public TableCell<ObservableList, String> call(TableColumn<ObservableList, String> param) {
                    return new TableCell<ObservableList, String>() {

                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {

                                setText(item);
                                this.setTextFill(Color.BEIGE);
                                // System.out.println(param.getText());

                                ObservableList observableList = getTableView().getItems().get(getIndex());
                                if (observableList.get(2).toString().equalsIgnoreCase("0")) {
                                    getTableView().getSelectionModel().select(getIndex());

                                    if (getTableView().getSelectionModel().getSelectedItems().contains(observableList)) {

                                        this.setTextFill(Color.valueOf("#76FF03"));
                                    }

                                    //System.out.println(getItem());
                                    // Get fancy and change color based on data
                                    //if (item.contains("X"))
                                    //this.setTextFill(Color.valueOf("#EFA747"));
                                }
                            }
                        }
                    };
                }
            });

            col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>,
                    ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString())


            );

            col.setSortable(false);
            groupInfoTable.getColumns().addAll(col);
        }

        /********************************
         * Data added to ObservableList *
         ********************************/
        int size = positionData.size();
        for (Map.Entry<String, Object[]> data : positionData.entrySet()) {

            //Iterate Row
            ObservableList<String> row = FXCollections.observableArrayList();

            String key = data.getKey();
            Object[] values = data.getValue();

            row.add(key);
            row.add(values[1] + "");
            row.add(values[2] + "");
            row.add(values[3] + "");
            row.add(values[5] + "");
            row.add(values[6] + "");
            row.add(values[7] + "");
            row.add(values[8] + "");
            row.add(values[9] + "");

            dataItems.add(row);
        }

        groupInfoTable.setItems(dataItems);
        //  groupInfoTable.scrollTo(size - 1);

        setUpPatternChart(points, text);
    }

    @SuppressWarnings("unchecked")
    private void injectChartWithData(Map<String, Object[]> positionData, String text) {
//
//        if(radioBtnAndChartHbox.getChildren().size() < 3) {
//            radioBtnAndChartHbox.getChildren().remove(1);
//        }
//        else if(radioBtnAndChartHbox.getChildren().size() > 2){
//
//            for(int i = 0; i < radioBtnAndChartHbox.getChildren().size(); i++){
//
//                Object obj = radioBtnAndChartHbox.getChildren().get(i);
//
//                if(obj instanceof LineChart){
//
//                    radioBtnAndChartHbox.getChildren().remove(i);
//                }
//            }
//
//        }
//            //radioBtnAndChartHbox.getChildren().remove(2);

        String[] colors = {"#FF0000", "#FF0000"};

        List<Integer> numList = (List<Integer>) positionData.get(text)[0];
        globalListOfNumbers = new ArrayList<>(numList);

        // Set up data for game out hit info table
        String[] minMaxValues = ChartHelperTwo.getMinMax(text);

        List<Integer> numListTwo = new ArrayList<>(numList);
        Collections.sort(numListTwo);

        List<Integer> specialList = (List<Integer>) ChartHelperTwo.getRepeatedNumberList(numList)[0];
        //ChartHelperTwo.formCompanionHitTrio( numList );
        calculateMountainSpacings(numList);

        int[][] remainderData = (int[][]) allGameData.get(4);

        List<Integer> remainderList = Arrays.stream(remainderData[drawPositionGlobal]).boxed().collect(Collectors.toList());

        GameOutLottoHitFinder gameOutLottoHitFinder = new GameOutLottoHitFinder(Integer.parseInt(minMaxValues[0]), Integer.parseInt(minMaxValues[1]), numList);
        setUpGameOutPatternChart(gameOutLottoHitFinder.getLottoNumberHitTracker());

        List<List<Integer>> dataPoints = new ArrayList<>();
        List<Integer> movingAverages = calculateMovingAverage(specialList);

        // TrioNumberPatternFinder trioNumberPatternFinder = new TrioNumberPatternFinder(numList);

//        BollingerBand bollingerBand = new BollingerBand(numList,14,150);
//        List<List<Integer>> data = bollingerBand.getBollingerBands();

        //dataPoints.add((specialList.size() > 200) ? specialList.subList(specialList.size() - 200, specialList.size()) : specialList);
        dataPoints.add((numList.size() > 200) ? numList.subList(numList.size() - 200, numList.size()) : numList);
//        data.forEach( val -> {
//
//            dataPoints.add( val );
//        });
        // dataPoints.add( (movingAverages.size() > 100) ? movingAverages.subList(movingAverages.size()-100,movingAverages.size()) : movingAverages);

//        List<Integer> pointTwo = (numList.size() > 0) ? ChartHelper.getListOfNumbersBasedOnCurrentWinningNumber(numList) : new ArrayList<>();
//
//        if(pointTwo.size() > 0)
//            dataPoints.add((pointTwo.size() > 100) ? pointTwo.subList(pointTwo.size() - 100, pointTwo.size()) : pointTwo);

        Set<Integer> unique = new HashSet<>(numListTwo);

        int minValue, maxValue;

        if (numListTwo.size() > 1) {
            minValue = numListTwo.get(0);
            maxValue = numListTwo.get(numList.size() - 1);
        } else {
            minValue = 1;
            maxValue = 2;
        }
        LineChartWithHover lc = new LineChartWithHover(dataPoints,
                null,
                minValue,
                maxValue, unique.toString(), text, 996, 263, 9);

        chartPane.getChildren().setAll(lc.getLineChart());

//        System.out.println();
//        Map<Integer,Integer[]> lineSpacingMap = (Map<Integer,Integer[]>)ChartHelperTwo.getRepeatedNumberList(numList)[1];
//
//
//        lineSpacingMap.forEach((k,vv) -> {
//
//            System.out.printf("Multiple %s %15s %3s %15s %3s\n",k,"Hits:",vv[0],"Games Out:",vv[1]);
//        });

        //radioBtnAndChartHbox.getChildren().add(lc.getLineChart());

    }

    private void calculateMountainSpacings(List<Integer> listt) {

        List<Integer> specialList = new ArrayList<>(listt);
        List<Integer> idHitHolderList = new ArrayList<>();
        Set<Integer> uniqueNumbers = new TreeSet<>(specialList);
        Map<Integer, List<Integer>> numberSpacingHolder = new TreeMap<>();
        Map<String, RangeHitTracker> rangeHitTrackerMap = new LinkedHashMap<>();
        insertDefaultRanges(rangeHitTrackerMap);

        Map<Integer, Integer[]> lastTwoIndexHolderMap = new TreeMap<>();

        final int currentWinningNumberLastIndex = specialList.lastIndexOf(specialList.get(specialList.size() - 1));
        final int winningNumber = specialList.get(currentWinningNumberLastIndex);

        Iterator<Integer> iterator = uniqueNumbers.iterator();
        while (iterator.hasNext()) {

            final int value = iterator.next();

            int[] indexes = IntStream.range(0, specialList.size()).filter(num -> specialList.get(num) == value).toArray();
            List<Integer> list = Arrays.stream(indexes).boxed().collect(Collectors.toList());
            numberSpacingHolder.put(value, list);

            List<Integer> lastTwoIndex = list.subList(list.size() - 1, list.size()).stream().collect(Collectors.toList());
            lastTwoIndex.add(currentWinningNumberLastIndex);

            Integer[] p = lastTwoIndex.stream().toArray(Integer[]::new);
            lastTwoIndexHolderMap.put(value, p);
        }

//        List<Integer> currentWinningNumberHitIndex = numberSpacingHolder.get(specialList.get(specialList.size() - 1));
//        specialList.removeAll(currentWinningNumberHitIndex);

        for (int i = 0; i < specialList.size(); i++) {

            final int num = specialList.get(i);
            for (Map.Entry<Integer, List<Integer>> entry : numberSpacingHolder.entrySet()) {

                if (num == entry.getKey()) {

                    for (int j = 0; j < entry.getValue().size() - 1; j++) {

                        int dif = Math.abs(entry.getValue().get(j) - entry.getValue().get(j + 1));
                        rangeHitTrackerMap.forEach((k, v) -> {

                            String[] rangeVals = k.split(",");
                            int upperLimit;
                            int lowerLimit;
                            if (rangeVals.length > 1) {

                                lowerLimit = Integer.parseInt(rangeVals[0]);
                                upperLimit = Integer.parseInt(rangeVals[1]);
                                if (dif >= lowerLimit && dif <= upperLimit) {

                                    int hits = v.getHits();
                                    v.setHits(++hits);
                                    idHitHolderList.add(v.getId());
                                    v.setGamesOut(0);
                                    v.incrementGamesOut(rangeHitTrackerMap, k);
                                }
                            } else {
                                upperLimit = Integer.parseInt(rangeVals[0]);
                                if (dif >= upperLimit) {

                                    int hits = v.getHits();
                                    v.setHits(++hits);
                                    idHitHolderList.add(v.getId());
                                    v.setGamesOut(0);
                                    v.incrementGamesOut(rangeHitTrackerMap, k);
                                }

                            }
                        });

                        break;
                    }

                    entry.getValue().remove(0);
                    break;
                }
            }
        }

        lastTwoIndexHolderMap.forEach((k, v) -> {

            if (v.length > 1) {

                int dif = Math.abs(v[0] - v[1]);
                rangeHitTrackerMap.forEach((kk, vv) -> {

                    Integer[] ranges = Arrays.stream(kk.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
                    if (ranges.length > 1) {

                        if (dif >= ranges[0] && dif <= ranges[1]) {

                            vv.getNumbersWithinRangeHolder().add(k);

                            if (Arrays.equals(ranges, RANGE) && !vv.getNumbersWithinRangeHolder().contains(winningNumber)) {

                                vv.getNumbersWithinRangeHolder().add(winningNumber);
                            }
                        }

                    } else {

                        if (dif >= ranges[0]) {

                            vv.getNumbersWithinRangeHolder().add(k);
                        }
                    }
                });

            }
        });

        List<Integer> currentGamesOut = rangeHitTrackerMap.values().stream().filter(i -> i.getNumbersWithinRangeHolder().size() > 0).map( val -> val.getId()).collect(Collectors.toList());
        idHitHolderList.removeIf(val -> !currentGamesOut.contains(val));
        setUpSpacingChart(rangeHitTrackerMap, idHitHolderList);
    }

    private void setUpSpacingChart(Map<String, RangeHitTracker> rangeHitTrackerMap, List<Integer> idHitHolderList) {

        setUpSpacingTable(rangeHitTrackerMap);

        List<List<Integer>> dataPoints = new ArrayList<>();

        Set<Integer> unique = new HashSet<>(idHitHolderList);
        List<Integer> minMaxVals = new ArrayList<>(idHitHolderList);
        Collections.sort(minMaxVals);

        Object[] data = ChartHelperTwo.getRepeatedNumberList(idHitHolderList);

        List<Integer> specialList = (List<Integer>) data[0];

        //dataPoints.add((specialList.size() > 150) ? specialList.subList(specialList.size() - 150, specialList.size()) : specialList);
        dataPoints.add((idHitHolderList.size() > 100) ? idHitHolderList.subList(idHitHolderList.size()-100,idHitHolderList.size()) : idHitHolderList);

        LineChartWithHover lc = new LineChartWithHover(dataPoints,
                null,
                minMaxVals.get(0),
                minMaxVals.get(minMaxVals.size() - 1), unique.toString(), "Gap Spacing Hit Performance Chart", 654, 346, 7);

        gapSpacingPane.getChildren().setAll(lc.getLineChart());
    }

    private void setUpSpacingTable(Map<String, RangeHitTracker> rangeHitTrackerMap) {


        ObservableList<ObservableList> dataItems = FXCollections.observableArrayList();
        //Map<Integer, Integer[]> totalNumberPresentTracker = betSlipAnalyzer.getTotalNumberPresentTracker();

        TableView tableView = new TableView();
        //tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        String[] colNames = {"ID", "Range", "Hts", "Out", "Nums @ Rng"};

        for (int i = 0; i < colNames.length; i++) {

            final int j = i;
            TableColumn col = new TableColumn(colNames[i]);
            col.setCellFactory(new Callback<TableColumn<ObservableList, String>, TableCell<ObservableList, String>>() {

                @Override
                public TableCell<ObservableList, String> call(TableColumn<ObservableList, String> param) {
                    return new TableCell<ObservableList, String>() {

                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {

                                setText(item);
                                this.setTextFill(Color.BEIGE);
                                // System.out.println(param.getText());

                                ObservableList observableList = getTableView().getItems().get(getIndex());
                                if (observableList.get(3).toString().equalsIgnoreCase("0")) {
                                    getTableView().getSelectionModel().select(getIndex());

                                    if (getTableView().getSelectionModel().getSelectedItems().contains(observableList)) {

                                        this.setTextFill(Color.valueOf("#76FF03"));
                                    }

                                    //System.out.println(getItem());
                                    // Get fancy and change color based on data
                                    //if (item.contains("X"))
                                    //this.setTextFill(Color.valueOf("#EFA747"));
                                }

                                observableList.forEach(val -> {

                                    if (val.toString().contains("[")) {

                                        this.setOnMouseClicked(event -> {

                                            List<Integer> moddedList = new ArrayList<>(globalListOfNumbers);
                                            List<Integer> rangeNumbers = Arrays.stream(val.toString().split("\\W+"))
                                                    .filter( value -> !value.equals(""))
                                                    .map(Integer::parseInt).collect(Collectors.toList());
                                            moddedList.removeIf(value -> !rangeNumbers.contains(value));

                                            setUpLottoNumberPane( moddedList );

                                        });
                                    }
                                });

                            }
                        }
                    };
                }
            });

            col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>,
                    ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString())


            );

            col.setSortable(false);
            tableView.getColumns().addAll(col);
        }

        /********************************
         * Data added to ObservableList *
         ********************************/
        //int size = columnInfoData.size();
        for (Map.Entry<String, RangeHitTracker> data : rangeHitTrackerMap.entrySet()) {

            //Iterate Row
            ObservableList<String> row = FXCollections.observableArrayList();

            if (data.getValue().getNumbersWithinRangeHolder().size() > 0) {
                row.add(String.format("%s", data.getValue().getId()));
                row.add(data.getKey());
                row.add(data.getValue().getHits() + "");
                row.add(data.getValue().getGamesOut() + "");
                row.add(Arrays.toString(data.getValue().getNumbersWithinRangeHolder().toArray()));


                dataItems.add(row);
            }

        }

        tableView.setItems(dataItems);
        //tableView.scrollTo(tableView.getItems().size() - 1);
        gapSpacingInfoPane.getChildren().setAll(tableView);

    }

    private void setUpLottoNumberPane(List<Integer> moddedList) {


        List<List<Integer>> dataPoints = new ArrayList<>();

        Set<Integer> unique = new HashSet<>(moddedList);
        List<Integer> minMaxVals = new ArrayList<>(moddedList);
        Collections.sort(minMaxVals);

        Object[] data = ChartHelperTwo.getRepeatedNumberList(moddedList);

        List<Integer> specialList = (List<Integer>) data[0];

        dataPoints.add((specialList.size() > 100) ? specialList.subList(specialList.size() - 100, specialList.size()) : specialList);
       // dataPoints.add((moddedList.size() > 100) ? moddedList.subList(moddedList.size()-100,moddedList.size()) : moddedList);

        LineChartWithHover lc = new LineChartWithHover(dataPoints,
                null,
                minMaxVals.get(0),
                minMaxVals.get(minMaxVals.size() - 1), unique.toString(), "Lotto Number Hit Performance Chart", 654, 346, 4);

        lottoNumberPane.getChildren().setAll(lc.getLineChart());
    }


    private void insertDefaultRanges(Map<String, RangeHitTracker> rangeHitTrackerMap) {

        rangeHitTrackerMap.put("1,10", new RangeHitTracker(1));
        rangeHitTrackerMap.put("11,20", new RangeHitTracker(2));
        rangeHitTrackerMap.put("21,30", new RangeHitTracker(3));
        rangeHitTrackerMap.put("31,40", new RangeHitTracker(4));
        rangeHitTrackerMap.put("41,50", new RangeHitTracker(5));
        rangeHitTrackerMap.put("51", new RangeHitTracker(6));
    }

    public static List<Integer> calculateMovingAverage(List<Integer> numList) {

        float yesterdayEMA = numList.get(0);
        List<Integer> movingAverages = new ArrayList<>();
        List<Integer> threePeriodHolder = new ArrayList<>();

        for (int i = 0; i < numList.size() - 1; i++) {
//            if( threePeriodHolder.size() >= 15 )
//            {
//
//                Double doubles = new Double(threePeriodHolder.stream().collect(Collectors.averagingInt( j -> j )));
//                int average = doubles.intValue();
//                movingAverages.add( average );
//
//                threePeriodHolder.remove(0);
//                threePeriodHolder.add( numList.get(i +1));
//            }
//            else
//            {
//                threePeriodHolder.add( numList.get(i) );
//            }

            float ema = calculateMovingAverage(numList.get(i), 10, yesterdayEMA);
            movingAverages.add((int) ema);
            yesterdayEMA = ema;
        }

        return movingAverages;
    }

    private static float calculateMovingAverage(float todayWinningNumber, float numberOfDays, float yesterdayEMA) {

        float k = 2 / (numberOfDays + 1);
        return todayWinningNumber * k + yesterdayEMA * (1 - k);
    }

    private void setUpGameOutPatternChart(Map<Integer, Integer[]> lottoNumberHitTracker) {

        tbl_gameOutPerformance.refresh();

        tbl_gameOutPerformance.getItems().clear();
        tbl_gameOutPerformance.getColumns().clear();

        // Create columns
        String[] colNames = {"Lotto#", "Hits", "Prv G Out", "Gms Out", "Gm Out Hts", "Game Out Lst Ht"};
        for (int i = 0; i < colNames.length; i++) {

            final int j = i;
            TableColumn col = new TableColumn(colNames[i]);
            col.setCellFactory(new Callback<TableColumn<ObservableList, String>, TableCell<ObservableList, String>>() {

                @Override
                public TableCell<ObservableList, String> call(TableColumn<ObservableList, String> param) {
                    return new TableCell<ObservableList, String>() {

                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!isEmpty()) {

                                setText(item);
                                this.setTextFill(Color.BEIGE);
                                // System.out.println(param.getText());

                                ObservableList observableList = getTableView().getItems().get(getIndex());
                                if (observableList.get(3).toString().equalsIgnoreCase("0")) {
                                    getTableView().getSelectionModel().select(getIndex());

                                    if (getTableView().getSelectionModel().getSelectedItems().contains(observableList)) {

                                        this.setTextFill(Color.valueOf("#76FF03"));
                                    }

                                    //System.out.println(getItem());
                                    // Get fancy and change color based on data
                                    //if (item.contains("X"))
                                    //this.setTextFill(Color.valueOf("#EFA747"));
                                }
                            }
                        }
                    };
                }
            });

            col.setCellValueFactory((Callback<TableColumn.CellDataFeatures<ObservableList, String>,
                    ObservableValue<String>>) param -> new SimpleStringProperty(param.getValue().get(j).toString())


            );

            col.setSortable(false);
            tbl_gameOutPerformance.getColumns().addAll(col);
        }

        ObservableList<ObservableList> items = FXCollections.observableArrayList();
        /********************************
         * Data added to ObservableList *
         ********************************/
        for (Map.Entry<Integer, Integer[]> data : lottoNumberHitTracker.entrySet()) {

            //Iterate Row
            ObservableList<String> row = FXCollections.observableArrayList();

            Integer key = data.getKey();
            Integer[] values = data.getValue();

            row.add(key + "");
            row.add(values[0] + "");
            row.add(values[4] + "");
            row.add(values[1] + "");
            row.add(values[2] + "");
            row.add(values[3] + "");

            items.add(row);
        }

        tbl_gameOutPerformance.setItems(items);

    }


    // getters and setters

    public static int getRowIndex() {
        return rowIndex;
    }

    public static void setRowIndex(int rowIndex) {
        GroupChartController.rowIndex = rowIndex;
    }

    public LottoGame getLotteryGame() {
        return lotteryGame;
    }

    private void setLotteryGame(LottoGame lotteryGame) {
        this.lotteryGame = lotteryGame;
    }

    public int[][] getDrawPositionalNumbers() {
        return drawPositionalNumbers;
    }

    private void setDrawPositionalNumbers(int[][] drawPositionalNumbers) {
        this.drawPositionalNumbers = drawPositionalNumbers;
    }

    public static void setGlobalDrawPosition(int globalDrawPosition) {
        GroupChartController.globalDrawPosition = globalDrawPosition;
    }


    private class RangeHitTracker {

        private int id;
        private int hits;
        private int gamesOut;
        private List<Integer> numbersWithinRangeHolder = new ArrayList<>();

        public List<Integer> getNumbersWithinRangeHolder() {
            return numbersWithinRangeHolder;
        }

        RangeHitTracker(int id) {

            this.id = id;
        }

        public int getId() {
            return id;
        }

        public int getHits() {
            return hits;
        }

        public void setHits(int hits) {
            this.hits = hits;
        }

        public int getGamesOut() {
            return gamesOut;
        }

        public void setGamesOut(int gamesOut) {
            this.gamesOut = gamesOut;
        }

        public void incrementGamesOut(Map<String, RangeHitTracker> rangeHitTrackerMap, String value) {

            rangeHitTrackerMap.forEach((k, v) -> {

                if (!k.equals(value)) {
                    int gamesOut = v.getGamesOut();
                    v.setGamesOut(++gamesOut);
                }
            });

        }
    }
}
