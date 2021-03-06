package com.lottoanalysis.models.managers;

import com.lottoanalysis.models.lottogames.data.LotteryGameDao;
import com.lottoanalysis.models.lottogames.data.LotteryGameDaoImpl;
import com.lottoanalysis.interfaces.LotteryGameManager;
import com.lottoanalysis.models.lottogames.LottoGame;
import com.lottoanalysis.utilities.fileutilities.FileTweaker;

import java.util.List;

public class LotteryGameManagerImpl implements LotteryGameManager {

    public LotteryGameManagerImpl(){}

    /**
     * Selects all lottogames from the database to populate a dropdown list
     * @return
     */
    @Override
    public List<String> getAllGames() {

        return getDaoInstance().selectAllGames();
    }

    @Override
    public void populateDrawings(LottoGame game) {

        getDaoInstance().loadUpDrawings(game);

    }

    /**
     * Returns a game instance
     * @param game
     * @return
     */
    @Override
    public LottoGame loadLotteryData(LottoGame game) {

        Object[] data = getDaoInstance().retrieveGameId( game.getGameName() );

        game.setLottoId((int)data[0]);
        game.setPositionNumbersAllowed((int)data[3]);
        game.setMinNumber((int) data[1]);
        game.setMaxNumber((int) data[2]);
        game.setGameName( FileTweaker.trimStateAbrrFromGameName( game.getGameName() ) );

        populateDrawings(game);

        return game;
    }

    @Override
    public LotteryGameDao getDaoInstance() {

        return null;
    }
}
