package TCPdemo;


import model.*;
import service.Game;

import java.util.List;
import java.util.Random;

//管理单局的游戏状态
//单局状态机
public class GameRoom {
    private String roomId;
    private ClientHandler player1Handler;
    private ClientHandler player2Handler;
    //卡牌游戏引擎
    private Game game;
    //游戏是否已开始
    private boolean isGameStarted;
    //游戏玩家1
    private Player player1;
    //游戏玩家2
    private Player player2;
    //0=玩家1回合，1=玩家2回合
    private int currentTurn;
    //是否正在等待玩家行动
    private boolean waitingForAction;
    private Random rand;

    public GameRoom(String roomId, ClientHandler host) {
        this.roomId = roomId;
        this.player1Handler = host;
        this.player2Handler = null;
        this.isGameStarted = false;
        this.waitingForAction = false;
        this.rand = new Random();
    }

    public int getPlayerCount() {

        return (player1Handler == null ? 0 : 1) + (player2Handler == null ? 0 : 1);
    }

    public void addPlayer(ClientHandler player) {
        if (player2Handler == null) {
            this.player2Handler = player;
        }
    }

    //开始游戏
    public synchronized void startGame() {
        if (isGameStarted || player2Handler == null) {
            return;
        }

        //创建两个游戏玩家
        player1 = new Player(player1Handler.getUsername());
        player2 = new Player(player2Handler.getUsername());

        //发牌
        player1.setDeck(CardFactory.createDeck());
        player2.setDeck(CardFactory.createDeck());
        for (int i = 0; i < 4; i++) {
            player1.draw();
            player2.draw();
        }

        //创建Game引擎
        game = new Game(player1, player2);

        //决定先手
        boolean player1First = rand.nextBoolean();
        currentTurn = player1First ? 0 : 1;
        isGameStarted = true;
        waitingForAction = true;

        //让先手玩家执行回合开始
        Player firstPlayer = player1First ? player1 : player2;
        game.startTurnForPlayer(firstPlayer);

        System.out.println("房间 " + roomId + " 游戏开始！先手：" +
                (player1First ? player1Handler.getUsername() : player2Handler.getUsername()));

        //广播游戏开始+初始状态
        broadcastGameState();

        //通知先手玩家行动
        notifyCurrentPlayerTurn();
    }

    //处理玩家出牌
    public synchronized void handlePlayCard(ClientHandler handler, int cardIndex, String target) {
        //只有当前回合玩家才能操作
        ClientHandler currentHandler = (currentTurn == 0) ? player1Handler : player2Handler;
        if (handler != currentHandler) {
            sendMessage(handler, "还没轮到你的回合");
            return;
        }
        //检查游戏状态
        if (!isGameStarted || !waitingForAction) {
            sendMessage(handler, "游戏尚未开始或不是你的行动阶段");
            return;
        }

        //找到当前玩家
        Player currentPlayer = getPlayerByHandler(handler);
        if (currentPlayer == null) {
            sendMessage(handler, "你不在这个房间里");
            return;
        }
        //校验卡牌是否合法
        if (cardIndex < 0 || cardIndex >= currentPlayer.getHand().size()) {
            sendMessage(handler, "手牌序号无效");
            return;
        }
        Player opponent = game.getOpponent(currentPlayer);
        Minion targetMinion = null;
        //如果是单体法术，解析目标
        if (target != null && !target.isEmpty()) {
            Card card = currentPlayer.getHand().get(cardIndex);
            if (card instanceof SingleAttackCard) {
                try {
                    int targetIndex = Integer.parseInt(target);
                    List<Minion> fieldMinions = opponent.getField();
                    if (targetIndex >= 0 && targetIndex < fieldMinions.size()) {
                        targetMinion = fieldMinions.get(targetIndex);
                    } else {
                        sendMessage(handler, "目标随从不存在");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendMessage(handler, "目标格式错误");
                    return;
                }
            }
        }

        //执行出牌
        boolean success = game.playCard(currentPlayer, cardIndex, opponent, targetMinion);
        if (!success) {
            sendMessage(handler, "出牌失败，请检查费用或手牌序号或或单体法术是否指定了目标");
            return;
        }

        //检查游戏是否结束
        if (game.isGameOver()) {
            broadcastGameOver();
            return;
        }

        //广播更新后的状态
        broadcastGameState();

        //还有费用或者手牌时由玩家自行决定继续出牌或输入end结束
        if (game.haveAttackableMinion(currentPlayer)) {
            sendMessage(handler, "【战斗阶段】可攻击（attack 随从序号 h 或 attack 随从序号 目标序号），或继续出牌（play 序号）/ 结束回合（end）");
        } else {
            sendMessage(handler, "可以继续出牌（play 序号）或结束回合（end）");
        }
        waitingForAction = true;
    }

    //处理随从攻击
    public synchronized void handleAttack(ClientHandler handler, int minionIndex, String targetType, int targetIndex) {
        //只有当前回合玩家才能操作
        ClientHandler currentHandler = (currentTurn == 0) ? player1Handler : player2Handler;
        if (handler != currentHandler) {
            sendMessage(handler, "还没轮到你的回合");
            return;
        }
        if (!isGameStarted || !waitingForAction) {
            sendMessage(handler, "游戏尚未开始或不是你的行动阶段");
            return;
        }

        Player currentPlayer = getPlayerByHandler(handler);
        if (currentPlayer == null) {
            sendMessage(handler, "你不在这个房间里");
            return;
        }

        //检查是否还有可攻击的随从
        if (!game.haveAttackableMinion(currentPlayer)) {
            sendMessage(handler, "没有可攻击的随从，请出牌（play 序号）或结束回合（end）");
            return;
        }

        boolean success = game.attack(currentPlayer, minionIndex, targetType, targetIndex);
        if (!success) {
            sendMessage(handler, "攻击失败，请检查随从序号或目标");
            return;
        }

        //检查游戏是否结束
        if (game.isGameOver()) {
            broadcastGameOver();
            return;
        }

        //广播更新后的状态
        broadcastGameState();

        //检查是否还有可攻击的随从（回合只由 end 指令结束）
        if (game.haveAttackableMinion(currentPlayer)) {
            sendMessage(handler, "继续攻击（attack 随从序号 h 或 attack 随从序号 目标序号），或结束回合（end）");
        } else {
            sendMessage(handler, "所有随从已攻击完毕，请出牌（play 序号）或结束回合（end）");
        }
        waitingForAction = true;
    }

    //处理结束回合
    public synchronized void handleEndTurn(ClientHandler handler) {
        //只有当前回合玩家才能操作
        ClientHandler currentHandler = (currentTurn == 0) ? player1Handler : player2Handler;
        if (handler != currentHandler) {
            sendMessage(handler, "还没轮到你的回合");
            return;
        }
        if (!isGameStarted || !waitingForAction) {
            sendMessage(handler, "游戏尚未开始或不是你的行动阶段");
            return;
        }

        Player currentPlayer = getPlayerByHandler(handler);
        if (currentPlayer == null) {
            sendMessage(handler, "你不在这个房间里");
            return;
        }

        //结束当前回合
        game.endTurn();

        //检查游戏是否结束
        if (game.isGameOver()) {
            broadcastGameOver();
            return;
        }

        //切换到下一位玩家
        nextTurn();
    }

    //切换到下一个回合
    private void nextTurn() {
        //切换当前玩家
        currentTurn = (currentTurn == 0) ? 1 : 0;
        Player currentPlayer = (currentTurn == 0) ? player1 : player2;

        //回合开始（恢复费用、抽牌、随从恢复攻击）
        game.startTurnForPlayer(currentPlayer);

        waitingForAction = true;

        //广播新状态
        broadcastGameState();

        //通知当前玩家行动
        notifyCurrentPlayerTurn();
    }

    //广播当前游戏状态
    private void broadcastGameState() {
        if (game == null)
            return;

        String msg1 = GameStateFormatter.format(game, player1, player2, currentTurn,player1);
        String msg2 = GameStateFormatter.format(game, player1, player2, currentTurn,player2);
        player1Handler.sendMessage("GAME_STATE|" + msg1);
        player2Handler.sendMessage("GAME_STATE|" + msg2);
    }

    //广播游戏结束
    private void broadcastGameOver() {
        Player winner = game.getWinner();
        String msg = "GAME_OVER|" + winner.getName() + " 获胜！";
        player1Handler.sendMessage(msg);
        player2Handler.sendMessage(msg);
        waitingForAction = false;
    }

    //通知当前玩家行动
    private void notifyCurrentPlayerTurn() {
        ClientHandler currentHandler = (currentTurn == 0) ? player1Handler : player2Handler;
        ClientHandler opponentHandler = (currentTurn == 0) ? player2Handler : player1Handler;
        currentHandler.sendMessage("你的回合请出牌或结束回合（指令：play 序号 / end）");
        opponentHandler.sendMessage("等待对手行动...");
    }

    //根据ClientHandler获取对应的Player
    private Player getPlayerByHandler(ClientHandler handler) {
        if (handler == player1Handler) return player1;
        if (handler == player2Handler) return player2;
        return null;
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean containsHandler(ClientHandler handler) {
        return handler == player1Handler || handler == player2Handler;
    }

    //玩家断线，通知另一方，终止对局
    public void handleDisconnect(ClientHandler disconnected) {
        ClientHandler other = (disconnected == player1Handler) ? player2Handler : player1Handler;
        if (other != null) {
            other.sendMessage("对方已断线，本局结束");
            other.clearRoom();
        }
        disconnected.clearRoom();
        waitingForAction = false;
        isGameStarted = false;
    }

    //发送消息给单个玩家
    private void sendMessage(ClientHandler handler, String msg) {
        handler.sendMessage(msg);
    }
}
