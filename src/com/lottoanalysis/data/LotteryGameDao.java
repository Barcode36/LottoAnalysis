package com.lottoanalysis.data;

import com.lottoanalysis.lottogames.LottoGame;

import java.util.List;

public interface LotteryGameDao {

    boolean isDbConnected();

    void updateDbTableForGame(String drawNum, String date, String[] positionNumbers, int gameId);

    Object[] retrieveGameId(String gameName);

    List<String> selectAllGames();

    void loadUpDrawings(LottoGame game);

    int getCurrentWinningGameNumber(int id);

}
