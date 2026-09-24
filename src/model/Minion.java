package model;

public class Minion extends Card{
    private int attack;
    private int hp;
    private int maxHp;
    private boolean canAttack;

    public Minion(String name, int cost, int attack, int hp) {
        super(name, cost);
        this.attack = attack;
        this.hp = hp;
        this.maxHp = hp;
        this.canAttack = false;
    }

    @Override
    public String getTypeCode() {
        return "M";
    }

    @Override
    public void execute(Player caster, Player target) {
        //随从进入场地
        caster.summonMinion(this);
        System.out.println(caster.getName() + " 召唤了 " + getName() + "（" + attack + "/" + hp + "）");
    }

    //攻击随从
    public void attackMinion(Minion target) {
        System.out.println(getName() + " 攻击 " + target.getName());
        target.takeDamage(this.attack);
        this.takeDamage(target.getAttack());
    }

    //攻击主站者
    public void attackPlayer(Player target) {
        System.out.println(getName() + " 攻击 " + target.getName());
        target.takeDamage(this.attack);
    }

    //随从收到伤害
    public void takeDamage(int damage) {
        hp = hp- damage;
        if (hp <= 0) {
            hp = 0;
            System.out.println(getName() + " 被消灭");
        } else {
            System.out.println(getName() + " 剩余 HP: " + hp + "/" + maxHp);
        }
    }

    //判断随从死亡
    public boolean isDead() {
        return hp <= 0;
    }

    //新回合开始
    //随从恢复攻击能力
    public void startAction() {
        canAttack = true;
    }

    public int getAttack() {
        return attack;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public boolean canAttack() {
        return canAttack;
    }

    public void setCanAttack(boolean canAttack) {
        this.canAttack = canAttack;
    }




    public String toString() {
        return getName() + "(" + attack + "/" + hp + ", 费" + getCost() + ")";
    }
}
