package service;

import model.*;

import java.util.List;



public class Game {

    private Player player;
    private Player player2;
    private boolean gameOver;
    private Player winner;

    //新增网络模式
    public Game(Player player1, Player player2) {
        this.player = player1;
        this.player2 = player2;
        this.gameOver = false;
        this.winner = null;
    }

    //判断胜负
    private void checkGameOver() {
        if (!player.isAlive()) {
            gameOver = true;
            winner = player2;
            System.out.println("你已死亡！");
        } else if (!player2.isAlive()) {
            gameOver = true;
            winner = player;
            System.out.println(player.getName() + "被击败！");
        }
    }

    //网络模式
    //获取玩家1
    public Player getPlayer1() {
        return player;
    }

    //获取玩家2
    public Player getPlayer2() {
        return player2;
    }

    //获取对手
    public Player getOpponent(Player p) {
        return (p == player) ? player2 : player;
    }

    //检查游戏是否结束
    public boolean isGameOver() {
        return gameOver;
    }

    //获取获胜者
    public Player getWinner() {
        return winner;
    }

    //执行出牌
    public boolean playCardFromNetwork(Player currentPlayer, int cardIndex, Player target, Minion targetMinion) {
        if (gameOver) {
            return false;
        }

        List<Card> hand = currentPlayer.getHand();
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            return false;
        }

        Card card = hand.get(cardIndex);

        //检查费用
        if (currentPlayer.getPlayPoint() < card.getCost()) {
            return false;
        }

        //如果是单体随从法术，设置目标
        if (card instanceof SingleAttackCard && targetMinion != null) {
            ((SingleAttackCard) card).setTargetMinion(targetMinion);
        }

        //执行出牌
        currentPlayer.playCard(cardIndex, target);

        //清理死亡随从
        currentPlayer.removeDeadMinions();
        getOpponent(currentPlayer).removeDeadMinions();

        //检查游戏是否结束
        checkGameOver();

        return true;
    }

    //执行随从攻击
    public boolean attackFromNetwork(Player attacker, int minionIndex, String targetType, int targetIndex) {
        if (gameOver) {
            return false;
        }

        List<Minion> field = attacker.getField();
        if (minionIndex < 0 || minionIndex >= field.size()) {
            return false;
        }

        Minion attackerMinion = field.get(minionIndex);
        if (!attackerMinion.canAttack()) {
            return false;
        }

        Player defender = getOpponent(attacker);

        if ("player".equals(targetType)) {
            //攻击主战者
            attackerMinion.attackPlayer(defender);
        } else if ("minion".equals(targetType)) {
            List<Minion> enemyField = defender.getField();
            if (targetIndex < 0 || targetIndex >= enemyField.size()) {
                return false;
            }
            attackerMinion.attackMinion(enemyField.get(targetIndex));
        } else {
            return false;
        }

        attackerMinion.setCanAttack(false);

        //清理死亡随从
        attacker.removeDeadMinions();
        defender.removeDeadMinions();

        //检查游戏是否结束
        checkGameOver();

        return true;
    }

    //回合结束
    public void endTurnFromNetwork() {
        player.removeDeadMinions();
        player2.removeDeadMinions();
        checkGameOver();
    }

    //回合开始
    public void startTurnForPlayer(Player p) {
        p.roundStart();
    }

    //检查是否有可攻击的随从
    public boolean hasAttackableMinion(Player p) {
        for (Minion m : p.getField()) {
            if (m.canAttack()) {
                return true;
            }
        }
        return false;
    }



}