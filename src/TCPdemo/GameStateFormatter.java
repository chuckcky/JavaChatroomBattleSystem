package TCPdemo;

import model.Player;
import model.Card;
import model.Minion;
import service.Game;
import java.util.List;

public class GameStateFormatter {

    //把游戏状态转成一行文本
    public static String format(Game game, Player p1, Player p2, int currentTurn,Player player) {
        StringBuilder sb = new StringBuilder();

        //玩家1信息
        sb.append("P1|").append(p1.getName())
                .append("|HP|").append(p1.getHp())
                .append("|MAXHP|").append(p1.getMaxHp())
                .append("|PP|").append(p1.getPlayPoint())
                .append("|MAXPP|").append(p1.getMaxPlayPoint());

        //玩家1手牌
        sb.append("|HAND|");
        if (player==p1){
            List<Card> hand1 = p1.getHand();
            for (int i = 0; i < hand1.size(); i++) {
                sb.append(hand1.get(i).getName()).append("(费").append(hand1.get(i).getCost()).append(")");
                if (i < hand1.size() - 1) sb.append(",");
            }
        }else {
            sb.append(p1.getHand().size());
        }


        //玩家1场面（格式：名称(攻击/血量)）
        sb.append("|FIELD|");
        List<Minion> field1 = p1.getField();
        for (int i = 0; i < field1.size(); i++) {
            Minion m = field1.get(i);
            sb.append(m.getName()).append("(").append(m.getAttack()).append("/").append(m.getHp()).append(")");
            if (i < field1.size() - 1) sb.append(",");
        }

        //分隔符
        sb.append("|");

        //玩家2信息
        sb.append("P2|").append(p2.getName())
                .append("|HP|").append(p2.getHp())
                .append("|MAXHP|").append(p2.getMaxHp())
                .append("|PP|").append(p2.getPlayPoint())
                .append("|MAXPP|").append(p2.getMaxPlayPoint());

        //玩家2手牌
        sb.append("|HAND|");
        if (player==p2){
            List<Card> hand2 = p2.getHand();
            for (int i = 0; i < hand2.size(); i++) {
                sb.append(hand2.get(i).getName()).append("(费").append(hand2.get(i).getCost()).append(")");
                if (i < hand2.size() - 1) sb.append(",");
            }
        }else {
            sb.append(p2.getHand().size());
        }


        //玩家2场面
        sb.append("|FIELD|");
        List<Minion> field2 = p2.getField();
        for (int i = 0; i < field2.size(); i++) {
            Minion m = field2.get(i);
            sb.append(m.getName()).append("(").append(m.getAttack()).append("/").append(m.getHp()).append(")");
            if (i < field2.size() - 1) sb.append(",");
        }

        //当前回合玩家
        sb.append("|TURN|").append(currentTurn == 0 ? p1.getName() : p2.getName());

        return sb.toString();
    }
}