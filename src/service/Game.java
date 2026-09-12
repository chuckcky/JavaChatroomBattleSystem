package service;

import model.*;

import java.util.List;



public class Game {

    private Player player1;
    private Player player2;
    private boolean gameOver;
    private Player winner;

    //新增网络模式
    public Game(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.gameOver = false;
        this.winner = null;
    }

    //判断胜负
    private void checkGameOver() {
        if (!player1.isAlive()) {
            gameOver = true;
            winner = player2;
            System.out.println("被击败！");
        } else if (!player2.isAlive()) {
            gameOver = true;
            winner = player1;
            System.out.println(player1.getName() + "被击败！");
            return;
        }
        if (player1.isDeckEmpty() && player2.isDeckEmpty()) {
            gameOver = true;
            if (player1.getHp() >= player2.getHp()) {
                winner = player1;
            } else {
                winner = player2;
            }
        }
    }

    //网络模式
    //获取玩家1
    public Player getPlayer1() {
        return player1;
    }

    //获取玩家2
    public Player getPlayer2() {
        return player2;
    }

    //获取对手
    public Player getOpponent(Player p) {
        return (p == player1) ? player2 : player1;
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
    public boolean playCard(Player currentPlayer, int cardIndex, Player target, Minion targetMinion) {
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
        if (card instanceof SingleAttackCard) {
            if (targetMinion == null) {
                return false;
            }
            //校验目标是否还在对手场上
            if (!target.getField().contains(targetMinion)) {
                return false;
            }
            ((SingleAttackCard) card).setTargetMinion(targetMinion);
        }

        //执行出牌
        boolean success = currentPlayer.playCard(cardIndex, target);
        if (!success) return false;

        //清理死亡随从
        currentPlayer.removeDeadMinions();
        getOpponent(currentPlayer).removeDeadMinions();
        //检查游戏是否结束
        checkGameOver();
        return true;
    }

    //执行随从攻击
    public boolean attack(Player attacker, int minionIndex, String targetType, int targetIndex) {
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
            List<Minion> fieldMinions = defender.getField();
            if (targetIndex < 0 || targetIndex >= fieldMinions.size()) {
                return false;
            }
            attackerMinion.attackMinion(fieldMinions.get(targetIndex));
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
    public void endTurn() {
        player1.removeDeadMinions();
        player2.removeDeadMinions();
        checkGameOver();
    }

    //回合开始
    public void startTurnForPlayer(Player player) {
        player.roundStart();
    }

    //检查是否有可攻击的随从
    public boolean haveAttackableMinion(Player player) {
        for (Minion m : player.getField()) {
            if (m.canAttack()) {
                return true;
            }
        }
        return false;
    }



}