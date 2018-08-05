package com.lottoanalysis.services.homeviewservices;

import com.lottoanalysis.enums.Databases;
import com.lottoanalysis.enums.Factory;
import com.lottoanalysis.enums.LotteryGame;
import com.lottoanalysis.factories.abstractfactory.AbstractFactory;
import com.lottoanalysis.factories.factoryproducer.FactoryProducer;
import com.lottoanalysis.interfaces.Database;
import com.lottoanalysis.lottogames.LottoGame;
import com.lottoanalysis.lottogames.drawing.Drawing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.WeakHashMap;

public class HomeRepositoryServiceImpl implements HomeServiceRepository{

    private AbstractFactory abstractFactory = FactoryProducer.getFactory(Factory.DataBaseFactory);
    private Database database = abstractFactory.getDataBase(Databases.MySql);

    @Override
    public LottoGame loadDefaultLotteryGame() throws Exception{

        AbstractFactory factory = FactoryProducer.getFactory(Factory.LotteryGameFactory);
        LottoGame lottoGame = factory.getLotteryGame(LotteryGame.FiveDigit);

        try(Connection connection = database.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(LOTTO_GAME_DATA_QUERY)){

            preparedStatement.setInt(1,1);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()){

                if(lottoGame.getGameName() == null && lottoGame.getLottoId() == 0){
                    lottoGame.setGameName(rs.getString("GAME"));
                    lottoGame.setLottoId( rs.getInt("ID"));
                    lottoGame.setMinNumber( rs.getInt("MIN"));
                    lottoGame.setMaxNumber( rs.getInt("MAX"));
                    lottoGame.startThreadForJackpotRetrieval();
                }

                Drawing drawing = buildDrawing(rs, lottoGame.getPositionNumbersAllowed());
                lottoGame.getDrawingData().add( drawing );
            }

        }

        return lottoGame;
    }

    private Drawing buildDrawing(ResultSet rs, int positionNumbersAllowed) {

        Drawing drawing = null;

        try {
            switch (positionNumbersAllowed) {
                case 3:
                    drawing = new Drawing(rs.getInt("DRAW_NUMBER"), rs.getString("DRAW_DATE"),
                            rs.getString("DRAW_POS_ONE"), rs.getString("DRAW_POS_TWO"),
                            rs.getString("DRAW_POS_THREE"));
                    break;
                case 4:
                    drawing = new Drawing(rs.getInt("DRAW_NUMBER"), rs.getString("DRAW_DATE"),
                            rs.getString("DRAW_POS_ONE"), rs.getString("DRAW_POS_TWO"),
                            rs.getString("DRAW_POS_THREE"), rs.getString("DRAW_POS_FOUR"));
                    break;
                case 5:
                    drawing = new Drawing(rs.getInt("DRAW_NUMBER"), rs.getString("DRAW_DATE"),
                            rs.getString("DRAW_POS_ONE"), rs.getString("DRAW_POS_TWO"),
                            rs.getString("DRAW_POS_THREE"), rs.getString("DRAW_POS_FOUR"),
                            rs.getString("DRAW_POS_FIVE"));
                    break;
                case 6:
                    drawing = new Drawing(rs.getInt("DRAW_NUMBER"), rs.getString("DRAW_DATE"),
                            rs.getString("DRAW_POS_ONE"), rs.getString("DRAW_POS_TWO"),
                            rs.getString("DRAW_POS_THREE"), rs.getString("DRAW_POS_FOUR"),
                            rs.getString("DRAW_POS_FIVE"), rs.getString("BONUS_NUMBER"));
                    break;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return drawing;
    }


    private static final String LOTTO_GAME_DATA_QUERY = "SELECT " +
                                                        " LG.GAME_NAME AS GAME,LG.GAME_MIN_NUMBER AS MAX, LG.GAME_MAX_NUMBER AS MIN, "
                                                        +" LG.ID as ID, d.* " +
                                                        " FROM" +
                                                        "    lottery_game LG" +
                                                        "        INNER JOIN" +
                                                        "    drawing d ON d.LOTTERY_GAME_ID = LG.ID" +
                                                        " WHERE" +
                                                        "    d.LOTTERY_GAME_ID = ?" +
                                                        " ORDER BY d.DRAW_NUMBER";
}