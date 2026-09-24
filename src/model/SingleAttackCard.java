package model;

public class SingleAttackCard extends Card{
    private final int damage;

    public SingleAttackCard(String name, int cost, int damage) {
        super(name, cost);
        this.damage = damage;
    }

    @Override
    public String getTypeCode() {
        return "S";
    }

    @Override
    public void execute(Player caster, Player target) {
        if (targetMinion == null) {
            System.out.println(getName() + " 没有指定目标随从");
            return;
        }
        // 检查目标是否还在场上
        if (!target.getField().contains(targetMinion)) {
            System.out.println("目标随从已不在场上");
            targetMinion = null;
            return;
        }
        System.out.println(caster.getName() + " 释放 " + getName() + "，对 " + targetMinion.getName() + " 造成 " + damage + " 点伤害！");
        targetMinion.takeDamage(damage);
        targetMinion = null; //清除，防止误用
    }

    private Minion targetMinion;

    //设置目标随从
    public void setTargetMinion(Minion target) {
        this.targetMinion = target;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public String toString() {
        return getName() + "(费" + getCost() + ", 单体" + damage + "伤, 仅随从)";
    }
}
