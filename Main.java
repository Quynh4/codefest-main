package com.example;

import com.example.logic_impl.ChestAttackLogic;
import com.example.logic_impl.GameLogicHandler;
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import java.io.IOException;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "101417";
    private static final String PLAYER_NAME = "Q";
    private static final String SECRET_KEY = "sk-ylFJB0fyQ_63cnYEC4Bqbw:r0brCBio4T5NvxE9Vu58_eh7NoES1vMyDH8Kb4w-4IIoQFVLP3L2kI-EMIsH4sPJ3Szji9g-X_aPpDMd5cotAQ";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            hero.getGameMap().updateOnUpdateMap(args[0]);

            GameLogicHandler logic = new ChestAttackLogic(hero);
            logic.handleTurn();

        } catch (Exception e) {
            System.err.println("Error in MapUpdateListener: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
