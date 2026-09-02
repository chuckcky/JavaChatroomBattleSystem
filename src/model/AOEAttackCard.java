package model;

import java.util.List;

public class AOEAttackCard extends Card{
    private final int damage;

    public AOEAttackCard(String name, int cost, int damage) {
        super(name, cost);
        this.damage = damage;
    }



    @Override
    public void execute(Player caster, Player target) {
        List<Minion> enemyField = target.getField();
        if (enemyField.isEmpty()) {
            System.out.println(caster.getName() + " 使用了 " + getName());
            return;
        }
        System.out.println(caster.getName() + " 使用了 " + getName() + "，对敌方所有随从造成 " + damage + " 点伤害！");
        for (Minion m : enemyField) {
            m.takeDamage(damage);
        }
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public String toString() {
        return getName() + "(费" + getCost() + ", AOE" + damage + "伤, 仅随从)";
    }
}
