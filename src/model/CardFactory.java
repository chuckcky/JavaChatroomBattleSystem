package model;

import java.util.*;

public class CardFactory {
    public static Queue<Card> createDeck() {
        List<Card> cards = new ArrayList<>();

        //直伤法术
        //1费打2点×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new AttackCard("魔弹", 1, 2));
        }

        //恢复法术
        //1费回2点×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new HealCard("治愈", 1, 3));
        }

        //1费打2点×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new SingleAttackCard("风暴", 1, 2));
        }

        //3费打2点全体×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new AOEAttackCard("咆哮", 3, 2));
        }
        //6费打5点全体×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new AOEAttackCard("横扫之刃",6,5));
        }

        //随从卡
        //2费2/2×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new Minion("剑斗士", 2, 2, 2));
        }
        //3费3/3×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new Minion("士兵", 3, 3, 3));
        }
        //5费5/5×3张
        for (int i = 0; i < 3; i++) {
            cards.add(new Minion("骑士",5,5,5));
        }
        //洗牌
        Collections.shuffle(cards);
        //放入队列
        return new LinkedList<>(cards);
    }
}
