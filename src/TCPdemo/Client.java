package TCPdemo;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static String currentUsername = "";
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 10000);

        OutputStream os = socket.getOutputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //接收线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String str;
                    while ((str = br.readLine()) != null) {
                        //判断是否是游戏状态消息
                        if (str.startsWith("GAME_STATE|")) {
                            //去掉GAME_STATE|前缀，只保留数据部分
                            String data = str.substring("GAME_STATE|".length());
                            printGameState(data);
                        } else {
                            //其他消息直接打印
                            System.out.println("[服务器] " + str);
                            //解析用户名
                            if (str.startsWith("用户名设置成功：")) {
                                currentUsername = str.substring("用户名设置成功：".length());
                            }
                        }

                    }
                } catch (IOException e) {
                    System.out.println("服务器连接已断开");
                }
            }
        }).start();

        //主线程负责发消息
        Scanner sc = new Scanner(System.in);
        System.out.println("===== 欢迎来到卡牌对战！=====");
        System.out.println("提示：先输入 user 你的名字 设置用户名");
        while (true) {
            String str = sc.nextLine();
            if ("886".equals(str)) {
                break;
            }
            os.write((str + "\n").getBytes());
            os.flush();
        }

        socket.close();
    }

    //解析GameState并格式化打印
    private static void printGameState(String data) {
        //数据格式示例：
        //P1|001|HP|20|MAXHP|20|PP|0|MAXPP|0|HAND|横扫之刃,剑斗士,魔弹,咆哮|FIELD||P2|002|HP|20|MAXHP|20|PP|0|MAXPP|0|HAND|咆哮,横扫之刃,横扫之刃,风暴|FIELD||TURN|001

        String[] parts = data.split("\\|");

        //解析玩家1信息
        //parts[0]=P1, parts[1]=玩家1名字, parts[2]=HP, parts[3]=血量, parts[4]=MAXHP, parts[5]=最大血量, parts[6]=PP, parts[7]=费用, parts[8]=MAXPP, parts[9]=最大费用, parts[10]=HAND, parts[11]=手牌, parts[12]=FIELD, parts[13]=场面
        String p1Name = parts[1];
        String p1Hp = parts[3];
        String p1MaxHp = parts[5];
        String p1PP = parts[7];
        String p1MaxPP = parts[9];
        String p1Hand = parts[11];
        String p1Field = parts[13];

        //玩家2信息
        String p2Name = parts[15];
        String p2Hp = parts[17];
        String p2MaxHp = parts[19];
        String p2PP = parts[21];
        String p2MaxPP = parts[23];
        String p2Hand = parts[25];
        String p2Field = parts[27];

        //当前回合玩家
        String turnPlayer = parts[29];

        //获取当前玩家自己的名字
        String myName = getMyName();

        //判断哪个是我，哪个是对手
        String myNameDisplay;
        String myHp, myMaxHp, myPP, myMaxPP, myHand, myField;
        String oppName, oppHp, oppMaxHp, oppPP, oppMaxPP, oppHand, oppField;

        if (myName.equals(p1Name)) {
            //我是玩家1
            myNameDisplay = p1Name; myHp = p1Hp; myMaxHp = p1MaxHp; myPP = p1PP; myMaxPP = p1MaxPP; myHand = p1Hand; myField = p1Field;
            oppName = p2Name; oppHp = p2Hp; oppMaxHp = p2MaxHp; oppPP = p2PP; oppMaxPP = p2MaxPP; oppHand = p2Hand; oppField = p2Field;
        } else {
            //我是玩家2
            myNameDisplay = p2Name; myHp = p2Hp; myMaxHp = p2MaxHp; myPP = p2PP; myMaxPP = p2MaxPP; myHand = p2Hand; myField = p2Field;
            oppName = p1Name; oppHp = p1Hp; oppMaxHp = p1MaxHp; oppPP = p1PP; oppMaxPP = p1MaxPP; oppHand = p1Hand; oppField = p1Field;
        }

        //判断当前回合是谁
        boolean isMyTurn = turnPlayer.equals(myNameDisplay);

        //打印对手信息（对手固定在我上方）
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("  %-8s HP: %2d/%-2d  │  费用: %d/%-2d  │  手牌: %-2s 张  %n",
                oppName, Integer.parseInt(oppHp), Integer.parseInt(oppMaxHp),
                Integer.parseInt(oppPP), Integer.parseInt(oppMaxPP),
                oppHand);

        System.out.printf("  %-8s 场面: %-40s %n", oppName, formatField(oppField));
        System.out.println("───────────────────────────────────────────────────────────────");

        //打印自己的场面与信息（在下方）
        System.out.printf("  %-8s 场面: %-40s %n", myNameDisplay, formatField(myField));
        System.out.printf("  %-8s HP: %2d/%-2d  │  费用: %d/%-2d  │  手牌: %-2d 张  %n",
                myNameDisplay, Integer.parseInt(myHp), Integer.parseInt(myMaxHp),
                Integer.parseInt(myPP), Integer.parseInt(myMaxPP),
                countCards(myHand));

        //打印手牌详情（自己的手牌）
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("  你的手牌: %-60s %n", formatHand(myHand));

        //打印当前回合提示（以自己的名字判断）
        System.out.println("───────────────────────────────────────────────────────────────");
        String turnIndicator = isMyTurn ? "你的回合" : "等待对手...";
        System.out.printf("  %-67s %n", turnIndicator);
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.println();
    }

    //获取我的用户名
    private static String getMyName() {
        return currentUsername;
    }

    //统计手牌数量
    private static int countCards(String handStr) {
        if (handStr == null || handStr.isEmpty()) {
            return 0;
        }
        return handStr.split(",").length;
    }

    //格式化场面（编号列出所有随从，名称+攻/血）
    private static String formatField(String fieldStr) {
        if (fieldStr == null || fieldStr.isEmpty()) {
            return "(空)";
        }
        String[] minions = fieldStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minions.length; i++) {
            sb.append("[").append(i + 1).append("]").append(minions[i]);
            if (i < minions.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString();
    }

    //格式化手牌
    private static String formatHand(String handStr) {
        if (handStr == null || handStr.isEmpty()) {
            return "(空)";
        }
        String[] cards = handStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.length; i++) {
            sb.append("[").append(i+1).append("]").append(cards[i]);
            if (i < cards.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString();
    }
}