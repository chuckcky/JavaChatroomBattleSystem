package model;

public class AttackCard extends Card {

    private final int damage;

    public AttackCard(String name, int cost, int damage) {
        super(name, cost);
        this.damage = damage;
    }

    @Override
    public String getTypeCode() {
        return "D";
    }

    @Override
    public void execute(Player caster, Player target) {
        System.out.println(caster.getName() + "打出" + getName() + "，对" + target.getName() + "造成" + damage + "点伤害！");
        target.takeDamage(damage);
    }
}
