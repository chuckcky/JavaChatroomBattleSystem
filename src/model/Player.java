package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Player {
    private String name;

    private int hp;
    private int maxHp;

    private int playPoint;
    private int maxPlayPoint;

    //手牌
    private List<Card> hand;
    //牌堆
    private Queue<Card> deck;
    //弃牌堆
    private List<Card> discard;
    //场地
    private List<Minion> field;

    //玩家状态，true存活
    private boolean alive;

    public Player(String name) {
        this.name = name;
        this.hp = 20;
        this.maxHp = 20;
        this.playPoint = 0;
        this.maxPlayPoint = 0;
        this.hand = new ArrayList<>();
        this.deck = new LinkedList<>();
        this.discard = new ArrayList<>();
        this.field = new ArrayList<>();
        this.alive = true;
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getPlayPoint() {
        return playPoint;
    }

    public int getMaxPlayPoint() {
        return maxPlayPoint;
    }

    public List<Card> getHand() {
        return hand;
    }

    public Queue<Card> getDeck() {
        return deck;
    }

    public List<Card> getDiscard() {
        return discard;
    }

    public boolean isAlive() {
        return alive;
    }

    public List<Minion> getField() {
        return field;
    }

    //判断手牌和牌堆是否为空
    public boolean isHandEmpty() {
        return hand.isEmpty();
    }

    public boolean isDeckEmpty() {
        return deck.isEmpty();
    }

    //回合开始
    //费用上限+1，回复所有费用
    public void roundStart() {
        maxPlayPoint = Math.min(maxPlayPoint + 1, 10);
        playPoint = maxPlayPoint;
        draw();
        for (Minion minion : field) {
            minion.startAction();
        }
    }

    //抽牌方法
    public void draw() {
        if (deck.isEmpty()) {
            System.out.println(name + "牌库已空，无法抽牌");
            return;
        }
        Card card = deck.poll();
        hand.add(card);
        System.out.println(name + "抽到: " + card.getName());
    }

    //出牌设计
    public boolean playCard(int index, Player target) {
        //校验序号是否合法
        if (index < 0 || index >= hand.size()) {
            System.out.println("手牌序号无效");
            return false;
        }
        //合法时拿到手牌中
        Card card = hand.get(index);
        //检查费用是否够用
        if (playPoint < card.getCost()) {
            System.out.println("费用不足！需要" + card.getCost() + "点，当前" + playPoint + "点");
            return false;
        }
        //随从预检查，检查场地是否已满
        if (card instanceof Minion && field.size() >= 5) {
            System.out.println("场地已满，无法打出随从");
            return false;
        }
        //从手牌移除掉使用的卡牌并扣除相应的费用
        hand.remove(index);
        playPoint = playPoint - card.getCost();
        System.out.println(name+"打出" + card.getName() + "（剩余费用 " + playPoint + "）");
        //执行卡牌效果
        card.execute(this, target);

        //打出的卡牌进入弃牌堆
        if (!(card instanceof Minion)) {
            discard.add(card);
        }
        return true;
    }

    //判断玩家当前费用是否足够打出指定的卡牌
    public boolean canPlayCard(Card card) {
        return playPoint >= card.getCost();
    }

    //伤害系统的设计，伤害的计算
    public void takeDamage(int damage) {
        if (!alive) return;

        hp = hp - damage;
        System.out.println(name + "受到" + damage + "点伤害，剩余 HP: " + hp);

        if (hp <= 0) {
            hp = 0;
            alive = false;
            System.out.println(name + "死亡！");
        }
    }

    //恢复系统的设计
    public void heal(int amount) {
        if (!alive) return;
        int before = hp;
        hp = Math.min(hp + amount, maxHp);
        System.out.println(name + "恢复了" + (hp - before) + "点生命");
    }

    //添加随从到场地上
    public void summonMinion(Minion minion) {
        if (field.size() >= 5) {
            System.out.println(name + "场地已满");
            return;
        }
        field.add(minion);
    }

    //清理死亡的随从
    public void removeDeadMinions() {
        List<Minion> removedMinions = new ArrayList<>();
        for (Minion minion : field) {
            if (minion.isDead()) {
                removedMinions.add(minion);
            }
        }
        for (Minion minion : removedMinions) {
            field.remove(minion);
            discard.add(minion);
            System.out.println(minion.getName() + " 从场面移除");
        }
    }

    //重置所有状态
    public void resetForNewGame() {
        hp = 20;
        maxHp = 20;
        maxPlayPoint = 0;
        playPoint = 0;
        hand.clear();
        deck.clear();
        discard.clear();
        field.clear();
        alive = true;
    }

    //设置牌堆，调用牌堆给玩家发牌
    public void setDeck(Queue<Card> deck) {
        this.deck = deck;
    }
}
