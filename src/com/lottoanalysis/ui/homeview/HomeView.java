package com.lottoanalysis.ui.homeview;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public interface HomeView {

    void setHomeViewListener(HomeViewListener homeViewListener);

    void injectView(AnchorPane pane);

    AnchorPane getView();

    void enableButtonAndDisableDashboardButton();
}
